package com.holger.mqttliga;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.Html;
import android.util.AttributeSet;

public class SettingsDialog extends DialogPreference {

        public SettingsDialog(Context context, AttributeSet attrs) {
                super(context, attrs);
        }

        public SettingsDialog(Context context, AttributeSet attrs, int defStyle) {
                super(context, attrs, defStyle);
        }

        @Override
        public void setDialogMessage(CharSequence dialogMessage)
        {
                super.setDialogMessage(Html.fromHtml((String) dialogMessage));
        }


}
