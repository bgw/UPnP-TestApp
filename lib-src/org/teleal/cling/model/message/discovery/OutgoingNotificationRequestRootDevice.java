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

package org.teleal.cling.model.message.discovery;

import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.message.header.InterfaceMacHeader;
import org.teleal.cling.model.message.header.RootDeviceHeader;
import org.teleal.cling.model.message.header.USNRootDeviceHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.types.NotificationSubtype;

import java.net.InetAddress;


public class OutgoingNotificationRequestRootDevice extends OutgoingNotificationRequest {

    public OutgoingNotificationRequestRootDevice(InetAddress localAddress, int localStreamPort,
                                                 byte[] localHardwareAddress,
                                                 LocalDevice device, NotificationSubtype type) {
        super(localAddress, localStreamPort, device, type);

        getHeaders().add(UpnpHeader.Type.NT, new RootDeviceHeader());
        getHeaders().add(UpnpHeader.Type.USN, new USNRootDeviceHeader(device.getIdentity().getUdn()));

        if (localHardwareAddress != null) {
            getHeaders().add(UpnpHeader.Type.EXT_IFACE_MAC, new InterfaceMacHeader(localHardwareAddress));
        }
    }

}
