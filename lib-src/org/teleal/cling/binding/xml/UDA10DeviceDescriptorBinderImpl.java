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

package org.teleal.cling.binding.xml;

import org.teleal.cling.binding.staging.MutableDeviceDescriptor;
import org.teleal.cling.binding.staging.MutableDeviceService;
import org.teleal.cling.binding.staging.MutableIcon;
import org.teleal.cling.binding.xml.parser.DeviceDOM;
import org.teleal.cling.binding.xml.parser.DeviceDOMParser;
import org.teleal.cling.binding.xml.parser.DeviceElement;
import org.teleal.cling.binding.xml.parser.ELEMENT;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceService;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.common.xml.ParserException;

import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;


public class UDA10DeviceDescriptorBinderImpl implements DeviceDescriptorBinder {

    private static Logger log = Logger.getLogger(DeviceDescriptorBinder.class.getName());

    protected final DeviceDOMParser parser = new DeviceDOMParser();

    public DeviceDOMParser getParser() {
        return parser;
    }

    public <D extends Device> D describe(D undescribedDevice, String descriptorXml) throws DescriptorBindingException, ValidationException {

        try {
            log.fine("Populating device from XML descriptor: " + undescribedDevice);

            // We can not validate the XML document. There is no possible XML schema (maybe RELAX NG) that would properly
            // constrain the UDA 1.0 device descriptor documents: Any unknown element or attribute must be ignored, order of elements
            // is not guaranteed. Try to write a schema for that! No combination of <xsd:any namespace="##any"> and <xsd:choice>
            // works with that... But hey, MSFT sure has great tech guys! So what we do here is just parsing out the known elements
            // and ignoring the other shit. We'll also do some very very basic validation of required elements, but that's it.

            // And by the way... try this with JAXB instead of manual DOM processing! And you thought it couldn't get worse....

            DeviceDOM dom = getParser().parse(descriptorXml, false);

            // Read the XML into a mutable descriptor graph
            MutableDeviceDescriptor descriptor = new MutableDeviceDescriptor();
            DeviceElement rootElement = dom.getRoot(getParser().createXPath());
            hydrateRoot(descriptor, rootElement);

            // Build the immutable descriptor graph
            return (D) descriptor.build(undescribedDevice);

        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DescriptorBindingException("Could not parse device descriptor: " + ex.toString(), ex);
        }
    }

    protected void hydrateRoot(MutableDeviceDescriptor descriptor, DeviceElement rootElement) throws DescriptorBindingException, ParserException {

        if (!rootElement.getW3CElement().getNamespaceURI().equals(DeviceDOM.NAMESPACE_URI)) {
            throw new DescriptorBindingException("Wrong XML namespace declared on root element: "
                    + rootElement.getW3CElement().getNamespaceURI());
        }

        if (!rootElement.getElementName().equals(ELEMENT.root.name())) {
            throw new DescriptorBindingException("Root element name is not <root>: " + rootElement.getElementName());
        }

        DeviceElement rootDeviceElement = null;

        DeviceElement[] rootChildren = rootElement.getChildren();
        for (DeviceElement rootChild : rootChildren) {

            if (rootChild.equals(ELEMENT.specVersion)) {
                hydrateSpecVersion(descriptor, rootChild);
            } else if (rootChild.equals(ELEMENT.URLBase)) {
                try {
                    // We hope it's  RFC 2396 and RFC 2732 compliant
                    descriptor.baseURL = new URL(rootChild.getContent());
                } catch (Exception ex) {
                    throw new DescriptorBindingException("Invalid URLBase: " + ex.toString());
                }
            } else if (rootChild.equals(ELEMENT.device)) {
                // Just sanity check here...
                if (rootDeviceElement != null)
                    throw new DescriptorBindingException("Found multiple <device> elements in <root>");
                rootDeviceElement = rootChild;
            } else {
                log.finer("Ignoring unknown element: " + rootChild.getElementName());
            }
        }

        if (rootDeviceElement == null) {
            throw new DescriptorBindingException("No <device> element in <root>");
        }
        hydrateDevice(descriptor, rootDeviceElement);
    }

