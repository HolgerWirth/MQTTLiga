package com.holger.mqttliga;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Events implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 3751171634432301994L;
	/**
	 * 
	 */
    ArrayList<games> mygames;
	private int type;
	public boolean publish;
	public int pos;

    public Events()
    {
            this.mygames = new ArrayList<>();
    }

    public Events setGame(long ts, String topic,String liga, String event, Integer goalh, Integer goalg, Integer half, Integer hgoalh, Integer hgoalg, ArrayList<Scorer> scorer)
    {
    	int found = 0;
    	
    	String[] parts = topic.split("/");

		String topic1 = parts[4] + parts[5];
    	
    	// 0 = no change
    	// 1 = new entry
    	// 2 = Goal
    	// 3 = Halftime
    	// 4 = End
    	// 5 = Scorer
    	
    	if(event.equals("START"))
    	{
    		this.type = 1; 
    	}
    	if(event.equals("GOALH"))
    	{
    		this.type = 2; 
    	}
    	if(event.equals("GOALG"))
    	{
    		this.type = 2; 
    	}
    	if(event.equals("SCORER"))
    	{
    		this.type = 5; 
    	}
    	if(event.equals("HALF"))
    	{
    		this.type = 3; 
    	}
    	if(event.equals("END"))
    	{
    		this.type = 4; 
    	}
    	
		publish=false;
   		for(int i = 0; i<this.mygames.size(); i++)
    	{
			this.setChanged(i, 0);
			if(this.mygames.get(i).topic.equals(topic1))
			{
				if(this.getTS(i) < ts)
				{
					if(this.type == 4)
   					{
   						this.setActive(i,ts,false);
   						setHalfTime(i,hgoalh.toString(),hgoalg.toString());
   						setScorer(i,scorer);
   					}
   					if(this.type==5)
   					{
   						setScorer(i,scorer);
   					}
					if(this.type==3)
					{
						setHalfTime(i,hgoalh.toString(),hgoalg.toString());
						setScorer(i,scorer);
					}
					if(this.type==2)
					{
						setScorer(i,scorer);
					}
							 					
   					changeGame(i,ts,goalh.toString(),goalg.toString());
   					setHighlight(i,true);
   					Log.i("Events","Changed: Topic: "+ topic1 +", Event: "+ event +", TS: "+ts);
   					this.setChanged(i, type);
   					pos=i;
   					publish=true;
   				}
				if(this.getTS(i) > ts)
				{
					Log.i("Events","Found (2): Topic: "+ topic1 +", Event: "+ event +", TS 1: "+ts+" TS 2: "+this.getTS(i));

					if(this.type==3)
					{
						setHalfTime(i,hgoalh.toString(),hgoalg.toString());
	   					setHighlight(i,true);
						publish=true;
					}
   					if(this.type == 4)
   					{
   						this.setActive(i,ts,false);
   						setHalfTime(i,hgoalh.toString(),hgoalg.toString());
   	   					setHighlight(i,true);
   						publish=true;
   					}
   					if(this.type==5)
   					{
   	   					setHighlight(i,true);
   						publish=true;
   					}
   					setScorer(i,scorer);
   					pos=i;
				}
    			found = 1;
    		}
    	}
 
   		if(found==0)
   		{
   			this.mygames.add(0,new games(ts, topic1,liga,goalh.toString(),goalg.toString()));
   			setScorer(0,scorer);
			if(this.type == 4)
			{
				setHalfTime(0,hgoalh.toString(),hgoalg.toString());
				this.setActive(0,ts,false);
				this.setChanged(0, 0);
			}
			if(half==1)
			{
				setHalfTime(0,hgoalh.toString(),hgoalg.toString());
				this.setChanged(0, 0);
			}
   			publish=true;
// 			Log.i("Events","New: Topic: "+this.topic+", Event: "+this.event+", TS: "+ts);
   			Collections.sort(this.mygames,new TimestampSorter());
   			pos=0;
   		}

   		return this;
    }


    public int getChanged(int i)
    {
            return this.mygames.get(i).changed;
    }

    public String getScore(int i)
    {
    	String text = this.mygames.get(i).goalh + " : "+ this.mygames.get(i).goalg;
    	if(!this.mygames.get(i).halfH.equals(""))
    	{
    		text=text+"  ("+this.mygames.get(i).halfH+" : "+this.mygames.get(i).halfG+")";
    	}
    	return text;
    }
    public String getScorer(int i)
    {
            return this.mygames.get(i).allscorer;
    }
    
    @SuppressLint("NewApi")
	public void setScorer(int i,ArrayList<Scorer> scorer)
    {
    	this.mygames.get(i).allscorer = "";
    	for(int t=scorer.size();t>0;t--)
    	{
    		this.mygames.get(i).allscorer += scorer.get(t-1).name + " (" + scorer.get(t-1).minute + ".)";
    		if(t>1)
    		{
    			this.mygames.get(i).allscorer += ", ";
    		}
    	}
    }

    public boolean getHighlight(int i)
    {
            return this.mygames.get(i).highlight;
    }
    
    public String getTopic(int i)
    {
            return this.mygames.get(i).topic;
    }
    public long getTS(int i)
    {
    	return this.mygames.get(i).ts;
    }
    public int getCount()
	{
		return this.mygames.size();
    }

    public void setHalfTime(int i,String goalh, String goalg)
    {
        this.mygames.get(i).halfH=goalh;
        this.mygames.get(i).halfG=goalg;
    }
    
    public void changeGame(int i,long ts, String goalh, String goalg)
    {
    		this.mygames.get(i).ts=ts;
            this.mygames.get(i).goalh = goalh;
            this.mygames.get(i).goalg = goalg;
            if(this.mygames.get(i).active)
            {
            	this.mygames.get(i).changed = 1;
            }
   }
    
    public void setChanged(int i, int status)
    {
            this.mygames.get(i).changed = status;
    }

    public void setHighlight(int i,boolean status)
    {
            this.mygames.get(i).highlight = status;
    }

    
    private void setActive(int i, long ts, boolean active)
    {
   			this.mygames.get(i).ts=ts;
            this.mygames.get(i).active = active;
    }
    
    public boolean getActive(int i)
    {
            return this.mygames.get(i).active;
    } 
}

class TimestampSorter implements Comparator<games> {
	   public int compare(games one, games another){
	       return (int)(another.sortTS() - one.sortTS());
	   }
	}

class games implements Serializable {
// 0 = no change
// 1 = new entry
// 2 = Goal
// 3 = Halftime
// 4 = End
// 5 = Scorer
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 7276157722721043183L;
	String liga;
	String topic;
    String goalh;
    String goalg;
    String allscorer;
    long ts;
    String halfH;
    String halfG;
    boolean highlight;
    
    int changed;
    boolean active;
    public games(long ts, String topic, String liga, String goalh, String goalg)
    {
    		this.ts=ts;
			this.liga = liga;
    		this.active = true;
    		this.changed = 1;
    		this.topic = topic; 
            this.goalh = goalh;
            this.goalg = goalg;
            this.halfH="";
            this.halfG="";
            this.allscorer = "";
            this.highlight=false;
    }
    
    public long sortTS()
    {
    	return ts;
    }
}
