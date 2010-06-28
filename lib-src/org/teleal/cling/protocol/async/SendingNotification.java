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

import java.util.logging.Logger;
import java.util.logging.Level;

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.protocol.SendingAsync;
import org.teleal.cling.model.types.NotificationSubtype;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequest;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestServiceType;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestUDN;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestRootDevice;
import org.teleal.cling.model.message.discovery.OutgoingNotificationRequestDeviceType;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.common.util.HexBin;

import java.util.ArrayList;
import java.util.List;
import java.net.InetAddress;


public abstract class SendingNotification extends SendingAsync {

    protected static Logger log = Logger.getLogger(SendingNotification.class.getName());

    private LocalDevice device;
    private InetAddress[] bindingAddresses;
    private int localStreamPort;

    public SendingNotification(UpnpService upnpService, LocalDevice device) {
        super(upnpService);
        this.device = device;
        this.bindingAddresses = getUpnpService().getRouter().getNetworkAddressFactory().getBindAddresses();
        this.localStreamPort = getUpnpService().getRouter().getNetworkAddressFactory().getStreamListenPort();
    }

    public LocalDevice getDevice() {
        return device;
    }

    protected void execute() {
        for (int i = 0; i < getBulkRepeat(); i++) {
            try {

                sendMessages();

                // UDA 1.0 is silent about this but UDA 1.1 recomments "a few hundred milliseconds"
                log.finer("Sleeping " + getBulkIntervalMilliseconds() + " milliseconds");
                Thread.sleep(getBulkIntervalMilliseconds());

            } catch (InterruptedException ex) {
                log.warning("Advertisement thread was interrupted: " + ex);
            }
        }
    }

    protected int getBulkRepeat() {
        return 3; // UDA 1.0 says maximum 3 times for alive messages, let's just do it for all
    }

    protected int getBulkIntervalMilliseconds() {
        return 150;
    }

    public void sendMessages() {
        log.finer("Sending root device messages: " + getDevice());
        List<OutgoingNotificationRequest> rootDeviceMsgs = createDeviceMessages(getDevice());
        for (OutgoingNotificationRequest upnpMessage : rootDeviceMsgs) {
            getUpnpService().getRouter().send(upnpMessage);
        }

        if (getDevice().hasEmbeddedDevices()) {
            for (LocalDevice embeddedDevice : getDevice().findEmbeddedDevices()) {
                log.finer("Sending embedded device messages: " + embeddedDevice);
                List<OutgoingNotificationRequest> embeddedDeviceMsgs = createDeviceMessages(embeddedDevice);
                for (OutgoingNotificationRequest upnpMessage : embeddedDeviceMsgs) {
                    getUpnpService().getRouter().send(upnpMessage);
                }
            }
        }

        List<OutgoingNotificationRequest> serviceTypeMsgs = createServiceTypeMessages(getDevice());
        if (serviceTypeMsgs.size() > 0) {
            log.finer("Sending service type messages");
            for (OutgoingNotificationRequest upnpMessage : serviceTypeMsgs) {
                getUpnpService().getRouter().send(upnpMessage);
            }
        }
    }

    protected List<OutgoingNotificationRequest> createDeviceMessages(LocalDevice device) {
        List<OutgoingNotificationRequest> msgs = new ArrayList();

        // See the tables in UDA 1.0 section 1.1.2

        if (device.isRoot()) {
            for (InetAddress bindingAddress : bindingAddresses) {

                byte[] localHardwareAddress =
                        getUpnpService().getRouter().getNetworkAddressFactory().getHardwareAddress(bindingAddress);

                if (log.isLoggable(Level.FINER)) {
                    log.finer("Preparing outgoing notification with local hardware MAC address: " +
                            HexBin.bytesToString(localHardwareAddress, ":"));
                }

                msgs.add(
                        new OutgoingNotificationRequestRootDevice(
                                bindingAddress,
                                localStreamPort,
                                localHardwareAddress,
                                device,
                                getNotificationSubtype()
                        )
                );
            }
        }

        for (InetAddress bindingAddress : bindingAddresses) {
            msgs.add(new OutgoingNotificationRequestUDN(bindingAddress, localStreamPort, device, getNotificationSubtype()));
            msgs.add(new OutgoingNotificationRequestDeviceType(bindingAddress, localStreamPort, device, getNotificationSubtype()));
        }

        return msgs;
    }

    protected List<OutgoingNotificationRequest> createServiceTypeMessages(LocalDevice device) {
        List<OutgoingNotificationRequest> msgs = new ArrayList();

        for (ServiceType serviceType : device.findServiceTypes()) {
            for (InetAddress bindingAddress : bindingAddresses) {
                msgs.add(new OutgoingNotificationRequestServiceType(bindingAddress, localStreamPort, device, getNotificationSubtype(), serviceType));
            }
        }

        return msgs;
    }


    protected abstract NotificationSubtype getNotificationSubtype();

}
