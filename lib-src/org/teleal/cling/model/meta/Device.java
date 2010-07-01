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
import org.teleal.cling.model.Resource;
import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


public abstract class Device<DI extends DeviceIdentity, D extends Device, S extends Service> implements Validatable {

    private static Logger log = Logger.getLogger(Device.class.getName());

    final private DI identity;

    final private UDAVersion version;
    final private DeviceType type;
    final private DeviceDetails details;
    final private Icon[] icons;
    final private DeviceService<S>[] deviceServices;
    final private Resource[] resources;

    final private D[] embeddedDevices;

    // Package mutable state
    private D parentDevice;

    public Device(DI identity) throws ValidationException {
        this(identity, null, null, null, null, null, null);
    }

    public Device(DI identity, DeviceType type, DeviceDetails details,
                  Icon[] icons, DeviceService<S>[] deviceServices) throws ValidationException {
        this(identity, null, type, details, icons, deviceServices, null);
    }

    public Device(DI identity, DeviceType type, DeviceDetails details,
                  Icon[] icons, DeviceService<S>[] deviceServices, D[] embeddedDevices) throws ValidationException {
        this(identity, null, type, details, icons, deviceServices, embeddedDevices);
    }

    public Device(DI identity, UDAVersion version, DeviceType type, DeviceDetails details,
                  Icon[] icons, DeviceService<S>[] deviceServices, D[] embeddedDevices) throws ValidationException {

        this.identity = identity;
        this.version = version == null ? new UDAVersion() : version;
        this.type = type;
        this.details = details;

        this.icons = icons == null ? new Icon[0] : icons;
        for (Icon icon : this.icons) {
            icon.setDevice(this);
        }

        this.deviceServices = deviceServices == null ? new DeviceService[0] : deviceServices;
        for (DeviceService deviceService : this.deviceServices) {
            deviceService.setDevice(this);
        }

        this.embeddedDevices = embeddedDevices;
        if (embeddedDevices != null) {
            for (D embeddedDevice : embeddedDevices) {
                embeddedDevice.setParentDevice(this);
            }
        }

        List<ValidationError> errors = validate();

        if (isRoot()) {
            log.fine("Discovering local resources of device graph");
            Resource[] discoveredResources = discoverResources();
            Set<Resource> resources = new HashSet();
            for (Resource resource : discoveredResources) {
                log.finer("Discovered: " + resource);
                if (!resources.add(resource)) {
                    log.finer("Local resource already exists, queueing validation error");
                    errors.add(new ValidationError(
                            getClass(),
                            "resources",
                            "Local URI namespace conflict between resources of device: " + resource
                    ));
                }
            }
            this.resources = resources.toArray(new Resource[resources.size()]);
        } else {
            this.resources = null;
        }

        if (errors.size() > 0) {
            throw new ValidationException("Validation of device graph failed, call getErrors() on exception", errors);
        }
    }

    public DI getIdentity() {
        return identity;
    }

    public UDAVersion getVersion() {
        return version;
    }

    public DeviceType getType() {
        return type;
    }

    public DeviceDetails getDetails() {
        return details;
    }

    public Icon[] getIcons() {
        return icons;
    }

    public boolean hasIcons() {
        return getIcons() != null && getIcons().length > 0;
    }

    public DeviceService<S>[] getDeviceServices() {
        return deviceServices;
    }

    public boolean hasDeviceServices() {
        return getDeviceServices() != null && getDeviceServices().length > 0;
    }

    public D[] getEmbeddedDevices() {
        return embeddedDevices;
    }

    public boolean hasEmbeddedDevices() {
        return getEmbeddedDevices() != null && getEmbeddedDevices().length > 0;
    }

    public D getParentDevice() {
        return parentDevice;
    }

