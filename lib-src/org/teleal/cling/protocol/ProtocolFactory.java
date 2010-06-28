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

package org.teleal.cling.protocol;

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.message.IncomingDatagramMessage;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.protocol.async.SendingNotificationAlive;
import org.teleal.cling.protocol.async.SendingNotificationByebye;
import org.teleal.cling.protocol.async.SendingSearch;
import org.teleal.cling.protocol.sync.SendingAction;
import org.teleal.cling.protocol.sync.SendingEvent;
import org.teleal.cling.protocol.sync.SendingRenewal;
import org.teleal.cling.protocol.sync.SendingSubscribe;
import org.teleal.cling.protocol.sync.SendingUnsubscribe;

import java.net.URL;


public interface ProtocolFactory {

    public UpnpService getUpnpService();

    public ReceivingAsync createReceivingAsync(IncomingDatagramMessage message);
    public ReceivingSync createReceivingSync(StreamRequestMessage requestMessage);

    public SendingNotificationAlive createSendingNotificationAlive(LocalDevice localDevice);
    public SendingNotificationByebye createSendingNotificationByebye(LocalDevice localDevice);
    public SendingSearch createSendingSearch(UpnpHeader searchTarget);
    public SendingAction createSendingAction(ActionInvocation actionInvocation, URL controlURL);
    public SendingSubscribe createSendingSubscribe(RemoteGENASubscription subscription);
    public SendingRenewal createSendingRenewal(RemoteGENASubscription subscription);
    public SendingUnsubscribe createSendingUnsubscribe(RemoteGENASubscription subscription);
    public SendingEvent createSendingEvent(LocalGENASubscription subscription);
}
