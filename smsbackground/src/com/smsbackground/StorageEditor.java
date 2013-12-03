package com.smsbackground;

import java.util.Set;

public interface StorageEditor {

    public Set<String> getSelectedDevices();

    public void updateLastScannedTime(ScannableDevice device);

    public int updateDeviceStateInStorage(boolean isChecked,
            ScannableDevice device);

    public boolean isDevicePresent(ScannableDevice device);

    public void setBoolean(String key, boolean allowed);

    public boolean isDiscoveryScanAllowed();

    public boolean isSmsAllowed();

}
