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
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.NotificationSubtype;
import org.teleal.cling.model.message.header.ServiceTypeHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.message.header.ServiceUSNHeader;

import java.net.InetAddress;


public class OutgoingNotificationRequestServiceType extends OutgoingNotificationRequest {

    public OutgoingNotificationRequestServiceType(InetAddress localAddress, int localStreamPort,
                                                  LocalDevice device, NotificationSubtype type,
                                                  ServiceType serviceType) {

        super(localAddress, localStreamPort, device, type);

        getHeaders().add(UpnpHeader.Type.NT, new ServiceTypeHeader(serviceType));
        getHeaders().add(UpnpHeader.Type.USN, new ServiceUSNHeader(device.getIdentity().getUdn(), serviceType));
    }


}
