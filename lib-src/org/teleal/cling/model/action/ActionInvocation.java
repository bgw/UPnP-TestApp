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

import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Service;



public class ActionInvocation<S extends Service> {

    final protected Action<S> action;
    protected ActionInvocationValues input;
    protected ActionInvocationValues output;
    protected ActionException failure = null;

    public ActionInvocation(Action<S> action) {
        this(action, new ActionInvocationValues(action.getInputArguments(), true), new ActionInvocationValues(action.getOutputArguments(), false));
    }

    public ActionInvocation(Action<S> action, ActionInvocationValues<S> input) {
        this(action, input, new ActionInvocationValues(action.getOutputArguments(), false));
    }

    public ActionInvocation(Action<S> action, ActionInvocationValues<S> input, ActionInvocationValues<S> output) {
        if (action == null) {
            throw new IllegalArgumentException("Action can not be null");
        }
        this.action = action;
        this.input = input != null ? input : new ActionInvocationValues(action.getInputArguments(), true);
        this.output = output != null ? output : new ActionInvocationValues(action.getOutputArguments(), false);
    }

    public ActionInvocation(ActionException failure) {
        this.action = null;
        this.input = null;
        this.output = null;
        this.failure = failure;
    }

    public Action<S> getAction() {
        return action;
    }

    public ActionInvocationValues<S> getInput() {
        return input;
    }

    public void setInputOutput(ActionInvocationValues<S> input) {
        this.input = input;
        this.output = null;
    }

    public ActionInvocationValues<S> getOutput() {
        return output;
    }

    public void setOutput(ActionInvocationValues<S> output) {
        this.output = output;
    }

    public ActionException getFailure() {
        return failure;
    }

    public void setFailure(ActionException failure) {
        this.failure = failure;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") " + getAction();
    }
}