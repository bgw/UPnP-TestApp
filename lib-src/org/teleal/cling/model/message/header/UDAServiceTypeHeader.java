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

package org.teleal.cling.model.message.header;

import org.teleal.cling.model.types.UDAServiceType;

import java.net.URI;


public class UDAServiceTypeHeader extends UpnpHeader<UDAServiceType> {

    public UDAServiceTypeHeader() {
    }

    public UDAServiceTypeHeader(URI uri) {
        setString(uri.toString());
    }

    public UDAServiceTypeHeader(UDAServiceType value) {
        setValue(value);
    }

    public void setString(String s) throws InvalidHeaderException {
        try {
            setValue(UDAServiceType.fromString(s));
        } catch (Exception ex) {
            throw new InvalidHeaderException("Invalid UDA service type header value, " + ex.getMessage());
        }
    }

    public String getString() {
        return getValue().toString();
    }

}
