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
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.gena.IncomingEventRequestMessage;
import org.teleal.cling.model.message.gena.OutgoingEventResponseMessage;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.transport.spi.UnsupportedDataException;
import org.teleal.cling.UpnpService;
import org.teleal.cling.protocol.ReceivingSync;

import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public class ReceivingEvent extends ReceivingSync<StreamRequestMessage, OutgoingEventResponseMessage> {

    private static Logger log = Logger.getLogger(ReceivingEvent.class.getName());

    public ReceivingEvent(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    protected OutgoingEventResponseMessage executeSync() {

        ContentTypeHeader contentTypeHeader =
                getInputMessage().getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class);

        if (contentTypeHeader == null || !contentTypeHeader.isUDACompliant()) {
            log.fine("Unsupported content type in event request: " + contentTypeHeader);
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.UNSUPPORTED_MEDIA_TYPE));
        }

        Resource foundResource = getUpnpService().getRegistry().getResource(getInputMessage().getUri());

        if (foundResource == null || !foundResource.getType().equals(Resource.Type.EVENT_CALLBACK)) {
            log.fine("No local resource found: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.NOT_FOUND));
        }

        log.fine("Found local event callback matching relative request URI: " + getInputMessage().getUri());
        DeviceService deviceService = (DeviceService) foundResource.getModel();

        final IncomingEventRequestMessage requestMessage =
                new IncomingEventRequestMessage(getInputMessage(), deviceService);

        // Error conditions UDA 1.0 section 4.2.1
        if (requestMessage.getSubscrptionId() == null) {
            log.fine("Subscription ID missing in event request: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        if (!requestMessage.hasValidNotificationHeaders()) {
            log.fine("Missing NT and/or NTS headers in event request: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.BAD_REQUEST));
        }

        if (!requestMessage.hasValidNotificationHeaders()) {
            log.fine("Invalid NT and/or NTS headers in event request: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        if (requestMessage.getSequence() == null) {
            log.fine("Sequence missing in event request: " + getInputMessage());
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        try {

            getUpnpService().getConfiguration().getGenaEventProcessor().readBody(requestMessage);

        } catch (UnsupportedDataException ex) {
            log.fine("Can't read request body, " + ex);
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR));
        }

        final RemoteGENASubscription subscription =
                getUpnpService().getRegistry().getRemoteSubscription(requestMessage.getSubscrptionId());

        if (subscription == null) {
            log.fine("Invalid subscription ID, no active subscription: " + requestMessage);
            return new OutgoingEventResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(
                new Runnable() {
                    public void run() {
                        log.fine("Calling active subscription with event state variable values");
                        subscription.receive(
                                requestMessage.getSequence(),
                                requestMessage.getStateVariableValues()
                        );
                    }
                }
        );

        return new OutgoingEventResponseMessage();

    }
}
