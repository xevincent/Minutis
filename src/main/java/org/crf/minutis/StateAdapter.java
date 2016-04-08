package org.crf.minutis;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class StateAdapter extends BaseAdapter {

	private int statePosition;
	private State[] mStates;
	private final LayoutInflater mInflater;

	static class ViewHolder {
		ImageView icon;
		TextView text;
	}

	public StateAdapter(Context context, int statePosition) {
		super();
		mStates = State.values();
		this.statePosition = statePosition;
		mInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return mStates.length;
	}

	public Object getItem(int position) {
		return mStates[position];
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (position >= mStates.length) {
			return null;
		}

		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.state_row, null);

			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.state_text);
			holder.icon = (ImageView) convertView.findViewById(R.id.state_icon);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		State state = mStates[position];
		if (state != null) {
			holder.text.setText(state.text);
			holder.icon.setImageResource(state.icon);
			if (position == statePosition) {
				holder.text.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
				holder.text.setTypeface(Typeface.DEFAULT);
			}
		}

		return convertView;
	}
}