    public void hydrateSpecVersion(MutableDeviceDescriptor descriptor, DeviceElement specVersionElement)
            throws DescriptorBindingException, ParserException {

        descriptor.udaMajorVersion =
                Integer.valueOf(specVersionElement.getRequiredChild(ELEMENT.major.name()).getContent());
        descriptor.udaMinorVersion =
                Integer.valueOf(specVersionElement.getRequiredChild(ELEMENT.minor.name()).getContent());
    }

    public void hydrateDevice(MutableDeviceDescriptor descriptor, DeviceElement deviceElement) throws DescriptorBindingException {

        DeviceElement[] deviceElementChildren = deviceElement.getChildren();
        for (DeviceElement deviceElementChild : deviceElementChildren) {

            if (deviceElementChild.equals(ELEMENT.deviceType)) {
                descriptor.deviceType = deviceElementChild.getContent();
            } else if (deviceElementChild.equals(ELEMENT.friendlyName)) {
                descriptor.friendlyName = deviceElementChild.getContent();
            } else if (deviceElementChild.equals(ELEMENT.manufacturer)) {
                descriptor.manufacturer = deviceElementChild.getContent();
            } else if (deviceElementChild.equals(ELEMENT.manufacturerURL)) {
                // TODO: UPNP VIOLATION: Netgear DG834 uses a non-URI: 'www.netgear.com'
                if (deviceElementChild.getContent().startsWith("www.")) {
                    descriptor.manufacturerURI = URI.create("http://" + deviceElementChild.getContent());
                } else {
                    descriptor.manufacturerURI = URI.create(deviceElementChild.getContent());
                }
            } else if (deviceElementChild.equals(ELEMENT.modelDescription)) {
                descriptor.modelDescription = deviceElementChild.getContent();
            } else if (deviceElementChild.equals(ELEMENT.modelName)) {
                descriptor.modelName = deviceElementChild.getContent();
            } else if (deviceElementChild.equals(ELEMENT.modelNumber)) {
                descriptor.modelNumber = deviceElementChild.getContent();
            } else if (deviceElementChild.equals(ELEMENT.modelURL)) {
                // TODO: UPNP VIOLATION: Netgear DG834 uses a non-URI: 'www.netgear.com'
                if (deviceElementChild.getContent().startsWith("www.")) {
                    descriptor.modelURI = URI.create("http://" + deviceElementChild.getContent());
                } else {
                    descriptor.modelURI = URI.create(deviceElementChild.getContent());
                }
            } else if (deviceElementChild.equals(ELEMENT.presentationURL)) {
                descriptor.presentationURI = URI.create(deviceElementChild.getContent());
            } else if (deviceElementChild.equals(ELEMENT.UPC)) {
                descriptor.upc = deviceElementChild.getContent();
            } else if (deviceElementChild.equals(ELEMENT.serialNumber)) {
                descriptor.serialNumber = deviceElementChild.getContent();
            } else if (deviceElementChild.equals(ELEMENT.UDN)) {
                descriptor.udn = UDN.fromString(deviceElementChild.getContent());
            } else if (deviceElementChild.equals(ELEMENT.iconList)) {
                hydrateIconList(descriptor, deviceElementChild);
            } else if (deviceElementChild.equals(ELEMENT.serviceList)) {
                hydrateServiceList(descriptor, deviceElementChild);
            } else if (deviceElementChild.equals(ELEMENT.deviceList)) {
                hydrateDeviceList(descriptor, deviceElementChild);
            }
        }
    }

