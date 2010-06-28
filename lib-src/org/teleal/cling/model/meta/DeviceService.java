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
import org.teleal.cling.model.ServiceReference;
import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines the metadata of service as present in Device descriptor.
 */
public class DeviceService<S extends Service> implements Validatable {

    final private ServiceType serviceType;
    final private ServiceId serviceId;

    final private URI descriptorURI;
    final private URI controlURI;
    final private URI eventSubscriptionURI;

    final private S service;

    // Package mutable state
    private Device device;

    public DeviceService(ServiceType serviceType, ServiceId serviceId, URI descriptorURI, URI controlURI, URI eventSubscriptionURI) {
        this(serviceType, serviceId, descriptorURI, controlURI, eventSubscriptionURI, null);
    }

    public DeviceService(ServiceType serviceType, ServiceId serviceId, URI descriptorURI, URI controlURI, URI eventSubscriptionURI, S service) {
        this.serviceType = serviceType;
        this.serviceId = serviceId;

        this.descriptorURI = descriptorURI;
        this.controlURI = controlURI;
        this.eventSubscriptionURI = eventSubscriptionURI;

        this.service = service;

        if (service != null) {
            service.setDeviceService(this);
        }
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public ServiceId getServiceId() {
        return serviceId;
    }

    public URI getDescriptorURI() {
        return descriptorURI;
    }

    public URI getControlURI() {
        return controlURI;
    }

    public URI getEventSubscriptionURI() {
        return eventSubscriptionURI;
    }

    public S getService() {
        return service;
    }

    public Device getDevice() {
        return device;
    }

    void setDevice(Device device) {
        if (this.device != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.device = device;
    }

    public ServiceReference createServiceReference() {
        return new ServiceReference(getDevice().getIdentity().getUdn(), getServiceId());
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        if (getServiceType() == null) {
            errors.add(new ValidationError(
                    getClass(),
                    "serviceType",
                    "Service type/info is required"
            ));
        }

        if (getServiceId() == null) {
            errors.add(new ValidationError(
                    getClass(),
                    "serviceId",
                    "Service ID is required"
            ));
        }

        if (getDescriptorURI() == null) {
            errors.add(new ValidationError(
                    getClass(),
                    "descriptorURI",
                    "Descriptor location (SCPDURL) is required"
            ));
        }

        if (getControlURI() == null) {
            errors.add(new ValidationError(
                    getClass(),
                    "controlURI",
                    "Control URL is required"
            ));
        }

        if (getEventSubscriptionURI() == null) {
            errors.add(new ValidationError(
                    getClass(),
                    "eventSubscriptionURI",
                    "Event subscription URL is required"
            ));
        }

        return errors;
    }

    public URI prefixLocalURI(String uri) {
        if (getDevice() == null) {
            throw new IllegalStateException("Can't generate local URI prefix without device reference");
        }
        return prefixLocalURI(getDevice(), URI.create(uri));
    }

    public URI prefixLocalURI(Device device, URI uri) {
        if (!uri.isAbsolute()) {
            return URI.create(getLocalURIPrefix(device) +
                    (uri.getPath().startsWith("/") ? uri.toString() : "/" + uri.toString())
            );
        }
        return uri;
    }

    public String getLocalURIPrefix(Device device) {
        if (getServiceId() == null) {
            throw new IllegalStateException("Can't generate local URI prefix without service ID");
        }
        StringBuilder s = new StringBuilder();
        s.append(device.getLocalURIPrefix());
        s.append(Constants.RESOURCE_SERVICE_PREFIX);
        s.append("/");
        s.append(getServiceId().getNamespace());
        s.append("/");
        s.append(getServiceId().getId());
        return s.toString();
    }

    // Hint: The absolute URL is available only on the {@link RemoteGENASubscription}

    public URI getLocalEventCallbackURI() {
        return prefixLocalURI(Constants.RESOURCE_SERVICE_EVENTS_SUFFIX + Constants.RESOURCE_SERVICE_CALLBACK_FILE);
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") Descriptor URI: " + getDescriptorURI();
    }

}