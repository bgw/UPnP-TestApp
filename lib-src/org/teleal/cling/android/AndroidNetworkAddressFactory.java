package org.teleal.cling.android;

import org.teleal.cling.model.Constants;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.NetworkAddressFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public class AndroidNetworkAddressFactory implements NetworkAddressFactory {

    final private static Logger log = Logger.getLogger(NetworkAddressFactory.class.getName());

    final public static int DEFAULT_TCP_HTTP_LISTEN_PORT = 8081;

    protected List<NetworkInterface> networkInterfaces = new ArrayList();
    protected List<InetAddress> bindAddresses = new ArrayList();

    protected int streamListenPort;

    public AndroidNetworkAddressFactory(int streamListenPort) throws InitializationException {
        discoverNetworkInterfaces();
        discoverBindAddresses();
        this.streamListenPort = streamListenPort;
    }

    protected void discoverNetworkInterfaces() throws InitializationException {
        try {
            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaceEnumeration)) {
                if (!isLoopback(iface)) {
                    log.fine("Discovered usable network interface: " + iface.getDisplayName());
                    networkInterfaces.add(iface);
                }
            }

        } catch (Exception ex) {
            throw new InitializationException("Could not not analyze local network interfaces: " + ex, ex);
        }
    }

    protected void discoverBindAddresses() throws InitializationException {
        try {

            for (NetworkInterface networkInterface : networkInterfaces) {

                log.finer("Discovering addresses of interface: " + networkInterface.getDisplayName());
                for (InetAddress inetAddress : getInetAddresses(networkInterface)) {
                    if (inetAddress == null) {
                        log.warning("Network has a null address: " + networkInterface.getDisplayName());
                        continue;
                    }

                    if (isUsableAddress(inetAddress)) {
                        log.fine("Discovered usable network interface address: " + inetAddress.getHostAddress());
                        bindAddresses.add(inetAddress);
                    } else {
                        log.finer("Ignoring non-usable network interface address: " + inetAddress.getHostAddress());
                    }
                }

            }

        } catch (Exception ex) {
            throw new InitializationException("Could not not analyze local network interfaces: " + ex, ex);
        }
    }

    protected boolean isLoopback(NetworkInterface networkInterface) {
        for (InetAddress inetAddress : getInetAddresses(networkInterface)) {
            if (inetAddress.isLoopbackAddress()) return true;
        }
        return false;
    }

    protected boolean isUsableAddress(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            log.finer("Skipping unsupported non-IPv4 address: " + address);
            return false;
        }
        return true;
    }

    protected List<InetAddress> getInetAddresses(NetworkInterface networkInterface) {
        return Collections.list(networkInterface.getInetAddresses());
    }

    public InetAddress getMulticastGroup() {
        try {
            return InetAddress.getByName(Constants.IPV4_UPNP_MULTICAST_GROUP);
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getMulticastPort() {
        return Constants.UPNP_MULTICAST_PORT;
    }

    public int getStreamListenPort() {
        return streamListenPort;
    }

    public NetworkInterface[] getNetworkInterfaces() {
        return networkInterfaces.toArray(new NetworkInterface[networkInterfaces.size()]);
    }

    public InetAddress[] getBindAddresses() {
        return bindAddresses.toArray(new InetAddress[bindAddresses.size()]);
    }

    public byte[] getHardwareAddress(InetAddress inetAddress) {
        return null; // TODO: No low-level network interface methods available on Android API
    }

    public InetAddress getBroadcastAddress(InetAddress inetAddress) {
        return null; // TODO: No low-level network interface methods available on Android API
    }

    public InetAddress getLocalAddress(NetworkInterface networkInterface, boolean isIPv6, InetAddress remoteAddress) {
        // TODO: This is totally random because we can't access low level InterfaceAddress on Android!
        for (InetAddress localAddress: getInetAddresses(networkInterface)) {
            if (isIPv6 && localAddress instanceof Inet6Address)
                return localAddress;
            if (!isIPv6 && localAddress instanceof Inet4Address)
                return localAddress;
        }
        throw new IllegalStateException("Can't find any IPv4 or IPv6 address on interface: " + networkInterface.getDisplayName());
    }


}
