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

package org.teleal.cling.model.types.csv;

import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.types.InvalidValueException;
import org.teleal.cling.model.ModelUtil;
import org.teleal.common.util.Reflections;

import java.util.ArrayList;
import java.util.List;


public abstract class CSV<T> extends ArrayList<T> {

    protected final Datatype.Builtin datatype;

    public CSV() {
        datatype = getBuiltinDatatype();
    }

    public CSV(String s) throws InvalidValueException {
        datatype = getBuiltinDatatype();
        addAll(parseString(s));
    }

    protected List parseString(String s) throws InvalidValueException {
        String[] strings = ModelUtil.fromCommaSeparatedList(s);
        List values = new ArrayList();
        for (String string : strings) {
            values.add(datatype.getDatatype().fromString(string));
        }
        return values;
    }

    protected Datatype.Builtin getBuiltinDatatype() throws InvalidValueException {
        Class csvType = Reflections.getTypeArguments(ArrayList.class, getClass()).get(0);
        Datatype.Default defaultType = Datatype.Default.getByJavaType(csvType);
        if (defaultType == null) {
            throw new InvalidValueException("No built-in UPnP datatype for Java type of CSV: " + csvType);
        }
        return defaultType.getBuiltinType();
    }

    @Override
    public String toString() {
        List<String> stringValues = new ArrayList();
        for (T t : this) {
            stringValues.add(datatype.getDatatype().getString(t));
        }
        return ModelUtil.toCommaSeparatedList(stringValues.toArray(new Object[stringValues.size()]));
    }
}
