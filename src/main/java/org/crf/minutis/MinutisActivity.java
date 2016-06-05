package org.crf.minutis;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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

import java.util.ArrayList;


/* TODO
 * - reset screen when disconnected
 * - selector for edit, direction and connection
 * - check @ and phone number when connection
 */
public class MinutisActivity extends AppCompatActivity {

	private boolean mIsBound;
	private ArrayList<Message> messages;
	private ImageView mStateIcon;
	private ListView lv;
	private MinutisService mService;
	private TextView mStateText;


	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (MinutisService.STATE_UPDATED.equals(intent.getAction())) {
				updateState();
			} else if (MinutisService.CONNECTION_SUCCESS.equals(intent.getAction())) {
				setStatus(null);
			} else if (MinutisService.CONNECTION_ERROR.equals(intent.getAction())) {
				showSnackbar(R.string.error_cannot_connect);
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

		messages = new ArrayList<Message>();

		lv = (ListView) findViewById(R.id.list_messages);
		lv.setEmptyView(findViewById(R.id.empty_list));
		MessagesAdapter adapter = new MessagesAdapter(this, messages);
		lv.setAdapter(adapter);
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
	}

	@Override
	protected void onPause() {
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
		bm.unregisterReceiver(mReceiver);
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
        case R.id.lorem_ipsum:
			addLoremIpsum();
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

	private void connect() {
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if (sp.getString(SettingsFragment.KEY_PHONE_NUMBER, "").isEmpty()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.dialog_phone_number_title)
			    .setView(getLayoutInflater().inflate(R.layout.dialog_phone_number, null))
			    .setPositiveButton(R.string.all_validate, new DialogInterface.OnClickListener() {
			    	public void onClick(DialogInterface dialog, int id) {
			    		EditText et = (EditText) ((AlertDialog) dialog).findViewById(R.id.phone_number);
			    		String phone = et.getText().toString().replaceAll("\\s","");
			    		sp.edit().putString(SettingsFragment.KEY_PHONE_NUMBER, phone).apply();
			    	}
			    })
			    .setNegativeButton(R.string.all_cancel, null);
			AlertDialog dialog = builder.create();
			dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			dialog.show();
		}
		Intent service = new Intent(this, MinutisService.class);
		startService(service);
		bindService(service, mConnection, 0);
	}

	private void disconnect() {
		Intent service = new Intent(this, MinutisService.class);
		stopService(service);
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

	public void startNavigation(View v) {
		int position = lv.getPositionForView(v);
		String address = messages.get(position).address;
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("geo:0,0?q=" + address));
		if (intent.resolveActivity(getPackageManager()) != null) {
			startActivity(intent);
		} else {
			showSnackbar(R.string.no_navigation_app);
		}
	}

	private void addLoremIpsum() {
		((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
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

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MinutisService.LocalBinder binder = (MinutisService.LocalBinder) service;
			mService = binder.getService();
			mIsBound = true;
			LocalBroadcastManager bm = LocalBroadcastManager.getInstance(MinutisActivity.this);
			bm.sendBroadcast(new Intent(MinutisService.STATE_UPDATED));
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mIsBound = false;
		}
	};

}
