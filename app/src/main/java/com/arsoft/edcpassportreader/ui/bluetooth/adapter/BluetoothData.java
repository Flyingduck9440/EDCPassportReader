package com.arsoft.edcpassportreader.ui.bluetooth.adapter;

import java.io.Serializable;

public class BluetoothData implements Serializable {
    Integer uniqueID;
    String mac;
    String name;
    Boolean enable;
    public BluetoothData(String mac, String name,Integer uniqueID) {
        this.mac = mac;
        this.name = name;
        this.enable = false;
        this.uniqueID = uniqueID;
    }

    public String getMac() {
        return mac;
    }

    public String getName() {
        return name;
    }

    public Boolean getEnable() {
        return enable;
    }

    public Integer getUniqueID() {
        return uniqueID;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public void setUniqueID(Integer uniqueID) {
        this.uniqueID = uniqueID;
    }
}
