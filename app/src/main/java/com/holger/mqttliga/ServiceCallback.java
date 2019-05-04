package com.holger.mqttliga;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONObject;

import org.greenrobot.eventbus.EventBus;

import static android.content.Context.NOTIFICATION_SERVICE;

class ServiceCallback implements View.OnTouchListener {
	private Events myevent = new Events();
    private ContextWrapper context;
    private SharedPreferences settings;
	private TextToSpeech ttobj;
	private Integer overlayID;
    private WindowManager windowManager;
    private View floatyView;

	private static final String NOTIFICATION_TICKER_ID = "MQTTLiga_Ticker";
	private static final String DEBUG_TAG = "ServiceCallback"; // Debug TAG

    ServiceCallback(ContextWrapper context, TextToSpeech ttobj, SharedPreferences settings) {
        this.context = context;
        this.ttobj=ttobj;
        this.settings=settings;

        windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);

        Log.i(DEBUG_TAG, "ServiceCallback(): Init");
    	FileInputStream fis;
		try {
			String tempFile = "MQTTLiga_Events.obj";
			fis = this.context.openFileInput(tempFile);
        	ObjectInputStream is = new ObjectInputStream(fis);
        	myevent = (Events) is.readObject();
        	is.close();
		} catch (FileNotFoundException e) {
			Log.i(DEBUG_TAG, "ServiceCallback(): Cache file not found!");
		} catch (StreamCorruptedException e) {
			Log.i(DEBUG_TAG, "ServiceCallback(): Cache file corrupted!");
		} catch (IOException e) {
			Log.i(DEBUG_TAG, "ServiceCallback()(): Cache file IO exception!");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
    }
    
    @SuppressWarnings("deprecation")
	void doMessage(String topic, String message) throws Exception {
    	
        Integer goalh;
        Integer goalg;
        Integer hgoalh;
        Integer hgoalg;
        Integer half;
        overlayID=0;

        long ts;
        Long delete_games;
        ArrayList<Scorer> scorerItems = new ArrayList<>();
        JSONObject obj = new JSONObject(message);
 
        delete_games=Long.parseLong(settings.getString("delete","7"));
        if(delete_games>0)
        {
        	Long x2=(System.currentTimeMillis() / 1000L)-(delete_games*24*60*60);
        	Long x1=obj.getLong("TS");
        	if(x1 < x2)
        	{
        		Log.i(DEBUG_TAG,"Game expired: - Topic:"+ topic+" TS: "+x1+" Del: "+x2);
        		return;
        	}
        }
        JSONArray jScorers = obj.getJSONArray("Scorers");

        String alert;
        String title = null;

		String[] parts = topic.split("/");

		if(parts.length<6) {
			return;
		}
		String liga = parts[2];
		String home = parts[4];
		String guest = parts[5];
		String event = obj.getString("Event");

		if(event.equals("ERROR"))
		{
			return;
		}
        
        ts=obj.getLong("TS");
        goalh=obj.getInt("ScoreH");
        goalg=obj.getInt("ScoreG");
        half=obj.getInt("Half");
        hgoalh=obj.getInt("HScoreH");
        hgoalg=obj.getInt("HScoreG");
        
		for(int i=0; i<jScorers.length(); i++)
		{
			Scorer scorer = new Scorer();
			JSONObject json_scorer = jScorers.getJSONObject(i);
			scorer.counter=json_scorer.getInt("ScoreG") + json_scorer.getInt("ScoreH");
			scorer.goalg=json_scorer.getInt("ScoreG");
			scorer.goalh=json_scorer.getInt("ScoreH");
			scorer.name=json_scorer.getString("Scorer");
			scorer.minute=json_scorer.getInt("Minute");
				
			scorerItems.add(scorer);
		}
		
		myevent.setGame(ts,topic,liga,event,goalh,goalg,half, hgoalh, hgoalg, scorerItems);
		
        boolean notify=false;
        boolean voice = false;
        boolean overlay = false;

        if(myevent.getChanged(myevent.pos)>0)
        {
        	if(settings.getBoolean("notify",true))
        	{
        		if(!MQTTLiga.isActivityVisible())
        		{
        			int resID1=R.drawable.ic_launcher;
        			Resources res = this.context.getResources();
        			
        			String home_text;
        			String guest_text;
        			try
        			{
        				home_text=res.getString(res.getIdentifier(home, "string","com.holger.mqttliga"));
        		     } catch (Resources.NotFoundException e)
        		     {
        		    	 home_text=home;
        		     }
        			
        			try {
						guest_text=res.getString(res.getIdentifier(guest, "string","com.holger.mqttliga"));
					} catch (Resources.NotFoundException e) {
						guest_text=guest;
					}
        			
        			if(event.equals("GOALH"))
        			{
        				resID1 = res.getIdentifier(home.toLowerCase(Locale.GERMANY),"drawable","com.holger.mqttliga");
        				title=res.getString(R.string.GOALH) + " " + home_text;
                        overlayID=resID1;
        				notify=true;
        				voice=true;
        				overlay=true;
        			}
        			if(event.equals("GOALG"))
        			{
        				resID1 = res.getIdentifier(guest.toLowerCase(Locale.GERMANY),"drawable","com.holger.mqttliga");
        				title=res.getString(R.string.GOALG) + " " + guest_text;
                        overlayID=resID1;
                        notify=true;
        				voice=true;
        				overlay=true;
        			}
        			if(event.equals("START"))
        			{
        				title=res.getString(R.string.START);
        				notify=true;
        			}
        			if(event.equals("HALF"))
        			{
        				title=res.getString(R.string.HALF);
        				notify=true;
        			}
        			if(event.equals("END"))
        			{
        				title=res.getString(R.string.END);
        				notify=true;
        			}
        			
        			alert = home_text + " : " + guest_text + " " + goalh + ":" + goalg; 

        			if(notify)
        			{
         				final Intent intent = new Intent(context,MainActivity.class);
        				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
        			            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        				
        				final PendingIntent activity = PendingIntent.getActivity(context, 0, intent, 0);

        				Notification mNotification;
        				mNotification = new NotificationCompat.Builder(context,NOTIFICATION_TICKER_ID)
        				.setContentTitle(title)
        				.setContentText(alert)
        				.setSmallIcon(resID1)
        				.setContentIntent(activity)
        				.build();

        				if (Build.VERSION.SDK_INT >= 21 /*Checking for Lollipop**/)
            			{

        					Bitmap bm = BitmapFactory.decodeResource(this.context.getResources(), resID1);
            				mNotification = new NotificationCompat.Builder(context,NOTIFICATION_TICKER_ID)
            				.setContentTitle(title)
            				.setContentText(alert)
            				.setLargeIcon(bm)
            				.setSmallIcon(R.drawable.lolli_logo)
            				.setContentIntent(activity)
							.build();

            			}

        				final NotificationManager notificationManager = (NotificationManager)
        						context.getSystemService(NOTIFICATION_SERVICE);
        				mNotification.number += 1;
        				
        				// Hide the notification after its selected
        				mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        				        				
                		mNotification.defaults |= Notification.DEFAULT_LIGHTS;
                		mNotification.defaults |= Notification.DEFAULT_SOUND;
                		mNotification.defaults |= Notification.DEFAULT_VIBRATE;
                		
                		mNotification.ledARGB = Color.MAGENTA;

						assert notificationManager != null;
						notificationManager.notify(0, mNotification);

						if(settings.getBoolean("overlay",true))
                        {
                            if(overlay) {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
									if (Settings.canDrawOverlays(context)) {

										new Handler(Looper.getMainLooper()).post(new Runnable() {
											@Override
											public void run() {
												if (overlayID > 0) {
													addOverlayView(overlayID);
												}
											}
										});
									}
								}
                            }
                        }

                		if(settings.getBoolean("voice",true))
                		{
                			if(voice)
                			{
                				if(ttobj!=null)
                				{
                					String colon=res.getString(R.string.COLON);
                					String zu=res.getString(R.string.TO);

                					String voice_alert = home_text + " "+colon+" " + guest_text + " " + goalh + " "+zu+" " + goalg; 
                					ttobj.speak(voice_alert, TextToSpeech.QUEUE_ADD, null);
                				}
                			}
                		}
        			}
        		}
        	}
        }

