/*
 * An XMLittre to Stardict converter written in Golang
 * Copyright (C) 2010 Alexis Robert <alexis.robert@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package main

import (
		"fmt"
		"xml"
		"os"
		"strings"
		"container/list"
	   )

// I know, **** blocks are UGLY :)

/*******************
 * Data structures 
 *******************/
type Definition struct {
	Terme string
	Entete Entete
	Corps string
	Rubriques *list.List
}

type Rubrique struct {
	Nom string
	Corps string
}

type Entete struct {
	XMLName xml.Name "entete"
	Prononciation string
	Nature string
}

func (d Definition) String() string {
	if d.Entete.Nature == "" {
		return d.Terme
	}

	return fmt.Sprintf("%s (%s)", d.Terme, d.Entete.Nature);
}

func (d Definition) Content() string {
	data := "<div class='entete'>"
	if d.Entete.Nature != "" {
		data += fmt.Sprintf("<span class='prononciation'>%s</span>", d.Entete.Prononciation)
		data += fmt.Sprintf("<span class='nature'>%s</span>", d.Entete.Nature)
	}

	data += d.Corps

	for {
		n := d.Rubriques.Len()

		if n == 0 {
			break
		}

		rub := d.Rubriques.Front().Value.(Rubrique)

		data += fmt.Sprintf("<span class='rubrique' title='%s'>%s</span>", rub.Nom, rub.Corps)

		d.Rubriques.Remove(d.Rubriques.Front())
	}

	data += "</div>"
	return data
}

/********************
 * XML parsing land
 ********************/

/* XML parsing error definition */
type ParseError struct {
	Message string
}

func (p ParseError) String() string {
	return fmt.Sprintf("XML parse error : %s", p.Message)
}

/* Helper function to get an attribute of an XML element */
func getAttribute(n xml.StartElement, name string) (val string, err os.Error) {
	for _, attr := range n.Attr {
		if attr.Name.Local == name {
			return attr.Value, nil
		}
	}

	return "", ParseError{fmt.Sprintf("Attribute \"%s\" not found.", name)}
}

/* Fills a definition object when an entree object is found */
func parseDefinition(terme string, p *xml.Parser) (*Definition, os.Error) {
	ret := Definition {};

	ret.Terme = terme;
	ret.Rubriques = list.New();

	for {
		t, err := p.Token()

		if err != nil {
			return &ret, ParseError{err.String()};
		}

		switch t := t.(type) {
			case xml.StartElement:
				switch t.Name.Local {
					case "entete": // If we meet an entete, unmarshall it
						if err := p.Unmarshal(&ret.Entete, &t) ; err != nil {
							return &ret, ParseError{err.String()};
						}

					case "corps":
						if err := parseBody("corps", &(ret.Corps), p) ; err != nil {
							return &ret, err;
						}

					case "rubrique":
						name, err := getAttribute(t, "nom")
						if err != nil {
							return &ret, err;
						}

						rub := Rubrique{name, ""};

						if err := parseBody("rubrique", &(rub.Corps), p) ; err != nil {
							return &ret, err;
						}

						ret.Rubriques.PushBack(rub)

				}
			case xml.EndElement:
				if t.Name.Local == "entree" {
					return &ret, nil;
				}
		}
	}

	return &ret, nil;
}

/* Recursively parses a definition body and replaces XML attributes with the
   corresponding HTML tag

   TODO : Use a templating system.
   TODO2: Really parse variants. */
func parseBody(node_name string, buf *string, p *xml.Parser) (os.Error) {
	for {
		t, err := p.Token()

		if err != nil {
			return ParseError{err.String()};
		}

		switch t := t.(type) {
			case xml.StartElement:
				switch t.Name.Local {
					case "variante":
						(*buf) += "<span class='variante'>";

						if err := parseBody(t.Name.Local, buf, p) ; err != nil {
							return err;
						}

						(*buf) += "</span>";

					case "indent":
						(*buf) += "<span class='indent'>";

						if err := parseBody(t.Name.Local, buf, p) ; err != nil {
							return err;
						}

						(*buf) += "</span>";

					case "cit":
						aut, err := getAttribute(t, "aut")
						if err != nil {
							return err;
						}

						ref, err := getAttribute(t, "ref")
						if err != nil {
							return err;
						}

						(*buf) += "<span class='citation'><span class='cit'>";

						if err = parseBody(t.Name.Local, buf, p) ; err != nil {
							return err;
						}

						(*buf) += "</span>";

						(*buf) += fmt.Sprintf("<span class='aut'>%s</span>", aut);
						(*buf) += fmt.Sprintf("<span class='ref'>%s</span>", ref);

						(*buf) += "</span>";
					}
			case xml.CharData:
				(*buf) += strings.Replace(
						strings.Replace(string([]byte(t)), "\t", "", -1), "\n", "", -1)
			case xml.EndElement:
				if t.Name.Local == node_name {
					return nil;
				}
		}
	}

	return ParseError{"No end element found ! Verify your XML file."};
}

