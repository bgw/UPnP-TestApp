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


public class NamedServiceType {

    private UDN udn;
    private ServiceType serviceType;

    public NamedServiceType(UDN udn, ServiceType serviceType) {
        this.udn = udn;
        this.serviceType = serviceType;
    }

    public UDN getUdn() {
        return udn;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public static NamedServiceType fromString(String s) throws RuntimeException {
        String[] strings = s.split("::");
        if (strings.length != 2) {
            throw new RuntimeException("Can't parse UDN::ServiceType from: " + s);
        }

        UDN udn;
        try {
            udn = UDN.valueOf(strings[0]);
        } catch (Exception ex) {
            throw new RuntimeException("Can't parse UDN: " + strings[0]);
        }

        ServiceType serviceType = ServiceType.valueOf(strings[1]);
        return new NamedServiceType(udn, serviceType);
    }

    @Override
    public String toString() {
        return getUdn().toString() + "::" + getServiceType().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamedServiceType that = (NamedServiceType) o;

        if (!serviceType.equals(that.serviceType)) return false;
        if (!udn.equals(that.udn)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = udn.hashCode();
        result = 31 * result + serviceType.hashCode();
        return result;
    }
}