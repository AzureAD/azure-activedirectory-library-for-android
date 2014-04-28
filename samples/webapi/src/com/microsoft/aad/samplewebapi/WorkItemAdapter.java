package com.microsoft.aad.samplewebapi;

import java.util.Calendar;
import java.util.Date;

import android.R.color;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Adapter to bind a WorkItemAdapter List to a view
 */
public class WorkItemAdapter extends ArrayAdapter<WorkItem> {

	/**
	 * Adapter context
	 */
	Context mContext;

	/**
	 * Adapter View layout
	 */
	int mLayoutResourceId;

	public WorkItemAdapter(Context context, int layoutResourceId) {
		super(context, layoutResourceId);

		mContext = context;
		mLayoutResourceId = layoutResourceId;
	}

	/**
	 * Returns the view for a specific item on the list
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;

		final WorkItem currentItem = getItem(position);

		if (row == null) {
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			row = inflater.inflate(mLayoutResourceId, parent, false);
		}

		row.setTag(currentItem);
		final CheckBox checkBox = (CheckBox) row
				.findViewById(R.id.checkToDoItem);
		final TextView txtDate = (TextView) row
				.findViewById(R.id.textDueDate);

		final Calendar c = Calendar.getInstance();
		c.setTime(currentItem.getDueDate());

		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		@SuppressWarnings("deprecation")
		String gmtTime = currentItem.getDueDate().toGMTString();
		// set current date into textview
		txtDate.setText(gmtTime);
		
		checkBox.setText(currentItem.getTitle());
		checkBox.setChecked(currentItem.isComplete());
		checkBox.setEnabled(true);
		checkBox.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (checkBox.isChecked()) {
					checkBox.setEnabled(false);
					if (mContext instanceof ToDoActivity) {
						ToDoActivity activity = (ToDoActivity) mContext;
						activity.checkItem(currentItem);
					}
				}
			}
		});
		
		Date currentDate = new Date();
		row.setBackgroundResource(R.color.normal);
		if(currentItem.getDueDate().before(currentDate))
		{
			row.setBackgroundResource(R.color.highlight);	
		}
		
		return row;
	}

	
}
