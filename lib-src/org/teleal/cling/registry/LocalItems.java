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
import org.teleal.cling.model.gena.LocalGENASubscription;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.protocol.SendingAsync;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;


class LocalItems extends RegistryItems<LocalDevice, LocalGENASubscription> {

    private static Logger log = Logger.getLogger(LocalItems.class.getName());

    LocalItems(RegistryImpl registry) {
        super(registry);
    }

    void add(LocalDevice localDevice) throws RegistrationException {

        log.fine("Adding local device to registry: " + localDevice);

        for (Resource deviceResource : localDevice.getResources()) {

            if (registry.getResource(deviceResource.getLocalPathQuery()) != null) {
                throw new RegistrationException("URI namespace conflict with already registered resource: " + deviceResource);
            }

            registry.addResource(deviceResource);
            log.fine("Registered resource: " + deviceResource);

        }

        log.fine("Adding item to registry with expiration in seconds: " + localDevice.getIdentity().getMaxAgeSeconds());

        RegistryItem<UDN, LocalDevice> localItem = new RegistryItem<UDN, LocalDevice>(
                localDevice.getIdentity().getUdn(),
                localDevice,
                localDevice.getIdentity().getMaxAgeSeconds()
        );

        deviceItems.add(localItem);
        log.fine("Registered local device: " + localItem);

        advertiseAlive(localDevice);

        for (RegistryListener listener : registry.getListeners()) {
            listener.localDeviceAdded(registry, localDevice);
        }

    }

    Collection<LocalDevice> get() {
        Set<LocalDevice> c = new HashSet();
        for (RegistryItem<UDN, LocalDevice> item : deviceItems) {
            c.add(item.getItem());
        }
        return Collections.unmodifiableCollection(c);
    }

    boolean remove(final LocalDevice localDevice) throws RegistrationException {
        return remove(localDevice, false);
    }

    boolean remove(final LocalDevice localDevice, boolean shuttingDown) throws RegistrationException {

        LocalDevice registeredDevice = get(localDevice.getIdentity().getUdn(), true);
        if (registeredDevice != null) {

            log.fine("Removing local device from registry: " + localDevice);

            deviceItems.remove(new RegistryItem(localDevice.getIdentity().getUdn()));

            for (Resource deviceResource : localDevice.getResources()) {
                if (registry.removeResource(deviceResource)) {
                    log.fine("Unregistered resource: " + deviceResource);
                }
            }

            // Active subscriptions
            Iterator<RegistryItem<String, LocalGENASubscription>> it = subscriptionItems.iterator();
            while (it.hasNext()) {
                final RegistryItem<String, LocalGENASubscription> incomingSubscription = it.next();

                UDN subscriptionForUDN =
                        incomingSubscription.getItem().getDeviceService().getDevice().getIdentity().getUdn();

                if (subscriptionForUDN.equals(registeredDevice.getIdentity().getUdn())) {
                    log.fine("Removing incoming subscription: " + incomingSubscription.getKey());
                    it.remove();
                    if (!shuttingDown) {
                        registry.getConfiguration().getRegistryListenerExecutor().execute(
                                new Runnable() {
                                    public void run() {
                                        incomingSubscription.getItem().end(CancelReason.DEVICE_WAS_REMOVED);
                                    }
                                }
                        );
                    }
                }
            }

            advertiseByebye(localDevice, !shuttingDown);

            if (!shuttingDown) {
                for (final RegistryListener listener : registry.getListeners()) {
                    registry.getConfiguration().getRegistryListenerExecutor().execute(
                            new Runnable() {
                                public void run() {
                                    listener.localDeviceRemoved(registry, localDevice);
                                }
                            }
                    );
                }
            }

            return true;
        }

        return false;
    }

    void removeAll() {
        removeAll(false);
    }

    void removeAll(boolean shuttingDown) {
        LocalDevice[] allDevices = get().toArray(new LocalDevice[get().size()]);
        for (LocalDevice device : allDevices) {
            remove(device, shuttingDown);
        }
    }

    /* ############################################################################################################ */

    void maintain() {

        // Refresh expired local devices
        Set<RegistryItem<UDN, LocalDevice>> expiredLocalItems = new HashSet();
        for (RegistryItem<UDN, LocalDevice> localItem : deviceItems) {
            if (localItem.hasExpired(true)) {
                log.finer("Local item has expired: " + localItem);
                expiredLocalItems.add(localItem);
            }
        }
        for (RegistryItem<UDN, LocalDevice> expiredLocalItem : expiredLocalItems) {
            log.fine("Refreshing local device advertisement: " + expiredLocalItem.getItem());
            advertiseAlive(expiredLocalItem.getItem());
            expiredLocalItem.stampLastRefresh();
        }

        // Expire incoming subscriptions
        Set<RegistryItem<String, LocalGENASubscription>> expiredIncomingSubscriptions = new HashSet();
        for (RegistryItem<String, LocalGENASubscription> item : subscriptionItems) {
            if (item.hasExpired(false)) {
                expiredIncomingSubscriptions.add(item);
            }
        }
        for (RegistryItem<String, LocalGENASubscription> subscription : expiredIncomingSubscriptions) {
            log.fine("Removing expired: " + subscription);
            removeSubscription(subscription.getItem());
            subscription.getItem().end(CancelReason.EXPIRED);
        }

    }

    void shutdown() {
        log.fine("Clearing all registered subscriptions to local devices during shutdown");
        subscriptionItems.clear();

        log.fine("Removing all local devices from registry during shutdown");
        removeAll(true);
    }

    /* ############################################################################################################ */

    protected Random randomGenerator = new Random();

    protected void advertiseAlive(final LocalDevice localDevice) {
        registry.executeAsyncProtocol(new Runnable() {
            public void run() {
                try {
                    log.finer("Sleeping some milliseconds to avoid flooding the network with ALIVE msgs");
                    Thread.sleep(randomGenerator.nextInt(100));
                } catch (InterruptedException ex) {
                    log.severe("Background execution interrupted: " + ex.getMessage());
                }
                registry.getProtocolFactory().createSendingNotificationAlive(localDevice).run();
            }
        });
    }

    protected void advertiseByebye(final LocalDevice localDevice, boolean asynchronous) {
        final SendingAsync prot = registry.getProtocolFactory().createSendingNotificationByebye(localDevice);
        if (asynchronous) {
            registry.executeAsyncProtocol(prot);
        } else {
            prot.run();
        }
    }

}
