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
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.model.types.InvalidValueException;

import java.util.List;
import java.util.logging.Logger;


public class ActionInvocationValues<S extends Service> {

    private static Logger log = Logger.getLogger(ActionInvocationValues.class.getName());

    private boolean input;
    private List<ActionArgument<S>> arguments;
    private ActionArgumentValue<S>[] values;

    public ActionInvocationValues(List<ActionArgument<S>> arguments, boolean input) {
        this.input = input;
        this.arguments = arguments;
        this.values = new ActionArgumentValue[arguments.size()];
    }

    public List<ActionArgument<S>> getArguments() {
        return arguments;
    }

    public ActionArgumentValue<S>[] getValues() {
        return values;
    }

    public void setValues(Object... values) throws ActionException {
        for (Object value : values) {
            addValue(value);
        }
    }

    public void setValue(int i, Object value) throws ActionException {
        if (i < 0 || i >= values.length) {
            throw new ActionException(ErrorCode.INVALID_ARGS, "Argument #" + i + " doesn't exist");
        }
        if (value instanceof ActionArgumentValue) {
            values[i] = (ActionArgumentValue) value;
        } else {
            values[i] = createValue(arguments.get(i), value);
        }
    }

    public boolean addValue(Object value) throws ActionException {
        int emptySlot = findFirstEmptySlot(values);
        if (emptySlot != -1) {
            if (value instanceof ActionArgumentValue) {
                values[emptySlot] = (ActionArgumentValue) value;
            } else {
                values[emptySlot] = createValue(arguments.get(emptySlot), value);
            }
            return emptySlot == values.length - 1;
        } else {
            throw new ActionException(ErrorCode.INVALID_ARGS, "All arguments have been set");
        }
    }

    public ActionArgumentValue<S> createValue(ActionArgument<S> argument, Object value) throws ActionException {
        try {
            return new ActionArgumentValue(argument, value);
        } catch (InvalidValueException ex) {
            throw new ActionException(
                    ErrorCode.ARGUMENT_VALUE_INVALID,
                    "Wrong type or invalid value for " + argument.getName(),
                    ex
            );
        }
    }

    public boolean isValid() {
        int i = 0;
        for (ActionArgument argument : arguments) {
            if (values[i] == null) {
                log.fine("Input/output argument of call missing: " + argument);
                return false;
            }
            if (!values[i++].getArgument().equals(argument)) {
                log.fine("Input/output argument values of call missing or not in correct order");
                return false;
            }
        }
        return true;
    }

    public void fillValuesWithNull() {
        for (int i = 0; i < values.length; i++) {
            ActionArgumentValue value = values[i];
            if (value == null) {
                values[i] = new ActionArgumentValue(getArguments().get(i), null);
            }
        }
    }

    public boolean isInput() {
        return input;
    }

    protected int findFirstEmptySlot(ActionArgumentValue[] array) {
        for (int i = 0; i < array.length; i++) {
            ActionArgumentValue argumentValue = array[i];
            if (argumentValue == null) return i;
        }
        return -1;
    }

}