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

package org.teleal.cling.model.gena;

import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.common.util.URIUtil;

import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.Collection;

/**
 * @author Christian Bauer
 */
public abstract class RemoteGENASubscription extends GENASubscription<RemoteService> {

    public static final String ALL_STATE_VARIABLE_VALUES = "_allStateVariableValues";

    protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    protected RemoteGENASubscription(DeviceService deviceService) {
        super(deviceService);
    }

    protected RemoteGENASubscription(DeviceService deviceService, int requestedDurationSeconds) {
        super(deviceService, requestedDurationSeconds);
    }
    
    synchronized public URL getEventSubscriptionURL() {
        return ((RemoteDevice)getDeviceService().getDevice()).normalizeURI(
                deviceService.getEventSubscriptionURI()
        );
    }

    synchronized public URL getLocalEventCallbackURL(int streamListenPort) {
        return URIUtil.createAbsoluteURL(
                ((RemoteDevice)getDeviceService().getDevice()).getIdentity().getReachableLocalAddress(),
                streamListenPort,
                getDeviceService().getLocalEventCallbackURI()
        );
    }

    synchronized public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    /* The following four methods should always be called in an independent thread, not within the
       message receiving thread. Otherwise the user who implements the abstract delegate methods can
       block the network communication.
     */

    synchronized public void establish() {
        established();
    }

    synchronized public void fail(UpnpResponse responseStatus) {
        failed(responseStatus);
    }

    synchronized public void end(CancelReason reason, UpnpResponse response) {
        ended(reason, response);
    }

    synchronized public void receive(UnsignedIntegerFourBytes sequence, Collection<StateVariableValue> newValues) {

        if (this.currentSequence != null) {

            // TODO: Handle rollover to 1!
            if (this.currentSequence.getValue().equals(this.currentSequence.getBits().getMaxValue()) && sequence.getValue() == 1) {
                System.err.println("TODO: HANDLE ROLLOVER");
                return;
            }

            if (this.currentSequence.getValue() >= sequence.getValue()) {
                return;
            }

            int difference;
            long expectedValue = currentSequence.getValue() + 1;
            if ((difference = (int) (sequence.getValue() - expectedValue)) != 0) {
                eventsMissed(difference);
            }

        }
        this.currentSequence = sequence;

        for (StateVariableValue newValue : newValues) {

            StateVariableValue oldValue =
                    currentValues.put(newValue.getStateVariable().getName(), newValue);

            // Fire individual property changes for individual state variables
            propertyChangeSupport.firePropertyChange(
                    newValue.getStateVariable().getName(),
                    oldValue != null ? oldValue.getValue() : null,
                    newValue.getValue()
            );
        }

        // Fire a property change for the whole collection
        propertyChangeSupport.firePropertyChange(
                ALL_STATE_VARIABLE_VALUES,
                currentValues.values(),
                newValues
        );

        eventReceived();
    }

    public abstract void failed(UpnpResponse responseStatus);
    public abstract void ended(CancelReason reason, UpnpResponse responseStatus);
    public abstract void eventsMissed(int numberOfMissedEvents);

    @Override
    public String toString() {
        return "(SID: " + getSubscriptionId() + ") " + getDeviceService();
    }
}
