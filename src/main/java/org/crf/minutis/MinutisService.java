package org.crf.minutis;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.Manifest.permission;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket ;
import io.socket.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

public class MinutisService extends Service {

	public static final String CONNECTION_ERROR = "connection_error";
	public static final String CONNECTION_SUCCESS = "connection_success";
	public static final String GPS_DISABLED = "gps_disabled";
	public static final String MESSAGES_UPDATED = "messages_updated";
	public static final String RADIO_CODE_UPDATED = "radio_code_updated";
	public static final String STATE_UPDATED = "state_updated";

	private final IBinder mBinder = new LocalBinder();
	private boolean mMinutisRequestedGpsActivation;
	private HandlerThread mHandlerThread;
	private LocationManager mLocationManager;
	private SharedPreferences mPrefs;
	private Socket ioSocket;
	private State mState;
	private String mRadioCode = "";

	public class LocalBinder extends Binder {
		MinutisService getService() {
			return MinutisService.this;
		}
	}

	LocationListener mLocationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (!ioSocket.connected()) {
				return;
			}
			JSONObject position = new JSONObject();
			JSONObject data = new JSONObject();
			try {
				position.put("lat", location.getLatitude());
				position.put("lng", location.getLongitude());
				data.put("position", position);
			} catch(JSONException ex) {}
			ioSocket.emit("update", data);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {}

		public void onProviderDisabled(String provider) {}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String url = mPrefs.getString(SettingsFragment.KEY_SERVER_ADDRESS, "").trim();
		if (url.isEmpty()) {
			url = BuildConfig.MINUTIS_URL;
		}
		try {
			ioSocket = IO.socket(url);
		} catch(URISyntaxException ex) {}

		ioSocket.on(Socket.EVENT_CONNECT, onConnect);
		ioSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
		ioSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
		ioSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
		ioSocket.on(Socket.EVENT_RECONNECT, onReconnect);
		ioSocket.on("update", onUpdate);
		ioSocket.on("message", onMessage);

		ioSocket.connect();

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		ioSocket.emit("unregister", new JSONObject());
		mLocationManager.removeUpdates(mLocationListener);
		if (mHandlerThread != null) {
			mHandlerThread.quit();
		}
		stopForeground(true);
		ioSocket.off(Socket.EVENT_CONNECT, onConnect);
		ioSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
		ioSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
		ioSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
		ioSocket.off(Socket.EVENT_RECONNECT, onReconnect);
		ioSocket.off("update", onUpdate);
		ioSocket.off("message", onMessage);
		if(ioSocket.connected()) {
			ioSocket.disconnect();
		}
		super.onDestroy();
	}

	private void startForeground() {
		startForeground(false);
	}

	private void startForeground(boolean notifyNewMessage) {
		int icon;
		String text;
		if (mState == null) {
			icon = R.drawable.ic_person_pin_white_24dp;
			text = "";
		} else {
			icon = mState.iconNotif;
			text = getString(mState.text);
		}
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
		    .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_logo))
		    .setSmallIcon(icon)
		    .setContentTitle(getString(R.string.app_name))
		    .setContentText(text);

		if (notifyNewMessage) {
			boolean buzzer = mPrefs.getBoolean("notif_buzzer", true);
			boolean sound = mPrefs.getBoolean("notif_sound", true);
			if (buzzer && sound) {
				builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
			} else if (buzzer) {
				builder.setDefaults(Notification.DEFAULT_VIBRATE);
			} else if (sound) {
				builder.setDefaults(Notification.DEFAULT_SOUND);
			}
		}
		Intent intent = new Intent(this, MinutisActivity.class);
		PendingIntent pIntent =
		    PendingIntent.getActivity(this, 0, intent,
		                              PendingIntent.FLAG_UPDATE_CURRENT);

		builder.setContentIntent(pIntent);
		startForeground(314159, builder.build());
	}

	private void startLocation() {
		if (ContextCompat.checkSelfPermission(this,
		    permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
		    && ContextCompat.checkSelfPermission(this,
		    permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		if (!mPrefs.getBoolean("enable_gps", true)) {
			return;
		}
		int mode = Settings.Secure.getInt(getContentResolver(),
		    Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
		if (mode == Settings.Secure.LOCATION_MODE_OFF) {
			notifyChanges(GPS_DISABLED);
			mMinutisRequestedGpsActivation = true;
		}
		mLocationManager.removeUpdates(mLocationListener);
		long minTime = mState == null ? 6000L : mState.locationUpdateInterval;
		if (mHandlerThread == null) {
			mHandlerThread = new HandlerThread("MyLocationHandlerThread");
			mHandlerThread.start();
		}

		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		    minTime, 0, mLocationListener, mHandlerThread.getLooper());
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

	public void sendMessage(String content) {
		long date = System.currentTimeMillis();
		ContentValues cv = new ContentValues();
		cv.put("type", -1);
		cv.put("date", date);
		cv.put("content", content);

		MessageDBHelper helper = new MessageDBHelper(MinutisService.this);
		SQLiteDatabase db = helper.getWritableDatabase();
		db.insert("messages", null, cv);
		helper.close();

		JSONObject message = new JSONObject();
		try {
			message.put("text", content);
			message.put("time", date);
		} catch(JSONException ex) {}
		ioSocket.emit("message", message);
		notifyChanges(MESSAGES_UPDATED);
	}

	public boolean minutisRequestedGpsActivation() {
		return mMinutisRequestedGpsActivation;
	}

	private void notifyChanges (String action) {
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
		bm.sendBroadcast(new Intent(action));
	}

	private Emitter.Listener onConnect = new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				JSONObject phone = new JSONObject();
				try {
					phone.put("phone", mPrefs.getString(SettingsFragment.KEY_PHONE_NUMBER, ""));
				} catch(JSONException ex) {}
				ioSocket.emit("register", phone);
				startForeground();
				notifyChanges(CONNECTION_SUCCESS);
				ioSocket.off(Socket.EVENT_CONNECT, onConnect);
				ioSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
				ioSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
			}
		};

	private Emitter.Listener onConnectError = new Emitter.Listener() {
			@Override
			public void call(Object... args) {
				notifyChanges(CONNECTION_ERROR);
			}
		};

	private Emitter.Listener onDisconnect = new Emitter.Listener() {
		@Override
		public void call(Object... args) {
			mLocationManager.removeUpdates(mLocationListener);
		}
	};

	private Emitter.Listener onReconnect = new Emitter.Listener() {
		@Override
		public void call(Object... args) {
			JSONObject phone = new JSONObject();
			try {
				phone.put("phone", mPrefs.getString(SettingsFragment.KEY_PHONE_NUMBER, ""));
			} catch(JSONException ex) {}
			ioSocket.emit("register", phone);
			startLocation();
			// TODO send unsend messages
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
						startLocation();
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

	private Emitter.Listener onMessage = new Emitter.Listener() {
		@Override
		public void call(Object... args) {
			JSONObject json = (JSONObject) args[0];
			try {
				ContentValues cv = new ContentValues();
				cv.put("type", json.getInt("type"));
				cv.put("date", json.getLong("time"));
				cv.put("content", json.getString("text"));
				if (json.has("address")) {
					cv.put("address", json.getString("address"));
				}

				MessageDBHelper helper = new MessageDBHelper(MinutisService.this);
				SQLiteDatabase db = helper.getWritableDatabase();
				db.insert("messages", null, cv);
				helper.close();
				notifyChanges(MESSAGES_UPDATED);
				startForeground(true);
			} catch(JSONException ex) {}
		}
	};
}
