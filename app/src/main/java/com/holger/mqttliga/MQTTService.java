package com.holger.mqttliga;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class MQTTService extends Service implements MqttCallback {
    public static final String                 DEBUG_TAG = "MQTTService"; // Debug TAG

    private static final String                MQTT_THREAD_NAME = "MQTTService[" + DEBUG_TAG + "]"; // Handler Thread ID

    public static final int                        MQTT_QOS_0 = 0; // QOS Level 0 ( Delivery Once no confirmation )

    private static final long                 MQTT_KEEP_ALIVE = 1200000; // KeepAlive Interval in MS
    private static final String                MQTT_KEEP_ALIVE_TOPIC_FORAMT = "/Bundesliga/devices/%s/keepalive"; // Topic format for KeepAlives
    private static final byte[]         MQTT_KEEP_ALIVE_MESSAGE = { 0 }; // Keep Alive message to send
    private static final int                MQTT_KEEP_ALIVE_QOS = MQTT_QOS_0; // Default Keepalive QOS

    private static final boolean         MQTT_CLEAN_SESSION = false;

    private static final String         MQTT_URL_FORMAT = "tcp://%s:%d";

    private static final String         ACTION_START         = DEBUG_TAG + ".START"; // Action to start
    private static final String         ACTION_STOP                = DEBUG_TAG + ".STOP"; // Action to stop
    private static final String         ACTION_VOICE                = DEBUG_TAG + ".VOICE"; // Action to stop
    private static final String         ACTION_KEEPALIVE= DEBUG_TAG + ".KEEPALIVE"; // Action to keep alive used by alarm manager

    private static final String         DEVICE_ID_FORMAT = "BL_%s"; // Device ID Format, add any prefix you'd like

    private boolean mStarted = false; // Is the Client started?
    private String mDeviceId;                  // Device ID, Secure.ANDROID_ID

    private MqttDefaultFilePersistence mDataStore = null; // Defaults to FileStore

    private String mKeepAliveTopic;                        // Instance Variable for Keepalive topic

    private MqttAsyncClient mClient;                                        // Mqtt Client

    private AlarmManager mAlarmManager;                        // Alarm manager to perform repeating tasks

    private ServiceCallback push;
    private PendingIntent pi;
    private BackgroundDataPreferenceReceiver backgroundDataPreferenceMonitor;
    private volatile boolean backgroundDataEnabled = true;
    private NetworkConnectionIntentReceiver networkConnectionMonitor;
    private SharedPreferences settings;
    private TextToSpeech ttobj=null;
    private Boolean isConnected=false;
    private Boolean isConnecting=false;
    private Boolean isnewStarted=false;
    private Boolean heartbeat=false;

    @Override
    public void onCreate() {
        super.onCreate();

        initVoice();
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        heartbeat = settings.getBoolean("heartbeat", true);

        HandlerThread thread = new HandlerThread(MQTT_THREAD_NAME);
        thread.start();
        isnewStarted=false;
//        Handler mConnHandler = new Handler(thread.getLooper());
    }

    private void initVoice()
    {
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean voice = settings.getBoolean("voice", false);
        if(voice)
        {
        	Log.i(DEBUG_TAG, "initVoice: voice output enanbled");
        }
        else
        {
        	Log.i(DEBUG_TAG, "initVoice: voice output disabled");
        }
        
        if(voice)
    	{	
    		ttobj = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
    			@Override
    			public void onInit(int status) {
    				if(status != TextToSpeech.ERROR){
    					ttobj.setLanguage(Locale.GERMANY);
    	            	}				
    	         	}
    	      	});
    	}
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(intent==null)
        {
        	Log.i(DEBUG_TAG, "onStartCommand: received null intent");
        }

        String action;

            mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            
            if(intent!=null)
            {
            	action = intent.getAction();
            	Log.i(DEBUG_TAG, "onStartCommand: Received action of " + action);
            }
            else
            {
            	action=null;
            }

			if (action == null) {
				Log.i(DEBUG_TAG,
						"Starting service with no action\n Probably from a crash");
				start();
			} else {
                switch (action) {
                    case ACTION_START:
                        isnewStarted = true;
                        Log.i(DEBUG_TAG, "onStartCommand: Received ACTION_START");
                        start();
                        break;
                    case ACTION_STOP:
                        stop();
                        break;
                    case ACTION_KEEPALIVE:
                        keepAlive();
                        break;
                    case ACTION_VOICE:
                        initVoice();
                        break;
                }
			}

        return START_STICKY;
    }

    private synchronized void start() {
        if(mStarted) {
            Log.i(DEBUG_TAG,"Attempt to start while already started");
            return;
        }

        isConnected=false;
        push = new ServiceCallback(this,ttobj,settings);
        
        if(hasScheduledKeepAlives()) {
            stopKeepAlives();
        }

        try {
			connect();
		} catch (MqttException e) {
			e.printStackTrace();
		}

        registerBroadcastReceivers();
        
    }

    private synchronized void stop() {
        if(!mStarted) {
            Log.i(DEBUG_TAG,"Attempting to stop connection that isn't running");
            return;
        }

        Log.i(DEBUG_TAG,"Stop connection");
        unregisterBroadcastReceivers();
//        wl.release();
    }

    private synchronized void connect() throws MqttException {
    	stopKeepAlives();

    	Log.i(DEBUG_TAG,"Connect!");
        String MQTT_BROKER = settings.getString("broker_url", "108.61.178.24");
        int MQTT_PORT = Integer.parseInt(settings.getString("broker_port", "1883"));
        final Boolean BL1=settings.getBoolean("BL1", true);
        final Boolean BL2=settings.getBoolean("BL2", true);
        
        Log.i(DEBUG_TAG, "Preferences read: MQTT Server: "+ MQTT_BROKER +" Port: "+ MQTT_PORT);

    	String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
        Log.i(DEBUG_TAG,"Connecting with URL: " + url);
        
        mDeviceId = String.format(DEVICE_ID_FORMAT,
        Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
      
        isConnecting=true;

        MqttConnectOptions mOpts = new MqttConnectOptions();
        mOpts.setKeepAliveInterval(0);
        mOpts.setConnectionTimeout(10);
        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
        
        mClient = new MqttAsyncClient(url,mDeviceId,mDataStore);
        mClient.connect(mOpts,null, new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
                try {
                    mStarted = true; // Service is now connected
                    mKeepAliveTopic=null;
                    isConnected=true;
                    Log.i(DEBUG_TAG,"Successfully connected");
                    mClient.setCallback(MQTTService.this);  	
                     
                    if(isnewStarted) {
                        mClient.unsubscribe("/Bundesliga/Game/#");

                        if (!BL1) {
                            mClient.unsubscribe("/Bundesliga/BL1/Game/#");
                        }
                        if (!BL2) {
                            mClient.unsubscribe("/Bundesliga/BL2/Game/#");
                        }

                        List<String> subs_list = new ArrayList<>();
                        List<Integer> subs_qos = new ArrayList<>();

                        if (BL1) {
                            subs_list.add("/Bundesliga/BL1/Game/#");
                            subs_qos.add(1);
                        }
                        if (BL2) {
                            subs_list.add("/Bundesliga/BL2/Game/#");
                            subs_qos.add(1);
                        }

                        String[] subs = new String[ subs_list.size() ];
                        int[] qos = new int[subs_qos.size()];
                        subs_list.toArray(subs);
                        int q=0;
                        for( int qosItem : subs_qos) {
                            qos[q]=qosItem;
                            q++;
                        }
                        if(subs.length>0)
                        {
                            mClient.subscribe(subs, qos);
                            Log.i(DEBUG_TAG, "Successfully subscribed to " + Arrays.toString(subs) +" QoS: "+Arrays.toString(qos));
                        }
                        else
                        {
                            Log.i(DEBUG_TAG, "No subscription");
                        }
                        isnewStarted = false;
                    }
                    if(heartbeat)
                    {
                    	startKeepAlives();
                    	Log.i(DEBUG_TAG,"Starting keep alives");
                    }
                    isConnecting=false;

                } catch (MqttException e) {
                	isConnected=false;
                	isConnecting=false;
                	Log.i(DEBUG_TAG,"Error during connect");
                	Log.i(DEBUG_TAG, "Connection lost from broker! Reason: ",e);
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            	isConnected=false;
            	isConnecting=false;
            	Log.i(DEBUG_TAG,"Connect failure");
            }
        }); 
    }

    /**
     * Schedules keep alives via a PendingIntent
     * in the Alarm Manager
     */
    private void startKeepAlives() {
        Intent i = new Intent();
        i.setClass(this, MQTTService.class);
        i.setAction(ACTION_KEEPALIVE);

        pi = PendingIntent.getService(this, 0, i, 0);
        mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + MQTT_KEEP_ALIVE,
                MQTT_KEEP_ALIVE, pi);
    }
    /**
     * Cancels the Pending Intent
     * in the alarm manager
     */
    private void stopKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, MQTTService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i , 0);

        mAlarmManager.cancel(pi);
    }
    /**
     * Publishes a KeepALive to the topic
     * in the broker
     */
    private synchronized void keepAlive() {
        if(isConnected)
        {
        	Log.i(DEBUG_TAG, "keepAlive(): connected!");
            try {
                sendKeepAlive();
            } catch(MqttConnectivityException ex) {
                Log.i(DEBUG_TAG,"Reconnect!");
                reconnectIfNecessary();
            } catch(MqttPersistenceException ex) {
                Log.i(DEBUG_TAG,"Stop1: Reconnect!");
            } catch(MqttException ex) {
                Log.i(DEBUG_TAG,"Stop2: Reconnect!");
                reconnectIfNecessary();
            }
        }
        else
        {
        	mClient=null;
        	Log.i(DEBUG_TAG, "keepAlive(): not connected!");
            Log.i(DEBUG_TAG, "keepAlive(): Reconnect!");
            reconnectIfNecessary();
        }
    }

    private synchronized void reconnectIfNecessary() {
    	Log.i(DEBUG_TAG, "reconnectIfNecessary()");
        if(mClient== null) {
        	Log.i(DEBUG_TAG, "reconnectIfNecessary(): try to connect");
            try {
				connect();
			} catch (MqttException e) {
				e.printStackTrace();
			}
        }
    }
    @SuppressWarnings("deprecation")
    private void registerBroadcastReceivers() {
                  if (networkConnectionMonitor == null) {
                          networkConnectionMonitor = new NetworkConnectionIntentReceiver();
                          registerReceiver(networkConnectionMonitor, new IntentFilter(
                                          ConnectivityManager.CONNECTIVITY_ACTION));
                  }

                  if (Build.VERSION.SDK_INT < 14 /**Build.VERSION_CODES.ICE_CREAM_SANDWICH**/) {
                          // Support the old system for background data preferences
                          ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                          backgroundDataEnabled = cm.getBackgroundDataSetting();
                          if (backgroundDataPreferenceMonitor == null) {
                                  backgroundDataPreferenceMonitor = new BackgroundDataPreferenceReceiver();
                                  registerReceiver(
                                                  backgroundDataPreferenceMonitor,
                                                  new IntentFilter(
                                                                  ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED));
                          }
                  }
    }

    private void unregisterBroadcastReceivers(){
          if(networkConnectionMonitor != null){
                  unregisterReceiver(networkConnectionMonitor);
                  networkConnectionMonitor = null;
          }

          if (Build.VERSION.SDK_INT < 14 /**Build.VERSION_CODES.ICE_CREAM_SANDWICH**/) {
                  if(backgroundDataPreferenceMonitor != null){
                          unregisterReceiver(backgroundDataPreferenceMonitor);
                  }
                  }
    }

    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

        @SuppressLint("Wakelock")
		@Override
        public void onReceive(Context context, Intent intent)
        {
                // we protect against the phone switching off
                // by requesting a wake lock - we request the minimum possible wake
                // lock - just enough to keep the CPU running until we've finished
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
                wl.acquire();

                Log.i(DEBUG_TAG, "Internal network status receive called with: "+intent);
                
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
                boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
              
    			Log.i(DEBUG_TAG, "Extra Reason: "+reason);
          		if(noConnectivity)
           		{
        			Log.i(DEBUG_TAG, "NotifClientsOffline");
        			mClient=null;
           		}

           		if(isFailover)
                {
                	Log.i(DEBUG_TAG,"IsFailover: TRUE");
                }
                else
                {
                	Log.i(DEBUG_TAG,"IsFailover: FALSE");
                }
           		
           		if(!isConnecting)
           		{
           		    if(isOnline())
           		    {
           			    Log.i(DEBUG_TAG,"isOnline:TRUE");
           			    if(!isConnected)
           			    {
           				    mClient=null;
           				    Log.i(DEBUG_TAG,"Not connected: trying to reconnect");
           				    reconnectIfNecessary();
           			    }
                    }
           		    else
           		    {
           			    Log.i(DEBUG_TAG,"isOnline:FALSE");
                    }
                }

                wl.release();
        }
    }

    public boolean isOnline() {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo() != null
                            && cm.getActiveNetworkInfo().isAvailable()
                            && cm.getActiveNetworkInfo().isConnected()
                            && backgroundDataEnabled) {
                    return true;
            }

