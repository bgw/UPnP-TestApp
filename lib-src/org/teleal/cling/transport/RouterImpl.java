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

package org.teleal.cling.transport;

import org.teleal.cling.UpnpServiceConfiguration;
import org.teleal.cling.model.message.IncomingDatagramMessage;
import org.teleal.cling.model.message.OutgoingDatagramMessage;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.protocol.ProtocolFactory;
import org.teleal.cling.transport.spi.DatagramIO;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.MulticastReceiver;
import org.teleal.cling.transport.spi.NetworkAddressFactory;
import org.teleal.cling.transport.spi.StreamClient;
import org.teleal.cling.transport.spi.StreamServer;
import org.teleal.cling.transport.spi.UpnpStream;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class RouterImpl implements Router {

    private static Logger log = Logger.getLogger(Router.class.getName());

    protected final UpnpServiceConfiguration configuration;
    protected final ProtocolFactory protocolFactory;

    protected final StreamClient streamClient;
    protected final NetworkAddressFactory networkAddressFactory;

    protected final Map<NetworkInterface, MulticastReceiver> multicastReceivers = new HashMap();
    protected final Map<InetAddress, DatagramIO> datagramIOs = new HashMap();
    protected final Map<InetAddress, StreamServer> streamServers = new HashMap();

    public RouterImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory)
            throws InitializationException {

        log.info("Creating Router: " + getClass().getName());

        this.configuration = configuration;
        this.protocolFactory = protocolFactory;

        log.fine("Starting networking services...");
        networkAddressFactory = getConfiguration().createNetworkAddressFactory();

        streamClient = getConfiguration().createStreamClient();

        for (NetworkInterface networkInterface : networkAddressFactory.getNetworkInterfaces()) {
            MulticastReceiver multicastReceiver = getConfiguration().createMulticastReceiver(networkAddressFactory);
            if (multicastReceiver != null) {
                multicastReceivers.put(networkInterface, multicastReceiver);
            }
        }

        for (InetAddress inetAddress : networkAddressFactory.getBindAddresses()) {

            DatagramIO datagramIO = getConfiguration().createDatagramIO(networkAddressFactory);
            if (datagramIO != null) {
                datagramIOs.put(inetAddress, datagramIO);
            }
            StreamServer streamServer = getConfiguration().createStreamServer(networkAddressFactory);
            if (streamServer != null) {
                streamServers.put(inetAddress, streamServer);
            }
        }

        if (streamClient != null) {
            streamClient.init(this);
        }

        // Start this first so we get a BindException if it's already started on this machine
        for (Map.Entry<InetAddress, StreamServer> entry : streamServers.entrySet()) {
            log.fine("Starting stream server on address: " + entry.getKey());
            entry.getValue().init(entry.getKey(), this);
            getConfiguration().getStreamServerExecutor().execute(entry.getValue());
        }

        for (Map.Entry<NetworkInterface, MulticastReceiver> entry : multicastReceivers.entrySet()) {
            log.fine("Starting multicast receiver on interface: " + entry.getKey().getDisplayName());
            entry.getValue().init(entry.getKey(), this, getConfiguration().getDatagramProcessor());
            getConfiguration().getMulticastReceiverExecutor().execute(entry.getValue());
        }

        for (Map.Entry<InetAddress, DatagramIO> entry : datagramIOs.entrySet()) {
            log.fine("Starting datagram I/O on address: " + entry.getKey());
            entry.getValue().init(entry.getKey(), this, getConfiguration().getDatagramProcessor());
            getConfiguration().getDatagramIOExecutor().execute(entry.getValue());
        }

    }

    public UpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    public StreamClient getStreamClient() {
        return streamClient;
    }

    public NetworkAddressFactory getNetworkAddressFactory() {
        return networkAddressFactory;
    }

    protected Map<NetworkInterface, MulticastReceiver> getMulticastReceivers() {
        return multicastReceivers;
    }

    protected Map<InetAddress, DatagramIO> getDatagramIOs() {
        return datagramIOs;
    }

    protected Map<InetAddress, StreamServer> getStreamServers() {
        return streamServers;
    }

    public void shutdown() {
        log.fine("Shutting down network services");

        for (Map.Entry<InetAddress, StreamServer> entry : streamServers.entrySet()) {
            log.fine("Stopping stream server on address: " + entry.getKey());
            entry.getValue().stop();
        }
        streamServers.clear();

        for (Map.Entry<NetworkInterface, MulticastReceiver> entry : multicastReceivers.entrySet()) {
            log.fine("Stopping multicast receiver on interface: " + entry.getKey().getDisplayName());
            entry.getValue().stop();
        }
        multicastReceivers.clear();

        for (Map.Entry<InetAddress, DatagramIO> entry : datagramIOs.entrySet()) {
            log.fine("Stopping datagram I/O on address: " + entry.getKey());
            entry.getValue().stop();
        }
        datagramIOs.clear();
    }

    public void received(IncomingDatagramMessage msg) {
        log.fine("Received asynchronous message: " + msg);
        getConfiguration().getAsyncProtocolExecutor().execute(
                getProtocolFactory().createReceivingAsync(msg)
        );
    }

    public void received(UpnpStream stream) {
        log.fine("Received synchronous stream: " + stream);
        getConfiguration().getSyncProtocolExecutor().execute(stream);
    }

    public void send(OutgoingDatagramMessage msg) {
        for (DatagramIO datagramIO : getDatagramIOs().values()) {
            datagramIO.send(msg);
        }
    }

    public StreamResponseMessage send(StreamRequestMessage msg) {
        if (getStreamClient() == null) {
            log.fine("No StreamClient available, ignoring: " + msg);
            return null;
        }
        log.fine("Sending via TCP unicast stream: " + msg);
        return getStreamClient().sendRequest(msg);
    }

    public void broadcast(byte[] bytes) {
        for (Map.Entry<InetAddress, DatagramIO> entry : getDatagramIOs().entrySet()) {
            InetAddress broadcast = getNetworkAddressFactory().getBroadcastAddress(entry.getKey());
            if (broadcast != null) {
                log.fine("Sending UDP datagram to broadcast address: " + broadcast.getHostAddress());
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, broadcast, 9);
                entry.getValue().send(packet);
            }
        }
    }
}
