/*
 * Littre dictionnary for Android - JNI library
 * Copyright (C) 2009 Alexis ROBERT <alexis.robert@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, at version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// HUGE thanks for Pierre-Hugues Husson, which helped me to solve a stupid issue
// which finally was not in the C code.

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "org_alexis_libstardict_Index.h"

#define LOG_BASE10_2 0.30102999566398114

struct Word {
	int id;
	char* name;
	long offset;
	long size;
};

struct Wordlist {
	struct Word* words;
	int number;
};

long getnum(unsigned char* buf) {
	long data = buf[0];
	data = (data << 8) + buf[1];
	data = (data << 8) + buf[2];
	data = (data << 8) + buf[3];
	return data;
}

int intlength() {
	return ceil((powf(sizeof(int),2)-1)*LOG_BASE10_2); // ceil((2^n-1)*log_10(2))
}

int callback_getword(struct Word word, struct Word **work, int *size, char* param) {
	if (strcmp(word.name, param) == 0) {
		(*work) = malloc(sizeof(struct Word));
		(*size) = 1;

		// Using strdup heres makes word.name automatically freed when not used
		// and avoid copying it if not needed.
		(*work)[0].name = strdup(word.name);
		(*work)[0].offset = word.offset;
		(*work)[0].size = word.size;
		(*work)[0].id = word.id;

		return 1;
	}
	return 0;
}

int callback_getwordfromid(struct Word word, struct Word **work, int *size, char* param) {
	int id = atoi(param); // This is a hack. Haha.

	if (id == word.id) {
		(*work) = malloc(sizeof(struct Word));
		(*size) = 1;

		(*work)[0].name = strdup(word.name);
		(*work)[0].offset = word.offset;
		(*work)[0].size = word.size;
		(*work)[0].id = word.id;

		return 1;
	}

	return 0;
}

int callback_match(struct Word word, struct Word **work, int *size, char* param) {
	if (strcasestr(word.name, param) != 0) { /* in-text matching */
		(*size)++;
		
		if (*size == 1) // if was size = 0, was empty, so malloc.
			(*work) = malloc(sizeof(struct Word));
		else
			(*work) = realloc(*work, (*size)*(sizeof(struct Word)));

		(*work)[(*size)-1].id = word.id;
		(*work)[(*size)-1].name = strdup(word.name);
		(*work)[(*size)-1].offset = word.offset;
		(*work)[(*size)-1].size = word.size;
	}
	return 0;
}

int callback_getletter(struct Word word, struct Word **work, int *size, char* param) {
	if (!strncasecmp(word.name, param, strlen(param))) {
		(*size)++;
		
		if (*size == 1) // if was size = 0, was empty, so malloc.
			(*work) = malloc(sizeof(struct Word));
		else
			(*work) = realloc(*work, (*size)*(sizeof(struct Word)));

		(*work)[(*size)-1].id = word.id;
		(*work)[(*size)-1].name = strdup(word.name);
		(*work)[(*size)-1].offset = word.offset;
		(*work)[(*size)-1].size = word.size;
	}
	return 0;
}


struct Wordlist parse(char *filename, int (*callbackptr)(struct Word, struct Word**, int *, char*), char* param) {
	FILE *f = fopen(filename,"r");
	int c;
	int buf_size = 8;
	char *buf = malloc(buf_size);
	int idx = 0;

	int wordid = 0;

	struct Word *work;
	int work_size = 0;

	struct Word word;
	word.name = NULL;
	word.offset = 0;
	word.size = 0;

	unsigned char *num_buf = malloc(8);

	while(1) {
		if ((c = fgetc(f)) == EOF)
			break;

		// I love using exponential buffers. It enables to reduce reallocing cycles by allowing a bit too much data than we'll use at each step.
		if (idx >= buf_size) {
			__android_log_print(ANDROID_LOG_DEBUG,"libstardict-native","Growing the buffer up from %d to %d bytes ...\n", buf_size, buf_size*2);
			buf = realloc(buf, buf_size*2);
			memset(buf+buf_size, '\0', buf_size); // is useless, keeping it for the style
			buf_size = buf_size*2;
		}

		buf[idx] = c;
		idx++;

		if (c == '\0') {
			word.id = wordid;

			word.name = buf;

			fread(num_buf, 4, 1, f); // read the offset (4 bytes = 32 bits)
			word.offset = getnum(num_buf);

			memset(num_buf, '\0', 8);
			
			fread(num_buf, 4, 1, f); // read the size (32 bits)
			word.size = getnum(num_buf);
			memset(num_buf, '\0', 8);

			if ((*callbackptr)(word, &work, &work_size, param) != 0)
				break;

			idx=0;
			memset(buf, '\0', buf_size);

			wordid++;
		}

	}

	free(buf);
	free(num_buf);

	fclose(f);

	struct Wordlist final;
	final.words = work;
	final.number = work_size;

	return final;
}

JNIEXPORT jobjectArray JNICALL Java_org_alexis_libstardict_Index_getRawWords (JNIEnv *env, jobject parent, jstring querystr) {
	jobjectArray retval = NULL;

	// Getting filename
	char* filename = (char*)(*env)->GetStringUTFChars(env,
				(*env)->GetObjectField(env, parent,
					(*env)->GetFieldID(env, (*env)->FindClass(env, "org/alexis/libstardict/Index"),
					       "indexpath", "Ljava/lang/String;")), NULL);

	// Getting query
	char* query = (char*)(*env)->GetStringUTFChars(env, querystr, NULL);
	__android_log_write(ANDROID_LOG_DEBUG,"libstardict-native","JNI call for searching received !\n");
	struct Wordlist words = parse(filename, &callback_match, query);

	int i = 0;

	static jclass StringClass = NULL;
	StringClass = (*env)->FindClass(env, "java/lang/String");

	retval = (*env)->NewObjectArray(env, words.number, StringClass, NULL);

	jobject wordstring = NULL;
	for (i = 0; i < words.number; i++) {
		wordstring = (*env)->NewStringUTF(env, words.words[i].name);
		(*env)->SetObjectArrayElement(env, retval, i, wordstring);
		(*env)->DeleteLocalRef(env, wordstring);
		free(words.words[i].name);
	}
	free(query);
	free(filename);

	return retval;
}

