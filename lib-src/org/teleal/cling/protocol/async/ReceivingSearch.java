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

package org.teleal.cling.protocol.async;

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.message.IncomingDatagramMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.discovery.IncomingSearchRequest;
import org.teleal.cling.model.message.discovery.OutgoingSearchResponse;
import org.teleal.cling.model.message.discovery.OutgoingSearchResponseDeviceType;
import org.teleal.cling.model.message.discovery.OutgoingSearchResponseRootDevice;
import org.teleal.cling.model.message.discovery.OutgoingSearchResponseRootDeviceUDN;
import org.teleal.cling.model.message.discovery.OutgoingSearchResponseServiceType;
import org.teleal.cling.model.message.discovery.OutgoingSearchResponseUDN;
import org.teleal.cling.model.message.header.DeviceTypeHeader;
import org.teleal.cling.model.message.header.MXHeader;
import org.teleal.cling.model.message.header.RootDeviceHeader;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.message.header.ServiceTypeHeader;
import org.teleal.cling.model.message.header.UDADeviceTypeHeader;
import org.teleal.cling.model.message.header.UDAServiceTypeHeader;
import org.teleal.cling.model.message.header.UDNHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.protocol.ReceivingAsync;
import org.teleal.common.util.HexBin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ReceivingSearch extends ReceivingAsync<IncomingSearchRequest> {

    private static Logger log = Logger.getLogger(ReceivingSearch.class.getName());

    final protected Random randomGenerator = new Random();
    final protected int localStreamPort;

    public ReceivingSearch(UpnpService upnpService, IncomingDatagramMessage<UpnpRequest> inputMessage) {
        super(upnpService, new IncomingSearchRequest(inputMessage));
        this.localStreamPort = getUpnpService().getRouter().getNetworkAddressFactory().getStreamListenPort();
    }

    protected void execute() {

        if (!getInputMessage().isMANSSDPDiscover()) {
            log.fine("Invalid search request, no or invalid MAN ssdp:discover header: " + getInputMessage());
            return;
        }

        UpnpHeader searchTarget = getInputMessage().getSearchTarget();

        if (searchTarget == null) {
            log.fine("Invalid search request, did not contain ST header: " + getInputMessage());
            return;
        }

        sendResponses(searchTarget);
    }

    @Override
    protected boolean waitBeforeExecution() throws InterruptedException {

        Integer mx = getInputMessage().getMX();

        if (mx == null) {
            log.fine("Invalid search request, did not contain MX header: " + getInputMessage());
            return false;
        }

        // Spec says we should assume "less" if it's 120 or more
        if (mx > 120) mx = MXHeader.DEFAULT_VALUE;

        // Only wait if there is something to wait for
        if (getUpnpService().getRegistry().getLocalDevices().size() > 0) {
            int sleepTime = randomGenerator.nextInt(mx * 1000);
            log.fine("Sleeping " + sleepTime + " milliseconds to avoid flooding with search responses");
            Thread.sleep(sleepTime);
        }

        return true;
    }

    protected void sendResponses(UpnpHeader searchTarget) {
        if (searchTarget instanceof STAllHeader) {

            sendSearchResponseAll();

        } else if (searchTarget instanceof RootDeviceHeader) {

            sendSearchResponseRootDevices();

        } else if (searchTarget instanceof UDNHeader) {

            sendSearchResponseUDN((UDN) searchTarget.getValue());

        } else if (searchTarget instanceof DeviceTypeHeader || searchTarget instanceof UDADeviceTypeHeader) {

            sendSearchResponseDeviceType((DeviceType) searchTarget.getValue());

        } else if (searchTarget instanceof ServiceTypeHeader || searchTarget instanceof UDAServiceTypeHeader) {

            sendSearchResponseServiceType((ServiceType) searchTarget.getValue());

        } else {
            log.warning("Non-implemented search request target: " + searchTarget.getClass());
        }
    }

    protected void sendSearchResponseAll() {
        log.fine("Responding to 'all' search with advertisement messages for all local devices");
        for (LocalDevice localDevice : getUpnpService().getRegistry().getLocalDevices()) {

            // We are re-using the regular notification messages here but override the NT with the ST header

            log.finer("Sending root device messages: " + localDevice);
            List<OutgoingSearchResponse> rootDeviceMsgs = createDeviceMessages(localDevice);
            for (OutgoingSearchResponse upnpMessage : rootDeviceMsgs) {
                getUpnpService().getRouter().send(upnpMessage);
            }

            if (localDevice.hasEmbeddedDevices()) {
                for (LocalDevice embeddedDevice : localDevice.findEmbeddedDevices()) {
                    log.finer("Sending embedded device messages: " + embeddedDevice);
                    List<OutgoingSearchResponse> embeddedDeviceMsgs = createDeviceMessages(embeddedDevice);
                    for (OutgoingSearchResponse upnpMessage : embeddedDeviceMsgs) {
                        getUpnpService().getRouter().send(upnpMessage);
                    }
                }
            }

            List<OutgoingSearchResponse> serviceTypeMsgs = createServiceTypeMessages(localDevice);
            if (serviceTypeMsgs.size() > 0) {
                log.finer("Sending service type messages");
                for (OutgoingSearchResponse upnpMessage : serviceTypeMsgs) {
                    getUpnpService().getRouter().send(upnpMessage);
                }
            }

        }
    }

    protected List<OutgoingSearchResponse> createDeviceMessages(LocalDevice device) {
        List<OutgoingSearchResponse> msgs = new ArrayList();

        // See the tables in UDA 1.0 section 1.1.2

        if (device.isRoot()) {
            msgs.add(
                    new OutgoingSearchResponseRootDevice(
                            getInputMessage(),
                            localStreamPort,
                            getLocalHardwareAddress(),
                            device
                    )
            );
        }

        msgs.add(
                new OutgoingSearchResponseUDN(
                        getInputMessage(),
                        localStreamPort,
                        getLocalHardwareAddress(),
                        device
                )
        );

        msgs.add(
                new OutgoingSearchResponseDeviceType(
                        getInputMessage(),
                        localStreamPort,
                        getLocalHardwareAddress(),
                        device
                )
        );

        return msgs;
    }

    protected List<OutgoingSearchResponse> createServiceTypeMessages(LocalDevice device) {
        List<OutgoingSearchResponse> msgs = new ArrayList();
        for (ServiceType serviceType : device.findServiceTypes()) {
            msgs.add(
                    new OutgoingSearchResponseServiceType(
                            getInputMessage(),
                            localStreamPort,
                            getLocalHardwareAddress(),
                            device,
                            serviceType
                    )
            );
        }
        return msgs;
    }

    protected void sendSearchResponseRootDevices() {
        log.fine("Responding to root device search with advertisement messages for all local root devices");
        for (LocalDevice device : getUpnpService().getRegistry().getLocalDevices()) {

            getUpnpService().getRouter().send(
                    new OutgoingSearchResponseRootDeviceUDN(
                            getInputMessage(),
                            localStreamPort,
                            getLocalHardwareAddress(),
                            device
                    )
            );
        }
    }

    protected void sendSearchResponseUDN(UDN udn) {
        Device device = getUpnpService().getRegistry().getDevice(udn, false);
        if (device != null && device instanceof LocalDevice) {
            log.fine("Responding to UDN device search: " + udn);
            getUpnpService().getRouter().send(
                    new OutgoingSearchResponseUDN(
                            getInputMessage(),
                            localStreamPort,
                            getLocalHardwareAddress(),
                            (LocalDevice) device
                    )
            );
        }
    }

    protected void sendSearchResponseDeviceType(DeviceType deviceType) {
        log.fine("Responding to device type search: " + deviceType);
        Collection<Device> devices = getUpnpService().getRegistry().getDevices(deviceType);
        for (Device device : devices) {
            if (device instanceof LocalDevice) {
                log.finer("Sending matching device type search result for: " + device);
                getUpnpService().getRouter().send(
                        new OutgoingSearchResponseDeviceType(
                                getInputMessage(),
                                localStreamPort,
                                getLocalHardwareAddress(),
                                (LocalDevice) device
                        )
                );
            }
        }
    }

    protected void sendSearchResponseServiceType(ServiceType serviceType) {
        log.fine("Responding to service type search: " + serviceType);
        Collection<Device> devices = getUpnpService().getRegistry().getDevices(serviceType);
        for (Device device : devices) {
            if (device instanceof LocalDevice) {
                log.finer("Sending matching service type search result: " + device);
                getUpnpService().getRouter().send(
                        new OutgoingSearchResponseServiceType(
                                getInputMessage(),
                                localStreamPort,
                                getLocalHardwareAddress(),
                                (LocalDevice) device,
                                serviceType
                        )
                );
            }
        }
    }

    protected byte[] getLocalHardwareAddress() {
        byte[] localHardwareAddress =
                getUpnpService().getRouter().getNetworkAddressFactory().getHardwareAddress(getInputMessage().getLocalAddress());

        if (localHardwareAddress != null && log.isLoggable(Level.FINER)) {
            log.finer("Preparing outgoing search response with local hardware MAC address: " +
                    HexBin.bytesToString(localHardwareAddress, ":"));
        }
        return localHardwareAddress;
    }

}
