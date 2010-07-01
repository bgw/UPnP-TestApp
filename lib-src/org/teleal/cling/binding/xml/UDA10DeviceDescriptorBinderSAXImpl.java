package org.teleal.cling.binding.xml;

import org.teleal.cling.binding.staging.MutableDeviceDescriptor;
import org.teleal.cling.binding.staging.MutableDeviceService;
import org.teleal.cling.binding.staging.MutableIcon;
import org.teleal.cling.binding.staging.MutableUDAVersion;
import org.teleal.cling.binding.xml.parser.ELEMENT;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.types.ServiceId;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.common.xml.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A SAX parser implementation, which is actually slower (on desktop and on Android)!
 *
 * @author Christian Bauer
 */
public class UDA10DeviceDescriptorBinderSAXImpl extends UDA10DeviceDescriptorBinderImpl {

    private static Logger log = Logger.getLogger(DeviceDescriptorBinder.class.getName());

    public <D extends Device> D describe(D undescribedDevice, String descriptorXml) throws DescriptorBindingException, ValidationException {

        try {
            log.fine("Populating device from XML descriptor: " + undescribedDevice);

            // Read the XML into a mutable descriptor graph

            SAXParser parser = new SAXParser();

            MutableDeviceDescriptor descriptor = new MutableDeviceDescriptor();
            new RootHandler(descriptor, parser);

            parser.parse(
                    new InputSource(
                            new StringReader(descriptorXml)
                    )
            );

            // Build the immutable descriptor graph
            return (D) descriptor.build(undescribedDevice);

        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DescriptorBindingException("Could not parse device descriptor: " + ex.toString(), ex);
        }
    }

    protected static class RootHandler extends DeviceDescriptorHandler<MutableDeviceDescriptor> {