JNIEXPORT jobject JNICALL Java_org_alexis_libstardict_Index_getWord (JNIEnv *env, jobject parent, jstring wordname) {
	jobject word = NULL;
	static jclass WordClass = NULL;
	WordClass = (*env)->FindClass(env, "org/alexis/libstardict/Word");

        // Getting filename
        char* filename = (char*)(*env)->GetStringUTFChars(env,
                                (*env)->GetObjectField(env, parent,
                                        (*env)->GetFieldID(env, (*env)->FindClass(env, "org/alexis/libstardict/Index"),
                                               "indexpath", "Ljava/lang/String;")), NULL);

	char* query = (char*)(*env)->GetStringUTFChars(env, wordname, NULL);
	__android_log_write(ANDROID_LOG_DEBUG,"libstardict-native","JNI call for getting word received!\n");
	struct Wordlist words = parse(filename, &callback_getword, query);

	if (words.number == 0)
		return NULL;

	word = (*env)->NewObject(env, WordClass, (*env)->GetMethodID(env, WordClass, "<init>", "()V"));
	(*env)->SetIntField(env, word, (*env)->GetFieldID(env, WordClass, "id", "I"), words.words[0].id);
	(*env)->SetObjectField(env, word, (*env)->GetFieldID(env, WordClass, "name", "Ljava/lang/String;"),
			(*env)->NewStringUTF(env, words.words[0].name));
	(*env)->SetLongField(env, word, (*env)->GetFieldID(env, WordClass, "offset", "J"), words.words[0].offset);
	(*env)->SetIntField(env, word, (*env)->GetFieldID(env, WordClass, "size", "I"), words.words[0].size);

	return word;
}

// The next method is a nearly copy-paste of getWord
JNIEXPORT jobject JNICALL Java_org_alexis_libstardict_Index_getWordFromId (JNIEnv *env, jobject parent, jint wordid) {
	jobject word = NULL;
	static jclass WordClass = NULL;
	WordClass = (*env)->FindClass(env, "org/alexis/libstardict/Word");

        // Getting filename
        char* filename = (char*)(*env)->GetStringUTFChars(env,
                                (*env)->GetObjectField(env, parent,
                                        (*env)->GetFieldID(env, (*env)->FindClass(env, "org/alexis/libstardict/Index"),
                                               "indexpath", "Ljava/lang/String;")), NULL);

	// These are the only differences, these four next lines !
	char *query;
	query = malloc(sizeof(char)*intlength());
	memset(query, '\0', sizeof(char)*intlength());
	sprintf(query, "%d", (int)wordid);
	__android_log_write(ANDROID_LOG_DEBUG,"libstardict-native","JNI call for getting word from ID received!\n");
	struct Wordlist words = parse(filename, &callback_getwordfromid, query);

	if (words.number == 0)
		return NULL;

	word = (*env)->NewObject(env, WordClass, (*env)->GetMethodID(env, WordClass, "<init>", "()V"));
	(*env)->SetIntField(env, word, (*env)->GetFieldID(env, WordClass, "id", "I"), words.words[0].id);
	(*env)->SetObjectField(env, word, (*env)->GetFieldID(env, WordClass, "name", "Ljava/lang/String;"),
			(*env)->NewStringUTF(env, words.words[0].name));
	(*env)->SetLongField(env, word, (*env)->GetFieldID(env, WordClass, "offset", "J"), words.words[0].offset);
	(*env)->SetIntField(env, word, (*env)->GetFieldID(env, WordClass, "size", "I"), words.words[0].size);

	return word;
}

// The next method is a nearly copy-paste of getRawWords
JNIEXPORT jobjectArray JNICALL Java_org_alexis_libstardict_Index_getRawLetter(JNIEnv *env, jobject parent, jstring querystr) {
	jobjectArray retval = NULL;

	// Getting filename
	char* filename = (char*)(*env)->GetStringUTFChars(env,
				(*env)->GetObjectField(env, parent,
					(*env)->GetFieldID(env, (*env)->FindClass(env, "org/alexis/libstardict/Index"),
					       "indexpath", "Ljava/lang/String;")), NULL);

	// Getting query
	char* query = (char*)(*env)->GetStringUTFChars(env, querystr, NULL);
	__android_log_write(ANDROID_LOG_DEBUG,"libstardict-native","JNI call for getting letter received !\n");
	struct Wordlist words = parse(filename, &callback_getletter, query); // <= THAT IS THE ONLY DIFFERENCE (oh yeah the previous one too, be it doesn't count)

	int i = 0;

	static jclass StringClass = NULL;
	StringClass = (*env)->FindClass(env, "java/lang/String");

	retval = (*env)->NewObjectArray(env, words.number, StringClass, NULL);

	jobject wordstring = NULL;
	for (i = 0; i < words.number; i++) {
		wordstring = (*env)->NewStringUTF(env, words.words[i].name);
		(*env)->SetObjectArrayElement(env, retval, i, wordstring);
		(*env)->DeleteLocalRef(env, wordstring);
		free(words.words[i].name);
	}
	free(query);
	free(filename);

	return retval;
}
