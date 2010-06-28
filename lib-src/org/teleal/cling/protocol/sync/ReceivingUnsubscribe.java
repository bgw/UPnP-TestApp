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

package org.teleal.cling.protocol.sync;

import org.teleal.cling.model.Resource;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.gena.IncomingUnsubscribeRequestMessage;
import org.teleal.cling.UpnpService;
import org.teleal.cling.protocol.ReceivingSync;

import java.util.logging.Logger;


public class ReceivingUnsubscribe extends ReceivingSync<StreamRequestMessage, StreamResponseMessage> {

    private static Logger log = Logger.getLogger(ReceivingUnsubscribe.class.getName());

    public ReceivingUnsubscribe(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    protected StreamResponseMessage executeSync() {

        Resource foundResource = getUpnpService().getRegistry().getResource(getInputMessage().getUri());

        if (foundResource == null) {
            log.fine("No local resource found: " + getInputMessage());
            return null;
        }

        if (!foundResource.getType().equals(Resource.Type.EVENT_SUBSCRIPTION)) {
            log.warning("Unknown local resource found, ignoring it: " + foundResource);
            return null;
        }

        log.fine("Found local event subscription matching relative request URI: " + getInputMessage().getUri());
        DeviceService deviceService = (DeviceService) foundResource.getModel();

        IncomingUnsubscribeRequestMessage requestMessage =
                new IncomingUnsubscribeRequestMessage(getInputMessage(), deviceService);

        // Error conditions UDA 1.0 section 4.1.3
        if (requestMessage.getSubscriptionId() != null &&
                (requestMessage.hasNotificationHeader() || requestMessage.hasCallbackHeader())) {
            log.fine("Subscription ID and NT or Callback in unsubcribe request: " + getInputMessage());
            return new StreamResponseMessage(UpnpResponse.Status.BAD_REQUEST);
        }

        LocalGENASubscription subscription =
                getUpnpService().getRegistry().getLocalSubscription(requestMessage.getSubscriptionId());

        if (subscription == null) {
            log.fine("Invalid subscription ID for unsubscribe request: " + getInputMessage());
            return new StreamResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        log.fine("Unregistering subscription: " + subscription);
        if (getUpnpService().getRegistry().removeLocalSubscription(subscription)) {
            subscription.end(null); // No reason, just an unsubscribe
        } else {
            log.fine("Subscription was already removed from registry");
        }

        return new StreamResponseMessage(UpnpResponse.Status.OK);
    }
}