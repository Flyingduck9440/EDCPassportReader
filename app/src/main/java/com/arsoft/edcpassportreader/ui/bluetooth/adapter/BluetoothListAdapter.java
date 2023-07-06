package com.arsoft.edcpassportreader.ui.bluetooth.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.arsoft.edcpassportreader.R;

import java.util.ArrayList;

public class BluetoothListAdapter extends RecyclerView.Adapter<BluetoothListAdapter.ViewHolder> {
    private final ArrayList<BluetoothData> localBluetoothData;
    BluetoothListAdapterListener listener;

    public interface BluetoothListAdapterListener {
        void onSwitchChange(Boolean enable, Integer position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mac;
        private final TextView name;
        private final SwitchCompat switchEnable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mac = (TextView) itemView.findViewById(R.id.bluetooth_list_item_tv_mac);
            name = (TextView) itemView.findViewById(R.id.bluetooth_list_item_tv_name);
            switchEnable = (SwitchCompat) itemView.findViewById(R.id.bluetooth_list_item_sw_enable);
        }

        public TextView getMac() { return mac; }
        public TextView getName() { return name; }
        public SwitchCompat getSwitchEnable() { return switchEnable; }
    }

    public BluetoothListAdapter(ArrayList<BluetoothData> data) {
        localBluetoothData = data;
    }

    @NonNull
    @Override
    public BluetoothListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BluetoothListAdapter.ViewHolder holder, int position) {
        holder.getMac().setText(localBluetoothData.get(position).mac);
        holder.getName().setText(localBluetoothData.get(position).name);
        holder.getSwitchEnable().setChecked(localBluetoothData.get(position).enable);

        holder.getSwitchEnable().setOnCheckedChangeListener((compoundButton, isCheck) -> {
            listener.onSwitchChange(isCheck, position);
        });
    }

    @Override
    public int getItemCount() {
        return localBluetoothData.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        try {
            listener = (BluetoothListAdapterListener) recyclerView.getContext();
        } catch (ClassCastException e) {
            throw new ClassCastException(recyclerView.getContext().getClass().getSimpleName() + " must implement BluetoothAdapterListener.");
        }
    }
}
