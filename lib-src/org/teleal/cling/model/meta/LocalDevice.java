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
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Christian Bauer
 */
public class LocalDevice extends Device<DeviceIdentity, LocalDevice, LocalService> {

    public static Constructor<LocalDevice> getConstructor() {
        try {
            return LocalDevice.class.getConstructor(
                    DeviceIdentity.class, DeviceType.class, DeviceDetails.class,
                    Icon[].class, DeviceService[].class, LocalDevice[].class
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public LocalDevice(DeviceIdentity identity) throws ValidationException {
        super(identity);
    }

    public LocalDevice(DeviceIdentity identity, DeviceType type, DeviceDetails details,
                       Icon[] icons, DeviceService<LocalService>[] deviceServices) throws ValidationException {
        super(identity, type, details, icons, deviceServices);
    }

    public LocalDevice(DeviceIdentity identity, DeviceType type, DeviceDetails details,
                       Icon[] icons, DeviceService<LocalService>[] deviceServices, LocalDevice[] embeddedDevices) throws ValidationException {
        super(identity, type, details, icons, deviceServices, embeddedDevices);
    }

    public LocalDevice(DeviceIdentity identity, UDAVersion version, DeviceType type, DeviceDetails details,
                       Icon[] icons, DeviceService<LocalService>[] deviceServices, LocalDevice[] embeddedDevices) throws ValidationException {
        super(identity, version, type, details, icons, deviceServices, embeddedDevices);
    }

    public URI getDescriptorURI() {
        // The descriptor is always the descriptor of the root device of this graph
        return URI.create(getRoot().getLocalURIPrefix() + Constants.RESOURCE_DESCRIPTOR_FILE);
    }

    @Override
    public LocalDevice newInstance(UDN udn, UDAVersion version, DeviceType type, DeviceDetails details,
                                   Icon[] icons, DeviceService<LocalService>[] deviceServices, List<LocalDevice> embeddedDevices)
            throws ValidationException {
        return new LocalDevice(
                new DeviceIdentity(udn, getIdentity().getMaxAgeSeconds()),
                version, type, details, icons,
                deviceServices,
                embeddedDevices.size() > 0 ? embeddedDevices.toArray(new LocalDevice[embeddedDevices.size()]) : null
        );
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();
        errors.addAll(super.validate());

        // We have special rules for local icons, the URI must always be a relative path which will
        // be added to the device base URI!
        if (hasIcons()) {
            for (Icon icon : getIcons()) {
                if (icon.getUri().isAbsolute()) {
                    errors.add(new ValidationError(
                            getClass(),
                            "icons",
                            "Local icon URI can not be absolute: " + icon.getUri()
                    ));
                }
                if (icon.getUri().toString().contains("../")) {
                    errors.add(new ValidationError(
                            getClass(),
                            "icons",
                            "Local icon URI must not contain '../': " + icon.getUri()
                    ));
                }
                if (icon.getUri().toString().startsWith("/")) {
                    errors.add(new ValidationError(
                            getClass(),
                            "icons",
                            "Local icon URI must not start with '/': " + icon.getUri()
                    ));
                }
            }
        }

        return errors;
    }

    @Override
    protected Resource[] discoverResources() {
        List<Resource> discovered = new ArrayList();

        // Device
        if (isRoot()) {
            // This should guarantee that each logical local device tree (with all its embedded devices) has only
            // one DEVICE_DESCRIPTOR resource - because only one device in the tree isRoot().
            discovered.add(new Resource<LocalDevice>(Resource.Type.DEVICE_DESCRIPTOR, getDescriptorURI(), this));
        }

        // Services
        for (DeviceService deviceService : getDeviceServices()) {

            discovered.add(
                    new Resource<DeviceService>(
                            Resource.Type.SERVICE_DESCRIPTOR, deviceService.getDescriptorURI(), deviceService
                    )
            );

            // Control
            if (deviceService.getControlURI() != null) {
                discovered.add(
                        new Resource<DeviceService>(
                                Resource.Type.CONTROL, deviceService.getControlURI(), deviceService
                        )
                );
            }

            // Event subscription
            if (deviceService.getEventSubscriptionURI() != null) {
                discovered.add(
                        new Resource<DeviceService>(
                                Resource.Type.EVENT_SUBSCRIPTION, deviceService.getEventSubscriptionURI(), deviceService
                        )
                );
            }

        }

        // Icons
        for (Icon icon : getIcons()) {
            // Don't forget to prefix the relative path URI with the local device base URI!
            discovered.add(new Resource<Icon>(Resource.Type.ICON, prefixLocalURI(icon.getUri()), icon));
        }

        // Embedded devices
        if (hasEmbeddedDevices()) {
            for (Device embeddedDevice : getEmbeddedDevices()) {
                discovered.addAll(Arrays.asList(embeddedDevice.discoverResources()));
            }
        }

        return discovered.toArray(new Resource[discovered.size()]);
    }

    @Override
    public LocalDevice getRoot() {
        if (isRoot()) return this;
        LocalDevice current = this;
        while (current.getParentDevice() != null) {
            current = current.getParentDevice();
        }
        return current;
    }

    @Override
    public LocalDevice[] findEmbeddedDevices() {
        Collection<LocalDevice> col = findEmbeddedDevices(this);
        return col.size() > 0 ? col.toArray(new LocalDevice[col.size()]) : null;
    }

    @Override
    public LocalDevice findDevice(UDN udn) {
        return find(udn, this);
    }

    @Override
    public LocalDevice[] findDevices(DeviceType deviceType) {
        Collection<LocalDevice> col = find(deviceType, this);
        return col.size() > 0 ? col.toArray(new LocalDevice[col.size()]) : null;
    }

    @Override
    public LocalDevice[] findDevices(ServiceType serviceType) {
        Collection<LocalDevice> col = find(serviceType, this);
        return col.size() > 0 ? col.toArray(new LocalDevice[col.size()]) : null;
    }

    @Override
    public DeviceService<LocalService>[] findDeviceServices() {
        Collection<DeviceService> col = findDeviceServices(null, null, this);
        return col.size() > 0 ? col.toArray(new DeviceService[col.size()]) : null;
    }

    @Override
    public DeviceService<LocalService>[] findDeviceServices(ServiceType serviceType) {
        Collection<DeviceService> col = findDeviceServices(serviceType, null, this);
        return col.size() > 0 ? col.toArray(new DeviceService[col.size()]) : null;
    }

}
