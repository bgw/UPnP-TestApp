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

package org.teleal.cling;

import org.teleal.cling.binding.xml.DeviceDescriptorBinder;
import org.teleal.cling.binding.xml.ServiceDescriptorBinder;
import org.teleal.cling.protocol.ProtocolFactory;
import org.teleal.cling.transport.spi.DatagramIO;
import org.teleal.cling.transport.spi.DatagramProcessor;
import org.teleal.cling.transport.spi.GENAEventProcessor;
import org.teleal.cling.transport.spi.MulticastReceiver;
import org.teleal.cling.transport.spi.NetworkAddressFactory;
import org.teleal.cling.transport.spi.SOAPActionProcessor;
import org.teleal.cling.transport.spi.StreamClient;
import org.teleal.cling.transport.spi.StreamServer;

import java.util.concurrent.Executor;

/**
 * @author Christian Bauer
 */
public interface UpnpServiceConfiguration {

    // NETWORK
    public NetworkAddressFactory createNetworkAddressFactory();

    public DatagramProcessor getDatagramProcessor();
    public SOAPActionProcessor getSoapActionProcessor();
    public GENAEventProcessor getGenaEventProcessor();

    public StreamClient createStreamClient();
    public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory);
    public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory);
    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory);

    public Executor getMulticastReceiverExecutor();
    public Executor getDatagramIOExecutor();
    public Executor getStreamServerExecutor();

    // DESCRIPTORS
    public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10();
    public ServiceDescriptorBinder getServiceDescriptorBinderUDA10();

    public Executor getAsyncProtocolExecutor();
    public Executor getSyncProtocolExecutor();

    // REGISTRY
    public Executor getRegistryMaintainerExecutor();
    public Executor getRegistryListenerExecutor();

}
