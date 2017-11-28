package com.holger.mqttliga;

import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class GameListHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

//    public static final String                 DEBUG_TAG = "MQTTLiga"; // Debug TAG

    private final ImageView icon;
    private final TextView team1;
    private final TextView line1;
    private final TextView team2;
    private final TextView line2;
    private final ImageView icon1;
    private final TextView detail1;
    private final TextView detail2;

    private final LinearLayout scorerlists;
    private final TextView scorerListH;
    private final TextView scorerListG;
    private final LinearLayout detailteams;

    private int originalHeight = 0;
    private boolean isViewExpanded = false;

    GameListHolder(View itemView) {
        super(itemView);

        this.icon = itemView.findViewById(R.id.icon);
        this.team1= itemView.findViewById(R.id.team1);
        this.line1 = itemView.findViewById(R.id.firstLine);
        this.team2 = itemView.findViewById(R.id.team2);
        this.line2 = itemView.findViewById(R.id.secondLine);
        this.icon1 = itemView.findViewById(R.id.icon1);
        this.detail1 = itemView.findViewById(R.id.detail1);
        this.detail2 = itemView.findViewById(R.id.detail2);

        this.scorerlists = itemView.findViewById(R.id.scorerlists);
        this.scorerListH = itemView.findViewById(R.id.scorerlistH);
        this.scorerListG = itemView.findViewById(R.id.scorerlistG);
        this.detailteams = itemView.findViewById(R.id.detailteams);

        itemView.setOnClickListener(this);

        //View is not expanded
        if (!isViewExpanded) {
            // Set Views to View.GONE and .setEnabled(false)
            this.detailteams.setVisibility(View.GONE);
            this.scorerlists.setVisibility(View.GONE);
            this.scorerlists.setEnabled(false);
        }
    }

    void bindGameList(Events listEvent, int position) {
        String logo1;
        String logo2;
        Resources res = itemView.getContext().getResources();

        logo1=listEvent.getTopic(position).substring(0,3);
        logo2=listEvent.getTopic(position).substring(3,6);

        int resID1 = res.getIdentifier(logo1.toLowerCase(Locale.GERMAN),"drawable","com.holger.mqttliga");
        int resID2 = res.getIdentifier(logo2.toLowerCase(Locale.GERMAN),"drawable","com.holger.mqttliga");

        this.icon.setImageResource(resID1);

        if(listEvent.getActive(position))
        {
            this.line1.setTypeface(null, Typeface.BOLD);
        }
        this.line1.setText(listEvent.getScore(position));
        this.line2.setText(listEvent.getScorer(position));
        this.scorerListH.setText(listEvent.getScorerH(position));
        this.scorerListG.setText(listEvent.getScorerG(position));

        this.icon1.setImageResource(resID2);
        this.team1.setText("");
        this.team2.setText("");
        this.detail1.setText("");
        this.detail2.setText("");

        if(MQTTLiga.tablet)
        {
            this.detailteams.setVisibility(View.GONE);
            this.detailteams.setEnabled(false);
            this.scorerlists.setEnabled(false);
            try
            {
                this.team1.setText(res.getString(res.getIdentifier(logo1, "string","com.holger.mqttliga")));
            }
            catch(Exception e)
            {
                this.team1.setText(logo1);
            }
            try
            {
                this.team2.setText(res.getString(res.getIdentifier(logo2, "string","com.holger.mqttliga")));
            }
            catch(Exception e)
            {
                this.team2.setText(logo2);
            }
        }
        else
        {
            int teamID1=res.getIdentifier(logo1+"_S", "string","com.holger.mqttliga");
            int teamID2=res.getIdentifier(logo2+"_S", "string","com.holger.mqttliga");

            if(res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                this.detailteams.setVisibility(View.GONE);
                this.detailteams.setEnabled(false);

                if(teamID1==0)
                {
                    try
                    {
                        this.team1.setText(res.getString(res.getIdentifier(logo1, "string","com.holger.mqttliga")));
                    }
                    catch(Exception e)
                    {
                        this.team1.setText(logo1);
                    }
                }
                else
                {
                    this.team1.setText(res.getString(teamID1));
                }
                if(teamID2==0)
                {
                    try
                    {
                        this.team2.setText(res.getString(res.getIdentifier(logo2, "string","com.holger.mqttliga")));
                    }
                    catch(Exception e)
                    {
                        this.team2.setText(logo2);
                    }
                }
                else
                {
                    this.team2.setText(res.getString(teamID2));
                }
            }
            else
            {
                if(teamID1==0) {
                    this.detail1.setText(res.getIdentifier(logo1, "string", "com.holger.mqttliga"));
                }
                else {
                    this.detail1.setText(teamID1);
                }
                if(teamID2==0)
                {
                    this.detail2.setText(res.getIdentifier(logo2, "string", "com.holger.mqttliga"));
                }
                else {
                    this.detail2.setText(teamID2);
                }
            }
        }
        if(listEvent.getChanged(position)>0 && listEvent.getChanged(position)<5)
        {
            Animation animation;

            if(listEvent.getChanged(position)==1)
            {
                animation = AnimationUtils.loadAnimation(itemView.getContext(),R.anim.up_from_bottom);
                itemView.startAnimation(animation);
            }
            else
            {
                animation = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.push_left_in);
                itemView.startAnimation(animation);
            }
        }

        if(listEvent.getHighlight(position))
        {
            itemView.setBackgroundColor(0x8000ffff);
        }
    }

    @Override
    public void onClick(final View v) {
        Resources res = itemView.getContext().getResources();
        int extent=0;

        if (originalHeight == 0) {
            originalHeight = v.getHeight();
        }

        // Declare a ValueAnimator object
        ValueAnimator valueAnimator;
        if (!isViewExpanded) {
            this.line2.setVisibility(View.GONE);
            this.scorerlists.setVisibility(View.VISIBLE);
            this.scorerlists.setEnabled(true);
            if(res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                this.detailteams.setVisibility(View.GONE);
                this.detailteams.setEnabled(false);

            }
            else
            {
                this.detailteams.setVisibility(View.VISIBLE);
                this.detailteams.setEnabled(true);
                extent=55;
            }

            this.scorerListH.measure(0,0);
            this.scorerListG.measure(0,0);

            isViewExpanded = true;
            if(( (int) (originalHeight * 1.0))>(Math.max(this.scorerListH.getMeasuredHeight(),this.scorerListG.getMeasuredHeight()))+extent) {
                valueAnimator = ValueAnimator.ofInt(originalHeight, originalHeight + (int) (originalHeight * 1.0)); // These values in this method can be changed to expand however much you like
            }
            else
            {
                int offset = (Math.max(this.scorerListH.getMeasuredHeight(),this.scorerListG.getMeasuredHeight())+extent)-originalHeight;
                valueAnimator = ValueAnimator.ofInt(originalHeight,originalHeight+(int) (originalHeight * 1.0)+offset);
            }
        } else {
            isViewExpanded = false;
            valueAnimator = ValueAnimator.ofInt(originalHeight + (int) (originalHeight * 1.0), originalHeight);

            Animation a = new AlphaAnimation(1.00f, 0.00f); // Fade out

            a.setDuration(200);
            // Set a listener to the animation and configure onAnimationEnd
            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    line2.setVisibility(View.VISIBLE);
                    scorerlists.setVisibility(View.INVISIBLE);
                    scorerlists.setEnabled(false);
                    detailteams.setVisibility(View.GONE);
                    detailteams.setEnabled(false);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            // Set the animation on the custom view
            v.startAnimation(a);
        }
        valueAnimator.setDuration(200);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (Integer) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.start();
    }
}
