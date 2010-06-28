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



import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.types.Datatype;

import java.util.ArrayList;
import java.util.List;


public class StateVariableTypeDetails implements Validatable {

    final private Datatype datatype;
    final private String defaultValue;
    final private String[] allowedValues;
    final private StateVariableAllowedValueRange allowedValueRange;

    public StateVariableTypeDetails(Datatype datatype) {
        this(datatype, null, null, null);
    }

    public StateVariableTypeDetails(Datatype datatype, String defaultValue) {
        this(datatype, defaultValue, null, null);
    }

    public StateVariableTypeDetails(Datatype datatype, String defaultValue, String[] allowedValues, StateVariableAllowedValueRange allowedValueRange) {
        this.datatype = datatype;
        this.defaultValue = defaultValue;
        this.allowedValues = allowedValues;
        this.allowedValueRange = allowedValueRange;
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String[] getAllowedValues() {
        return allowedValues;
    }

    public StateVariableAllowedValueRange getAllowedValueRange() {
        return allowedValueRange;
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        if (getDatatype() == null) {
            errors.add(new ValidationError(
                    getClass(),
                    "datatype",
                    "Service state variable has no datatype"
            ));
        }

        if (getAllowedValues() != null) {

            if (getAllowedValueRange() != null) {
                errors.add(new ValidationError(
                        getClass(),
                        "allowedValues",
                        "Allowed value list of state variable can not also be restricted with allowed value range"
                ));
            }

            if (!getDatatype().getBuiltin().equals(Datatype.Builtin.STRING)) {
                errors.add(new ValidationError(
                        getClass(),
                        "allowedValues",
                        "Allowed value list of state variable only available for string datatype, not: " + getDatatype()
                ));
            }

            String defaultValue = getDefaultValue();
            boolean foundDefaultInAllowed = defaultValue == null;
            for (String s : getAllowedValues()) {
                if (!foundDefaultInAllowed)
                    foundDefaultInAllowed = s.equals((defaultValue));
                if (s.length() > 31) {
                    errors.add(new ValidationError(
                            getClass(),
                            "allowedValues",
                            "Allowed string value in list of state variable must be less than 32 chars"
                    ));
                }
            }
            if(!foundDefaultInAllowed) {
                errors.add(new ValidationError(
                        getClass(),
                        "allowedValues",
                        "Allowed string values of state variable don't contain default value: " + defaultValue
                ));
            }
        }

        if (getAllowedValueRange() != null) {
            errors.addAll(getAllowedValueRange().validate());
        }

        return errors;
    }
}
