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

import org.teleal.cling.model.Constants;
import org.teleal.cling.model.ServiceManager;
import org.teleal.cling.model.message.header.SubscriptionIdHeader;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public abstract class LocalGENASubscription extends GENASubscription<LocalService> implements PropertyChangeListener {

    private static Logger log = Logger.getLogger(LocalGENASubscription.class.getName());

    final List<URL> callbackURLs;

    // Moderation history
    final Map<String, Long> lastSentTimestamp = new HashMap();
    final Map<String, Long> lastSentNumericValue = new HashMap();

    public LocalGENASubscription(DeviceService<LocalService> deviceService, List<URL> callbackURLs) {
        this(deviceService, null, callbackURLs);
    }

    public LocalGENASubscription(DeviceService<LocalService> deviceService, Integer requestedDurationSeconds, List<URL> callbackURLs) {
        super(deviceService);

        setSubscriptionDuration(requestedDurationSeconds);

        try {
            log.fine("Reading initial state of local service at subscription time");
            long currentTime = new Date().getTime();
            this.currentValues.clear();

            List<StateVariableValue> values =
                    getDeviceService().getService().getManager().readEventedStateVariableValues();

            log.finer("Got evented state variable values: " + values.size());

            for (StateVariableValue value : values) {
                this.currentValues.put(value.getStateVariable().getName(), value);

                if (log.isLoggable(Level.FINER)) {
                    log.finer("Read state variable value '" + value.getStateVariable().getName() + "': " + value.toString());
                }

                // Preserve "last sent" state for future moderation
                lastSentTimestamp.put(value.getStateVariable().getName(), currentTime);
                if (value.getStateVariable().isModeratedNumericType()) {
                    lastSentNumericValue.put(value.getStateVariable().getName(), Long.valueOf(value.toString()));
                }
            }
        } catch (Exception ex) {
            log.fine("Creation of local subscription failed: " + ex.toString());
            failed(ex);
        }

        this.subscriptionId = SubscriptionIdHeader.PREFIX + UUID.randomUUID();

        this.currentSequence = new UnsignedIntegerFourBytes(0);

        this.callbackURLs = callbackURLs;
    }

    synchronized public List<URL> getCallbackURLs() {
        return callbackURLs;
    }

    synchronized public void registerOnService() {
        getDeviceService().getService().getManager()
                .getPropertyChangeSupport().addPropertyChangeListener(this);
    }

    synchronized public void establish() {
        established();
    }

    synchronized public void end(CancelReason reason) {
        getDeviceService().getService().getManager()
                .getPropertyChangeSupport().removePropertyChangeListener(this);
        ended(reason);
    }

    synchronized public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals(ServiceManager.EVENTED_STATE_VARIABLES)) {

            long currentTime = new Date().getTime();

            Collection<StateVariableValue> newValues = (Collection) e.getNewValue();
            Set<String> excludedVariables = moderateStateVariables(currentTime, newValues);

            // Map<String, StateVariableValue> oldValues = currentValues;
            currentValues.clear();
            for (StateVariableValue newValue : newValues) {
                String name = newValue.getStateVariable().getName();
                if (!excludedVariables.contains(name)) {
                    log.fine("Adding state variable value to current values of event: " + newValue.getStateVariable() + " = " + newValue);
                    currentValues.put(newValue.getStateVariable().getName(), newValue);

                    // Preserve "last sent" state for future moderation
                    lastSentTimestamp.put(name, currentTime);
                    if (newValue.getStateVariable().isModeratedNumericType()) {
                        lastSentNumericValue.put(name, Long.valueOf(newValue.toString()));
                    }
                }
            }

            if (currentValues.size() > 0) {
                log.fine("State of local service changed, propagating event to subscription: " + this);
                // TODO: I'm not happy with this design, this dispatches to a separate thread which _then_
                // is supposed to lock and read the values off this instance. That obviously doesn't work
                // so it's currently a hack in in SendingEvent.java
                eventReceived();
            } else {
                log.fine("No state variable values for event (all moderated out?), not triggering event");
            }

        }
    }

    synchronized protected Set<String> moderateStateVariables(long currentTime, Collection<StateVariableValue> values) {

        Set<String> excludedVariables = new HashSet();

        // Moderate event variables that have a maximum rate or minimum delta
        for (StateVariableValue stateVariableValue : values) {

            StateVariable stateVariable = stateVariableValue.getStateVariable();
            String stateVariableName = stateVariableValue.getStateVariable().getName();

            if (stateVariable.getEventDetails().getEventMaximumRateMilliseconds() == 0 &&
                    stateVariable.getEventDetails().getEventMinimumDelta() == 0) {
                log.finer("Variable is not moderated: " + stateVariable);
                continue;
            }

            // That should actually never happen, because we always "send" it as the initial state/event
            if (!lastSentTimestamp.containsKey(stateVariableName)) {
                log.finer("Variable is moderated but was never sent before: " + stateVariable);
                continue;
            }

            if (stateVariable.getEventDetails().getEventMaximumRateMilliseconds() > 0) {
                long timestampLastSent = lastSentTimestamp.get(stateVariableName);
                long timestampNextSend = timestampLastSent + (stateVariable.getEventDetails().getEventMaximumRateMilliseconds());
                if (currentTime <= timestampNextSend) {
                    log.finer("Excluding state variable with maximum rate: " + stateVariable);
                    excludedVariables.add(stateVariableName);
                    continue;
                }
            }

            if (stateVariable.isModeratedNumericType() && lastSentNumericValue.get(stateVariableName) != null) {

                long oldValue = Long.valueOf(lastSentNumericValue.get(stateVariableName));
                long newValue = Long.valueOf(stateVariableValue.toString());
                long minDelta = stateVariable.getEventDetails().getEventMinimumDelta();

                if (newValue > oldValue && newValue - oldValue < minDelta) {
                    log.finer("Excluding state variable with minimum delta: " + stateVariable);
                    excludedVariables.add(stateVariableName);
                    continue;
                }

                if (newValue < oldValue && oldValue - newValue < minDelta) {
                    log.finer("Excluding state variable with minimum delta: " + stateVariable);
                    excludedVariables.add(stateVariableName);
                }
            }

        }
        return excludedVariables;
    }

    synchronized public void incrementSequence() {
        this.currentSequence.increment(true);
    }

    synchronized public void setSubscriptionDuration(Integer requestedDurationSeconds) {
        this.requestedDurationSeconds =
                requestedDurationSeconds == null
                        ? Constants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS
                        : requestedDurationSeconds;

        setActualSubscriptionDurationSeconds(this.requestedDurationSeconds);
    }

    public abstract void failed(Exception ex);

    public abstract void ended(CancelReason reason);

}
