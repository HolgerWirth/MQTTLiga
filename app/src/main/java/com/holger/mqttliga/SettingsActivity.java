package com.holger.mqttliga;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences_mqtt);
	    getPreferenceScreen().findPreference("broker_url").setEnabled(false);
	    getPreferenceScreen().findPreference("broker_port").setEnabled(false);

        Preference notifyPref = findPreference("overlay");
        notifyPref.setOnPreferenceChangeListener(this);
 	  }
	 
		@Override
	    protected void onResume() {
	      super.onResume();
	    }

		@Override
	    protected void onPause() {
	      super.onPause();
	    }
	    
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            Log.i(getClass().getSimpleName(), "requestedCode: "+requestCode);
            if (requestCode == 1) {
	            if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
	            	Log.i(getClass().getSimpleName(), "Missing TTS data. install it");
	                Intent installIntent = new Intent();
	                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	                startActivity(installIntent);
	            }
	        }
	    }

    public void checkDrawOverlayPermission() {

        // Checks if app already has permission to draw overlays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {

                // If not, form up an Intent to launch the permission request
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));

                // Launch Intent, with the supplied request code
                startActivityForResult(intent, 110010);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String key=preference.getKey();
        Log.i(getClass().getSimpleName(), "Key: "+key);
	    		
        if(key.equals("notify"))
        {
		    MQTTLiga.notify = prefs.getBoolean("notify", true);
        }
        if(key.equals("overlay")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                Log.i(getClass().getSimpleName(), "Overlay=" + MQTTLiga.overlay);
                if (Settings.canDrawOverlays(getApplicationContext()))
                {
                    MQTTLiga.overlay = prefs.getBoolean("overlay", false);
                    Log.i(getClass().getSimpleName(), "Setting OK: Overlay=" + MQTTLiga.overlay);
                } else {
                    MQTTLiga.overlay = false;
                    Log.i(getClass().getSimpleName(), "Setting NOK: Overlay=" + MQTTLiga.overlay);
                    checkDrawOverlayPermission();
                }
            }
        }
        if(key.equals("broker_url"))
        {
            MQTTLiga.broker_url = prefs.getString("broker_url", "");
        }
        if(key.equals("broker_port"))
        {
            MQTTLiga.broker_port = prefs.getString("broker_port", "1883");
        }
        if(key.equals("heartbeat"))
        {
            MQTTLiga.heartbeat = prefs.getBoolean("heartbeat", true);
        }
        if(key.equals("highlight"))
        {
            MQTTLiga.highlight_min= prefs.getString("highlight", "5");
        }
        if(key.equals("BL1"))
        {
            MQTTLiga.BL1 = prefs.getBoolean("BL1", true);
        }
        if(key.equals("BL2"))
        {
            MQTTLiga.BL2 = prefs.getBoolean("BL2",true);
        }

        if(key.equals("voice"))
        {
            MQTTLiga.voice = prefs.getBoolean("voice", false);
            if(MQTTLiga.voice)
            {
                Log.i(getClass().getSimpleName(), "Checking for TTS data");
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, 1);
            }
            MQTTLiga.voice_change=true;
        }
        if(key.equals("delete"))
        {
            MQTTLiga.delete_games= prefs.getString("delete","7");
        }
        if(key.equals("screenon"))
        {
            MQTTLiga.screen_on= prefs.getBoolean("screenon",true);
            Log.i(getClass().getSimpleName(), "screenon="+MQTTLiga.screen_on);
        }
        return true;
    }
}
