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


public class SearchPort {

    public static int MIN_PORT = 49152;
    public static int MAX_PORT = 65535;

    public SearchPort(int port) {
        //super(Integer.toString(port));
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException(
                    "Port must be between " + MIN_PORT + " and " + MAX_PORT + ", was: " + port
            );
        }
    }

    public String getKey() {
        return "SEARCHPORT.UPNP.ORG";
    }

}