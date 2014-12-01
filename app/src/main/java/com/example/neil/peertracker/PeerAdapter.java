package com.example.neil.peertracker;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class PeerAdapter extends ArrayAdapter<Peer> implements ListAdapter{
    private LayoutInflater m_layoutInflater;
    private List<Peer> m_properties;

    public PeerAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        m_properties = new ArrayList<Peer>();
    }

    public void setLayoutInflator(LayoutInflater layoutInflater) {
        m_layoutInflater = layoutInflater;
    }

    public void addDevice(Peer newDevice) {
        boolean found = false;
        Peer oldDevice = null;

        for (Peer m_property : m_properties) {
            oldDevice = m_property;
            if (oldDevice == null || newDevice == null) {
                return;
            }

            if (oldDevice.uuid.equals(newDevice.uuid)) {
                found = true;
                oldDevice.aboutMap = newDevice.aboutMap;
                oldDevice.busName = newDevice.busName;
                oldDevice.displayName = oldDevice.displayName;
                oldDevice.interfaces = newDevice.interfaces;
                oldDevice.port = newDevice.port;
                oldDevice.updateSupportedDevices();
                break;
            }
        }

        if(!found){
            m_properties.add(newDevice);
        }
    }

    public void removeByBusName(String busName) {
        for(int i = 0; i < m_properties.size(); i++){
            Peer device = m_properties.get(i);
            if(device != null && device.busName.equals(busName)){
                m_properties.remove(i);
                break;
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row;

        if (convertView == null)
            row = m_layoutInflater.inflate(R.layout.peer_property, parent, false);
        else
            row = convertView;

        final Peer property = m_properties.get(position);
        if(property != null){
            //Property name
            TextView propertyName = (TextView) row.findViewById(R.id.propertyName);
            propertyName.setText(property.busName);

            //Property value
            TextView propertyValue = (TextView) row.findViewById(R.id.propertyValue);
            propertyValue.setText(property.displayName);
        }
        return row;
    }

    @Override
    public int getCount() {
        if (isEmpty()) {
            return 0;
        }
        return m_properties.size();
    }

    @Override
    public Peer getItem(int position) {
        if (isEmpty())
            return null;
        return m_properties.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (isEmpty())
            return -1;
        return m_properties.get(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return (m_properties == null || m_properties.size()==0);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver arg0) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver arg0) {

    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int arg0) {
        return true;
    }

    @Override
    public void clear() {
        super.clear();
        if(m_properties != null)
            m_properties.clear();
    }
}
