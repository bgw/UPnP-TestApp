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

package org.teleal.cling.model.meta;

import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.types.Datatype;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class Service<S extends Service> {

    final private UDAVersion version;

    final private Map<String, Action> actions = new HashMap();
    final private Map<String, StateVariable> stateVariables = new HashMap();

    // Package mutable state
    private DeviceService deviceService;

    public Service(Action<S>[] actions, StateVariable<S>[] stateVariables) throws ValidationException {
        this(null, actions, stateVariables);
    }

    public Service(UDAVersion version, Action<S>[] actions, StateVariable<S>[] stateVariables) throws ValidationException {
        this.version = version == null ? new UDAVersion() : version;

        if (actions != null) {
            for (Action action : actions) {
                this.actions.put(action.getName(), action);
                action.setService(this);
            }
        }

        if (stateVariables != null) {
            for (StateVariable stateVariable : stateVariables) {
                this.stateVariables.put(stateVariable.getName(), stateVariable);
                stateVariable.setService(this);
            }
        }

        List<ValidationError> errors = validate();
        if (errors.size() > 0) {
            throw new ValidationException("Validation of device graph failed, call getErrors() on exception", errors);
        }
    }

    public UDAVersion getVersion() {
        return version;
    }

    public boolean hasActions() {
        return getActions() != null && getActions().length > 0;
    }

    public Action<S>[] getActions() {
        return actions == null ? null : actions.values().toArray(new Action[actions.values().size()]);
    }

    public boolean hasStateVariables() {
        // TODO: Spec says always has to have at least one...
        return getStateVariables() != null && getStateVariables().length > 0;
    }

    public StateVariable<S>[] getStateVariables() {
        return stateVariables == null ? null : stateVariables.values().toArray(new StateVariable[stateVariables.values().size()]);
    }

    public DeviceService<S> getDeviceService() {
        return deviceService;
    }

    void setDeviceService(DeviceService<S> deviceService) {
        if (this.deviceService != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.deviceService = deviceService;
    }

    public Action<S> getAction(String name) {
        return actions == null ? null : actions.get(name);
    }

    public StateVariable<S> getStateVariable(String name) {
        // Some magic necessary for the deprected 'query state variable' action stuff
        if (QueryStateVariableAction.VIRTUAL_STATEVARIABLE_INPUT.equals(name)) {
            return new StateVariable(
                    QueryStateVariableAction.VIRTUAL_STATEVARIABLE_INPUT,
                    new StateVariableTypeDetails(Datatype.Builtin.STRING.getDatatype())
            );
        }
        if (QueryStateVariableAction.VIRTUAL_STATEVARIABLE_OUTPUT.equals(name)) {
            return new StateVariable(
                    QueryStateVariableAction.VIRTUAL_STATEVARIABLE_OUTPUT,
                    new StateVariableTypeDetails(Datatype.Builtin.STRING.getDatatype())
            );
        }
        return stateVariables == null ? null : stateVariables.get(name);
    }

    public StateVariable<S> getRelatedStateVariable(ActionArgument argument) {
        return getStateVariable(argument.getRelatedStateVariableName());
    }

    public Datatype<S> getDatatype(ActionArgument argument) {
        return getRelatedStateVariable(argument).getTypeDetails().getDatatype();
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        // TODO: If the service has no evented variables, it should not have an event subscription URL, which means
        // the url element in the device descriptor must be present, but empty!!!!

        if (getStateVariables().length == 0) {
            errors.add(new ValidationError(
                    getClass(),
                    "stateVariables",
                    "Service must have at least one state variable"
            ));
        }

        if (hasActions()) {
            for (Action action : getActions()) {
                errors.addAll(action.validate());
            }
        }

        if (hasStateVariables()) {
            for (StateVariable stateVariable : getStateVariables()) {
                errors.addAll(stateVariable.validate());
            }
        }

        return errors;
    }

    public abstract Action getQueryStateVariableAction();

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}