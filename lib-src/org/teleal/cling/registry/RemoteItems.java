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

import org.teleal.cling.model.Resource;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.RemoteGENASubscription;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteDeviceIdentity;
import org.teleal.cling.model.types.UDN;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


class RemoteItems extends RegistryItems<RemoteDevice, RemoteGENASubscription> {

    private static Logger log = Logger.getLogger(RemoteItems.class.getName());

    RemoteItems(RegistryImpl registry) {
        super(registry);
    }

    /**
     * Adds the given remote device to the registry, or udpates its expiration timestamp.
     * <p>
     * This method first checks if there is a remote device with the same UDN already registered. If so, it
     * updates the expiration timestamp of the remote device without notifying any registry listeners. If the
     * device is truly new, all its resources are tested for conflicts with existing resources in the registry's
     * namespace, then it is added to the registry and listeners are notified that a new fully described remote
     * device is now available.
     * </p>
     * @param device The remote device to be added
     */
    void add(final RemoteDevice device) {

        if (update(device.getIdentity())) {
            log.fine("Ignoring addition, device already registered");
            return;
        }

        for (Resource deviceResource : device.getResources()) {
            log.fine("Validating remote device resource; " + deviceResource);
            if (registry.getResource(deviceResource.getLocalPathQuery()) != null) {
                throw new RegistrationException("URI namespace conflict with already registered resource: " + deviceResource);
            }
        }

        for (Resource validatedResource : device.getResources()) {
            registry.addResource(validatedResource);
            log.fine("Added remote device resource: " + validatedResource);
        }

        RegistryItem item = new RegistryItem(
                device.getIdentity().getUdn(),
                device,
                device.getIdentity().getMaxAgeSeconds()
        );
        log.fine("Adding hydrated remote device to registry with " + item.getMaxAgeSeconds() + " seconds expiration: " + device);
        deviceItems.add(item);

        if (log.isLoggable(Level.FINER)) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append("-------------------------- START Registry Namespace -----------------------------------\n");
            for (Resource resource : registry.getResources()) {
                sb.append(resource).append("\n");
            }
            sb.append("-------------------------- END Registry Namespace -----------------------------------");
            log.finer(sb.toString());
        }

