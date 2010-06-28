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

import org.teleal.cling.model.Constants;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class UDAServiceType extends ServiceType {

    public static final String DEFAULT_NAMESPACE = "schemas-upnp-org";

    public static final Pattern PATTERN =
            Pattern.compile("urn:" + DEFAULT_NAMESPACE + ":service:(" + Constants.REGEX_TYPE + "):([0-9]+)");

    public UDAServiceType(String type, int version) {
        super(DEFAULT_NAMESPACE, type, version);
    }

    public static UDAServiceType fromString(String s) throws RuntimeException {
        Matcher matcher = UDAServiceType.PATTERN.matcher(s);
        if (matcher.matches()) {
            return new UDAServiceType(matcher.group(1), Integer.valueOf(matcher.group(2)));
        } else {
            throw new RuntimeException("Can't parse UDA service type string (namespace/type/version): " + s);
        }
    }

}
