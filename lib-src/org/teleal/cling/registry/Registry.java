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

package org.teleal.cling.registry;

import org.teleal.cling.UpnpServiceConfiguration;
import org.teleal.cling.model.Resource;
import org.teleal.cling.model.ServiceReference;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.meta.RemoteDeviceIdentity;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.protocol.ProtocolFactory;

import java.net.URI;
import java.util.Collection;


public interface Registry {

    public UpnpServiceConfiguration getConfiguration();
    public ProtocolFactory getProtocolFactory();

    // #################################################################################################

    public void shutdown();

    // #################################################################################################

    public void addListener(RegistryListener listener);

    public void removeListener(RegistryListener listener);

    public Collection<RegistryListener> getListeners();

    // #################################################################################################

    public void addDevice(LocalDevice localDevice);

    public void addDevice(RemoteDevice remoteDevice);

    public boolean update(RemoteDeviceIdentity rdIdentity);

    public boolean removeDevice(LocalDevice localDevice);

    public boolean removeDevice(RemoteDevice remoteDevice);

    public void removeAllLocalDevices();

    public void removeAllRemoteDevices();

    public Device getDevice(UDN udn, boolean rootOnly);

    public Collection<LocalDevice> getLocalDevices();

    public Collection<RemoteDevice> getRemoteDevices();

    public Collection<Device> getDevices();

    public Collection<Device> getDevices(DeviceType deviceType);

    public Collection<Device> getDevices(ServiceType serviceType);

    public DeviceService getDeviceService(ServiceReference serviceReference);

    // #################################################################################################

    public void addResource(Resource resource);

    public boolean removeResource(Resource resource);

    public Resource getResource(URI pathQuery) throws IllegalArgumentException;

    public <T> T getResourceModel(Class<T> modelType, Resource.Type type, URI pathQuery) throws IllegalArgumentException;

    public Collection<Resource> getResources();

    // #################################################################################################

    public void addLocalSubscription(LocalGENASubscription subscription);

    public LocalGENASubscription getLocalSubscription(String subscriptionId);

    public boolean updateLocalSubscription(LocalGENASubscription subscription);

    public boolean removeLocalSubscription(LocalGENASubscription subscription);

    public void addRemoteSubscription(RemoteGENASubscription subscription);

    public RemoteGENASubscription getRemoteSubscription(String subscriptionId);

    public void updateRemoteSubscription(RemoteGENASubscription subscription);

    public void removeRemoteSubscription(RemoteGENASubscription subscription);

    // #################################################################################################

}
