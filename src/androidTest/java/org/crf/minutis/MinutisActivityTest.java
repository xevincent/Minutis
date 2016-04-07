package org.crf.minutis;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class org.crf.minutis.MinutisActivityTest \
 * org.crf.minutis.tests/android.test.InstrumentationTestRunner
 */
public class MinutisActivityTest extends ActivityInstrumentationTestCase2<MinutisActivity> {

    public MinutisActivityTest() {
        super("org.crf.minutis", MinutisActivity.class);
    }

}