    public void hydrateIconList(MutableDeviceDescriptor descriptor, DeviceElement iconsElement) throws DescriptorBindingException {

        DeviceElement[] iconsChildren = iconsElement.getChildren(ELEMENT.icon.name());
        for (DeviceElement iconElement : iconsChildren) {

            MutableIcon icon = new MutableIcon();

            DeviceElement[] iconChildren = iconElement.getChildren();
            for (DeviceElement iconChild : iconChildren) {

                if (iconChild.equals(ELEMENT.width)) {
                    icon.width = (Integer.valueOf(iconChild.getContent()));
                } else if (iconChild.equals(ELEMENT.height)) {
                    icon.height = (Integer.valueOf(iconChild.getContent()));
                } else if (iconChild.equals(ELEMENT.depth)) {
                    icon.depth = (Integer.valueOf(iconChild.getContent()));
                } else if (iconChild.equals(ELEMENT.url)) {
                    icon.uri = (URI.create(iconChild.getContent()));
                } else if (iconChild.equals(ELEMENT.mimetype)) {
                    icon.mimeType = iconChild.getContent();
                }

            }

            descriptor.icons.add(icon);
        }
    }

    public void hydrateServiceList(MutableDeviceDescriptor descriptor, DeviceElement servicesElement) throws DescriptorBindingException {

        DeviceElement[] serviceElements = servicesElement.getChildren(ELEMENT.service.name());
        for (DeviceElement serviceElement : serviceElements) {

            MutableDeviceService service = new MutableDeviceService();

            DeviceElement[] serviceChildren = serviceElement.getChildren();

            for (DeviceElement serviceChild : serviceChildren) {

                if (serviceChild.equals(ELEMENT.serviceType)) {
                    service.serviceType = (ServiceType.fromString(serviceChild.getContent()));
                } else if (serviceChild.equals(ELEMENT.serviceId)) {
                    service.serviceId = (ServiceId.fromString(serviceChild.getContent()));
                } else if (serviceChild.equals(ELEMENT.SCPDURL)) {
                    service.descriptorURI = (URI.create(serviceChild.getContent()));
                } else if (serviceChild.equals(ELEMENT.controlURL)) {
                    service.controlURI = (URI.create(serviceChild.getContent()));
                } else if (serviceChild.equals(ELEMENT.eventSubURL)) {
                    service.eventSubscriptionURI = (URI.create(serviceChild.getContent()));
                }

            }

            descriptor.deviceServices.add(service);
        }
    }

    public void hydrateDeviceList(MutableDeviceDescriptor descriptor, DeviceElement devicesElement) throws DescriptorBindingException {

        DeviceElement[] deviceElements = devicesElement.getChildren(ELEMENT.device.name());
        for (DeviceElement deviceElement : deviceElements) {
            MutableDeviceDescriptor embeddedDevice = new MutableDeviceDescriptor();
            embeddedDevice.parentDevice = descriptor;
            descriptor.embeddedDevices.add(embeddedDevice);
            hydrateDevice(embeddedDevice, deviceElement);
        }
    }


    public String generate(Device deviceModel) throws DescriptorBindingException {
        try {
            log.fine("Generating XML descriptor from device model: " + deviceModel);

            DeviceDOM dom = getParser().createDocument();
            generateRoot(deviceModel, dom);
            return getParser().print(dom, 4, true);

        } catch (Exception ex) {
            throw new DescriptorBindingException("Could not generate device descriptor: " + ex.toString(), ex);
        }
    }

    private void generateRoot(Device deviceModel, DeviceDOM dom) {

        DeviceElement root = dom.createRoot(getParser().createXPath(), ELEMENT.root);

        generateSpecVersion(deviceModel, root);

        // UDA 1.1 spec says: Don't use URLBase anymore
        // if (deviceModel.getBaseURL() != null) {
        //     appendChildElementWithTextContent(descriptor, rootElement, "URLBase", deviceModel.getBaseURL().toString());
        // }

        generateDevice(deviceModel, root);
    }

    private void generateSpecVersion(Device deviceModel, DeviceElement root) {

        DeviceElement specVersion = root.createChild(ELEMENT.specVersion);
        specVersion.createChild(ELEMENT.major).setContent(Integer.toString(deviceModel.getVersion().getMajor()));
        specVersion.createChild(ELEMENT.minor).setContent(Integer.toString(deviceModel.getVersion().getMinor()));
    }


