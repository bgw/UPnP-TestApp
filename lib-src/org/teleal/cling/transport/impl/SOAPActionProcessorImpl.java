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
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionException;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.control.ActionRequestMessage;
import org.teleal.cling.model.message.control.ActionResponseMessage;
import org.teleal.cling.model.types.ErrorCode;
import org.teleal.cling.transport.spi.SOAPActionProcessor;
import org.teleal.cling.transport.spi.UnsupportedDataException;
import org.w3c.dom.Attr;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SOAPActionProcessorImpl implements SOAPActionProcessor {

    private static Logger log = Logger.getLogger(SOAPActionProcessor.class.getName());

    public void writeBody(ActionRequestMessage requestMessage, ActionInvocation actionInvocation) throws UnsupportedDataException {

        log.fine("Writing body of " + requestMessage + " for: " + actionInvocation);

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document d = factory.newDocumentBuilder().newDocument();
            Element body = writeBodyElement(d);

            writeBodyRequest(d, body, requestMessage, actionInvocation);

            if (log.isLoggable(Level.FINER)) {
                log.finer("===================================== SOAP BODY BEGIN ============================================");
                log.finer(requestMessage.getBody().toString());
                log.finer("-===================================== SOAP BODY END ============================================");
            }

        } catch (Exception ex) {
            throw new UnsupportedDataException("Can't transform message payload: " + ex, ex);
        }

    }

    public void writeBody(ActionResponseMessage responseMessage, ActionInvocation actionInvocation) throws UnsupportedDataException {

        log.fine("Writing body of " + responseMessage + " for: " + actionInvocation);

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document d = factory.newDocumentBuilder().newDocument();
            Element body = writeBodyElement(d);

            if (actionInvocation.getFailure() != null) {
                writeBodyFailure(d, body, responseMessage, actionInvocation);
            } else {
                writeBodyResponse(d, body, responseMessage, actionInvocation);
            }

            if (log.isLoggable(Level.FINER)) {
                log.finer("===================================== SOAP BODY BEGIN ============================================");
                log.finer(responseMessage.getBody().toString());
                log.finer("-===================================== SOAP BODY END ============================================");
            }

        } catch (Exception ex) {
            throw new UnsupportedDataException("Can't transform message payload: " + ex, ex);
        }
    }

    public void readBody(ActionRequestMessage requestMessage, ActionInvocation actionInvocation) throws UnsupportedDataException {

        log.fine("Reading body of " + requestMessage + " for: " + actionInvocation);
        if (log.isLoggable(Level.FINER)) {
            log.finer("===================================== SOAP BODY BEGIN ============================================");
            log.finer(requestMessage.getBody().toString());
            log.finer("-===================================== SOAP BODY END ============================================");
        }

        if (requestMessage.getBody() == null || !requestMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
            throw new UnsupportedDataException("Can't transform null or non-string body of: " + requestMessage);
        }

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document d = factory.newDocumentBuilder().parse(
                    new InputSource(
                            new StringReader(requestMessage.getBodyString())
                    )
            );

            Element bodyElement = readBodyElement(d);

            readBodyRequest(d, bodyElement, requestMessage, actionInvocation);

        } catch (Exception ex) {
            throw new UnsupportedDataException("Can't transform message payload: " + ex, ex);
        }
    }

    public void readBody(ActionResponseMessage responseMsg, ActionInvocation actionInvocation) throws UnsupportedDataException {

        log.fine("Reading body of " + responseMsg + " for: " + actionInvocation);
        if (log.isLoggable(Level.FINER)) {
            log.finer("===================================== SOAP BODY BEGIN ============================================");
            log.finer(responseMsg.getBody().toString());
            log.finer("-===================================== SOAP BODY END ============================================");
        }

        if (responseMsg.getBody() == null || !responseMsg.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
            throw new UnsupportedDataException("Can't transform null or non-string body of: " + responseMsg);
        }

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document d = factory.newDocumentBuilder().parse(
                    new InputSource(
                            new StringReader(responseMsg.getBodyString())
                    )
            );

            Element bodyElement = readBodyElement(d);

            ActionException ex = readBodyFailure(d, bodyElement);

            if (ex == null) {
                readBodyResponse(d, bodyElement, responseMsg, actionInvocation);
            } else {
                actionInvocation.setFailure(ex);
            }

        } catch (Exception ex) {
            throw new UnsupportedDataException("Can't transform message payload: " + ex, ex);
        }
    }

    /* ##################################################################################################### */

    protected void writeBodyFailure(Document d,
                                    Element bodyElement,
                                    ActionResponseMessage message,
                                    ActionInvocation actionInvocation) throws Exception {

        writeFaultElement(d, bodyElement, actionInvocation);
        message.setBody(UpnpMessage.BodyType.STRING, toString(d));
    }

    protected void writeBodyRequest(Document d,
                                    Element bodyElement,
                                    ActionRequestMessage message,
                                    ActionInvocation actionInvocation) throws Exception {

        Element actionRequestElement = writeActionRequestElement(d, bodyElement, message, actionInvocation);
        writeActionInputArguments(d, actionRequestElement, actionInvocation);
        message.setBody(UpnpMessage.BodyType.STRING, toString(d));

    }

    protected void writeBodyResponse(Document d,
                                     Element bodyElement,
                                     ActionResponseMessage message,
                                     ActionInvocation actionInvocation) throws Exception {

        Element actionResponseElement = writeActionResponseElement(d, bodyElement, message, actionInvocation);
        writeActionOutputArguments(d, actionResponseElement, actionInvocation);
        message.setBody(UpnpMessage.BodyType.STRING, toString(d));
    }

    protected ActionException readBodyFailure(Document d, Element bodyElement) throws Exception {
        return readFaultElement(bodyElement);
    }

    protected void readBodyRequest(Document d,
                                   Element bodyElement,
                                   ActionRequestMessage message,
                                   ActionInvocation actionInvocation) throws Exception {

        Element actionRequestElement = readActionRequestElement(bodyElement, message, actionInvocation);
        readActionInputArguments(actionRequestElement, actionInvocation);
    }

    protected void readBodyResponse(Document d,
                                    Element bodyElement,
                                    ActionResponseMessage message,
                                    ActionInvocation actionInvocation) throws Exception {

        Element actionResponse = readActionResponseElement(bodyElement, actionInvocation);
        readActionOutputArguments(actionResponse, actionInvocation);
    }

    /* ##################################################################################################### */

    protected Element writeBodyElement(Document d) {

        Element envelopeElement = d.createElementNS(Constants.SOAP_NS_ENVELOPE, "s:Envelope");
        Attr encodingStyleAttr = d.createAttributeNS(Constants.SOAP_NS_ENVELOPE, "s:encodingStyle");
        encodingStyleAttr.setTextContent(Constants.SOAP_URI_ENCODING_STYLE);
        envelopeElement.setAttributeNode(encodingStyleAttr);
        d.appendChild(envelopeElement);

        Element bodyElement = d.createElementNS(Constants.SOAP_NS_ENVELOPE, "s:Body");
        envelopeElement.appendChild(bodyElement);

        return bodyElement;
    }

    protected Element readBodyElement(Document d) {

        Element envelopeElement = d.getDocumentElement();
        if (envelopeElement == null || !getUnprefixedNodeName(envelopeElement).equals("Envelope")) {
            throw new RuntimeException("Response root element was not 'Envelope'");
        }

        NodeList envelopeElementChildren = envelopeElement.getChildNodes();
        for (int i = 0; i < envelopeElementChildren.getLength(); i++) {
            Node envelopeChild = envelopeElementChildren.item(i);

            if (envelopeChild.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (getUnprefixedNodeName(envelopeChild).equals("Body")) {
                return (Element) envelopeChild;
            }
        }

        throw new RuntimeException("Response envelope did not contain 'Body' child element");
    }

    /* ##################################################################################################### */

    protected Element writeActionRequestElement(Document d,
                                                Element bodyElement,
                                                ActionRequestMessage message,
                                                ActionInvocation actionInvocation) {

        log.fine("Writing action request element: " + actionInvocation.getAction().getName());

        Element actionRequestElement = d.createElementNS(
                message.getActionNamespace(),
                "u:" + actionInvocation.getAction().getName()
        );
        bodyElement.appendChild(actionRequestElement);

        return actionRequestElement;
    }

    protected Element readActionRequestElement(Element bodyElement,
                                               ActionRequestMessage message,
                                               ActionInvocation actionInvocation) {
        NodeList bodyChildren = bodyElement.getChildNodes();

        log.fine("Looking for action request element matching namespace:" + message.getActionNamespace());

        for (int i = 0; i < bodyChildren.getLength(); i++) {
            Node bodyChild = bodyChildren.item(i);

            if (bodyChild.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (getUnprefixedNodeName(bodyChild).equals(actionInvocation.getAction().getName()) &&
                    bodyChild.getNamespaceURI().equals(message.getActionNamespace())) {
                log.fine("Reading action request element: " + getUnprefixedNodeName(bodyChild));
                return (Element) bodyChild;
            }
        }
        log.info("Could not read action request element matching namespace: " + message.getActionNamespace());
        return null;
    }

    /* ##################################################################################################### */

    protected Element writeActionResponseElement(Document d,
                                                 Element bodyElement,
                                                 ActionResponseMessage message,
                                                 ActionInvocation actionInvocation) {

        log.fine("Writing action response element: " + actionInvocation.getAction().getName());
        Element actionResponseElement = d.createElementNS(
                message.getActionNamespace(),
                "u:" + actionInvocation.getAction().getName() + "Response"
        );
        bodyElement.appendChild(actionResponseElement);

        return actionResponseElement;
    }

    protected Element readActionResponseElement(Element bodyElement, ActionInvocation actionInvocation) {
        NodeList bodyChildren = bodyElement.getChildNodes();

        for (int i = 0; i < bodyChildren.getLength(); i++) {
            Node bodyChild = bodyChildren.item(i);

            if (bodyChild.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (getUnprefixedNodeName(bodyChild).equals(actionInvocation.getAction().getName() + "Response")) {
                log.fine("Reading action response element: " + getUnprefixedNodeName(bodyChild));
                return (Element) bodyChild;
            }
        }
        log.fine("Could not read action response element");
        return null;
    }

    /* ##################################################################################################### */

    protected void writeActionInputArguments(Document d,
                                             Element actionRequestElement,
                                             ActionInvocation actionInvocation) {

        for (ActionArgumentValue callValue : actionInvocation.getInput().getValues()) {
            log.fine("Writing action input argument: " + callValue.getArgument().getName());
            appendChildElementWithTextContent(d, actionRequestElement, callValue.getArgument().getName(), callValue.toString());
        }
    }

    public void readActionInputArguments(Element actionRequestElement,
                                         ActionInvocation actionInvocation) throws ActionException {
        NodeList actionRequestChildren = actionRequestElement.getChildNodes();

        List<String> inputArgumentNames = actionInvocation.getAction().getInputArgumentNames();

        for (int i = 0; i < actionRequestChildren.getLength(); i++) {
            Node actionRequestChild = actionRequestChildren.item(i);

            if (actionRequestChild.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (inputArgumentNames.contains(getUnprefixedNodeName(actionRequestChild))) {
                log.fine("Reading action input argument: " + getUnprefixedNodeName(actionRequestChild));
                String value = actionRequestChild.getTextContent();
                actionInvocation.getInput().addValue(value);
            }
        }
    }

    /* ##################################################################################################### */

    protected void writeActionOutputArguments(Document d,
                                              Element actionResponseElement,
                                              ActionInvocation actionInvocation) {
        for (ActionArgumentValue callValue : actionInvocation.getOutput().getValues()) {
            log.fine("Writing action outut argument: " + callValue.getArgument().getName());
            appendChildElementWithTextContent(d, actionResponseElement, callValue.getArgument().getName(), callValue.toString());
        }
    }

    protected void readActionOutputArguments(Element actionResponseElement,
                                             ActionInvocation actionInvocation) throws ActionException {
        NodeList actionResponseChildren = actionResponseElement.getChildNodes();

        List<String> outputArgumentNames = actionInvocation.getAction().getOutputArgumentNames();

        for (int i = 0; i < actionResponseChildren.getLength(); i++) {
            Node actionResponseChild = actionResponseChildren.item(i);

            if (actionResponseChild.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (outputArgumentNames.contains(getUnprefixedNodeName(actionResponseChild))) {
                log.fine("Reading action output argument: " + getUnprefixedNodeName(actionResponseChild));
                String value = actionResponseChild.getTextContent();
                actionInvocation.getOutput().addValue(value);
            }
        }
    }

    /* ##################################################################################################### */

    protected void writeFaultElement(Document d, Element bodyElement, ActionInvocation actionInvocation) {

        Element faultElement = d.createElementNS(Constants.SOAP_NS_ENVELOPE, "s:Fault");
        bodyElement.appendChild(faultElement);

        // This stuff is really completely arbitrary nonsense... let's hope they fired the guy who decided this
        appendChildElementWithTextContent(d, faultElement, "faultcode", "s:Client");
        appendChildElementWithTextContent(d, faultElement, "faultstring", "UPnPError");

        Element detailElement = d.createElement("detail");
        faultElement.appendChild(detailElement);

        Element upnpErrorElement = d.createElementNS(Constants.NS_UPNP_CONTROL_10, "UPnPError");
        detailElement.appendChild(upnpErrorElement);

        int errorCode = actionInvocation.getFailure().getErrorCode();
        String errorDescription = actionInvocation.getFailure().getMessage();

        log.fine("Writing fault element: " + errorCode + " - " + errorDescription);

        appendChildElementWithTextContent(d, upnpErrorElement, "errorCode", Integer.toString(errorCode));
        appendChildElementWithTextContent(d, upnpErrorElement, "errorDescription", errorDescription);

    }

    protected ActionException readFaultElement(Element bodyElement) {

        boolean receivedFaultElement = false;
        String errorCode = null;
        String errorDescription = null;

        NodeList bodyChildren = bodyElement.getChildNodes();

        for (int i = 0; i < bodyChildren.getLength(); i++) {
            Node bodyChild = bodyChildren.item(i);

            if (bodyChild.getNodeType() != Node.ELEMENT_NODE)
                continue;

            if (getUnprefixedNodeName(bodyChild).equals("Fault")) {

                receivedFaultElement = true;

                NodeList faultChildren = bodyChild.getChildNodes();

                for (int j = 0; j < faultChildren.getLength(); j++) {
                    Node faultChild = faultChildren.item(j);

                    if (faultChild.getNodeType() != Node.ELEMENT_NODE)
                        continue;

                    if (getUnprefixedNodeName(faultChild).equals("detail")) {

                        NodeList detailChildren = faultChild.getChildNodes();
                        for (int x = 0; x < detailChildren.getLength(); x++) {
                            Node detailChild = detailChildren.item(x);

                            if (detailChild.getNodeType() != Node.ELEMENT_NODE)
                                continue;

                            if (getUnprefixedNodeName(detailChild).equals("UPnPError")) {

                                NodeList errorChildren = detailChild.getChildNodes();
                                for (int y = 0; y < errorChildren.getLength(); y++) {
                                    Node errorChild = errorChildren.item(y);

                                    if (errorChild.getNodeType() != Node.ELEMENT_NODE)
                                        continue;

                                    if (getUnprefixedNodeName(errorChild).equals("errorCode"))
                                        errorCode = errorChild.getTextContent();

                                    if (getUnprefixedNodeName(errorChild).equals("errorDescription"))
                                        errorDescription = errorChild.getTextContent();
                                }
                            }
                        }
                    }
                }
            }
        }

        if (errorCode != null) {
            try {
                int numericCode = Integer.valueOf(errorCode);
                ErrorCode standardErrorCode = ErrorCode.getByCode(numericCode);
                if (standardErrorCode != null) {
                    log.fine("Reading fault element: " + standardErrorCode.getCode() + " - " + errorDescription);
                    return new ActionException(standardErrorCode, errorDescription, false);
                } else {
                    log.fine("Reading fault element: " + numericCode + " - " + errorDescription);
                    return new ActionException(numericCode, errorDescription);
                }
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Error code was not a number");
            }
        } else if (receivedFaultElement) {
            throw new RuntimeException("Received fault element but no error code");
        }
        return null;
    }


    /* ##################################################################################################### */

    protected String toString(Document d) throws Exception {
        // Is that convoluted enough for you, XML dudes? I just want a string!
        // And of course these magic settings for indentation are a big secret...
        TransformerFactory transFactory = TransformerFactory.newInstance();
        transFactory.setAttribute("indent-number", 4);
        Transformer transformer = transFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
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