        // Only notify the listeners when the device is fully usable
        log.fine("Completely hydrated remote device graph available, calling listeners: " + device);
        for (final RegistryListener listener : registry.getListeners()) {
            registry.getConfiguration().getRegistryListenerExecutor().execute(
                    new Runnable() {
                        public void run() {
                            listener.remoteDeviceAdded(registry, device);
                        }
                    }
            );
        }

    }

    boolean update(RemoteDeviceIdentity rdIdentity) {

        for (LocalDevice localDevice : registry.getLocalDevices()) {
            if (localDevice.findDevice(rdIdentity.getUdn()) != null) {
                log.fine("Ignoring update, a local device graph contains UDN");
                return true;
            }
        }

        RemoteDevice registeredRemoteDevice = get(rdIdentity.getUdn(), false);
        if (registeredRemoteDevice != null) {

            if (!registeredRemoteDevice.isRoot()) {
                log.fine("Updating root device of embedded: " + registeredRemoteDevice);
                registeredRemoteDevice = registeredRemoteDevice.getRoot();
            }

            final RegistryItem<UDN, RemoteDevice> item = new RegistryItem<UDN, RemoteDevice>(
                    registeredRemoteDevice.getIdentity().getUdn(),
                    registeredRemoteDevice,
                    rdIdentity.getMaxAgeSeconds()
            );

            log.fine("Updating expiration of: " + registeredRemoteDevice);
            deviceItems.remove(item);
            deviceItems.add(item);

            log.fine("Remote device updated, calling listeners: " + registeredRemoteDevice);
            for (final RegistryListener listener : registry.getListeners()) {
                registry.getConfiguration().getRegistryListenerExecutor().execute(
                        new Runnable() {
                            public void run() {
                                listener.remoteDeviceUpdated(registry, item.getItem());
                            }
                        }
                );
            }

            return true;

        }
        return false;
    }

    /**
     * Removes the given device from the registry and notifies registry listeners.
     *
     * @param remoteDevice The device to remove from the registry.
     * @return <tt>true</tt> if the given device was found and removed from the registry, false if it wasn't registered.
     */
    boolean remove(final RemoteDevice remoteDevice) {
        return remove(remoteDevice, false);
    }

    boolean remove(final RemoteDevice remoteDevice, boolean shuttingDown) throws RegistrationException {
        final RemoteDevice registeredDevice = get(remoteDevice.getIdentity().getUdn(), true);
        if (registeredDevice != null) {

            log.fine("Removing remote device from registry: " + remoteDevice);

            // Resources
            for (Resource deviceResource : registeredDevice.getResources()) {
                if (registry.removeResource(deviceResource)) {
                    log.fine("Unregistered resource: " + deviceResource);
                }
            }

            // Active subscriptions
            Iterator<RegistryItem<String, RemoteGENASubscription>> it = subscriptionItems.iterator();
            while (it.hasNext()) {
                final RegistryItem<String, RemoteGENASubscription> outgoingSubscription = it.next();

                UDN subscriptionForUDN =
                        outgoingSubscription.getItem().getDeviceService().getDevice().getIdentity().getUdn();

                if (subscriptionForUDN.equals(registeredDevice.getIdentity().getUdn())) {
                    log.fine("Removing outgoing subscription: " + outgoingSubscription.getKey());
                    it.remove();
                    if (!shuttingDown) {
                        registry.getConfiguration().getRegistryListenerExecutor().execute(
                                new Runnable() {
                                    public void run() {
                                        outgoingSubscription.getItem().end(CancelReason.DEVICE_WAS_REMOVED, null);
                                    }
                                }
                        );
                    }
                }
            }

            // Only notify listeners if we are NOT in the process of shutting down the registry
            if (!shuttingDown) {
                for (final RegistryListener listener : registry.getListeners()) {
                    registry.getConfiguration().getRegistryListenerExecutor().execute(
                            new Runnable() {
                                public void run() {
                                    listener.remoteDeviceRemoved(registry, registeredDevice);
                                }
                            }
                    );
                }
            }

            // Finally, remove the device from the registry
            deviceItems.remove(new RegistryItem(registeredDevice.getIdentity().getUdn()));

            return true;
        }

        return false;
    }

    void removeAll() {
        removeAll(false);
    }

    void removeAll(boolean shuttingDown) {
        RemoteDevice[] allDevices = get().toArray(new RemoteDevice[get().size()]);
        for (RemoteDevice device : allDevices) {
            remove(device, shuttingDown);
        }
    }

    /* ############################################################################################################ */

    void start() {
        // Noop
    }

    void maintain() {

        // Remove expired remote devices
        Map<UDN, RemoteDevice> expiredRemoteDevices = new HashMap();
        for (RegistryItem<UDN, RemoteDevice> remoteItem : deviceItems) {
            log.finest("Device '"+remoteItem.getItem()+"' expires in seconds: " + remoteItem.getSecondsUntilExpiration());
            if (remoteItem.hasExpired(false)) {
                expiredRemoteDevices.put(remoteItem.getKey(), remoteItem.getItem());
            }
        }
        for (RemoteDevice remoteDevice : expiredRemoteDevices.values()) {
            log.fine("Removing expired: " + remoteDevice);
            remove(remoteDevice);
        }

        // Renew outgoing subscriptions
        Set<RemoteGENASubscription> expiredOutgoingSubscriptions = new HashSet();
        for (RegistryItem<String, RemoteGENASubscription> item : subscriptionItems) {
            if (item.hasExpired(true)) {
                expiredOutgoingSubscriptions.add(item.getItem());
            }
        }
        for (RemoteGENASubscription subscription : expiredOutgoingSubscriptions) {
            log.fine("Renewing outgoing subscription: " + subscription);
            renewOutgoingSubscription(subscription);
        }
    }

    void shutdown() {
        log.fine("Cancelling all outgoing subscriptions to remote devices during shutdown");
        for (RegistryItem<String, RemoteGENASubscription> item : subscriptionItems) {
            // This will remove the active subscription from the registry!
            registry.getProtocolFactory()
                    .createSendingUnsubscribe(item.getItem())
                    .run();
        }

        log.fine("Removing all remote devices from registry during shutdown");
        removeAll(true);
    }

    /* ############################################################################################################ */

    protected void renewOutgoingSubscription(final RemoteGENASubscription subscription) {
        registry.executeAsyncProtocol(
                registry.getProtocolFactory().createSendingRenewal(subscription)
        );
    }
}
