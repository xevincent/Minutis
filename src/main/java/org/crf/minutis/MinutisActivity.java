package org.crf.minutis;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/* TODO
 * - reset screen when disconnected
 * - selector for edit, direction and connection
 * - check @ and phone number when connection
 */
public class MinutisActivity extends AppCompatActivity implements
    LoaderManager.LoaderCallbacks<Cursor>  {

	private boolean mIsBound;
	private ImageView mStateIcon;
	private ListView lv;
	private MessagesAdapter mAdapter;
	private MinutisService mService;
	private TextView mStateText;


	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (MinutisService.MESSAGE_RECEIVED.equals(intent.getAction())) {
				getLoaderManager().restartLoader(0, null, MinutisActivity.this);
			} else if (MinutisService.STATE_UPDATED.equals(intent.getAction())) {
				updateState();
			} else if (MinutisService.CONNECTION_SUCCESS.equals(intent.getAction())) {
				setStatus(null);
			} else if (MinutisService.CONNECTION_ERROR.equals(intent.getAction())) {
				showSnackbar(R.string.error_cannot_connect);
				disconnect();
			} else if (MinutisService.RADIO_CODE_UPDATED.equals(intent.getAction())) {
				updateRadioCode();
			}
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mStateText = (TextView) findViewById (R.id.state_value);
		mStateIcon = (ImageView) findViewById (R.id.state_icon);

		lv = (ListView) findViewById(R.id.list_messages);
		lv.setEmptyView(findViewById(R.id.empty_list));
		mAdapter = new MessagesAdapter(this, R.layout.message, null, 0);
		lv.setAdapter(mAdapter);
    }

	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter();
		filter.addAction(MinutisService.CONNECTION_ERROR);
		filter.addAction(MinutisService.CONNECTION_SUCCESS);
		filter.addAction(MinutisService.STATE_UPDATED);
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
		bm.registerReceiver(mReceiver, filter);

		bindIfServiceRunning();
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
		bm.unregisterReceiver(mReceiver);
		getLoaderManager().destroyLoader(0);
		super.onPause();
	}

	@Override
	protected void onStop() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
        case R.id.connect:
			showConnect();
            return true;
		case R.id.settings:
			startSettings();
			return true;
        case R.id.gps_test:
			if (mIsBound) {
				mService.updateGPS();
			}
            return true;
        default:
            return super.onOptionsItemSelected(item);
		}
	}

	private void showConnect() {
		int message = mIsBound ? R.string.connect_is_connected :
			R.string.connect_is_not_connected;
		int action = mIsBound ? R.string.connect_disconnect :
			R.string.connect_connect;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
			.setPositiveButton(action, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (mIsBound) {
							disconnect();
						} else {
							connect();
						}
					}
				})
			.setNegativeButton(R.string.all_cancel, null);
        builder.create().show();
	}

	private void fillPhoneNumber() {
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_phone_number_title)
		    .setView(getLayoutInflater().inflate(R.layout.dialog_phone_number, null))
		    .setPositiveButton(R.string.all_validate, new DialogInterface.OnClickListener() {
		    	public void onClick(DialogInterface dialog, int id) {
		    		EditText et = (EditText) ((AlertDialog) dialog).findViewById(R.id.phone_number);
		    		String phone = et.getText().toString();
		    		if (phoneNumberIsValid(phone)) {
		    			PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		    			try {
		    				PhoneNumber numberProto = phoneUtil.parse(phone, "FR");
		    				phone = phoneUtil.format(numberProto, PhoneNumberFormat.NATIONAL)
		    				    .replaceAll("\\s","");
		    			} catch (NumberParseException e) {
		    			}
		    			sp.edit().putString(SettingsFragment.KEY_PHONE_NUMBER, phone).apply();
		    			connect();
		    		} else {
		    			showSnackbar(R.string.error_cannot_connect_without_phone);
		    		}
		    	}
		    })
		    .setNegativeButton(R.string.all_cancel, new DialogInterface.OnClickListener() {
		    	public void onClick(DialogInterface dialog, int id) {
		    		showSnackbar(R.string.error_cannot_connect_without_phone);
		    	}
		    })
		    .setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.show();
	}

	private boolean phoneNumberIsValid(String phone) {
		boolean ret;
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber numberProto = phoneUtil.parse(phone, "FR");
			ret = phoneUtil.isValidNumber(numberProto);
		} catch (NumberParseException e) {
			ret = false;
		}
		return ret;
	}

	private void connect() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if (phoneNumberIsValid(sp.getString(SettingsFragment.KEY_PHONE_NUMBER, ""))) {
			Intent service = new Intent(this, MinutisService.class);
			startService(service);
			bindService(service, mConnection, 0);
		} else {
			fillPhoneNumber();
		}
	}

	private void disconnect() {
		Intent service = new Intent(this, MinutisService.class);
		stopService(service);
		mStateText.setText(R.string.state_undefined);
		mStateIcon.setImageResource(R.drawable.ic_person_pin_black_24dp);
		TextView radioCode = (TextView) findViewById (R.id.radio_code_value);
		radioCode.setText("");
	}

	private void startSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void setStatus(View v) {
		if (!mIsBound) {
			showSnackbar(R.string.error_connect_first);
			return;
		}
		int currentStateCode = -1;
		State state = mService.getState();
		if (state != null) {
			currentStateCode = state.code;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.main_select_state)
		    .setCancelable(currentStateCode != -1)
		    .setAdapter(new StateAdapter(this, currentStateCode),
		                new DialogInterface.OnClickListener() {
		    	public void onClick(DialogInterface dialog, int which) {
		    		State state = State.values()[which];
		    		mService.updateState(state.code);
		    	}
		    });
		builder.create().show();
	}

	public void updateState() {
		State state = mService.getState();
		if (state != null) {
			mStateText.setText(state.text);
			mStateIcon.setImageResource(state.icon);
		}
	}

	public void updateRadioCode() {
		TextView radioCode = (TextView) findViewById (R.id.radio_code_value);
		radioCode.setText(mService.getRadioCode());
	}

	public void startNavigation(View v) {
		int position = lv.getPositionForView(v);
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		String address = cursor.getString(4);
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("geo:0,0?q=" + address));
		if (intent.resolveActivity(getPackageManager()) != null) {
			startActivity(intent);
		} else {
			showSnackbar(R.string.no_navigation_app);
		}
	}

	private void showSnackbar(int res) {
		Snackbar sb = Snackbar.make(lv, res, Snackbar.LENGTH_LONG);
		int sbTextId = android.support.design.R.id.snackbar_text;
		TextView tv = (TextView) sb.getView().findViewById(sbTextId);
		tv.setTextColor(getResources().getColor(R.color.accent));
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		sb.show();
	}

	private void bindIfServiceRunning() {
		boolean isRunning = false;
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo rsi : am.getRunningServices(Integer.MAX_VALUE)){
			if("org.crf.minutis.MinutisService".equals(rsi.service.getClassName())) {
				isRunning = true;
				break;
			}
		}

		if (isRunning) {
			Intent intent = new Intent(this, MinutisService.class);
			bindService(intent, mConnection, 0);
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, null, null, null, null, "date DESC") {
			@Override
			public Cursor loadInBackground() {
				MessageDBHelper helper = new MessageDBHelper(MinutisActivity.this);
				SQLiteDatabase db = helper.getReadableDatabase();
				return db.query("messages", getProjection(), getSelection(),
				                getSelectionArgs(), null, null, getSortOrder(), null );
			}
		};
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data == null) {
			return;
		} else {
			mAdapter.changeCursor(data);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MinutisService.LocalBinder binder = (MinutisService.LocalBinder) service;
			mService = binder.getService();
			mIsBound = true;
			updateState();
			updateRadioCode();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mIsBound = false;
		}
	};

}
