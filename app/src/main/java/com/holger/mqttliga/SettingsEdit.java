package com.holger.mqttliga;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class SettingsEdit extends EditTextPreference {

            /* ---------------------------- Constructors ---------------------------- */

            public SettingsEdit(final Context ctx, final AttributeSet attrs)
            {
                super(ctx, attrs);
            }
            public SettingsEdit(final Context ctx)
            {
                super(ctx);
            }

	    /* ----------------------------- Overrides ------------------------------ */

	    @Override
	    public void setText(final String value)
	    {
	        super.setText(value);
	        setSummary(getText());
	    }
}
