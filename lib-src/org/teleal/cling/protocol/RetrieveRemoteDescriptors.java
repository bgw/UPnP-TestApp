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

package org.teleal.cling.protocol;

import org.teleal.cling.UpnpService;
import org.teleal.cling.binding.xml.DescriptorBindingException;
import org.teleal.cling.binding.xml.DeviceDescriptorBinder;
import org.teleal.cling.binding.xml.ServiceDescriptorBinder;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.RemoteService;
import org.teleal.cling.registry.RegistrationException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public class RetrieveRemoteDescriptors implements Runnable {

    private static Logger log = Logger.getLogger(RetrieveRemoteDescriptors.class.getName());

    private final UpnpService upnpService;
    private RemoteDevice rd;

    private static final Set<URL> activeRetrievals = new CopyOnWriteArraySet();

    public RetrieveRemoteDescriptors(UpnpService upnpService, RemoteDevice rd) {
        this.upnpService = upnpService;
        this.rd = rd;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public void run() {

        URL deviceURL = rd.getIdentity().getDescriptorURL();

        // Performance optimization, try to avoid concurrent GET requests for device descriptor,
        // if we retrieve it once, we have the hydrated device. There is no different outcome
        // processing this several times concurrently.

        if (activeRetrievals.contains(deviceURL)) {
            log.finer("Exiting early, active retrieval for URL already in progress: " + deviceURL);
            return;
        }

        try {
            activeRetrievals.add(deviceURL);
            describe();
        } finally {
            activeRetrievals.remove(deviceURL);
        }
    }

    protected void describe() {

        // All of the following is a very expensive and time consuming procedure, thanks to the
        // braindead design of UPnP. Several GET requests, several descriptors, several XML parsing
        // steps - all of this could be done with one and it wouldn't make a difference. So every
        // call of this method has to be really necessary and rare.

        StreamRequestMessage deviceDescRetrievalMsg =
                new StreamRequestMessage(UpnpRequest.Method.GET, rd.getIdentity().getDescriptorURL());

        log.fine("Sending device descriptor retrieval message: " + deviceDescRetrievalMsg);
        StreamResponseMessage deviceDescMsg = getUpnpService().getRouter().send(deviceDescRetrievalMsg);

        if (deviceDescMsg == null) {
            log.warning("Could not retrieve device descriptor: " + rd);
            return;
        }

        if (deviceDescMsg.getOperation().isFailed()) {
            log.warning(
                    "Device descriptor retrieval failed:" + rd.getIdentity().getDescriptorURL() +
                    ", " + deviceDescMsg.getOperation().getResponseDetails()
            );
            return;
        }

        if (deviceDescMsg.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE) == null) {
            log.warning("Received device descriptor without Content-Type: " + rd.getIdentity().getDescriptorURL());
            return;
        }

        if (!deviceDescMsg.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).isUDACompliant()) {
            log.warning("Received device descriptor was received with an invalid Content-Type: " + rd.getIdentity().getDescriptorURL());
            return;
        }

        log.fine("Received root device descriptor: " + deviceDescMsg);
        describe(deviceDescMsg.getBodyString());
    }

    protected void describe(String descriptorXML) {
        try {

            DeviceDescriptorBinder deviceDescriptorBinder =
                    getUpnpService().getConfiguration().getDeviceDescriptorBinderUDA10();

            RemoteDevice describedDevice = deviceDescriptorBinder.describe(rd, descriptorXML);
            describedDevice = describeServicesRecursive(describedDevice, describedDevice);

            log.fine("Adding fully hydrated remote device to registry: " + describedDevice);
            // The registry will do the right thing: A new root device is going to be added, if it's
            // already present or we just received the descriptor again (because we got an embedded
            // devices' notification), it will simply update the expiration timestamp of the root
            // device.
            getUpnpService().getRegistry().addDevice(describedDevice);

        } catch (ValidationException ex) {
            log.warning("Could not validate device model: " + rd);
            for (ValidationError validationError : ex.getErrors()) {
                log.warning(validationError.toString());
            }
        } catch (DescriptorBindingException ex) {
            log.warning("Could not hydrate device or its services from descriptor: " + rd);
            log.warning("Cause was: " + ex.toString());
        } catch (RegistrationException ex) {
            log.warning("Adding hydrated device to registry failed: " + rd);
            log.warning("Cause was: " + ex.toString());
        }
    }

    protected RemoteDevice describeServicesRecursive(RemoteDevice rootDevice, RemoteDevice currentDevice)
            throws DescriptorBindingException, ValidationException {

        List<DeviceService> describedServices = new ArrayList();
        if (currentDevice.hasDeviceServices()) {
            for (DeviceService deviceService : currentDevice.getDeviceServices()) {
                describedServices.add(readServiceDescriptor(rootDevice, deviceService));
            }
        }

        List<RemoteDevice> describedEmbeddedDevices = new ArrayList();
        if (currentDevice.hasEmbeddedDevices()) {
            for (RemoteDevice embeddedDevice : currentDevice.getEmbeddedDevices()) {
                describedEmbeddedDevices.add(describeServicesRecursive(rootDevice, embeddedDevice));
            }
        }

        // Yes, we create a completely new immutable graph here (this time with DS linked to S)
        return currentDevice.newInstance(
                currentDevice.getIdentity().getUdn(),
                currentDevice.getVersion(),
                currentDevice.getType(),
                currentDevice.getDetails(),
                currentDevice.getIcons(),
                describedServices.toArray(new DeviceService[describedServices.size()]),
                describedEmbeddedDevices
        );
    }

    protected DeviceService readServiceDescriptor(RemoteDevice device, DeviceService deviceService)
            throws DescriptorBindingException, ValidationException {

        URL descriptorURL = device.normalizeURI(deviceService.getDescriptorURI());
        StreamRequestMessage serviceDescRetrievalMsg = new StreamRequestMessage(UpnpRequest.Method.GET, descriptorURL);

        log.fine("Sending service descriptor retrieval message: " + serviceDescRetrievalMsg);
        StreamResponseMessage serviceDescMsg = getUpnpService().getRouter().send(serviceDescRetrievalMsg);

        if (serviceDescMsg == null) {
            log.warning("Could not retrieve service descriptor: " + deviceService);
            return null;
        }

        if (serviceDescMsg.getOperation().isFailed()) {
            log.warning("Service descriptor retrieval failed:" + serviceDescMsg.getOperation().getResponseDetails());
            return null;
        }

        if (!serviceDescMsg.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).isUDACompliant()) {
            log.warning("Received service descriptor was received with an invalid Content-Type: " + serviceDescMsg);
            return null;
        }

        log.fine("Received service descriptor, hydrating service model: " + serviceDescMsg);
        ServiceDescriptorBinder serviceDescriptorBinder =
                getUpnpService().getConfiguration().getServiceDescriptorBinderUDA10();

        RemoteService service = serviceDescriptorBinder.read(RemoteService.class, serviceDescMsg.getBodyString());

        // The DS now has a reference to an S - it's immutable so we create a new one
        return new DeviceService(
                deviceService.getServiceType(),
                deviceService.getServiceId(),
                deviceService.getDescriptorURI(),
                deviceService.getControlURI(),
                deviceService.getEventSubscriptionURI(),
                service
        );
    }

}
