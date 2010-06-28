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

package org.teleal.cling.protocol.sync;

import org.teleal.cling.binding.xml.DescriptorBindingException;
import org.teleal.cling.binding.xml.DeviceDescriptorBinder;
import org.teleal.cling.binding.xml.ServiceDescriptorBinder;
import org.teleal.cling.model.Resource;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.UpnpService;
import org.teleal.cling.protocol.ReceivingSync;

import java.net.URI;
import java.util.logging.Logger;


public class ReceivingRetrieval extends ReceivingSync<StreamRequestMessage, StreamResponseMessage> {

    private static Logger log = Logger.getLogger(ReceivingRetrieval.class.getName());

    public ReceivingRetrieval(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    protected StreamResponseMessage executeSync() {

        if (!getInputMessage().hasHostHeader()) {
            log.fine("Ignoring message, missing HOST header: " + getInputMessage());
            return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        URI requestedURI = getInputMessage().getOperation().getURI();

        Resource foundResource = getUpnpService().getRegistry().getResource(requestedURI);

        if (foundResource == null) {
            log.fine("No local resource found: " + getInputMessage());
            return null;
        }

        try {
            switch (foundResource.getType()) {

                case DEVICE_DESCRIPTOR:

                    log.fine("Found local device matching relative request URI: " + requestedURI);
                    LocalDevice device = (LocalDevice) foundResource.getModel();

                    DeviceDescriptorBinder deviceDescriptorBinder =
                            getUpnpService().getConfiguration().getDeviceDescriptorBinderUDA10();
                    String deviceDescriptor = deviceDescriptorBinder.generate(device);
                    return new StreamResponseMessage(deviceDescriptor, new ContentTypeHeader());

                case SERVICE_DESCRIPTOR:

                    log.fine("Found local service matching relative request URI: " + requestedURI);
                    DeviceService deviceService = (DeviceService) foundResource.getModel();

                    ServiceDescriptorBinder serviceDescriptorBinder =
                            getUpnpService().getConfiguration().getServiceDescriptorBinderUDA10();
                    String serviceDescriptor = serviceDescriptorBinder.generate(deviceService.getService());
                    return new StreamResponseMessage(serviceDescriptor, new ContentTypeHeader());

                case ICON:

                    log.fine("Found local icon matching relative request URI: " + requestedURI);
                    Icon icon = (Icon) foundResource.getModel();
                    return new StreamResponseMessage(icon.getData(), icon.getMimeType());

                default:
                    // TODO
                    log.warning("TODO: unknown local resource found, ignoring it: " + foundResource);
                    return null;
            }
        } catch(DescriptorBindingException ex) {
            log.warning("Error generating requested device/service descriptor: " + ex.toString());
            log.warning("Cause: " + ex.getCause().toString());
            return new StreamResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
        }
    }

}
