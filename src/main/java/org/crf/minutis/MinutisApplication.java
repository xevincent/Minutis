package org.crf.minutis;

import android.app.Application;
import android.content.Context;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(mailTo = "",
                mode = ReportingInteractionMode.TOAST,
                resToastText = R.string.crash_toast_text)
public class MinutisApplication extends Application {
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}
}
