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
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteDeviceIdentity;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.protocol.ProtocolFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RegistryImpl implements Registry {

    private static Logger log = Logger.getLogger(Registry.class.getName());

    protected final UpnpServiceConfiguration configuration;
    protected final ProtocolFactory protocolFactory;

    protected final RegistryMaintainer registryMaintainer;

    public RegistryImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory) {
        log.fine("Creating Registry: " + getClass().getName());

        this.configuration = configuration;
        this.protocolFactory = protocolFactory;

        log.fine("Starting registry background maintenance...");
        registryMaintainer = createRegistryMaintainer();
        if (registryMaintainer != null) {
            this.configuration.getRegistryMaintainerExecutor().execute(registryMaintainer);
        }
    }

    public UpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    protected RegistryMaintainer createRegistryMaintainer() {
        return new RegistryMaintainer(this);
    }

    // #################################################################################################

    protected final Set<RegistryListener> registryListeners = new HashSet();
    protected final Set<Resource> resources = new HashSet();
    protected final List<Runnable> pendingExecutions = new ArrayList();

    protected final RemoteItems remoteItems = new RemoteItems(this);
    protected final LocalItems localItems = new LocalItems(this);

    // #################################################################################################

    synchronized public void addListener(RegistryListener listener) {
        registryListeners.add(listener);
    }

    synchronized public void removeListener(RegistryListener listener) {
        registryListeners.remove(listener);
    }

    synchronized public Collection<RegistryListener> getListeners() {
        return Collections.unmodifiableCollection(registryListeners);
    }

    // #################################################################################################

    synchronized public void addDevice(LocalDevice localDevice) {
        localItems.add(localDevice);
    }

    synchronized public void addDevice(RemoteDevice remoteDevice) {
        remoteItems.add(remoteDevice);
    }

    synchronized public boolean update(RemoteDeviceIdentity rdIdentity) {
        return remoteItems.update(rdIdentity);
    }

    synchronized public boolean removeDevice(LocalDevice localDevice) {
        return localItems.remove(localDevice);
    }

    synchronized public boolean removeDevice(RemoteDevice remoteDevice) {
        return remoteItems.remove(remoteDevice);
    }

    synchronized public void removeAllLocalDevices() {
        localItems.removeAll();
    }

    synchronized public void removeAllRemoteDevices() {
        remoteItems.removeAll();
    }

    synchronized public Device getDevice(UDN udn, boolean rootOnly) {
        Device device;
        if ((device = localItems.get(udn, rootOnly)) != null) return device;
        if ((device = remoteItems.get(udn, rootOnly)) != null) return device;
        return null;
    }

    synchronized public Collection<LocalDevice> getLocalDevices() {
        return Collections.unmodifiableCollection(localItems.get());
    }

    synchronized public Collection<RemoteDevice> getRemoteDevices() {
        return Collections.unmodifiableCollection(remoteItems.get());
    }

    synchronized public Collection<Device> getDevices() {
        Set all = new HashSet();
        all.addAll(localItems.get());
        all.addAll(remoteItems.get());
        return Collections.unmodifiableCollection(all);
    }

    synchronized public Collection<Device> getDevices(DeviceType deviceType) {
        Collection<Device> devices = new HashSet();

        devices.addAll(localItems.get(deviceType));
        devices.addAll(remoteItems.get(deviceType));

        return Collections.unmodifiableCollection(devices);
    }

    synchronized public Collection<Device> getDevices(ServiceType serviceType) {
        Collection<Device> devices = new HashSet();

        devices.addAll(localItems.get(serviceType));
        devices.addAll(remoteItems.get(serviceType));

        return Collections.unmodifiableCollection(devices);
    }

    synchronized public DeviceService getDeviceService(ServiceReference serviceReference) {
        Device device;
        if ((device = getDevice(serviceReference.getUdn(), false)) != null) {
            return device.findDeviceService(serviceReference.getServiceId());
        }
        return null;
    }

    // #################################################################################################

    synchronized public Resource getResource(URI pathQuery) throws IllegalArgumentException {
        if (pathQuery.isAbsolute()) {
            throw new IllegalArgumentException("Resource URI can not be absolute, only path and query:" + pathQuery);
        }
        for (Resource resource : resources) {
            if (resource.matchesPathQuery(pathQuery)) {
                return resource;
            }
        }

        // TODO: UPNP VIOLATION: Fuppes on my ReadyNAS thinks it's a cool idea to add a slash at the end of the callback URI...
        // It also cuts off any query parameters in the callback URL - nice!
        if (pathQuery.getPath().endsWith("/")) {
            URI pathQueryWithoutSlash = URI.create(pathQuery.toString().substring(0, pathQuery.toString().length() - 1));
            for (Resource resource : resources) {
                if (resource.matchesPathQuery(pathQueryWithoutSlash)) {
                    return resource;
                }
            }
        }

        return null;
    }

    synchronized public <T> T getResourceModel(Class<T> modelType, Resource.Type type, URI pathQuery) throws IllegalArgumentException {
        Resource foundResource = getResource(pathQuery);
        if (foundResource != null
                && foundResource.getType().equals(type)
                && foundResource.getModel().getClass().isAssignableFrom(modelType)) {
            return (T) foundResource.getModel();
        }
        return null;
    }

    synchronized public void addResource(Resource resource) {
        resources.add(resource);

    }

    synchronized public boolean removeResource(Resource resource) {
        return resources.remove(resource);
    }

    synchronized public Collection<Resource> getResources() {
        return Collections.unmodifiableCollection(resources);
    }

    // #################################################################################################

    synchronized public void addLocalSubscription(LocalGENASubscription subscription) {
        localItems.addSubscription(subscription);
    }

    synchronized public LocalGENASubscription getLocalSubscription(String subscriptionId) {
        return localItems.getSubscription(subscriptionId);
    }

    synchronized public boolean updateLocalSubscription(LocalGENASubscription subscription) {
        return localItems.updateSubscription(subscription);
    }

    synchronized public boolean removeLocalSubscription(LocalGENASubscription subscription) {
        return localItems.removeSubscription(subscription);
    }

    synchronized public void addRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItems.addSubscription(subscription);
    }

    synchronized public RemoteGENASubscription getRemoteSubscription(String subscriptionId) {
        return remoteItems.getSubscription(subscriptionId);
    }

    synchronized public void updateRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItems.updateSubscription(subscription);
    }

    synchronized public void removeRemoteSubscription(RemoteGENASubscription subscription) {
        remoteItems.removeSubscription(subscription);
    }

    /* ############################################################################################################ */

    // When you call this, make sure you have the Router lock before this lock is obtained!
    synchronized public void shutdown() {
        log.fine("Shutting down registry...");

        registryMaintainer.stop();
        remoteItems.shutdown();
        localItems.shutdown();
    }

    /* ############################################################################################################ */

    synchronized void maintain() {
        log.finest("Maintaining registry...");

        // These add all their operations to the pendingExecutions queue
        remoteItems.maintain();
        localItems.maintain();

        // We now run the queue asynchronously so the maintenance thread can continue its loop undisturbed
        log.finest("Executing pending operations: " + pendingExecutions.size());
        for (Runnable pendingExecution : pendingExecutions) {
            getConfiguration().getAsyncProtocolExecutor().execute(pendingExecution);
        }
        if (pendingExecutions.size() > 0) {
            pendingExecutions.clear();
        }
    }

    synchronized void executeAsyncProtocol(Runnable runnable) {
        pendingExecutions.add(runnable);
    }

    /* ############################################################################################################ */

    public void printDebugLog() {
        if (log.isLoggable(Level.FINE)) {
            log.fine("====================================    REMOTE   ================================================");

            for (RemoteDevice remoteDevice : remoteItems.get()) {
                log.fine(remoteDevice.toString());
            }

            log.fine("====================================    LOCAL    ================================================");

            for (LocalDevice localDevice : localItems.get()) {
                log.fine(localDevice.toString());
            }

            log.fine("====================================  RESOURCES  ================================================");

            for (Resource resource : resources) {
                log.fine(resource.toString());
            }

            log.fine("=================================================================================================");

        }

    }


}
