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

import org.teleal.cling.model.message.header.InterfaceMacHeader;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.message.IncomingDatagramMessage;
import org.teleal.cling.model.message.OutgoingDatagramMessage;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.header.EXTHeader;
import org.teleal.cling.model.message.header.LocationHeader;
import org.teleal.cling.model.message.header.MaxAgeHeader;
import org.teleal.cling.model.message.header.ServerHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.common.util.URIUtil;

public class OutgoingSearchResponse extends OutgoingDatagramMessage<UpnpResponse> {

    public OutgoingSearchResponse(IncomingDatagramMessage request,
                                  int localStreamPort,
                                  byte[] localHardwareAddress,
                                  LocalDevice device) {

        super(new UpnpResponse(UpnpResponse.Status.OK), request.getSourceAddress(), request.getSourcePort());

        getHeaders().add(UpnpHeader.Type.MAX_AGE, new MaxAgeHeader(device.getIdentity().getMaxAgeSeconds()));
        getHeaders().add(
                UpnpHeader.Type.LOCATION,
                new LocationHeader(
                        URIUtil.createAbsoluteURL(request.getLocalAddress(), localStreamPort, device.getDescriptorURI())
                )
        );
        getHeaders().add(UpnpHeader.Type.SERVER, new ServerHeader());
        getHeaders().add(UpnpHeader.Type.EXT, new EXTHeader());

        if (localHardwareAddress != null) {
            getHeaders().add(UpnpHeader.Type.EXT_IFACE_MAC, new InterfaceMacHeader(localHardwareAddress));
        }
    }

}
