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


import org.teleal.cling.model.Resource;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.common.util.URIUtil;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;


public class RemoteDevice extends Device<RemoteDeviceIdentity, RemoteDevice, RemoteService> {

    private static Logger log = Logger.getLogger(RemoteDevice.class.getName());

    public static Constructor<RemoteDevice> getConstructor() {
        try {
            return RemoteDevice.class.getConstructor(
                    RemoteDeviceIdentity.class, DeviceType.class, DeviceDetails.class,
                    Icon[].class, DeviceService[].class, RemoteDevice[].class
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public RemoteDevice(RemoteDeviceIdentity identity) throws ValidationException {
        super(identity);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details,
                        Icon[] icons, DeviceService<RemoteService>[] deviceServices) throws ValidationException {
        super(identity, type, details, icons, deviceServices);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details,
                        Icon[] icons, DeviceService<RemoteService>[] deviceServices, RemoteDevice[] embeddedDevices) throws ValidationException {
        super(identity, type, details, icons, deviceServices, embeddedDevices);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, UDAVersion version, DeviceType type, DeviceDetails details,
                        Icon[] icons, DeviceService<RemoteService>[] deviceServices, RemoteDevice[] embeddedDevices) throws ValidationException {
        super(identity, version, type, details, icons, deviceServices, embeddedDevices);
    }

    public URL normalizeURI(URI relativeOrAbsoluteURI) {

        // TODO: I have one device (Netgear 834DG DSL Router) that sends a <URLBase>, and even that is wrong (port)!
        // This can be fixed by "re-enabling" UPnP in the upnpService after a reboot, it will then use the right port...
        // return URIUtil.createAbsoluteURL(getDescriptorURL(), relativeOrAbsoluteURI);

        if (getDetails() != null && getDetails().getBaseURL() != null) {
            // If we have an <URLBase>, all URIs are relative to it
            return URIUtil.createAbsoluteURL(getDetails().getBaseURL(), relativeOrAbsoluteURI);
        } else {
            // Otherwise, they are relative to the descriptor location
            return URIUtil.createAbsoluteURL(getIdentity().getDescriptorURL(), relativeOrAbsoluteURI);
        }

    }

    public RemoteDevice newInstance(UDN udn, UDAVersion version, DeviceType type, DeviceDetails details,
                                    Icon[] icons, DeviceService<RemoteService>[] deviceServices,
                                    List<RemoteDevice> embeddedDevices) throws ValidationException {
        return new RemoteDevice(
                new RemoteDeviceIdentity(udn, getIdentity()),
                version, type, details, icons,
                deviceServices,
                embeddedDevices.size() > 0 ? embeddedDevices.toArray(new RemoteDevice[embeddedDevices.size()]) : null
        );
    }

    protected Resource[] discoverResources() {
        List<Resource> discovered = new ArrayList();

        // Services
        for (DeviceService deviceService : getDeviceServices()) {

            discovered.add(
                    new Resource<DeviceService>(
                            Resource.Type.EVENT_CALLBACK, deviceService.getLocalEventCallbackURI(), deviceService
                    )
            );

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
    public RemoteDevice getRoot() {
        if (isRoot()) return this;
        RemoteDevice current = this;
        while (current.getParentDevice() != null) {
            current = current.getParentDevice();
        }
        return current;
    }

    @Override
    public RemoteDevice[] findEmbeddedDevices() {
        Collection<RemoteDevice> col = findEmbeddedDevices(this);
        return col.size() > 0 ? col.toArray(new RemoteDevice[col.size()]) : null;
    }

    @Override
    public RemoteDevice findDevice(UDN udn) {
        return find(udn, this);
    }

    @Override
    public RemoteDevice[] findDevices(DeviceType deviceType) {
        Collection<RemoteDevice> col = find(deviceType, this);
        return col.size() > 0 ? col.toArray(new RemoteDevice[col.size()]) : null;
    }

    @Override
    public RemoteDevice[] findDevices(ServiceType serviceType) {
        Collection<RemoteDevice> col = find(serviceType, this);
        return col.size() > 0 ? col.toArray(new RemoteDevice[col.size()]) : null;
    }

    @Override
    public DeviceService<RemoteService>[] findDeviceServices() {
        Collection<DeviceService> col = findDeviceServices(null, null, this);
        return col.size() > 0 ? col.toArray(new DeviceService[col.size()]) : null;
    }

    @Override
    public DeviceService<RemoteService>[] findDeviceServices(ServiceType serviceType) {
        Collection<DeviceService> col = findDeviceServices(serviceType, null, this);
        return col.size() > 0 ? col.toArray(new DeviceService[col.size()]) : null;
    }
}