    void setParentDevice(D parentDevice) {
        if (this.parentDevice != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.parentDevice = parentDevice;
    }

    public Resource[] getResources() {
        return resources;
    }

    public boolean isRoot() {
        return getParentDevice() == null;
    }

    public Resource getLocalResource(URI localPathQuery) {
        for (Resource localResource : getResources()) {
            if (localResource.matchesPathQuery(localPathQuery))
                return localResource;
        }
        return null;
    }

    public abstract D getRoot();
    public abstract D[] findEmbeddedDevices();
    public abstract D findDevice(UDN udn);
    public abstract D[] findDevices(DeviceType deviceType);
    public abstract D[] findDevices(ServiceType serviceType);
    public abstract DeviceService<S>[] findDeviceServices();
    public abstract DeviceService<S>[] findDeviceServices(ServiceType serviceType);

    protected D find(UDN udn, D current) {
        if (current.getIdentity().getUdn().equals(udn)) return current;
        if (current.hasEmbeddedDevices()) {
            for (D embeddedDevice : (D[])current.getEmbeddedDevices()) {
                D match;
                if ((match = find(udn, embeddedDevice)) != null) return match;
            }
        }
        return null;
    }

    protected Collection<D> findEmbeddedDevices(D current) {
        Collection<D> devices = new HashSet();
        if (!current.isRoot()) {
            devices.add(current);
        }
        if (current.hasEmbeddedDevices()) {
            for (D embeddedDevice : (D[])current.getEmbeddedDevices()) {
                devices.addAll(findEmbeddedDevices(embeddedDevice));
            }
        }
        return devices;
    }

    protected Collection<D> find(DeviceType deviceType, D current) {
        Collection<D> devices = new HashSet();
        // Type might be null if we just discovered the device and it hasn't yet been hydrated
        if (current.getType() != null && current.getType().implementsVersion(deviceType)) {
            devices.add(current);
        }
        if (current.hasEmbeddedDevices()) {
            for (D embeddedDevice : (D[])current.getEmbeddedDevices()) {
                devices.addAll(find(deviceType, embeddedDevice));
            }
        }
        return devices;
    }

    protected Collection<D> find(ServiceType serviceType, D current) {
        Collection<DeviceService> deviceServices  = findDeviceServices(serviceType, null, current);
        Collection<D> devices = new HashSet();
        for (DeviceService deviceService : deviceServices) {
            devices.add((D)deviceService.getDevice());
        }
        return devices;
    }

    protected Collection<DeviceService> findDeviceServices(ServiceType serviceType, ServiceId serviceId, D current) {
        Collection<DeviceService> deviceServices  = new HashSet();
        if (current.hasDeviceServices()) {
            for (DeviceService deviceService : current.getDeviceServices()) {
                if (isMatch(deviceService, serviceType, serviceId))
                    deviceServices.add(deviceService);
            }
        }
        Collection<D> embeddedDevices = findEmbeddedDevices(current);
        if (embeddedDevices != null) {
            for (D embeddedDevice : embeddedDevices) {
                if (embeddedDevice.hasDeviceServices()) {
                    for (DeviceService deviceService : embeddedDevice.getDeviceServices()) {
                        if (isMatch(deviceService, serviceType, serviceId))
                            deviceServices.add(deviceService);
                    }
                }
            }
        }
        return deviceServices;
    }

    public DeviceService<S> findDeviceService(ServiceId serviceId) {
        Collection<DeviceService> deviceServices  = findDeviceServices(null, serviceId, (D)this);
        return deviceServices.size() == 1 ? deviceServices.iterator().next() : null;
    }

    public DeviceService<S> findFirstDeviceService(ServiceType serviceType) {
        Collection<DeviceService> deviceServices  = findDeviceServices(serviceType, null, (D)this);
        return deviceServices.size() > 0 ? deviceServices.iterator().next() : null;
    }

    public ServiceType[] findServiceTypes() {
        Collection<DeviceService> deviceServices  = findDeviceServices(null, null, (D)this);
        Collection<ServiceType> col = new HashSet();
        for (DeviceService deviceService : deviceServices) {
            col.add(deviceService.getServiceType());
        }
        return col.toArray(new ServiceType[col.size()]);
    }

    private boolean isMatch(DeviceService ds, ServiceType serviceType, ServiceId serviceId) {
        boolean matchesType = serviceType == null || ds.getServiceType().implementsVersion(serviceType);
        boolean matchesId = serviceId == null || ds.getServiceId().equals(serviceId);
        return matchesType && matchesId;
    }

    public String getDisplayString() {

        // The goal is to have a clean string with "<manufacturer> <model name> <model#>"

        String cleanModelName = null;
        String cleanModelNumber = null;

        if (getDetails() != null && getDetails().getModelDetails() != null) {

            // Some vendors end the model name with the model number, let's remove that
            ModelDetails modelDetails = getDetails().getModelDetails();
            if (modelDetails.getModelName() != null) {
                cleanModelName = modelDetails.getModelNumber() != null && modelDetails.getModelName().endsWith(modelDetails.getModelNumber())
                        ? modelDetails.getModelName().substring(0, modelDetails.getModelName().length() - modelDetails.getModelNumber().length())
                        : modelDetails.getModelName();
            }

            // Some vendors repeat the model name as the model number, no good
            if (cleanModelName != null) {
                cleanModelNumber = modelDetails.getModelNumber() != null && !cleanModelName.startsWith(modelDetails.getModelNumber())
                        ? modelDetails.getModelNumber()
                        : "";
            } else {
                cleanModelNumber = modelDetails.getModelNumber();
            }
        }

        StringBuilder sb = new StringBuilder();

        if (getDetails() != null && getDetails().getManufacturerDetails() != null) {

            // Some vendors repeat the manufacturer in model name, let's remove that too
            if (cleanModelName != null && getDetails().getManufacturerDetails().getManufacturer() != null) {
                cleanModelName = cleanModelName.startsWith(getDetails().getManufacturerDetails().getManufacturer())
                        ? cleanModelName.substring(getDetails().getManufacturerDetails().getManufacturer().length()).trim()
                        : cleanModelName.trim();
            }

            if (getDetails().getManufacturerDetails().getManufacturer() != null) {
                sb.append(getDetails().getManufacturerDetails().getManufacturer());
            }
        }

        sb.append((cleanModelName != null && cleanModelName.length() > 0 ? " " + cleanModelName : ""));
        sb.append((cleanModelNumber != null && cleanModelNumber.length() > 0 ? " " + cleanModelNumber.trim() : ""));
        return sb.toString();
    }

    public byte[] getFirstIcon() {
        if (getIcons().length > 0) {
            return getIcons()[0].getData();
        }
        return null;
    }

    public String getLocalURIPrefix() {
        if (getIdentity().getUdn() == null) {
            throw new IllegalStateException("Can't generate local URI prefix without UDN");
        }
        StringBuilder s = new StringBuilder();
        s.append(Constants.RESOURCE_DEVICE_PREFIX);
        s.append("/");
        if (isRoot()) {
            s.append(getIdentity().getUdn().getIdentifierString());
        } else {
            List<Device> devices = new ArrayList();
            Device temp = this;
            while (temp != null) {
                devices.add(temp);
                temp = temp.getParentDevice();
            }
            Collections.reverse(devices);
            for (Device d : devices) {
                if (d == this) continue;
                s.append(d.getIdentity().getUdn().getIdentifierString());
                s.append(Constants.RESOURCE_EMBEDDED_PREFIX);
                s.append("/");
            }
            s.append(getIdentity().getUdn().getIdentifierString());
        }
        return s.toString();
    }

    public URI prefixLocalURI(URI uri) {
        if (!uri.isAbsolute() && !uri.getPath().startsWith("/")) {
            return URI.create(getLocalURIPrefix() + "/" + uri.toString());
        }
        return uri;
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        if (getType() != null) {

            // Only validate the graph if we have a device type - that means we validate only if there
            // actually is a fully hydrated graph, not just a discovered device of which we haven't even
            // retrieved the descriptor yet. This assumes that the descriptor will ALWAYS contain a device
            // type. Now that is a risky assumption...

            errors.addAll(getVersion().validate());

            if (getDetails() != null) {
                errors.addAll(getDetails().validate());
            }

            if (hasIcons()) {
                for (Icon icon : getIcons()) {
                    errors.addAll(icon.validate());
                }
            }

            if (hasDeviceServices()) {
                for (DeviceService service : getDeviceServices()) {
                    errors.addAll(service.validate());
                }
            }

            if (hasEmbeddedDevices()) {
                for (Device embeddedDevice : getEmbeddedDevices()) {
                    errors.addAll(embeddedDevice.validate());
                }
            }
        }

        return errors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Device device = (Device) o;

        if (!identity.equals(device.identity)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    public abstract D newInstance(UDN udn, UDAVersion version, DeviceType type, DeviceDetails details,
                                  Icon[] icons, DeviceService<S>[] deviceServices, List<D> embeddedDevices) throws ValidationException;

    protected abstract Resource[] discoverResources();

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") Identity: " + getIdentity().toString() + ", Root: " + isRoot();
    }
}
