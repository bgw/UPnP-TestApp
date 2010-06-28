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
import org.teleal.cling.binding.xml.UDA10DeviceDescriptorBinderImpl;
import org.teleal.cling.binding.xml.UDA10ServiceDescriptorBinderImpl;
import org.teleal.cling.transport.impl.DatagramIOConfigurationImpl;
import org.teleal.cling.transport.impl.DatagramIOImpl;
import org.teleal.cling.transport.impl.DatagramProcessorImpl;
import org.teleal.cling.transport.impl.GENAEventProcessorImpl;
import org.teleal.cling.transport.impl.MulticastReceiverConfigurationImpl;
import org.teleal.cling.transport.impl.MulticastReceiverImpl;
import org.teleal.cling.transport.impl.NetworkAddressFactoryImpl;
import org.teleal.cling.transport.impl.SOAPActionProcessorImpl;
import org.teleal.cling.transport.impl.StreamClientConfigurationImpl;
import org.teleal.cling.transport.impl.StreamClientImpl;
import org.teleal.cling.transport.impl.StreamServerConfigurationImpl;
import org.teleal.cling.transport.impl.StreamServerImpl;
import org.teleal.cling.transport.spi.DatagramIO;
import org.teleal.cling.transport.spi.DatagramProcessor;
import org.teleal.cling.transport.spi.GENAEventProcessor;
import org.teleal.cling.transport.spi.MulticastReceiver;
import org.teleal.cling.transport.spi.NetworkAddressFactory;
import org.teleal.cling.transport.spi.SOAPActionProcessor;
import org.teleal.cling.transport.spi.StreamClient;
import org.teleal.cling.transport.spi.StreamServer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public class DefaultUpnpServiceConfiguration implements UpnpServiceConfiguration {

    private static Logger log = Logger.getLogger(DefaultUpnpServiceConfiguration.class.getName());

    final private int streamListenPort;

    final private ThreadPoolExecutor defaultExecutor;

    final private DatagramProcessor datagramProcessor;
    final private SOAPActionProcessor soapActionProcessor;
    final private GENAEventProcessor genaEventProcessor;

    final private Executor multicastReceiverExecutor;
    final private Executor datagramIOExecutor;
    final private Executor streamServerExecutor;

    final private DeviceDescriptorBinder deviceDescriptorBinderUDA10;
    final private ServiceDescriptorBinder serviceDescriptorBinderUDA10;

    final private Executor asyncProtocolExecutor;
    final private Executor syncProtocolExecutor;

    final private Executor registryMaintainerExecutor;
    final private Executor registryListenerExecutor;

    public DefaultUpnpServiceConfiguration() {
        this(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT);
    }

    public DefaultUpnpServiceConfiguration(int streamListenPort) {

        this.streamListenPort = streamListenPort;

        defaultExecutor = new ThreadPoolExecutor(8, 64, 30, TimeUnit.SECONDS, new ArrayBlockingQueue(128)) {
            @Override
            protected void beforeExecute(Thread thread, Runnable runnable) {
                super.beforeExecute(thread, runnable);
                thread.setName("Thread " + thread.getId() + " (Active: " + getActiveCount() + ")");
            }
        };

        defaultExecutor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.DiscardPolicy() {
                    @Override
                    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {

                        // Log and discard
                        log.warning(
                                "Thread pool saturated, discarding execution " +
                                "of '"+runnable.getClass()+"', consider raising the " +
                                "maximum pool or queue size"
                        );
                        super.rejectedExecution(runnable, threadPoolExecutor);
                    }
                }
        );

        datagramProcessor = new DatagramProcessorImpl();
        soapActionProcessor = new SOAPActionProcessorImpl();
        genaEventProcessor = new GENAEventProcessorImpl();

        multicastReceiverExecutor = createDefaultExecutor();
        datagramIOExecutor = createDefaultExecutor();
        streamServerExecutor = createDefaultExecutor();

        deviceDescriptorBinderUDA10 = new UDA10DeviceDescriptorBinderImpl();
        serviceDescriptorBinderUDA10 = new UDA10ServiceDescriptorBinderImpl();

        asyncProtocolExecutor = createDefaultExecutor();
        syncProtocolExecutor = createDefaultExecutor();

        registryMaintainerExecutor = createDefaultExecutor();
        registryListenerExecutor = createDefaultExecutor();

    }

    public DatagramProcessor getDatagramProcessor() {
        return datagramProcessor;
    }

    public SOAPActionProcessor getSoapActionProcessor() {
        return soapActionProcessor;
    }

    public GENAEventProcessor getGenaEventProcessor() {
        return genaEventProcessor;
    }

    public StreamClient createStreamClient() {
        return new StreamClientImpl(new StreamClientConfigurationImpl());
    }

    public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
        return new MulticastReceiverImpl(
                new MulticastReceiverConfigurationImpl(
                        networkAddressFactory.getMulticastGroup(),
                        networkAddressFactory.getMulticastPort()
                )
        );
    }

    public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
        return new DatagramIOImpl(
                new DatagramIOConfigurationImpl(
                        networkAddressFactory.getMulticastGroup(),
                        networkAddressFactory.getMulticastPort()
                )
        );
    }

    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
        return new StreamServerImpl(
                new StreamServerConfigurationImpl(
                        networkAddressFactory.getStreamListenPort()
                )
        );
    }

    public Executor getMulticastReceiverExecutor() {
        return multicastReceiverExecutor;
    }

    public Executor getDatagramIOExecutor() {
        return datagramIOExecutor;
    }

    public Executor getStreamServerExecutor() {
        return streamServerExecutor;
    }

    public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
        return deviceDescriptorBinderUDA10;
    }

    public ServiceDescriptorBinder getServiceDescriptorBinderUDA10() {
        return serviceDescriptorBinderUDA10;
    }

    public Executor getAsyncProtocolExecutor() {
        return asyncProtocolExecutor;
    }

    public Executor getSyncProtocolExecutor() {
        return syncProtocolExecutor;
    }

    public Executor getRegistryMaintainerExecutor() {
        return registryMaintainerExecutor;
    }

    public Executor getRegistryListenerExecutor() {
        return registryListenerExecutor;
    }

    public NetworkAddressFactory createNetworkAddressFactory() {
        return new NetworkAddressFactoryImpl() {
            @Override
            public int getStreamListenPort() {
                return streamListenPort;
            }
        };
    }

    /**
     * @return An executor that spawns a new thread for each task.
     */
    protected Executor createDefaultExecutor() {
        return defaultExecutor;
/*
        return new Executor() {
            public void execute(Runnable r) {
                new Thread(r).start();
            }
        };
*/
    }
}
