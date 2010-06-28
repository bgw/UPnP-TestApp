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

import org.teleal.cling.model.Command;
import org.teleal.cling.model.ServiceManager;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.state.StateVariableAccessor;
import org.teleal.cling.model.types.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public abstract class ActionExecutor {

    private static Logger log = Logger.getLogger(ActionExecutor.class.getName());

    protected Map<ActionArgument<LocalService>, StateVariableAccessor> outputArgumentAccessors = new HashMap();

    protected ActionExecutor() {
    }

    protected ActionExecutor(Map<ActionArgument<LocalService>, StateVariableAccessor> outputArgumentAccessors) {
        this.outputArgumentAccessors = outputArgumentAccessors;
    }

    public Map<ActionArgument<LocalService>, StateVariableAccessor> getOutputArgumentAccessors() {
        return outputArgumentAccessors;
    }

    public void execute(final ActionInvocation<LocalService> actionInvocation) {

        log.fine("Invoking on local service: " + actionInvocation);

        if (!actionInvocation.getInput().isValid()) {
            actionInvocation.setFailure(new ActionException(ErrorCode.INVALID_ARGS));
            return;
        }

        final LocalService service = actionInvocation.getAction().getService();

        try {

            if (service.getManager() == null) {
                throw new IllegalStateException("Service has no implementation factory, can't get service instance");
            }

            service.getManager().execute(new Command() {
                public void execute(ServiceManager serviceManager) throws Exception {
                    ActionExecutor.this.execute(
                            actionInvocation,
                            serviceManager.getImplementation()
                    );
                }

                @Override
                public String toString() {
                    return "Action invocation: " + actionInvocation.getAction();
                }
            });

        } catch (ActionException ex) {
            log.fine("ActionException thrown by service method, wrapping in invocation and returning: " + ex);
            actionInvocation.setFailure(ex);
        } catch (Exception ex) {
            log.fine("Exception thrown by execution, wrapping in ActionException and returning: " + ex);
            actionInvocation.setFailure(
                    new ActionException(
                            ErrorCode.ACTION_FAILED,
                            "Action method invocation failed: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()),
                            ex
                    )
            );
        }
    }

    protected abstract void execute(ActionInvocation<LocalService> actionInvocation, Object serviceImpl) throws Exception;

    protected Object readOutputArgumentValues(Action<LocalService> action, Object instance) throws Exception {
        Object[] results = new Object[action.getOutputArguments().size()];
        log.fine("Attempting to retrieve output argument values using accessor: " + results.length);

        int i = 0;
        for (ActionArgument outputArgument : action.getOutputArguments()) {
            log.finer("Calling acccessor method for: " + outputArgument);

            StateVariableAccessor accessor = getOutputArgumentAccessors().get(outputArgument);
            if (accessor != null) {
                log.fine("Calling accessor to read output argument value: " + accessor);
                results[i++] = accessor.read(instance);
            } else {
                throw new IllegalStateException("No accessor bound for: " + outputArgument);
            }
        }

        if (results.length == 1) {
            return results[0];
        }
        return results.length > 0 ? results : null;
    }

    protected void setOutputArgumentValues(ActionInvocation<LocalService> actionInvocation, Object result) throws Exception {

        LocalService service = actionInvocation.getAction().getService();

        if (result instanceof Object[]) {

            Object[] results = (Object[]) result;
            log.fine("Result of invocation is Object[], setting output argument values: " + results.length);
            for (Object o : results) {
                if (service.isStringConvertibleType(o)) {
                    actionInvocation.getOutput().addValue(o.toString());
                } else {
                    actionInvocation.getOutput().addValue(o);
                }
            }

        } else if (result != null) {

            if (service.isStringConvertibleType(result)) {
                log.fine("Result of invocation matches convertible type, setting toString() single output argument value");
                actionInvocation.getOutput().addValue(result.toString());
            } else {
                log.fine("Result of invocation is Object, setting single output argument value");
                actionInvocation.getOutput().addValue(result);
            }

        } else {

            log.fine("Result of invocation is null, not setting any output argument value(s)");
        }

        // TODO: Some error message about WHAT actually failed?
        if (!actionInvocation.getOutput().isValid()) {
            throw new ActionException(
                    ErrorCode.ACTION_FAILED, "Output arguments validation failed"
            );
        }
    }

}
