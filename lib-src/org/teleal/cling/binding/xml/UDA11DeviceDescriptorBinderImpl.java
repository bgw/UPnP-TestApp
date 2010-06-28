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

import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.logging.Logger;

/**
 * TODO: Not used
 */
public class UDA11DeviceDescriptorBinderImpl implements DeviceDescriptorBinder {

    private static Logger log = Logger.getLogger(UDA11DeviceDescriptorBinderImpl.class.getName());

    // The usual clutter of nonsense for XML stuff
    public static final String NAMESPACE_URI = "urn:schemas-upnp-org:device-1-0";
    public static final String SCHEMA_RESOURCE = "org/teleal/upnp/descriptor/schema/uda10-device.xsd";
    public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    protected final Schema schema;

    public UDA11DeviceDescriptorBinderImpl() {
        schema = createSchema();
    }

    protected Schema createSchema() {
        try {
            InputStream schemaInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(SCHEMA_RESOURCE);
            return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(schemaInputStream));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T extends org.teleal.cling.model.meta.Device> T describe(T undescribedDevice, String descriptorXml) throws DescriptorBindingException {
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            //factory.setValidating(true);
            //factory.setAttribute(JAXP_SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
            //factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", NAMESPACE_URI);
            factory.setSchema(schema);

            DocumentBuilder parser = factory.newDocumentBuilder();
            parser.setEntityResolver(new ClasspathEntityResolver());
            parser.setErrorHandler(new DescriptorErrorHandler());

            Document d = parser.parse(new InputSource(new StringReader(descriptorXml)));
            System.out.println("#### PARSE RESULT: " + d.getDocumentElement().getNodeName() );




            /*
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);

            SAXParser saxParser = factory.newSAXParser();
            saxParser.setProperty(JAXP_SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);

            XMLReader reader = saxParser.getXMLReader();

            reader.setEntityResolver(new ClasspathEntityResolver());
            reader.setContentHandler(new DescriptorContentHandler());
            reader.setErrorHandler(new DescriptorErrorHandler());

            reader.parse(new InputSource(new StringReader(descriptorXml)));
            */

        } catch (Exception ex) {
            throw new DescriptorBindingException("Could not parse device descriptor: " + ex.getMessage(), ex);
        }
        return null;
    }

    public String generate(org.teleal.cling.model.meta.Device device) throws DescriptorBindingException {
        return null;
    }

    public static class DescriptorContentHandler implements ContentHandler {

        public void setDocumentLocator(Locator locator) {

        }

        public void startDocument() throws SAXException {
            System.out.println("### START DOCUMENT");

        }

        public void endDocument() throws SAXException {

        }

        public void startPrefixMapping(String s, String s1) throws SAXException {
        }

        public void endPrefixMapping(String s) throws SAXException {

        }

        public void endElement(String s, String s1, String s2) throws SAXException {

        }

        public void characters(char[] chars, int i, int i1) throws SAXException {

        }

        public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException {

        }

        public void processingInstruction(String s, String s1) throws SAXException {

        }

        public void skippedEntity(String s) throws SAXException {

        }

        public void startElement(String uri, String localname, String qname, Attributes attributes) throws SAXException {
            System.out.println("###: URI:       " + uri);
            System.out.println("###: localname: " + localname);
            System.out.println("###: qname:     " + qname);
        }

    }

    public static class DescriptorErrorHandler implements ErrorHandler {
        public void warning(SAXParseException e) throws SAXException {
            System.out.println("#### WARN: " + e.getMessage());
            throw e;
        }

        public void error(SAXParseException e) throws SAXException {
            System.out.println("#### ERROR: " + e.getMessage());
            throw e;

        }

        public void fatalError(SAXParseException e) throws SAXException {
            System.out.println("#### FATAL: " + e.getMessage());
            throw e;
        }
    }


    public static class ClasspathEntityResolver implements EntityResolver {

        private static final String NAMESPACE = "http://openremote.org/schemas/";
        private static final String SCHEMA_PACKAGE = "org/openremote/schemas/";
        private static final String CUSTOM_NAMESPACE = "classpath://";

        public ClasspathEntityResolver() {
        }

        public InputSource resolveEntity(String publicId, String systemId) {
            System.out.println("#########################################################################################################################");
            if (systemId != null) {
                System.out.println("trying to resolve system-id [" + systemId + "], checking for namespace: " + NAMESPACE);
                if (systemId.startsWith(NAMESPACE)) {
                    String path = SCHEMA_PACKAGE + systemId.substring(NAMESPACE.length());
                    System.out.println("attempting to find resource in classpath: " + path);

                    InputStream schemaInputStream = resolveInORNamespace(path);
                    if (schemaInputStream == null) {
                        System.out.println("unable to locate URI '" + systemId + "' on classpath: " + path);
                    } else {
                        System.out.println("located URI '" + systemId + "' as a resource on classpath: " + path);
                        InputSource source = new InputSource(schemaInputStream);
                        source.setPublicId(publicId);
                        source.setSystemId(systemId);
                        return source;
                    }
                } else if (systemId.startsWith(CUSTOM_NAMESPACE)) {
                    System.out.println("recognized custom namespace; attempting to resolve on classpath");
                    String path = systemId.substring(CUSTOM_NAMESPACE.length());

                    InputStream stream = resolveInLocalNamespace(path);
                    if (stream == null) {
                        System.out.println("unable to locate URI '" + systemId + "' on classpath");
                    } else {
                        System.out.println("located '" + systemId + "' on classpath");
                        InputSource source = new InputSource(stream);
                        source.setPublicId(publicId);
                        source.setSystemId(systemId);
                        return source;
                    }
                } else {
                    System.out.println("system-id does not start with " + NAMESPACE + " or " + CUSTOM_NAMESPACE + ", not resolving");
                }
            }
            // use default behavior
            return null;
        }

        protected InputStream resolveInORNamespace(String path) {
            return this.getClass().getClassLoader().getResourceAsStream(path);
        }

        protected InputStream resolveInLocalNamespace(String path) {
            try {
                return getClass().getClassLoader().getResourceAsStream(path);
            } catch (Throwable t) {
                return null;
            }
        }
    }

}