/* Parses the data and gives a pointer to the definition object on the data channel */
func parser(filename string, data chan *Definition) {
	f,err := os.Open(filename, os.O_RDONLY, 0666)
	if err != nil {
		fmt.Printf("%s\n", err.String())
		data<-nil
		return
	}

	parser := xml.NewParser(f)
	for {
		t, err := parser.Token()

		if err != nil {
			if err == os.EOF {
				break;
			} else {
				panic(err);
			}
		}

		switch t := t.(type) {
			case xml.StartElement:
				if t.Name.Local == "entree" {
					name, err := getAttribute(t, "terme")
					if err != nil {
						panic(err);
					}

					// If the 'sens' attribute is present
					if sens, err := getAttribute(t, "sens") ; err == nil {
						name = name+" ("+sens+")";
					}

					// If there is a supplement
					// TODO : Really manages this.
					if _,err := getAttribute(t, "supplement") ; err == nil {
						name = name+" (suppl.)";
					}

					def, err := parseDefinition(name, parser);
					if err != nil {
						panic(err);
					}

					data <- def
				}
		}
	}

	data<-nil
}

/********************
 * Output goroutines
 ********************/

func int32_to_byte(integer int32) []byte {
	var data = make([]byte, 4); // 4 bytes => 32 bits

	for i := 0 ; i < 4; i++ {
		data[3-i] = uint8(integer & 0xFFFF);
		integer = integer >> 8;
	}

	return data;
}

func output_stdout(input_queue chan *Definition, quit chan bool) {
	for {
		def := <-input_queue
		if def == nil {
			break
		}

		fmt.Printf("%v\n", (*def));
	}

	quit<-true
}

type IndexPosition struct {
	Name string
	Offset int32
	Size int32
	Quit bool
}

func write_index(filename string, input_queue chan IndexPosition, quit chan bool) {
	f,err := os.Open(filename, os.O_WRONLY | os.O_CREAT, 0666)
	defer f.Close()

	if err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		quit<-true
		return
	}

	for {
		pos := <-input_queue
		if pos.Quit == true {
			break
		}

		f.Write([]byte(pos.Name))
		f.Write(int32_to_byte(pos.Offset))
		f.Write(int32_to_byte(pos.Size))
		f.Write([]byte{0})
	}

	quit<-true
}

func write_database(filename string, input_queue chan* Definition, index chan IndexPosition) {
	f,err := os.Open(filename, os.O_WRONLY | os.O_CREAT, 0666)
	defer f.Close()

	if err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		index<-IndexPosition{"",0,0,true}
		return
	}

	pos := int32(0)
	for {
		def := <-input_queue
		if def == nil {
			break
		}

		data := []byte(def.Content())

		f.Write(data)

		index<-IndexPosition{def.Terme, pos, int32(len(data)), false}

		pos += int32(len(data))
	}

	index<-IndexPosition{"",0,0,true}
}

/*******************
 * Main function
 *******************/
func main() {
	if len(os.Args) < 2 {
		fmt.Fprintf(os.Stderr, "Usage: %s [filename]\n", os.Args[0]);
		return
	}
	input_queue := make(chan *Definition, 100);
	index_chan := make(chan IndexPosition);
	quit_chan := make(chan bool);

	go parser(os.Args[1],input_queue)
	go write_database("XMLittre.dict", input_queue, index_chan)
	go write_index("XMLittre.idx", index_chan, quit_chan)

	for {
		data := <-quit_chan
		if data == true {
			break
		}
	}
}
