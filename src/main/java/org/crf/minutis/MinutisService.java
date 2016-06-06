package org.crf.minutis;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.IO;
import io.socket.client.Socket ;
import io.socket.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class MinutisService extends Service {

	public static final String CONNECTION_ERROR = "connection_error";
	public static final String CONNECTION_SUCCESS = "connection_success";
	public static final String RADIO_CODE_UPDATED = "radio_code_updated";
	public static final String STATE_UPDATED = "state_updated";

	private final IBinder mBinder = new LocalBinder();
	private Socket ioSocket;
	private State mState;
	private String mRadioCode;

	public class LocalBinder extends Binder {
		MinutisService getService() {
			return MinutisService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		String url = sp.getString(SettingsFragment.KEY_SERVER_ADDRESS, "").trim();
		if (url.isEmpty()) {
			url = BuildConfig.MINUTIS_URL;
		}
		try {
			ioSocket = IO.socket(url);
		} catch(URISyntaxException ex) {}

		ioSocket.on(Socket.EVENT_CONNECT, onConnect);
		ioSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
		ioSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
		ioSocket.on("update", onUpdate);

		ioSocket.connect();

		mRadioCode = getString(R.string.radio_code_undefined);

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		ioSocket.off(Socket.EVENT_CONNECT, onConnect);
		ioSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
		ioSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
		if(ioSocket.connected()) {
			ioSocket.disconnect();
		}
		super.onDestroy();
	}

	private void startForeground() {
		int icon;
		String text;
		if (mState == null) {
			icon = R.drawable.ic_person_pin_black_24dp;
			text = "";
		} else {
			icon = mState.icon;
			text = getString(mState.text);
		}
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_logo))
		    .setSmallIcon(icon)
		    .setContentTitle(getString(R.string.app_name))
		    .setContentText(text);

		Intent intent = new Intent(this, MinutisActivity.class);
		PendingIntent pIntent =
		    PendingIntent.getActivity(this, 0, intent,
		                              PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(pIntent);
		startForeground(314159, mBuilder.build());
	}

	public boolean isConnected() {
		return ioSocket.connected();
	}

	public String getRadioCode() {
		return mRadioCode;
	}

	public State getState() {
		return mState;
	}

	public boolean updateState(int newStateCode) {
		if (!ioSocket.connected()) {
			return false;
		}
		JSONObject state = new JSONObject();
		try {
			state.put("status", newStateCode);
		} catch(JSONException ex) {}
		ioSocket.emit("update", state);

		return true;
	}

	public void updateGPS() {
		String[] lat = {
			"48.85894393201113",
			"48.858322765835325",
			"48.86193671554044",
			"48.87322862657411"
		};
		String[] lng = {
			"2.2931814193725586",
			"2.312922477722168",
			"2.334723472595215",
			"2.2953271865844727"
		};
		for (int index = 0; index < lat.length; index++) {
			final JSONObject position = new JSONObject();
			JSONObject gps = new JSONObject();
			try {
				gps.put("lat", lat[index]);
				gps.put("lng", lng[index]);
				position.put("position", gps);
			} catch(JSONException ex) {}
			new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						// this code will be executed after 2 seconds
						ioSocket.emit("update", position);
					}
				}, index * 3000);
		}
	}

	private void notifyChanges (String action) {
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
		bm.sendBroadcast(new Intent(action));
	}

	private Emitter.Listener onConnect = new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				SharedPreferences sp =
				    PreferenceManager.getDefaultSharedPreferences(MinutisService.this);
				JSONObject phone = new JSONObject();
				try {
					phone.put("phone", sp.getString(SettingsFragment.KEY_PHONE_NUMBER, ""));
				} catch(JSONException ex) {}
				ioSocket.emit("register", phone);
				startForeground();
				notifyChanges(CONNECTION_SUCCESS);
			}
		};

	private Emitter.Listener onConnectError = new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				notifyChanges(CONNECTION_ERROR);
				stopSelf();
			}
		};

	private Emitter.Listener onUpdate = new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject json = (JSONObject) args[0];
				if (json.has("status")) {
					updateState(json);
				} else if (json.has("indicatif")) {
					updateRadioCode(json);
				}

				startForeground();
			}

			private void updateState(JSONObject json) {
				int newStateCode = -1;
				try {
					newStateCode = json.getInt("status");
				} catch(JSONException ex) {}
				for(State state: State.values()) {
					if (state.code == newStateCode) {
						mState = state;
						notifyChanges(STATE_UPDATED);
						break;
					}
				}
			}

			private void updateRadioCode(JSONObject json) {
				String newRadioCode = "";
				try {
					newRadioCode = json.getString("indicatif");
				} catch(JSONException ex) {}
				if (!newRadioCode.isEmpty()) {
					mRadioCode = newRadioCode;
					notifyChanges(RADIO_CODE_UPDATED);
				}
			}
		};
}
