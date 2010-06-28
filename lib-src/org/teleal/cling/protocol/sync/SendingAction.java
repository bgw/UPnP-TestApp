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

import java.util.logging.Logger;
import org.teleal.cling.UpnpService;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.control.IncomingActionResponseMessage;
import org.teleal.cling.model.message.control.OutgoingActionRequestMessage;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.transport.spi.UnsupportedDataException;
import org.teleal.cling.protocol.SendingSync;

import java.net.URL;


public class SendingAction extends SendingSync<OutgoingActionRequestMessage, IncomingActionResponseMessage> {

    protected static Logger log = Logger.getLogger(SendingAction.class.getName());

    final protected ActionInvocation actionInvocation;

    public SendingAction(UpnpService upnpService, ActionInvocation actionInvocation, URL controlURL) {
        super(upnpService, new OutgoingActionRequestMessage(actionInvocation, controlURL));
        this.actionInvocation = actionInvocation;
    }

    protected IncomingActionResponseMessage executeSync() {
        return invokeRemote(getInputMessage());
    }

    protected IncomingActionResponseMessage invokeRemote(OutgoingActionRequestMessage requestMessage) {
        Device device = actionInvocation.getAction().getService().getDeviceService().getDevice();

        log.fine("Sending outgoing action call '" + actionInvocation.getAction().getName() + "' to remote service of: " + device);
        IncomingActionResponseMessage responseMessage = null;
        try {

            StreamResponseMessage streamResponse = sendRemoteRequest(requestMessage);

            if (streamResponse == null) {
                log.fine("No connection or no no response received, returning null");
                actionInvocation.setFailure(new ActionException(ErrorCode.ACTION_FAILED, "Connection error or no response received"));
                return null;
            }

            responseMessage = new IncomingActionResponseMessage(streamResponse);

            if (responseMessage.isFailedNonRecoverable()) {
                log.fine("Response was a non-recoverable failure: " + responseMessage);
                throw new ActionException(
                        ErrorCode.ACTION_FAILED, "Non-recoverable remote execution failure: " + responseMessage.getOperation().getResponseDetails()
                );
            }

            if (isFailedTryAgain(responseMessage, requestMessage)) {
                return tryAgain(requestMessage);
            }

            if (responseMessage.isFailedRecoverable()) {
                handleResponseFailure(responseMessage);
            } else {
                handleResponse(responseMessage);
            }

            return responseMessage;


        } catch (ActionException ex) {
            log.fine("Remote action invocation failed, returning Internal Server Error message: " + ex.getMessage());
            actionInvocation.setFailure(ex);
            if (responseMessage == null || !responseMessage.getOperation().isFailed()) {
                return new IncomingActionResponseMessage(new UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR));
            } else {
                return responseMessage;
            }
        }
    }

    protected boolean isFailedTryAgain(IncomingActionResponseMessage responseMessage,
                                       OutgoingActionRequestMessage requestMessage) throws ActionException {

        if (!responseMessage.getOperation().isFailed())
            return false;
        if (responseMessage.getOperation().getStatusCode() != UpnpResponse.Status.METHOD_NOT_SUPPORTED.getStatusCode())
            return false;

        if (requestMessage.getOperation().getMethod().equals(UpnpRequest.Method.MPOST)) {
            // We already tried that, so don't do it again
            throw new ActionException(
                    ErrorCode.ACTION_FAILED, "Second request (with MPOST) also failed, received Method Not Allowed again"
            );
        }
        return true;
    }

    protected StreamResponseMessage sendRemoteRequest(OutgoingActionRequestMessage requestMessage) throws ActionException {
        try {
            log.fine("Writing SOAP request body of: " + requestMessage);
            getUpnpService().getConfiguration().getSoapActionProcessor().writeBody(requestMessage, actionInvocation);

            log.fine("Sending SOAP body of message as stream to remote device");
            return getUpnpService().getRouter().send(requestMessage);

        } catch (UnsupportedDataException ex) {
            log.fine("Error writing SOAP body, cause: " + ex.getCause());
            throw new ActionException(ErrorCode.ACTION_FAILED, "Error writing request message. " + ex.getMessage());
        }
    }

    protected void handleResponse(IncomingActionResponseMessage responseMsg) throws ActionException {

        try {
            log.fine("Received response for outgoing call, reading SOAP response body: " + responseMsg);
            getUpnpService().getConfiguration().getSoapActionProcessor().readBody(responseMsg, actionInvocation);
        } catch (UnsupportedDataException ex) {
            log.fine("Error reading SOAP body, cause: " + ex.getCause());
            throw new ActionException(ErrorCode.ACTION_FAILED, "Error reading response message. " + ex.getMessage());
        }
    }

    protected void handleResponseFailure(IncomingActionResponseMessage responseMsg) throws ActionException {

        try {
            log.fine("Received response with Internal Server Error, reading SOAP failure message");
            getUpnpService().getConfiguration().getSoapActionProcessor().readBody(responseMsg, actionInvocation);
        } catch (UnsupportedDataException ex) {
            log.fine("Error reading SOAP body, cause: " + ex.getCause());
            throw new ActionException(ErrorCode.ACTION_FAILED, "Error reading response failure message. " + ex.getMessage());
        }
    }

    protected IncomingActionResponseMessage tryAgain(OutgoingActionRequestMessage originalRequestMessage) throws ActionException {
        log.fine("Received 'Method Not Supported', trying again with M-POST");
        OutgoingActionRequestMessage mpostRequestMessage =
                new OutgoingActionRequestMessage(
                        actionInvocation.getAction(),
                        new UpnpRequest(UpnpRequest.Method.MPOST, originalRequestMessage.getOperation().getURI())
                );

        return invokeRemote(mpostRequestMessage);
    }

}

/*

- send request
   - UnsupportedDataException: Can't write body

- streamResponseMessage is null: No response received, return null to client

- streamResponseMessage >= 300 && !(405 || 500): Response was HTTP failure, set on anemic response and return

- streamResponseMessage >= 300 && 405: Try request again with different headers
   - UnsupportedDataException: Can't write body
   - (The whole streamResponse conditions apply again but this time, ignore 405)

- streamResponseMessage >= 300 && 500 && lastExecutionFailure != null: Try to read SOAP failure body
   - UnsupportedDataException: Can't read body

- streamResponseMessage < 300: Response was OK, try to read response body
   - UnsupportedDataException: Can't read body


*/