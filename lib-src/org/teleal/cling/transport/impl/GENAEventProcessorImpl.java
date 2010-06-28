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

package org.teleal.cling.transport.impl;

import org.teleal.cling.model.Constants;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.state.StateVariableValue;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.gena.IncomingEventRequestMessage;
import org.teleal.cling.model.message.gena.OutgoingEventRequestMessage;
import org.teleal.cling.transport.spi.GENAEventProcessor;
import org.teleal.cling.transport.spi.UnsupportedDataException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GENAEventProcessorImpl implements GENAEventProcessor {

    private static Logger log = Logger.getLogger(GENAEventProcessor.class.getName());


    public void writeBody(OutgoingEventRequestMessage requestMessage) throws UnsupportedDataException {
        log.fine("Writing body of: " + requestMessage);

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document d = factory.newDocumentBuilder().newDocument();
            Element propertysetElement = writePropertysetElement(d);

            writeProperties(d, propertysetElement, requestMessage);

            requestMessage.setBody(UpnpMessage.BodyType.STRING, toString(d));

            if (log.isLoggable(Level.FINER)) {
                log.finer("===================================== GENA BODY BEGIN ============================================");
                log.finer(requestMessage.getBody().toString());
                log.finer("-===================================== GENA BODY END ============================================");
            }

        } catch (Exception ex) {
            throw new UnsupportedDataException("Can't transform message payload: " + ex.getMessage(), ex);
        }
    }

    public void readBody(IncomingEventRequestMessage requestMessage) throws UnsupportedDataException {

        log.fine("Reading body of: " + requestMessage);
        if (log.isLoggable(Level.FINER)) {
            log.finer("===================================== GENA BODY BEGIN ============================================");
            log.finer(requestMessage.getBody().toString());
            log.finer("-===================================== GENA BODY END ============================================");
        }

        if (requestMessage.getBody() == null || !requestMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
            throw new UnsupportedDataException("Can't transform null or non-string body of: " + requestMessage);
        }

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // TODO: UPNP VIOLATION: Netgear 834DG DSL Router sends trailing spaces/newlines after last XML element, need to trim()
            Document d = factory.newDocumentBuilder().parse(
                    new InputSource(
                            new StringReader(requestMessage.getBodyString().trim())
                    )
            );

            Element propertysetElement = readPropertysetElement(d);

            readProperties(propertysetElement, requestMessage);

        } catch (Exception ex) {
            throw new UnsupportedDataException("Can't transform message payload: " + ex.getMessage(), ex);
        }

    }

    /* ##################################################################################################### */

    protected Element writePropertysetElement(Document d) {
        Element propertysetElement = d.createElementNS(Constants.NS_UPNP_EVENT_10, "e:propertyset");
        d.appendChild(propertysetElement);
        return propertysetElement;
    }

    protected Element readPropertysetElement(Document d) {

        Element propertysetElement = d.getDocumentElement();
        if (propertysetElement == null || !getUnprefixedNodeName(propertysetElement).equals("propertyset")) {
            throw new RuntimeException("Root element was not 'propertyset'");
        }
        return propertysetElement;
    }

    /* ##################################################################################################### */

    protected void writeProperties(Document d, Element propertysetElement, OutgoingEventRequestMessage message) {
        for (StateVariableValue stateVariableValue : message.getStateVariableValues()) {
            Element propertyElement = d.createElementNS(Constants.NS_UPNP_EVENT_10, "e:property");
            propertysetElement.appendChild(propertyElement);
            appendChildElementWithTextContent(
                    d,
                    propertyElement,
                    stateVariableValue.getStateVariable().getName(),
                    stateVariableValue.toString()
            );
        }
    }

    protected void readProperties(Element propertysetElement, IncomingEventRequestMessage message) {
        NodeList propertysetElementChildren = propertysetElement.getChildNodes();

        //Map<String, StateVariable> stateVariables = message.getDeviceService().getService().getStateVariables();
        StateVariable[] stateVariables = message.getDeviceService().getService().getStateVariables();

        for (int i = 0; i < propertysetElementChildren.getLength(); i++) {
            Node propertysetChild = propertysetElementChildren.item(i);

            if (propertysetChild.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (getUnprefixedNodeName(propertysetChild).equals("property")) {

                NodeList propertyChildren = propertysetChild.getChildNodes();

                for (int j = 0; j < propertyChildren.getLength(); j++) {
                    Node propertyChild = propertyChildren.item(j);

                    if (propertyChild.getNodeType() != Node.ELEMENT_NODE)
                        continue;

                    String stateVariableName = getUnprefixedNodeName(propertyChild);
                    for (StateVariable stateVariable : stateVariables) {
                        if (stateVariable.getName().equals(stateVariableName)) {
                            log.fine("Reading state variable value: " + stateVariableName);
                            String value = propertyChild.getTextContent();
                            message.getStateVariableValues().add(
                                    new StateVariableValue(stateVariable, value)
                            );
                            break;
                        }
                    }

                }
            }
        }
    }

    /* ##################################################################################################### */

    protected String toString(Document d) throws Exception {
        // Is that convoluted enough for you, XML dudes? I just want a string!
        // And of course these magic settings for indentation are a big secret...
        TransformerFactory transFactory = TransformerFactory.newInstance();
        transFactory.setAttribute("indent-number", 4);
        Transformer transformer = transFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes"); // TODO: this is ignored, no Sun bug reported...
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(d), new StreamResult(out));

        // Just to be safe, no newline at the end
        String output = out.toString();
        while (output.endsWith("\n") || output.endsWith("\r")) {
            output = output.substring(0, output.length() - 1);
        }

        return output;
    }

    protected String getUnprefixedNodeName(Node node) {
        return node.getPrefix() != null
                ? node.getNodeName().substring(node.getPrefix().length() + 1)
                : node.getNodeName();
    }

    protected Element appendChildElementWithTextContent(Document descriptor, Element parent, String childName, String childContent) {
        Element childElement = descriptor.createElement(childName);
        if (childContent != null) {
            childElement.setTextContent(childContent);
        }
        parent.appendChild(childElement);
        return childElement;
    }
}

