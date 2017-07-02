package com.opendev.securifi;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DoubleListAdapter extends BaseAdapter {
	Context _context;
	List<String> _items = new ArrayList<String>();
	List<String> _subitems = new ArrayList<String>();

	public DoubleListAdapter(Context context, List<String> items, List<String> subitems) {
		this._context = context;
		this._items = (items);
		this._subitems = subitems;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) this._context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.listitem, null);
		}
		String item = _items.get(position);
		String subitems = _subitems.get(position);

		TextView tv1 = (TextView) v.findViewById(R.id.tvitem);
		TextView tv2 = (TextView) v.findViewById(R.id.tvsubitem);

		tv1.setText(item);
		tv1.setTextColor(Color.BLACK);
		tv2.setText(subitems);

		return v;

	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return _items.size();
	}

	@Override
	public String getItem(int arg0) {
		// TODO Auto-generated method stub
		return _items.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
}
