package com.holger.mqttliga;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    public static final String SERVICE_CLASSNAME = "com.holger.mqttliga.MQTTService";
    private Menu menu;
    private RecyclerView listview;
    GameListAdapter adapter;
    Events listEvent;
    boolean initLoad;
    boolean highlight_timer=false;
    Integer highlight_min=0;
    Long delete_games;
    private final String tempFile = "MQTTLiga_Events.obj";
    
    private Timer HighlightTimer;
    public static final String                 DEBUG_TAG = "MQTTLiga"; // Debug TAG

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_launcher);
//        toolbar.setOverflowIcon(getResources().getDrawable( R.drawable.ic_menu_settings));

        listEvent = new Events();
        listview = findViewById(R.id.listview);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        MQTTLiga.tablet = getResources().getBoolean(R.bool.isTablet);
        MQTTLiga.notify=settings.getBoolean("notify", true);
        MQTTLiga.broker_url=settings.getString("broker_url", "108.61.178.24");
        MQTTLiga.broker_port=settings.getString("broker_port","1883");
        MQTTLiga.highlight_min=settings.getString("highlight","5");
        MQTTLiga.delete_games=settings.getString("delete", "7");
        MQTTLiga.screen_on=settings.getBoolean("screenon", true);
        MQTTLiga.voice=settings.getBoolean("voice", false);
        highlight_min=Integer.parseInt(MQTTLiga.highlight_min);
        delete_games=Long.parseLong(MQTTLiga.delete_games);

        View mDecorView = getWindow().getDecorView();
        mDecorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE) {
                    Log.i(DEBUG_TAG, "Stop fullscreen");
                    finish();
                    startActivity(getIntent());
                }
            }
        });
        
        Log.i(DEBUG_TAG, "OnCreate()...");
        if(!EventBus.getDefault().isRegistered(this)) {
            Log.i(DEBUG_TAG, "EventBus register");
            EventBus.getDefault().register(this);
        }

        Events myEvent = EventBus.getDefault().getStickyEvent(Events.class);
        if(myEvent != null)
        {
        	Log.i(DEBUG_TAG, "OnCreate(): Sticky event found!");
        	this.listEvent = myEvent;
        	adapter = new GameListAdapter(R.layout.gamelist,this.listEvent);
        	listview.setAdapter(adapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            listview.setLayoutManager(layoutManager);
        }
        else
        {
        	Log.i(DEBUG_TAG, "OnCreate(): Sticky event not found!");
        	FileInputStream fis;
			try {
				fis = getBaseContext().openFileInput(tempFile);
	        	ObjectInputStream is = new ObjectInputStream(fis);
	        	myEvent = (Events) is.readObject();
	        	is.close();
	        	this.listEvent = myEvent;
	        	adapter = new GameListAdapter(R.layout.gamelist,this.listEvent);
	        	listview.setAdapter(adapter);
	        	listview.invalidate();
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
                listview.setLayoutManager(layoutManager);

			} catch (FileNotFoundException e) {
				Log.i(DEBUG_TAG, "OnCreate(): Cache file not found!");
			} catch (StreamCorruptedException e) {
				Log.i(DEBUG_TAG, "OnCreate(): Cache file corrupted!");
			} catch (IOException e) {
				Log.i(DEBUG_TAG, "OnCreate(): Cache file IO exception!");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
    }

	@Override
    protected void onResume()
    {
      super.onResume();
            	
      if(deleteGames(listEvent))
      {
    	  adapter.notifyDataSetChanged();
      }
      
      if(MQTTLiga.screen_on)
      {        
    	  Log.i(DEBUG_TAG, "Keep screen on: "+MQTTLiga.screen_on);
    	  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      }
//      wl.acquire();
      
      if(highlight_min>0)
      {
    	  HighlightTimer = new Timer();
          int seconds = 60;
          HighlightTimer.schedule(new TimerTask()
    	  {          
    		  @Override
    		  public void run()
    		  {
    			  HighlightMethod();
    		  }

    	  }, 0, seconds * 1000);
    	  highlight_timer=true;
      }
      
      if(MQTTLiga.voice_change)
      {
    	  startVoice();
      }
      
      Log.i(DEBUG_TAG, "OnResume()...");
      MQTTLiga.activityResumed();
    }

    @Override
    protected void onPause() {
      super.onPause();
//      wl.release();
      MQTTLiga.voice_change=false;
      if(highlight_timer)
      {
    	  HighlightTimer.cancel();
      }
      Log.i(DEBUG_TAG, "OnPause()...");
      MQTTLiga.activityPaused();
    }
    
    private void HighlightMethod()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {

        Log.i(DEBUG_TAG,"HighlightTimer thread!");
        highlightRowHandler();
    	}
    };
    
    private void highlightRowHandler()
    {
        boolean changed=false;
        
        Log.i(DEBUG_TAG,"HighlightRowHandler: "+listEvent.getCount());
    	for(int i=0;i<listEvent.getCount();i++)
    	{
			Long x2=(System.currentTimeMillis() / 1000L)-(highlight_min*60);
			Long x1=listEvent.getTS(i);

    		if(listEvent.getHighlight(i))
    		{
    			Log.i(DEBUG_TAG,"Highlight Topic:"+ listEvent.getTopic(i) +" X1: "+x1+" X2: "+x2);
    			
    			if(x2>=x1)
    			{
        			Log.i(DEBUG_TAG,"Stop highlight Topic:"+ listEvent.getTopic(i));
    				listEvent.setHighlight(i, false);
    				changed=true;
    			}
    		}
    		if(listEvent.getChanged(i)>0)
    		{
    			Log.i(DEBUG_TAG,"Changed Topic:"+ listEvent.getTopic(i) +" X1: "+x1+" X2: "+x2);
    			
    			if(x2>=x1)
    			{
        			Log.i(DEBUG_TAG,"Stop changed Topic:"+ listEvent.getTopic(i));
    				listEvent.setChanged(i, 0);
    				changed=true;
    			}
    		}
    	}  
        //This method runs in the same thread as the UI.               
    	if(changed)
    	{
    		adapter.notifyDataSetChanged();
	    	FileOutputStream fos = null;
			try {
				fos = openFileOutput(tempFile, Context.MODE_PRIVATE);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
	    	ObjectOutputStream os;
			try {
				os = new ObjectOutputStream(fos);
				os.writeObject(listEvent);
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.i(DEBUG_TAG,"highlightRowHandler: Events saved!");
    	}
   	}
    
    @Override
    public void onStart() {
        Log.i(DEBUG_TAG, "OnStart()...");
        super.onStart();
    }

    @Override
    public void onStop() {
    	Log.i(DEBUG_TAG, "OnStop()...");
        super.onStop();
    }

    @Override
    public void onDestroy() {
    	Log.i(DEBUG_TAG, "OnDestroy()...");  	
    	EventBus.getDefault().unregister(this);

        super.onDestroy();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events myEvent){
    	Log.i(DEBUG_TAG, "OnEventMainTread");
    	this.listEvent = myEvent;
    	adapter = new GameListAdapter(R.layout.gamelist, this.listEvent);
    	listview.setAdapter(adapter);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        listview.setLayoutManager(layoutManager);

        if(initLoad)
        {
        	try {            
       		Thread.sleep(1000);
        	} catch (InterruptedException e) {
        		e.printStackTrace();
        	}
            initLoad=false;
        }
        else
        {
        	final Integer pos = listEvent.pos;
        	listview.postDelayed(new Runnable() {
        	    public void run() {
        	    	if(listEvent.getCount()>0)
        	    	{
        	    		listEvent.setChanged(pos, 0);
        	    	}
        	    }
        	}, 2500);
        }
    }
  
    private void stopMQTTService() {
        final Intent intent = new Intent(this, MQTTService.class);
        stopService(intent);
    }


    private boolean serviceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (SERVICE_CLASSNAME.equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startMQTTService() {
        final Intent intent = new Intent(this, MQTTService.class);
        intent.setAction("MQTTService.START");
        startService(intent);
    }

    private void startVoice() {
        final Intent intent = new Intent(this, MQTTService.class);
        intent.setAction("MQTTService.VOICE");
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu=menu;

    	if (serviceIsRunning())
    	{
            menu.findItem(R.id.action_connect).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.connected));
        }
    	else
    	{
    	    menu.findItem(R.id.action_connect).setIcon(ContextCompat.getDrawable(getApplicationContext(),R.drawable.notconnected));
    	}

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // Launch settings activity
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_connect) {
        	if (serviceIsRunning())
        	{
                stopMQTTService();
                menu.findItem(R.id.action_connect).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.notconnected));
        	}
        	else
        	{
        		initLoad=true;
        		startMQTTService();
        		menu.findItem(R.id.action_connect).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.connected));
        	}
            // Launch settings activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean deleteGames(Events myEvent)
    {
        Events xEvent = new Events();
        delete_games=Long.parseLong(MQTTLiga.delete_games);
        if(delete_games>0)
        {
        	Long x2=(System.currentTimeMillis() / 1000L)-(delete_games*24*60*60);

        	for(int i=0;i<myEvent.getCount();i++)
        	{
        		Long x1=myEvent.getTS(i);

        		if(x1 < x2)
        		{
        			Log.i(DEBUG_TAG,"Game expired: - Topic:"+ myEvent.getTopic(i)+" TS: "+x1+" Del: "+x2);

        			xEvent.mygames.add(myEvent.mygames.get(i));
        		}
        	}

        	if(xEvent.mygames.size()>0)
        	{
        		myEvent.mygames.removeAll(xEvent.mygames);
        		Log.i(DEBUG_TAG,"Size: "+myEvent.mygames.size());
        		
		    	FileOutputStream fos = null;
				try {
					fos = openFileOutput(tempFile, Context.MODE_PRIVATE);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
		    	ObjectOutputStream os;
				try {
					os = new ObjectOutputStream(fos);
					os.writeObject(listEvent);
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Log.i(DEBUG_TAG,"Events saved!");

        		return true;

        	}
        }
    	return false;
    }
}
