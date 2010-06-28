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

package org.teleal.cling.model.action;

import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.state.StateVariableAccessor;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.common.util.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public class MethodActionExecutor extends ActionExecutor {

    private static Logger log = Logger.getLogger(MethodActionExecutor.class.getName());

    protected Method method;

    public MethodActionExecutor(Method method) {
        this.method = method;
    }

    public MethodActionExecutor(Map<ActionArgument<LocalService>, StateVariableAccessor> outputArgumentAccessors, Method method) {
        super(outputArgumentAccessors);
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    protected void execute(ActionInvocation<LocalService> actionInvocation, Object serviceImpl) throws Exception {

        // Find the "real" parameters of the method we want to call, and create arguments
        Object[] inputArgumentValues = createInputArgumentValues(actionInvocation, method);

        // Simple case: no output arguments
        if (actionInvocation.getOutput().getArguments().size() == 0) {
            log.fine("Calling local service method with no output arguments");
            Reflections.invoke(method, serviceImpl, inputArgumentValues);
            return;
        }

        boolean isVoid = method.getReturnType().equals(Void.TYPE);

        log.fine("Calling local service method with output arguments");
        if (isVoid) {

            log.fine("Action method is void, calling declared accessors(s) on service instance to retrieve ouput argument(s)");
            Reflections.invoke(method, serviceImpl, inputArgumentValues);
            Object result = readOutputArgumentValues(actionInvocation.getAction(), serviceImpl);
            setOutputArgumentValues(actionInvocation, result);

        } else if (isUseOutputArgumentAccessors(actionInvocation)) {

            log.fine("Action method is not void, calling declared accessor(s) on returned instance to retrieve ouput argument(s)");
            Object returnedInstance = Reflections.invoke(method, serviceImpl, inputArgumentValues);
            Object result = readOutputArgumentValues(actionInvocation.getAction(), returnedInstance);
            setOutputArgumentValues(actionInvocation, result);

        } else {

            log.fine("Action method is not void, using returned value as (single) ouput argument");
            Object result = Reflections.invoke(method, serviceImpl, inputArgumentValues);
            setOutputArgumentValues(actionInvocation, result);
        }
    }

    protected boolean isUseOutputArgumentAccessors(ActionInvocation<LocalService> actionInvocation) {
        for (ActionArgument argument : actionInvocation.getOutput().getArguments()) {
            // If there is one output argument for which we have an accessor, all arguments need accessors
            if (getOutputArgumentAccessors().get(argument) != null) {
                return true;
            }
        }
        return false;
    }

    protected Object[] createInputArgumentValues(ActionInvocation<LocalService> actionInvocation, Method method) throws ActionException {

        LocalService service = actionInvocation.getAction().getService();

        Object[] values = new Object[actionInvocation.getInput().getValues().length];
        int i = 0;
        for (ActionArgumentValue inputCallValue : actionInvocation.getInput().getValues()) {
            try {
                Class methodParameterType = method.getParameterTypes()[i];
                if (service.isStringConvertibleType(methodParameterType) && !methodParameterType.isEnum()) {
                    // Note that we can't instantiate Enums!
                    Constructor<String> ctor = methodParameterType.getConstructor(String.class);
                    log.finer("Creating new input argument value instance with String.class constructor of type: " + methodParameterType);
                    Object o = ctor.newInstance(inputCallValue.getValue().toString());
                    values[i++] = o;
                } else {
                    values[i++] = inputCallValue.getValue();
                }
            } catch (Exception ex) {
                throw new ActionException(
                        ErrorCode.ACTION_FAILED, "Can't convert input argment string to desired type: " + ex
                );
            }
        }
        return values;
    }

}
