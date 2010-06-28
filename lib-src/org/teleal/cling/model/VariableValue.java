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

package org.teleal.cling.model;

import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.InvalidValueException;

import java.util.Date;

/**
 * @author Christian Bauer
 */
public class VariableValue<S extends Service> {

    final private Datatype datatype;
    final private Object value;
    final private Date createdOn = new Date();

    public VariableValue(Datatype datatype, Object value) throws InvalidValueException {
        this.datatype = datatype;
        this.value = value instanceof String ? datatype.fromString((String) value) : value;
        if (!isValid())
            throw new InvalidValueException("Invalid value for type '" + getDatatype() +"':" + getValue());
    }

    protected VariableValue(Datatype datatype, String value) {
        this.datatype = datatype;
        this.value = value;
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public Object getValue() {
        return value;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    protected boolean isValid() {
        return getValue() == null ||
                (getDatatype().getValueType().isAssignableFrom(getValue().getClass()) &&
                        getDatatype().isValid(getValue()));
    }

    @Override
    public String toString() {
        return getDatatype().getString(getValue());
    }

}
