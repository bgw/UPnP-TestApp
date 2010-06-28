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

import org.teleal.cling.model.ModelUtil;
import org.teleal.cling.model.ServiceManager;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.action.ActionExecutor;
import org.teleal.cling.model.state.StateVariableAccessor;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class LocalService<T> extends Service {

    public static Constructor<LocalService> getConstructor() {
        try {
            return LocalService.class.getConstructor(
                    Action[].class, StateVariable[].class
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    final protected Map<Action, ActionExecutor> actionExecutors;
    final protected Map<StateVariable, StateVariableAccessor> stateVariableAccessors;
    final protected Set<Class> stringConvertibleTypes;
    final protected boolean supportsQueryStateVariables;

    protected ServiceManager manager;

    public LocalService(Action[] actions, StateVariable[] stateVariables) throws ValidationException {
        super(actions, stateVariables);
        this.manager = null;
        this.actionExecutors = new HashMap();
        this.stateVariableAccessors = new HashMap();
        this.stringConvertibleTypes = new HashSet();
        this.supportsQueryStateVariables = true;
    }

    public LocalService(Map<Action, ActionExecutor> actionExecutors,
                        Map<StateVariable, StateVariableAccessor> stateVariableAccessors,
                        Set<Class> stringConvertibleTypes,
                        boolean supportsQueryStateVariables) throws ValidationException {

        this(null, actionExecutors, stateVariableAccessors,
                stringConvertibleTypes, supportsQueryStateVariables);
    }

    public LocalService(UDAVersion version,
                        Map<Action, ActionExecutor> actionExecutors,
                        Map<StateVariable, StateVariableAccessor> stateVariableAccessors,
                        Set<Class> stringConvertibleTypes,
                        boolean supportsQueryStateVariables) throws ValidationException {

        super(version,
                actionExecutors.keySet().toArray(new Action[actionExecutors.size()]),
                stateVariableAccessors.keySet().toArray(new StateVariable[stateVariableAccessors.size()])
        );

        this.supportsQueryStateVariables = supportsQueryStateVariables;
        this.stringConvertibleTypes = stringConvertibleTypes;
        this.stateVariableAccessors = stateVariableAccessors;
        this.actionExecutors = actionExecutors;

    }

    synchronized public void setManager(ServiceManager<T> manager) {
        if (this.manager != null) {
            throw new IllegalStateException("Manager is final");
        }
        this.manager = manager;
    }

    synchronized public ServiceManager<T> getManager() {
        if (manager == null) {
            throw new IllegalStateException("Unmanaged service, no implementation instance available");
        }
        return manager;
    }

    public boolean isSupportsQueryStateVariables() {
        return supportsQueryStateVariables;
    }

    public Set<Class> getStringConvertibleTypes() {
        return stringConvertibleTypes;
    }

    public boolean isStringConvertibleType(Object o) {
        return o != null && isStringConvertibleType(o.getClass());
    }

    public boolean isStringConvertibleType(Class clazz) {
        return ModelUtil.isStringConvertibleType(getStringConvertibleTypes(), clazz);
    }

    public StateVariableAccessor getAccessor(String stateVariableName) {
        StateVariable sv;
        return (sv = getStateVariable(stateVariableName)) != null ? getAccessor(sv) : null;
    }

    public StateVariableAccessor getAccessor(StateVariable stateVariable) {
        return stateVariableAccessors.get(stateVariable);
    }

    public ActionExecutor getExecutor(String actionName) {
        Action action;
        return (action = getAction(actionName)) != null ? getExecutor(action) : null;
    }

    public ActionExecutor getExecutor(Action action) {
        return actionExecutors.get(action);
    }

    @Override
    public Action getQueryStateVariableAction() {
        return getAction(QueryStateVariableAction.ACTION_NAME);
    }

    @Override
    public String toString() {
        return super.toString()  + " Manager: " + manager;
    }
}