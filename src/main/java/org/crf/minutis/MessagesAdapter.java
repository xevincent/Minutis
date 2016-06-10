package org.crf.minutis;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ResourceCursorAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MessagesAdapter extends ResourceCursorAdapter {

	static class ViewHolder {
		ImageView address;
		ImageView type;
		TextView date;
		TextView content;
	}

	private SimpleDateFormat sdf;

	public MessagesAdapter(Context context, int layout, Cursor c, int flags) {
		super(context, layout, c, flags);
		sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = super.newView(context, cursor, parent);

		ViewHolder holder = new ViewHolder();
		holder.content = (TextView) view.findViewById(R.id.message_content);
		holder.date = (TextView) view.findViewById(R.id.message_date);
		holder.address = (ImageView) view.findViewById(R.id.message_address);
		holder.type = (ImageView) view.findViewById(R.id.message_image);
		view.setTag(holder);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		holder.content.setText(cursor.getString(3));
		holder.date.setText(sdf.format(new Date(cursor.getLong(2))));
		int icon;
		int type = cursor.getInt(1);
		switch(type) {
		case 0:
			icon = R.drawable.ic_person_black_24dp;
			break;
		case 1:
			icon = R.drawable.ic_group_black_24dp;
			break;
		case -1:
			icon = R.drawable.ic_send_black_24dp;
			break;
		default:
			// Should never happen
			icon = R.drawable.ic_message_black_24dp;
			break;
		}
		holder.type.setImageResource(icon);
		if (cursor.getString(4).isEmpty()) {
			holder.address.setVisibility(View.INVISIBLE);
		} else {
			holder.address.setVisibility(View.VISIBLE);
		}
	}
}
