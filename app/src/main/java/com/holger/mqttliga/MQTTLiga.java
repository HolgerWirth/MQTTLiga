package com.holger.mqttliga;

import android.util.Log;

public class MQTTLiga  {

	public static boolean notify=true;
	public static String broker_url="";
	public static boolean heartbeat;
	public static String highlight_min;
	public static boolean voice=false;
	public static boolean tablet;
	public static String delete_games;
	public static boolean screen_on;
	public static boolean voice_change=false;
	public static boolean BL2=false;
	public static boolean BL1=false;
    public static boolean overlay;

    public static boolean isActivityVisible() {
		    return activityVisible;
		  }  

		  public static void activityResumed() {
			  Log.i("MQTTLiga", "activityResumed()");
		    activityVisible = true;
		  }

		  public static void activityPaused() {
			  Log.i("MQTTLiga", "activityPaused()");
			  activityVisible = false;
		  }

		  private static boolean activityVisible;
}
