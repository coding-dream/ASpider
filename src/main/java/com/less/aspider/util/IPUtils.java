package com.less.aspider.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by deeper on 2017/12/15.
 */

public abstract class IPUtils {

    public static String getFirstNoLoopbackIPAddresses() throws SocketException {

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        InetAddress localAddress = null;
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress address = inetAddresses.nextElement();
                if (!address.isLoopbackAddress() && !Inet6Address.class.isInstance(address)) {
                    return address.getHostAddress();
                } else if (!address.isLoopbackAddress()) {
                    localAddress = address;
                }
            }
        }

        return localAddress.getHostAddress();
    }
}