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

package org.teleal.cling.model.types;

import org.teleal.common.util.Base64Coder;
import org.teleal.common.util.ByteArray;


public class Base64Datatype extends AbstractDatatype<Byte[]> {

    public Base64Datatype() {
    }

    public Byte[] fromString(String s) throws InvalidValueException {
        if (s.equals("")) return null;
        try {
            return ByteArray.toWrapper(Base64Coder.decode(s));
        } catch (Exception ex) {
            throw new InvalidValueException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getString(Byte[] value) throws InvalidValueException {
        if (value == null) return "";
        try {
            return new String(Base64Coder.encode(ByteArray.toPrimitive(value)));
        } catch (Exception ex) {
            throw new InvalidValueException(ex.getMessage(), ex);
        }
    }

    public Byte[] getDefaultValue() {
        return new Byte[0];
    }
}