        public RootHandler(MutableDeviceDescriptor instance, SAXParser parser) {
            super(instance, parser);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

            if (element.equals(SpecVersionHandler.EL)) {
                MutableUDAVersion udaVersion = new MutableUDAVersion();
                getInstance().udaVersion = udaVersion;
                new SpecVersionHandler(udaVersion, this);
            }

            if (element.equals(DeviceHandler.EL)) {
                new DeviceHandler(getInstance(), this);
            }

        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case URLBase:
                    try {
                        // We hope it's  RFC 2396 and RFC 2732 compliant
                        getInstance().baseURL = new URL(getCharacters());
                    } catch (Exception ex) {
                        throw new SAXException("Invalid URLBase: " + ex.toString());
                    }
                    break;
            }
        }
    }

    protected static class SpecVersionHandler extends DeviceDescriptorHandler<MutableUDAVersion> {

        public static final ELEMENT EL = ELEMENT.specVersion;

        public SpecVersionHandler(MutableUDAVersion instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case major:
                    getInstance().major = Integer.valueOf(getCharacters());
                    break;
                case minor:
                    getInstance().minor = Integer.valueOf(getCharacters());
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceHandler extends DeviceDescriptorHandler<MutableDeviceDescriptor> {

        public static final ELEMENT EL = ELEMENT.device;

        public DeviceHandler(MutableDeviceDescriptor instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

            if (element.equals(IconListHandler.EL)) {
                List<MutableIcon> icons = new ArrayList();
                getInstance().icons = icons;
                new IconListHandler(icons, this);
            }

            if (element.equals(ServiceListHandler.EL)) {
                List<MutableDeviceService> services = new ArrayList();
                getInstance().deviceServices = services;
                new ServiceListHandler(services, this);
            }

            if (element.equals(DeviceListHandler.EL)) {
                List<MutableDeviceDescriptor> devices = new ArrayList();
                getInstance().embeddedDevices = devices;
                new DeviceListHandler(devices, this);
            }
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case deviceType:
                    getInstance().deviceType = getCharacters();
                    break;
                case friendlyName:
                    getInstance().friendlyName = getCharacters();
                    break;
                case manufacturer:
                    getInstance().manufacturer = getCharacters();
                    break;
                case manufacturerURL:
                    // TODO: UPNP VIOLATION: Netgear DG834 uses a non-URI: 'www.netgear.com'
                    if (getCharacters().startsWith("www.")) {
                        getInstance().manufacturerURI = URI.create("http://" + getCharacters());
                    } else {
                        getInstance().manufacturerURI = URI.create(getCharacters());
                    }
                    break;
                case modelDescription:
                    getInstance().modelDescription = getCharacters();
                    break;
                case modelName:
                    getInstance().modelName = getCharacters();
                    break;
                case modelNumber:
                    getInstance().modelNumber = getCharacters();
                    break;
                case modelURL:
                    // TODO: UPNP VIOLATION: Netgear DG834 uses a non-URI: 'www.netgear.com'
                    if (getCharacters().startsWith("www.")) {
                        getInstance().modelURI = URI.create("http://" + getCharacters());
                    } else {
                        getInstance().modelURI = URI.create(getCharacters());
                    }
                    break;
                case presentationURL:
                    getInstance().presentationURI = URI.create(getCharacters());
                    break;
                case UPC:
                    getInstance().upc = getCharacters();
                    break;
                case serialNumber:
                    getInstance().serialNumber = getCharacters();
                    break;
                case UDN:
                    getInstance().udn = UDN.valueOf(getCharacters());
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class IconListHandler extends DeviceDescriptorHandler<List<MutableIcon>> {

        public static final ELEMENT EL = ELEMENT.iconList;

        public IconListHandler(List<MutableIcon> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(IconHandler.EL)) {
                MutableIcon icon = new MutableIcon();
                getInstance().add(icon);
                new IconHandler(icon, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class IconHandler extends DeviceDescriptorHandler<MutableIcon> {

        public static final ELEMENT EL = ELEMENT.icon;

        public IconHandler(MutableIcon instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case width:
                    getInstance().width = Integer.valueOf(getCharacters());
                    break;
                case height:
                    getInstance().height = Integer.valueOf(getCharacters());
                    break;
                case depth:
                    getInstance().depth = Integer.valueOf(getCharacters());
                    break;
                case url:
                    getInstance().uri = URI.create(getCharacters());
                    break;
                case mimetype:
                    getInstance().mimeType = getCharacters();
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ServiceListHandler extends DeviceDescriptorHandler<List<MutableDeviceService>> {

        public static final ELEMENT EL = ELEMENT.serviceList;

        public ServiceListHandler(List<MutableDeviceService> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(ServiceHandler.EL)) {
                MutableDeviceService service = new MutableDeviceService();
                getInstance().add(service);
                new ServiceHandler(service, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ServiceHandler extends DeviceDescriptorHandler<MutableDeviceService> {

        public static final ELEMENT EL = ELEMENT.service;

        public ServiceHandler(MutableDeviceService instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case serviceType:
                    getInstance().serviceType = ServiceType.valueOf(getCharacters());
                    break;
                case serviceId:
                    getInstance().serviceId = ServiceId.valueOf(getCharacters());
                    break;
                case SCPDURL:
                    getInstance().descriptorURI = URI.create(getCharacters());
                    break;
                case controlURL:
                    getInstance().controlURI = URI.create(getCharacters());
                    break;
                case eventSubURL:
                    getInstance().eventSubscriptionURI = (URI.create(getCharacters()));
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceListHandler extends DeviceDescriptorHandler<List<MutableDeviceDescriptor>> {

        public static final ELEMENT EL = ELEMENT.deviceList;

        public DeviceListHandler(List<MutableDeviceDescriptor> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(DeviceHandler.EL)) {
                MutableDeviceDescriptor device = new MutableDeviceDescriptor();
                getInstance().add(device);
                new DeviceHandler(device, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceDescriptorHandler<I> extends SAXParser.Handler<I> {

        public DeviceDescriptorHandler(I instance) {
            super(instance);
        }

        public DeviceDescriptorHandler(I instance, SAXParser parser) {
            super(instance, parser);
        }

        public DeviceDescriptorHandler(I instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        public DeviceDescriptorHandler(I instance, SAXParser parser, DeviceDescriptorHandler parent) {
            super(instance, parser, parent);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null) return;
            startElement(el, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null) return;
            endElement(el);
        }

        @Override
        protected boolean isLastElement(String uri, String localName, String qName) {
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            return el != null && isLastElement(el);
        }

        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

        }

        public void endElement(ELEMENT element) throws SAXException {

        }

        public boolean isLastElement(ELEMENT element) {
            return false;
        }
    }
}