        if(myevent.publish)
        {
        	LoadEventsTimer eventstask = new LoadEventsTimer();
        	eventstask.run(); 
		  	String tempFile = "MQTTLiga_Events.obj";
	    	FileOutputStream fos = null;
			try {
				fos = this.context.openFileOutput(tempFile, Context.MODE_PRIVATE);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
	    	ObjectOutputStream os;
			try {
				os = new ObjectOutputStream(fos);
				os.writeObject(myevent);
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.i(DEBUG_TAG, "Events saved!");
       }
        else
        {
        	Log.i(DEBUG_TAG, "myevent.publish=false");
        }
    }

    class LoadEventsTimer extends TimerTask {
        private Handler mHandler = new Handler(Looper.getMainLooper());

        @Override
        public void run() {
           // ...
           mHandler.postDelayed(new Runnable() {
              public void run() {
            	  EventBus.getDefault().postSticky(myevent);
              }
           },2000);
         }
    }

    private void addOverlayView(int overlayID) {
		Log.i(DEBUG_TAG, "addoverlayView");

		final WindowManager.LayoutParams params;
			params = new WindowManager.LayoutParams(
					WindowManager.LayoutParams.MATCH_PARENT,
					WindowManager.LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
					0,
					PixelFormat.TRANSLUCENT);

		params.x = 0;
        params.y = 0;

        FrameLayout interceptorLayout = new FrameLayout(context) {

            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {

                // Only fire on the ACTION_DOWN event, or you'll get two events (one for _DOWN, one for _UP)
                if (event.getAction() == KeyEvent.ACTION_DOWN) {

                    // Check if the HOME button is pressed
                    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

                        Log.v(DEBUG_TAG, "BACK Button Pressed");

                        // As we've taken action, we'll return true to prevent other apps from consuming the event as well
                        return true;
                    }
                }

                // Otherwise don't intercept the event
                return super.dispatchKeyEvent(event);
            }
        };

        if(floatyView != null) {
            windowManager.removeViewImmediate(floatyView);
            Log.i(DEBUG_TAG, "Close existing Overlay");
        }
        floatyView = ((LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.alertwindow, interceptorLayout);

        floatyView.setOnTouchListener(this);
        Log.i(DEBUG_TAG, "Overlay ID: "+overlayID);
        ImageView img = floatyView.findViewById(R.id.overlayimage);
        img.setImageResource(overlayID);

        windowManager.addView(floatyView, params);
    }

    @SuppressLint("ClickableViewAccessibility")
	@Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.i(DEBUG_TAG, "onTouch...");
        if (floatyView != null) {

            windowManager.removeView(floatyView);

            floatyView = null;
        }
        return true;
    }
}
