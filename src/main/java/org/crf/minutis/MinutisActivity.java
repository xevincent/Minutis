package org.crf.minutis;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;



/* TODO
 * - reset screen when disconnected
 * - selector for edit, direction and connection
 * - check @ and phone number when connection
 */
public class MinutisActivity extends AppCompatActivity {

	private boolean isConnected;
	private int statePosition = -1;
	private ArrayList<Message> messages;
	private ListView lv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		messages = new ArrayList<Message>();

		lv = (ListView) findViewById(R.id.list_messages);
		lv.setEmptyView(findViewById(R.id.empty_list));
		MessagesAdapter adapter = new MessagesAdapter(this, messages);
		lv.setAdapter(adapter);

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		if (!sp.contains(SettingsFragment.KEY_SERVER_ADDRESS)) {
			sp.edit().putString(SettingsFragment.KEY_SERVER_ADDRESS,
			                    getString(R.string.default_server_address)).apply();
		}
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
        default:
            return super.onOptionsItemSelected(item);
		}
	}

	private void showConnect() {
		int message = isConnected ? R.string.connect_is_connected :
			R.string.connect_is_not_connected;
		int action = isConnected ? R.string.connect_disconnect :
			R.string.connect_connect;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
			.setPositiveButton(action, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						isConnected = !isConnected;
					}
				})
			.setNegativeButton(R.string.all_cancel, null);
        builder.create().show();

	}

	private void startSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void setStatus(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.main_select_state)
			.setAdapter(new StateAdapter(this, statePosition), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						statePosition = which;
						State state = State.values()[which];
						TextView tv = (TextView) findViewById (R.id.state_value);
						tv.setText(state.text);
						ImageView iv = (ImageView) findViewById (R.id.state_icon);
						iv.setImageResource(state.icon);
					}
				});
		builder.create().show();
	}

	public void startNavigation(View v) {
		int position = lv.getPositionForView(v);
		String address = messages.get(position).address;
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("geo:0,0?q=" + address));
		if (intent.resolveActivity(getPackageManager()) != null) {
			startActivity(intent);
		} else {
			Snackbar snackbar = Snackbar.make(lv, R.string.no_navigation_app, Snackbar.LENGTH_LONG);
			int snackbarTextId = android.support.design.R.id.snackbar_text;
			TextView tv = (TextView) snackbar.getView().findViewById(snackbarTextId);
			tv.setTextColor(getResources().getColor(R.color.accent));
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			snackbar.show();
		}
	}

	private void addLoremIpsum() {
		((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
	}
}
