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

import org.teleal.cling.model.ModelUtil;

import java.util.UUID;
import java.security.MessageDigest;
import java.math.BigInteger;

/**
 * UDA 1.0 does not specify a UUID format, however, UDA 1.1 specifies a format that is compatible
 * with <tt>java.util.UUID</tt> variant 4.
 */
public class UDN {

    public static final String PREFIX = "uuid:";

    private String uuidString;

    public UDN(String uuidString) {
        this.uuidString = uuidString;
    }

    public UDN(UUID uuid) {
        this.uuidString = uuid.toString();
    }

    public boolean isUDA11Compliant() {
        return getIdentifier() != null;
    }

    public String getIdentifierString() {
        return uuidString;
    }

    public UUID getIdentifier() {
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static UDN valueOf(String udnString) {
        return new UDN(udnString.substring(PREFIX.length()));
    }

    public static UDN uniqueSystemIdentifier(String salt) {
        StringBuilder systemSalt = new StringBuilder();

        try {
            java.net.InetAddress i = java.net.InetAddress.getLocalHost();
            systemSalt.append(i.getHostName()).append(i.getHostAddress());
        } catch (Exception ex) {
            // Could not find local host name, try to get the MAC address of loopback interface
            try {
                systemSalt.append(new String(ModelUtil.getFirstNetworkInterfaceHardwareAddress()));
            } catch (Exception ex1) {
                // Ignore, we did everything we can
            }

        }
        systemSalt.append(System.getProperty("os.name"));
        systemSalt.append(System.getProperty("os.version"));
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(systemSalt.toString().getBytes());
            return new UDN(
                    new UUID(
                            new BigInteger(-1, hash).longValue(),
                            salt.hashCode()
                    )
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String toString() {
        return PREFIX + getIdentifierString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UDN udn = (UDN) o;
        return uuidString.equals(udn.uuidString);
    }

    @Override
    public int hashCode() {
        return uuidString.hashCode();
    }

    // TODO: Should implement Validatable and be called in Device.validate()
}
