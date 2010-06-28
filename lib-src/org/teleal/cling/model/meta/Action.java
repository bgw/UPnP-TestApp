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
import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.action.ActionInvocationValues;

import java.util.ArrayList;
import java.util.List;


public class Action<S extends Service> implements Validatable {

    final private String name;
    final private ActionArgument[] arguments;

    // Package mutable state
    private S service;

    public Action(String name, ActionArgument[] arguments) {
        this.name = name;
        this.arguments = arguments;
        if (this.arguments != null) {
            for (ActionArgument argument : this.arguments) {
                argument.setAction(this);
            }
        }
    }

    public String getName() {
        return name;
    }

    public boolean hasArguments() {
        return getArguments() != null && getArguments().length > 0;
    }

    public ActionArgument[] getArguments() {
        return arguments;
    }

    public S getService() {
        return service;
    }

    void setService(S service) {
        if (this.service != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.service = service;
    }

    public List<ActionArgument<S>> getInputArguments() {
        List<ActionArgument<S>> args = new ArrayList();
        for (ActionArgument argument : getArguments()) {
            if (argument.getDirection().equals(ActionArgument.Direction.IN)) args.add(argument);
        }
        return args;
    }


    public List<ActionArgument<S>> getOutputArguments() {
        List<ActionArgument<S>> args = new ArrayList();
        for (ActionArgument argument : getArguments()) {
            if (argument.getDirection().equals(ActionArgument.Direction.OUT)) args.add(argument);
        }
        return args;
    }

    public ActionInvocationValues getInputInvocationValues() {
        return new ActionInvocationValues(getInputArguments(), true);
    }

    public ActionInvocationValues getOutputInvocationValues() {
        return new ActionInvocationValues(getOutputArguments(), false);
    }

    public List<String> getInputArgumentNames() {
        List<String> names = new ArrayList();
        for (ActionArgument argument : getInputArguments()) {
            names.add(argument.getName());
        }
        return names;
    }

    public List<String> getOutputArgumentNames() {
        List<String> names = new ArrayList();
        for (ActionArgument argument : getOutputArguments()) {
            names.add(argument.getName());
        }
        return names;
    }

    public boolean hasInputArguments() {
        return getInputArguments().size() > 0;
    }

    public boolean hasOutputArguments() {
        return getOutputArguments().size() > 0;
    }


    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() +
                ", Arguments: " + (getArguments() != null ? getArguments().length : "NO ARGS") +
                ") " + getName();
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        if (!ModelUtil.isValidUDAName(getName())) {
            errors.add(new ValidationError(
                    getClass(),
                    "name",
                    "Name '" + getName() + "' is not valid, see UDA specification"
            ));
        }

        for (ActionArgument actionArgument : getArguments()) {
            // Check argument relatedStateVariable in service state table

            if (getService().getStateVariable(actionArgument.getRelatedStateVariableName()) == null) {
                errors.add(new ValidationError(
                        getClass(),
                        "arguments",
                        "Action argument references an unknown state variable: " + actionArgument.getRelatedStateVariableName()
                ));
            }
        }

        ActionArgument retValueArgument = null;
        int retValueArgumentIndex = 0;
        int i = 0;
        for (ActionArgument actionArgument : getArguments()) {
            // Check retval
            if (actionArgument.isReturnValue()) {
                if (retValueArgument != null) {
                    errors.add(new ValidationError(
                            getClass(),
                            "arguments",
                            "Only one argument of action '" + getName() + "' can be <retval/>"
                    ));
                }
                retValueArgument = actionArgument;
                retValueArgumentIndex = i;
            }
            i++;
        }
        if (retValueArgument != null) {
            for (int j = 0; j < retValueArgumentIndex; j++) {
                ActionArgument a = getArguments()[j];
                if (a.getDirection() == ActionArgument.Direction.OUT) {
                    errors.add(new ValidationError(
                            getClass(),
                            "arguments",
                            "Argument '" + retValueArgument.getName() + "' of action '" + getName() + "' is <retval/> but not the first OUT argument"
                    ));
                }
            }
        }

        for (ActionArgument argument : arguments) {
            errors.addAll(argument.validate());
        }

        return errors;
    }

}