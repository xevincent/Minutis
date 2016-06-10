package org.crf.minutis;

import android.app.Application;
import android.content.Context;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(mailTo = BuildConfig.ACRA_EMAIL,
                mode = ReportingInteractionMode.DIALOG,
                resDialogText = R.string.crash_dialog_text,
                resDialogPositiveButtonText = R.string.all_send,
                resDialogTheme = R.style.AcraTheme_Dialog)
public class MinutisApplication extends Application {
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		if (!BuildConfig.ACRA_EMAIL.isEmpty()) {
			ACRA.init(this);
		}
	}
}
