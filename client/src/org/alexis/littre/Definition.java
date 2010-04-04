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

import org.alexis.libstardict.Word;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

public class Definition extends Activity {
    WebView web;
    Word word;
    public static final String XMLITTRE_URL = 
    	"http://francois.gannaz.free.fr/Littre/xmlittre.php?requete=%s";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.definition);
        
    	word = (Word) getIntent().getSerializableExtra("word");
    	if (word == null) {
    		finish();
    		return;
    	}
    	
    	setTitle(word.getName());
        
        web = (WebView)findViewById(R.id.webview);
        web.loadUrl(String.format(getString(R.string.definitionurl),
					word.getOffset(),
					word.getSize()));
        
        Button backbutton = (Button)findViewById(R.id.backButton);
        backbutton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
        	
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
    	MenuItem menuit_share = menu.add(0, Menu.FIRST, 0, getString(R.string.menu_share));
    	menuit_share.setIcon(android.R.drawable.ic_menu_share);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case Menu.FIRST:
        	// I would love to send full-text definitions. But the WebView API doesn't permit this.
        	
        	Intent i2 = new Intent(Intent.ACTION_SEND);
        	i2.setType("text/plain");
        	i2.putExtra(Intent.EXTRA_TEXT, String.format(XMLITTRE_URL, Uri.encode(word.getName())));
        	startActivity(i2);
        	return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
}
