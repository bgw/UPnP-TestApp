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

import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.message.header.SubscriptionIdHeader;
import org.teleal.cling.model.message.header.NTEventHeader;
import org.teleal.cling.model.message.header.NTSHeader;
import org.teleal.cling.model.message.header.EventSequenceHeader;
import org.teleal.cling.model.types.NotificationSubtype;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;

import java.util.List;
import java.util.ArrayList;


public class IncomingEventRequestMessage extends StreamRequestMessage {

    final private List<StateVariableValue> stateVariableValues = new ArrayList();
    final private DeviceService DeviceService;

    public IncomingEventRequestMessage(StreamRequestMessage source, DeviceService DeviceService) {
        super(source);
        this.DeviceService = DeviceService;
    }

    public DeviceService getDeviceService() {
        return DeviceService;
    }

    public List<StateVariableValue> getStateVariableValues() {
        return stateVariableValues;
    }

    public String getSubscrptionId() {
        SubscriptionIdHeader header =
                getHeaders().getFirstHeader(UpnpHeader.Type.SID,SubscriptionIdHeader.class);
        return header != null ? header.getValue() : null;
    }

    public UnsignedIntegerFourBytes getSequence() {
        EventSequenceHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.SEQ, EventSequenceHeader.class);
        return header != null ? header.getValue() : null;
    }

    public boolean hasNotificationHeaders() {
        UpnpHeader ntHeader = getHeaders().getFirstHeader(UpnpHeader.Type.NT);
        UpnpHeader ntsHeader = getHeaders().getFirstHeader(UpnpHeader.Type.NTS);
        return ntHeader != null && ntHeader.getValue() != null
                && ntsHeader != null && ntsHeader.getValue() != null;
    }

    public boolean hasValidNotificationHeaders() {
        NTEventHeader ntHeader = getHeaders().getFirstHeader(UpnpHeader.Type.NT, NTEventHeader.class);
        NTSHeader ntsHeader = getHeaders().getFirstHeader(UpnpHeader.Type.NTS, NTSHeader.class);
        return ntHeader != null && ntHeader.getValue() != null
                && ntsHeader != null && ntsHeader.getValue().equals(NotificationSubtype.PROPCHANGE);

    }

    @Override
    public String toString() {
        return super.toString() + " SEQUENCE: " + getSequence().getValue();
    }
}