//            mClient=null;
            return false;
    }
    
    private class BackgroundDataPreferenceReceiver extends BroadcastReceiver {

        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                if (cm.getBackgroundDataSetting()) {
                        if (!backgroundDataEnabled) {
                                backgroundDataEnabled = true;
                                // we have the Internet connection - have another try at
                                // connecting
                                reconnectIfNecessary();
                        }
                } else {
                        backgroundDataEnabled = false;
                		Log.i(DEBUG_TAG, "NotifClientsOffline");
                }
        }
}

    /**
     * Sends a Keep Alive message to the specified topic
     * @return MqttDeliveryToken specified token you can choose to wait for completion
     */
    private synchronized IMqttDeliveryToken sendKeepAlive()
            throws MqttConnectivityException, MqttException {
        if(!isConnected)
        {
        	mClient=null;
        	reconnectIfNecessary();
        	return null;
        }
        else
        {
        	Log.i(DEBUG_TAG,"Sending Keepalive");
        	mKeepAliveTopic = String.format(Locale.US, MQTT_KEEP_ALIVE_TOPIC_FORAMT,mDeviceId);
        	MqttMessage message = new MqttMessage(MQTT_KEEP_ALIVE_MESSAGE);
        	message.setQos(MQTT_KEEP_ALIVE_QOS);
        	return mClient.publish(mKeepAliveTopic,message,0, null);
        }
    }

    private synchronized boolean hasScheduledKeepAlives() {
        Intent i = new Intent();
        i.setClass(this, MQTTService.class);
        i.setAction(ACTION_KEEPALIVE);
        pi = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_NO_CREATE);

        return (pi != null);
    }


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    
    @Override
    public void onDestroy()
    {
    	Log.i(DEBUG_TAG, "MQTTService Stopped");

    	mAlarmManager.cancel(pi);

   		if (ttobj != null) {
   			Log.i(DEBUG_TAG, "Voice stopped!");
   			ttobj.stop();
   			ttobj.shutdown();
   		}
   		
   		stop();

    	if(mClient!=null)
    	{
    		try {
    			mClient.disconnect();
    		} catch (MqttException e) {
    			super.onDestroy();
    			stop();
    			return;
    		}
    	}

        super.onDestroy();
    }
    /**
     * Connectivity Lost from broker
     */

    public void connectionLost(Throwable arg0) {
    	if(!isConnecting)
    	{
    		Log.i(DEBUG_TAG, "Connection lost from broker! Reason: ",arg0);
    		isConnected=false;
    		
    		if(mClient!=null)
    		{
    	
    			try {
    				Log.i(DEBUG_TAG, "Disconnecting...");
    				mClient.disconnect(0);
    				mClient=null;
    				reconnectIfNecessary();
    			} catch (MqttException e) {
    				Log.i(DEBUG_TAG, "Disconnect failed");
    				mClient=null;
    			}
    		}

    		reconnectIfNecessary();
    	}
    }

    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    /**
     * Received Message from broker
     */
    public void messageArrived(String s, MqttMessage message)
            throws Exception {
//    	Log.i(DEBUG_TAG, "Message arrived");
    	
        push.doMessage(s, new String(message.getPayload()));
    }

    /**
     * MqttConnectivityException Exception class
     */
    private class MqttConnectivityException extends Exception {
        private static final long serialVersionUID = -7385866796799469420L;
    }
}

