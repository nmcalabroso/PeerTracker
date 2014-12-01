package com.example.neil.peertracker;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;

public class NetworkAdapter extends ArrayAdapter<ScanResult> implements ListAdapter {

    public final static String TAG = "NetworkAdapter";

    static class ViewHolder {
        public TextView textNetworkName;
        public TextView textNetworkFeatures;
        public TextView textNetworkLevel;
    }

    private LayoutInflater m_layoutInflater;

    public NetworkAdapter(Context context, int resource) {
        super(context, resource);
        m_layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        if (rowView == null) {
            rowView = m_layoutInflater.inflate(R.layout.network_property, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.textNetworkName = (TextView) rowView.findViewById(R.id.network_name_row_textview);
            viewHolder.textNetworkFeatures = (TextView) rowView.findViewById(R.id.network_features);
            viewHolder.textNetworkLevel = (TextView) rowView.findViewById(R.id.network_level);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.textNetworkName.setText(getItem(position).SSID);
        holder.textNetworkFeatures.setText(getItem(position).capabilities);
        int level = WifiManager.calculateSignalLevel(getItem(position).level, 100) + 1;
        holder.textNetworkLevel.setText(Integer.toString(level)+"%");
        return rowView;
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }
}
