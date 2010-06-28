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
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;

import java.util.LinkedHashMap;
import java.util.Map;


public abstract class GENASubscription<S extends Service> {

    protected DeviceService deviceService;
    protected String subscriptionId;
    protected int requestedDurationSeconds = Constants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS;
    protected int actualDurationSeconds;
    protected UnsignedIntegerFourBytes currentSequence;
    protected Map<String, StateVariableValue<S>> currentValues = new LinkedHashMap();

    protected GENASubscription(DeviceService<S> deviceService) {
        this.deviceService = deviceService;
    }

    public GENASubscription(DeviceService<S> deviceService, int requestedDurationSeconds) {
        this(deviceService);
        this.requestedDurationSeconds = requestedDurationSeconds;
    }

    synchronized public DeviceService<S> getDeviceService() {
        return deviceService;
    }

    synchronized public String getSubscriptionId() {
        return subscriptionId;
    }

    synchronized public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    synchronized public int getRequestedDurationSeconds() {
        return requestedDurationSeconds;
    }

    synchronized public int getActualDurationSeconds() {
        return actualDurationSeconds;
    }

    synchronized public void setActualSubscriptionDurationSeconds(int seconds) {
        this.actualDurationSeconds = seconds;
    }

    synchronized public UnsignedIntegerFourBytes getCurrentSequence() {
        return currentSequence;
    }

    synchronized public Map<String, StateVariableValue<S>> getCurrentValues() {
        return currentValues;
    }

    public abstract void established();
    public abstract void eventReceived();

    @Override
    public String toString() {
        return "(GENASubscription, SID: " + getSubscriptionId() + ", SEQUENCE: " + getCurrentSequence() + ")";
    }
}
