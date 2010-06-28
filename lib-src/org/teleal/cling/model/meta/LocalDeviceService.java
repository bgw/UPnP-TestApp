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

package org.teleal.cling.model.meta;

import org.teleal.cling.model.Constants;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;

import java.net.URI;

/**
 * @author Christian Bauer
 */
public class LocalDeviceService extends DeviceService<LocalService> {

    public LocalDeviceService(ServiceType serviceType, ServiceId serviceId, LocalService service) {
        super(serviceType, serviceId, null, null, null, service);
    }

    @Override
    public URI getDescriptorURI() {
        return prefixLocalURI(Constants.RESOURCE_DESCRIPTOR_FILE);
    }

    @Override
    public URI getControlURI() {
        return prefixLocalURI(Constants.RESOURCE_SERVICE_CONTROL_SUFFIX);
    }

    @Override
    public URI getEventSubscriptionURI() {
        return prefixLocalURI(Constants.RESOURCE_SERVICE_EVENTS_SUFFIX);
    }

}

