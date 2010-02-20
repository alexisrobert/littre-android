/*
 * Littre dictionnary for Android
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

package org.alexis.littre;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class littre extends Activity {
	static private final String[] MENUMAPPING_FROM = {"name","icon"};
	static private final int[] MENUMAPPING_TO = {R.id.menuname,R.id.menuicon};
	
	static final String INTENT_GET_HISTORY = "org.alexis.littre.GetHistory";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        
        setContentView(R.layout.welcome); // We need to have our ListView instancied to fill it!
        
        List<Map<String,String>> menulist = new ArrayList<Map<String,String>>();
        
        // Arggg... put this in a resource XML file! Or make them at-least final !
        addMenuItem("Alphabet", android.R.drawable.ic_menu_directions, menulist);
        addMenuItem("Rechercher", android.R.drawable.ic_menu_search, menulist);
        addMenuItem("Historique", android.R.drawable.ic_menu_recent_history, menulist);
        
        SimpleAdapter adapter = new SimpleAdapter(this, menulist, R.layout.welcomeitem, MENUMAPPING_FROM, MENUMAPPING_TO);
        ((ListView)this.findViewById(R.id.welcomelist)).setAdapter(adapter);
        ((ListView)this.findViewById(R.id.welcomelist)).setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long id) {
				switch ((int)id) {
				case 0:
					Intent i1 = new Intent(null,null,getApplicationContext(), AlphabetActivity.class);
					startActivity(i1);
					break;
				case 1:
					onSearchRequested();
					break;
				case 2:
					Intent i2 = new Intent(INTENT_GET_HISTORY, null, getApplicationContext(), HistoryActivity.class);
		        	startActivity(i2);
		        	break;
				}
				Log.i("littre", String.valueOf(id));
			}
		});
    }
    
    // I know, side-effects are dirty. But I don't see any macros in Java.
    // OK, I aknowledge, i talk a little bit too much in the comments.
    private void addMenuItem(String name, int icon, List<Map<String,String>> menulist) {
    	Map<String, String> map = new HashMap<String, String>();
    	
    	map.put("name", name);
    	map.put("icon", String.valueOf(icon));
    	
    	menulist.add(map);
    }
}