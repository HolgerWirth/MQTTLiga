package com.holger.mqttliga;

import java.util.Locale;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class GameListAdapter extends BaseAdapter {

	private static Events listEvent;
	private final Context activity;
	private static LayoutInflater inflater=null;

	GameListAdapter(Context context, Events gameEvent) {
        activity = context;
        listEvent = gameEvent;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
    public int getCount() {
        return listEvent.getCount();
    }

    @Override
    public Object getItem(int position) {
//        return gameEvent.getTopic(position);
    	return listEvent.mygames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
        

    @Override
	public boolean hasStableIds(){
		return true;
	}


	@Override
	public int getItemViewType(int pos){
		return IGNORE_ITEM_VIEW_TYPE;
	}

	@Override
	public int getViewTypeCount(){
		return 1;
	}

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
		ViewHolder holder;
		View row=convertView;
		Resources res = container.getResources();

        if (convertView == null  || row.getTag() == null) {
    		row = inflater.inflate(R.layout.gamelist, container, false);
        	holder = new ViewHolder();
            holder.icon = row.findViewById(R.id.icon);
            holder.team1= row.findViewById(R.id.team1);
            holder.line1 = row.findViewById(R.id.firstLine);
            holder.team2= row.findViewById(R.id.team2);
            holder.line2 = row.findViewById(R.id.secondLine);
            holder.icon1 = row.findViewById(R.id.icon1);
            row.setTag(holder);
        } else {
			holder = (ViewHolder) row.getTag();
		}          

        String logo1;
        String logo2;

        logo1=listEvent.getTopic(position).substring(0,3);
        logo2=listEvent.getTopic(position).substring(3,6);

        int resID1 = res.getIdentifier(logo1.toLowerCase(Locale.GERMAN),"drawable","com.holger.mqttliga");
        int resID2 = res.getIdentifier(logo2.toLowerCase(Locale.GERMAN),"drawable","com.holger.mqttliga");
        
        holder.icon.setImageResource(resID1);          	

        if(listEvent.getActive(position))
        {
        	holder.line1.setTypeface(null, Typeface.BOLD);
        }
        holder.line1.setText(listEvent.getScore(position));
        holder.line2.setText(listEvent.getScorer(position));
        holder.icon1.setImageResource(resID2);
        holder.team1.setText("");
        holder.team2.setText("");
        
        if(MQTTLiga.tablet)
        {
        	try
        	{
        		holder.team1.setText(res.getString(res.getIdentifier(logo1, "string","com.holger.mqttliga")));
        	}
        	catch(Exception e)
        	{
        		holder.team1.setText(logo1);
        	}
        	try
        	{
        		holder.team2.setText(res.getString(res.getIdentifier(logo2, "string","com.holger.mqttliga")));
        	}
        	catch(Exception e)
        	{
        		holder.team2.setText(logo2);
        	}
        }
        else
        {
        	if(container.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        	{
        		int teamID1=res.getIdentifier(logo1+"_S", "string","com.holger.mqttliga");
        		int teamID2=res.getIdentifier(logo2+"_S", "string","com.holger.mqttliga");
        		
        		if(teamID1==0)
        		{
        			try
        			{
        				holder.team1.setText(res.getString(res.getIdentifier(logo1, "string","com.holger.mqttliga")));
        			}
        			catch(Exception e)
        			{
        				holder.team1.setText(logo1);
        			}
        		}
        		else
        		{
        			holder.team1.setText(res.getString(teamID1));
        		}
        		if(teamID2==0)
        		{
        			try
        			{
        				holder.team2.setText(res.getString(res.getIdentifier(logo2, "string","com.holger.mqttliga")));
        			}
        			catch(Exception e)
        			{
        				holder.team2.setText(logo2);
        			}
        		}
        		else
        		{
        			holder.team2.setText(res.getString(teamID2));
        		}
        	}
        }
        
        if(listEvent.getChanged(position)>0 && listEvent.getChanged(position)<5)
        {
        	Animation animation;
        	Log.i("MQTTListAdapter","Animation position: "+position+" Change: "+listEvent.getChanged(position));
       
        	if(listEvent.getChanged(position)==1)
        	{
        		animation = AnimationUtils.loadAnimation(activity.getApplicationContext(),R.anim.up_from_bottom);
        		row.startAnimation(animation);
        	}
        	else
        	{
        		animation = AnimationUtils.loadAnimation(activity.getApplicationContext(), R.anim.push_left_in);
        		row.startAnimation(animation);
        	}
        	
        }

        if(listEvent.getHighlight(position))
    	{
        	row.setBackgroundColor(0x8000ffff);
    	}

        return row;
        
    }

private class ViewHolder {
        ImageView icon;
        TextView team1;
		TextView line1;
		TextView team2;
		TextView line2;
		ImageView icon1;
    }
}
