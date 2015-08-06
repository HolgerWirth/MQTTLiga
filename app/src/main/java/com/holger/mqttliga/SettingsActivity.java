package com.holger.mqttliga;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
	  @SuppressWarnings("deprecation")
	@Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences_mqtt);
	    getPreferenceScreen().findPreference("broker_url").setEnabled(false);
	    getPreferenceScreen().findPreference("broker_port").setEnabled(false);
	  }
	 
	    @SuppressWarnings("deprecation")
		@Override
	    protected void onResume() {
	      super.onResume();
	      getPreferenceScreen().getSharedPreferences()
	          .registerOnSharedPreferenceChangeListener(listener);
	    }

	    @SuppressWarnings("deprecation")
		@Override
	    protected void onPause() {
	      super.onPause();
	      getPreferenceScreen().getSharedPreferences()
	          .unregisterOnSharedPreferenceChangeListener(listener);
	    }
	    
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        if (requestCode == 1) {
	            if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
	            	Log.i(getClass().getSimpleName(), "Missing TTS data. install it");
	                Intent installIntent = new Intent();
	                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	                startActivity(installIntent);
	            }
	        }
	    }
	    
	    OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
	    	  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
	    		Log.i(getClass().getSimpleName(), "Key: "+key);
	    		
		    	if(key.equals("notify"))
		    	{
		            MQTTLiga.notify = prefs.getBoolean("notify", true);
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
	    	  }
	    };
}