    private void generateDevice(Device deviceModel, DeviceElement root) {

        DeviceElement device = root.createChild(ELEMENT.device);

        device.createChild(ELEMENT.deviceType).setContent(deviceModel.getType().toString());
        device.createChild(ELEMENT.UDN).setContent(deviceModel.getIdentity().getUdn().toString());

        device.createChildIfNotNull(
                ELEMENT.friendlyName,
                deviceModel.getDetails().getFriendlyName()
        );

        device.createChildIfNotNull(
                ELEMENT.manufacturer,
                deviceModel.getDetails().getManufacturerDetails().getManufacturer()
        );

        device.createChildIfNotNull(
                ELEMENT.manufacturerURL,
                deviceModel.getDetails().getManufacturerDetails().getManufacturerURI()
        );

        device.createChildIfNotNull(
                ELEMENT.modelDescription,
                deviceModel.getDetails().getModelDetails().getModelDescription()
        );

        device.createChildIfNotNull(
                ELEMENT.modelName,
                deviceModel.getDetails().getModelDetails().getModelName()
        );

        device.createChildIfNotNull(
                ELEMENT.modelNumber,
                deviceModel.getDetails().getModelDetails().getModelNumber()
        );

        device.createChildIfNotNull(
                ELEMENT.modelURL,
                deviceModel.getDetails().getModelDetails().getModelURI()
        );

        device.createChildIfNotNull(
                ELEMENT.serialNumber,
                deviceModel.getDetails().getSerialNumber()
        );

        device.createChildIfNotNull(
                ELEMENT.presentationURL,
                deviceModel.getDetails().getPresentationURI()
        );

        device.createChildIfNotNull(
                ELEMENT.UPC,
                deviceModel.getDetails().getUpc()
        );

        generateIconList(deviceModel, device);
        generateServiceList(deviceModel, device);
        generateDeviceList(deviceModel, device);
    }

    private void generateIconList(Device deviceModel, DeviceElement device) {
        if (!deviceModel.hasIcons()) return;

        DeviceElement iconList = device.createChild(ELEMENT.iconList);

        for (Icon icon : deviceModel.getIcons()) {
            DeviceElement iconElement = iconList.createChild(ELEMENT.icon);
            iconElement.createChildIfNotNull(ELEMENT.mimetype, icon.getMimeType());
            iconElement.createChildIfNotNull(ELEMENT.width, icon.getWidth());
            iconElement.createChildIfNotNull(ELEMENT.height, icon.getHeight());
            iconElement.createChildIfNotNull(ELEMENT.depth, icon.getDepth());
            iconElement.createChildIfNotNull(ELEMENT.url, icon.getUri());
        }
    }

    private void generateServiceList(Device deviceModel, DeviceElement device) {
        if (!deviceModel.hasDeviceServices()) return;

        DeviceElement serviceList = device.createChild(ELEMENT.serviceList);

        for (DeviceService deviceService : deviceModel.getDeviceServices()) {
            DeviceElement serviceElement = serviceList.createChild(ELEMENT.service);
            serviceElement.createChildIfNotNull(ELEMENT.serviceType, deviceService.getServiceType());
            serviceElement.createChildIfNotNull(ELEMENT.serviceId, deviceService.getServiceId());
            serviceElement.createChildIfNotNull(ELEMENT.controlURL, deviceService.getControlURI());
            serviceElement.createChildIfNotNull(ELEMENT.eventSubURL, deviceService.getEventSubscriptionURI());
            serviceElement.createChildIfNotNull(ELEMENT.SCPDURL, deviceService.getDescriptorURI());
        }
    }

    private void generateDeviceList(Device deviceModel, DeviceElement device) {
        if (!deviceModel.hasEmbeddedDevices()) return;

        DeviceElement deviceList = device.createChild(ELEMENT.deviceList);
        for (Device embeddedDevice : deviceModel.getEmbeddedDevices()) {
            generateDevice(embeddedDevice, deviceList);
        }
    }

}