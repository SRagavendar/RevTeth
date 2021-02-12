package com.revteth;
import android.os.Parcel;
import android.os.Parcelable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class VpnConfiguration implements Parcelable {
    private final InetAddress[] dnsServers;
    private final CIDR[] routes;
    public VpnConfiguration() {
        this.dnsServers = new InetAddress[0];
        this.routes = new CIDR[0];
    }

    public VpnConfiguration(InetAddress[] dnsServers, CIDR[] routes) {
        this.dnsServers = dnsServers;
        this.routes = routes;
    }

    private VpnConfiguration(Parcel source) {
        int dnsCount = source.readInt();
        dnsServers = new InetAddress[dnsCount];
        try {
            for (int i = 0; i < dnsCount; ++i) {
                dnsServers[i] = InetAddress.getByAddress(source.createByteArray());
            }
        } catch (UnknownHostException e) {
            throw new AssertionError("Invalid address", e);
        }
        routes = source.createTypedArray(CIDR.CREATOR);
    }

    public InetAddress[] getDnsServers() {
        return dnsServers;
    }

    public CIDR[] getRoutes() {
        return routes;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(dnsServers.length);
        for (InetAddress addr : dnsServers) {
            dest.writeByteArray(addr.getAddress());
        }
        dest.writeTypedArray(routes, 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VpnConfiguration> CREATOR = new Creator<VpnConfiguration>() {
        @Override
        public VpnConfiguration createFromParcel(Parcel source) {
            return new VpnConfiguration(source);
        }

        @Override
        public VpnConfiguration[] newArray(int size) {
            return new VpnConfiguration[size];
        }
    };
}
