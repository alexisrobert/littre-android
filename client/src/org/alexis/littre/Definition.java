package org.alexis.littre;

import org.alexis.libstardict.Word;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

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
    	setTitle(word.getName());
        
        web = (WebView)findViewById(R.id.webview);
        web.loadUrl(String.format(getString(R.string.definitionurl),
					word.getOffset(),
					word.getSize()));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuit = menu.add(0, Menu.FIRST, 0, "Recherche");
        menuit.setIcon(android.R.drawable.ic_menu_search);
    	MenuItem menuit_share = menu.add(0, Menu.FIRST+1, 0, "Partager");
    	menuit_share.setIcon(android.R.drawable.ic_menu_share);
    	MenuItem menuit_hist = menu.add(0, Menu.FIRST+2, 0, "Historique");
    	menuit_hist.setIcon(android.R.drawable.ic_menu_recent_history);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case Menu.FIRST:
        	onSearchRequested();
            return true;
        case Menu.FIRST+1:
        	// I would love to send full-text definitions. But the WebView API doesn't permit this.
        	
        	Intent i2 = new Intent(Intent.ACTION_SEND);
        	i2.setType("text/plain");
        	i2.putExtra(Intent.EXTRA_TEXT, String.format(XMLITTRE_URL, Uri.encode(word.getName())));
        	startActivity(i2);
        	return true;
        case Menu.FIRST+2:
        	Intent i = new Intent(littre.INTENT_GET_HISTORY, null, getApplicationContext(), littre.class);
        	startActivity(i);
        	return true;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
}
