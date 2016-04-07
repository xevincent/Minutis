package org.crf.minutis;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class MessagesAdapter extends BaseAdapter {

    private List<Message> mLocalList;
    private final LayoutInflater mInflater;

    static class ViewHolder {
		ImageView address;
		ImageView type;
        TextView date;
        TextView content;
    }

    public MessagesAdapter(Context context, List<Message> appList) {
        super();
        mLocalList = appList;
        mInflater = LayoutInflater.from(context);
    }

    public int getCount() {
        return mLocalList.size();
    }

    public Object getItem(int position) {
        return mLocalList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= mLocalList.size()) {
            return null;
		}

        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.message, null);

            holder = new ViewHolder();
            holder.content = (TextView) convertView.findViewById(R.id.message_content);
            holder.date = (TextView) convertView.findViewById(R.id.message_date);
            holder.address = (ImageView) convertView.findViewById(R.id.message_address);
            holder.type = (ImageView) convertView.findViewById(R.id.message_image);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Message message = (Message) mLocalList.get(position);
        if (message != null) {
            holder.content.setText(message.message);
            holder.date.setText(message.date);
            holder.type.setImageResource(message.type.image);
			if (message.address.isEmpty()) {
				holder.address.setVisibility(View.INVISIBLE);
			} else {
				holder.address.setVisibility(View.VISIBLE);
			}
        }

        return convertView;
    }
}
