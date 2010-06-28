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

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.Resource;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.control.IncomingActionRequestMessage;
import org.teleal.cling.model.message.control.OutgoingActionResponseMessage;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.protocol.ReceivingSync;
import org.teleal.cling.transport.spi.UnsupportedDataException;
import org.teleal.common.util.Exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;


public class ReceivingAction extends ReceivingSync<StreamRequestMessage, StreamResponseMessage> {

    private static Logger log = Logger.getLogger(ReceivingAction.class.getName());

    public ReceivingAction(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    protected StreamResponseMessage executeSync() {

        if (!getInputMessage().hasUDATextContentType()) {
            return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.UNSUPPORTED_MEDIA_TYPE));
        }

        Resource foundResource = getUpnpService().getRegistry().getResource(getInputMessage().getUri());

        if (foundResource == null) {
            log.fine("No local resource found: " + getInputMessage());
            return null; // NOT FOUND
        }

        switch (foundResource.getType()) {

            case CONTROL:

                log.fine("Found local service control matching relative request URI: " + getInputMessage().getUri());
                DeviceService deviceService = (DeviceService) foundResource.getModel();
                LocalService service;
                if (deviceService.getService() instanceof LocalService) {
                    service = (LocalService)deviceService.getService();
                } else {
                    return null;
                }

                ActionInvocation invocation;
                OutgoingActionResponseMessage responseMessage;

                try {
                    log.finer("Got local device service model: " + deviceService);

                    // Throws ActionException if the action can't be found
                    IncomingActionRequestMessage requestMessage =
                            new IncomingActionRequestMessage(getInputMessage(), deviceService);

                    log.finer("Created incoming action request message: " + requestMessage);
                    invocation = new ActionInvocation(requestMessage.getAction());

                    // Throws UnsupportedDataException if the body can't be read
                    log.fine("Reading body of request message");
                    getUpnpService().getConfiguration().getSoapActionProcessor().readBody(requestMessage, invocation);

                    log.fine("Executing on local service: " + invocation);
                    service.getExecutor(invocation.getAction()).execute(invocation);

                    if (invocation.getFailure() == null) {
                        responseMessage =
                                new OutgoingActionResponseMessage(invocation.getAction());
                    } else {
                        responseMessage =
                                new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR, invocation.getAction());

                    }

                } catch (ActionException ex) {
                    log.finer("Error executing local action: " + ex);

                    invocation = new ActionInvocation(ex);
                    responseMessage = new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);

                } catch (UnsupportedDataException ex) {
                    if (log.isLoggable(Level.FINER)) {
                        log.log(Level.FINER, "Error reading action request XML body: " + ex.toString(), Exceptions.unwrap(ex));
                    }

                    invocation = new ActionInvocation(new ActionException(ErrorCode.ACTION_FAILED, ex.getMessage()));
                    responseMessage = new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);

                }

                try {

                    log.fine("Writing body of response message");
                    getUpnpService().getConfiguration().getSoapActionProcessor().writeBody(responseMessage, invocation);

                    log.fine("Returning finished response message: " + responseMessage);
                    return responseMessage;

                } catch (UnsupportedDataException ex) {
                    log.fine("Failure writing body of response message, sending 500 Internal Server Error without body");
                    return new StreamResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
                }

            default:
                return null; // NOT FOUND
        }

    }
}
