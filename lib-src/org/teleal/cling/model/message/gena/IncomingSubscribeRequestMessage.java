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

package org.teleal.cling.model.message.gena;

import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.header.CallbackHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.message.header.NTEventHeader;
import org.teleal.cling.model.message.header.TimeoutHeader;
import org.teleal.cling.model.message.header.SubscriptionIdHeader;

import java.net.URL;
import java.util.List;


public class IncomingSubscribeRequestMessage extends StreamRequestMessage {

    final private DeviceService deviceService;

    public IncomingSubscribeRequestMessage(StreamRequestMessage source, DeviceService deviceService) {
        super(source);
        this.deviceService = deviceService;
    }

    public DeviceService getDeviceService() {
        return deviceService;
    }

    public List<URL> getCallbackURLs() {
        CallbackHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.CALLBACK, CallbackHeader.class);
        return header != null ? header.getValue() : null;
    }

    public boolean hasNotificationHeader() {
        return getHeaders().getFirstHeader(UpnpHeader.Type.NT, NTEventHeader.class) != null;
    }

    public Integer getRequestedTimeoutSeconds() {
        TimeoutHeader timeoutHeader = getHeaders().getFirstHeader(UpnpHeader.Type.TIMEOUT, TimeoutHeader.class);
        return timeoutHeader != null ? timeoutHeader.getValue() : null;
    }

    public String getSubscriptionId() {
        SubscriptionIdHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class);
        return header != null ? header.getValue() : null;
    }
}
