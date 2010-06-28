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
import org.teleal.cling.model.types.Datatype;

import java.util.List;
import java.util.ArrayList;


public class StateVariable<S extends Service> implements Validatable {

    final private String name;
    final private StateVariableTypeDetails type;
    final private StateVariableEventDetails eventDetails;

    // Package mutable state
    private S service;

    public StateVariable(String name, StateVariableTypeDetails type) {
        this(name, type, new StateVariableEventDetails());
    }

    public StateVariable(String name, StateVariableTypeDetails type, StateVariableEventDetails eventDetails) {
        this.name = name;
        this.type = type;
        this.eventDetails = eventDetails;
    }

    public String getName() {
        return name;
    }

    public StateVariableTypeDetails getTypeDetails() {
        return type;
    }

    public StateVariableEventDetails getEventDetails() {
        return eventDetails;
    }

    public S getService() {
        return service;
    }

    void setService(S service) {
        if (this.service != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.service = service;
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        if (!ModelUtil.isValidUDAName(getName())) {
            errors.add(new ValidationError(
                    getClass(),
                    "name",
                    "Name '"+getName()+"' is not valid, see UDA specification"
            ));
        }

        errors.addAll(getTypeDetails().validate());

        return errors;
    }

    public boolean isModeratedNumericType() {
        return Datatype.Builtin.isNumeric(getTypeDetails().getDatatype().getBuiltin()) && getEventDetails().getEventMinimumDelta() > 0;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(getClass().getSimpleName()).append(", ");
        sb.append(getName()).append(" ").append(getTypeDetails().getDatatype()).append(")");
        if (!getEventDetails().isSendEvents()) {
            sb.append(" (No Events)");
        }
        if (getTypeDetails().getDefaultValue() != null) {
            sb.append(" Default Value: ").append("'").append(getTypeDetails().getDefaultValue()).append("'");
        }
        if (getTypeDetails().getAllowedValues() != null) {
            sb.append(" Allowed Values: ");
            for (String s : getTypeDetails().getAllowedValues()) {
                sb.append(s).append("|");
            }
        }
        return sb.toString();
    }
}