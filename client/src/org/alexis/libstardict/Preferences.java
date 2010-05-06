package org.alexis.libstardict;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences {
	public static final String FILENAME = "XMLittre.idx";
	public static final String SDCARD_DATA = "/sdcard/Android/data/org.alexis.littre/";
	
	/* Stores index location (sdcard or internal memory) */
	public static File getIndexPath(Context ctx) {
		return new File(getIndexDir(ctx), FILENAME);
	}
	
	public static File getIndexDir(Context ctx) {
		if (isIndexSD(ctx)) {
			if (!makeSDDirs())
				return null;
			
			return new File(SDCARD_DATA);
		} else { // Internal memory
			return ctx.getFilesDir();
		}
	}
	
	public static boolean isIndexSD(Context ctx) {
		// If we find the index on the internal memory, always prefer it.
		if (getPreferences(ctx).getString("index_sdcard", "internal").equals("sdcard")
				&& new File(ctx.getFilesDir(), FILENAME).isFile()) {
			
			Log.i("littre", "Index detected on internal memory, changing the setting from sdcard to internal...");
			getPreferences(ctx).edit().putString("index_sdcard", "internal").commit();
		}
		
		if (!FileUtils.isSDMounted()) {
			return false;
		}
		
		return getPreferences(ctx).getString("index_sdcard", "internal").equals("sdcard");
	}
	
	public static boolean makeSDDirs() {
		File f = new File(SDCARD_DATA);
		
		return f.isDirectory() || f.mkdirs();
	}
	
	private static SharedPreferences getPreferences(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx);
	}
}
