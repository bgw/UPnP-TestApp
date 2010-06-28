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

package org.teleal.cling.controlpoint;

import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.control.IncomingActionResponseMessage;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.protocol.sync.SendingAction;

import java.net.URL;
import java.util.concurrent.Executor;


public abstract class ActionCallback implements Runnable {

    protected final ActionInvocation actionInvocation;

    protected ControlPoint controlPoint;

    protected ActionCallback(ActionInvocation actionInvocation, ControlPoint controlPoint) {
        this.actionInvocation = actionInvocation;
        this.controlPoint = controlPoint;
    }

    protected ActionCallback(ActionInvocation actionInvocation) {
        this.actionInvocation = actionInvocation;
    }

    public ActionInvocation getActionInvocation() {
        return actionInvocation;
    }

    synchronized public ControlPoint getControlPoint() {
        return controlPoint;
    }

    synchronized public void setControlPoint(ControlPoint controlPoint) {
        this.controlPoint = controlPoint;
    }

    public void run() {
        if (getControlPoint()  == null) {
            throw new IllegalStateException("Callback must be executed through ControlPoint");
        }

        Service service = actionInvocation.getAction().getService();

        // Local execution
        if (service instanceof LocalService) {
            LocalService localService = (LocalService)service;

            // Executor validates input inside the execute() call immediately
            localService.getExecutor(actionInvocation.getAction()).execute(actionInvocation);

            if (actionInvocation.getFailure() != null) {
                failure(actionInvocation, null);
            } else {
                success(actionInvocation);
            }

        // Remote execution
        } else if (service instanceof RemoteService){
            RemoteService remoteService = (RemoteService)service;

            // Validate remote service here
            if (!actionInvocation.getInput().isValid()) {
                actionInvocation.setFailure(new ActionException(ErrorCode.INVALID_ARGS));
                failure(actionInvocation, null);
                return;
            }

            // Figure out the remote URL where we'd like to send the action request to
            URL controLURL = ((RemoteDevice)remoteService.getDeviceService().getDevice()).normalizeURI(
                    remoteService.getDeviceService().getControlURI()
            );

            // Do it
            SendingAction prot = getControlPoint().getProtocolFactory().createSendingAction(actionInvocation, controLURL);
            prot.run();

            IncomingActionResponseMessage response = prot.getOutputMessage();

            if (response == null) {
                failure(actionInvocation, null);
            } else if (response.getOperation().isFailed()) {
                failure(actionInvocation, response.getOperation());
            } else {
                success(actionInvocation);
            }
        }
    }

    protected String createDefaultFailureMessage(ActionInvocation actionInvocation, UpnpResponse operation) {
        String message = "Error: ";
        final ActionException exception = actionInvocation.getFailure();
        if (exception != null) {
            message = message + exception.getMessage();
        }
        if (operation != null) {
            message = message + " (HTTP response was: " + operation.getResponseDetails() + ")";
        }
        return message;
    }

    public abstract void success(ActionInvocation actionInvocation);
    public abstract void failure(ActionInvocation actionInvocation, UpnpResponse operation);

    @Override
    public String toString() {
        return "(ActionCallback) " + actionInvocation;
    }
}
