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


public class NamedDeviceType {

    private UDN udn;
    private DeviceType deviceType;

    public NamedDeviceType(UDN udn, DeviceType deviceType) {
        this.udn = udn;
        this.deviceType = deviceType;
    }

    public UDN getUdn() {
        return udn;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public static NamedDeviceType fromString(String s) throws RuntimeException {
        String[] strings = s.split("::");
        if (strings.length != 2) {
            throw new RuntimeException("Can't parse UDN::DeviceType from: " + s);
        }

        UDN udn;
        try {
            udn = UDN.valueOf(strings[0]);
        } catch (Exception ex) {
            throw new RuntimeException("Can't parse UDN: " + strings[0]);
        }

        DeviceType deviceType = DeviceType.valueOf(strings[1]);
        return new NamedDeviceType(udn, deviceType);
    }

    @Override
    public String toString() {
        return getUdn().toString() + "::" + getDeviceType().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamedDeviceType that = (NamedDeviceType) o;

        if (!deviceType.equals(that.deviceType)) return false;
        if (!udn.equals(that.udn)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = udn.hashCode();
        result = 31 * result + deviceType.hashCode();
        return result;
    }
}
