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

import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.UDN;


public class ServiceReference {

    final private UDN udn;
    final private ServiceId serviceId;

    public ServiceReference(UDN udn, ServiceId serviceId) {
        this.udn = udn;
        this.serviceId = serviceId;
    }

    public UDN getUdn() {
        return udn;
    }

    public ServiceId getServiceId() {
        return serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceReference that = (ServiceReference) o;

        if (!serviceId.equals(that.serviceId)) return false;
        if (!udn.equals(that.udn)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = udn.hashCode();
        result = 31 * result + serviceId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") " + udn.toString() + ", Service ID: " + serviceId.toString();
    }
}