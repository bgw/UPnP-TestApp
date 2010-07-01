/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.transport.spi;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.InterfaceAddress;


public interface NetworkAddressFactory {

    // An implementation can honor these if it wants (the default does)
    public static final String SYSTEM_PROPERTY_NET_IFACES = "org.teleal.cling.network.useInterfaces";
    public static final String SYSTEM_PROPERTY_NET_ADDRESSES = "org.teleal.cling.network.useAddresses";

    public InetAddress getMulticastGroup();
    public int getMulticastPort();
    public int getStreamListenPort();

    // The local network interfaces we are joining the multicast group on
    public NetworkInterface[] getNetworkInterfaces();

    // The local addresses (of the local network interfaces) we are listening on for TCP streams
    public InetAddress[] getBindAddresses();

    public byte[] getHardwareAddress(InetAddress inetAddress);

    public InetAddress getBroadcastAddress(InetAddress inetAddress);

    public InetAddress getLocalAddress(NetworkInterface networkInterface,
                                       boolean isIPv6,
                                       InetAddress remoteAddress);

}
