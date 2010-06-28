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

import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.gena.IncomingSubscribeResponseMessage;
import org.teleal.cling.model.message.gena.OutgoingSubscribeRequestMessage;
import org.teleal.cling.UpnpService;
import org.teleal.cling.protocol.SendingSync;

import java.util.logging.Logger;


public class SendingSubscribe extends SendingSync<OutgoingSubscribeRequestMessage, IncomingSubscribeResponseMessage> {

    protected static Logger log = Logger.getLogger(SendingSubscribe.class.getName());

    final protected RemoteGENASubscription subscription;

    public SendingSubscribe(UpnpService upnpService, RemoteGENASubscription subscription) {
        super(
                upnpService,
                new OutgoingSubscribeRequestMessage(
                        subscription,
                        upnpService.getRouter().getNetworkAddressFactory().getStreamListenPort()
                )
        );
        
        this.subscription = subscription;
    }

    protected IncomingSubscribeResponseMessage executeSync() {

        log.fine("Sending subscription request: " + getInputMessage());

        StreamResponseMessage response = getUpnpService().getRouter().send(getInputMessage());

        if (response == null) {
            log.fine("Subscription failed, no response received");
            getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            subscription.fail(null);
                        }
                    }
            );
            return null;
        }

        final IncomingSubscribeResponseMessage responseMessage = new IncomingSubscribeResponseMessage(response);

        if (response.getOperation().isFailed()) {
            log.fine("Subscription failed, response was: " + responseMessage);
            getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            subscription.fail(responseMessage.getOperation());
                        }
                    }
            );
        } else if (!responseMessage.isVaildHeaders()) {
            log.severe("Subscription failed, invalid or missing (SID, Timeout) response headers");
            getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            subscription.fail(responseMessage.getOperation());
                        }
                    }
            );
        } else {
            log.fine("Subscription established, adding to registry, response was: " + response);
            subscription.setSubscriptionId(responseMessage.getSubscriptionId());
            subscription.setActualSubscriptionDurationSeconds(responseMessage.getSubscriptionDurationSeconds());

            getUpnpService().getRegistry().addRemoteSubscription(subscription);

            getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            subscription.establish();
                        }
                    }
            );
        }
        return responseMessage;

    }
}
