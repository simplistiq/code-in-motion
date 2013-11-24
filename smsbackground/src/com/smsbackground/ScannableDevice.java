package com.smsbackground;

public class ScannableDevice {

    private String address;
    private String name;
    private Long lastScannedTime;
    private boolean scanAllowed;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLastScannedTime() {
        return lastScannedTime;
    }

    public void setLastScannedTime(Long lastScannedTime) {
        this.lastScannedTime = lastScannedTime;
    }

    public boolean isScanAllowed() {
        return scanAllowed;
    }

    public void setScanAllowed(boolean scanAllowed) {
        this.scanAllowed = scanAllowed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result
                + ((lastScannedTime == null) ? 0 : lastScannedTime.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (scanAllowed ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScannableDevice other = (ScannableDevice) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        }
        else if (!address.equals(other.address))
            return false;
        if (lastScannedTime == null) {
            if (other.lastScannedTime != null)
                return false;
        }
        else if (!lastScannedTime.equals(other.lastScannedTime))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        if (scanAllowed != other.scanAllowed)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ScannableDevice [address=" + address + ", name=" + name
                + ", lastScannedTime=" + lastScannedTime + ", scanAllowed="
                + scanAllowed + "]";
    }
}
