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

import java.util.logging.Logger;
import java.util.logging.Level;

import org.teleal.cling.model.message.IncomingDatagramMessage;
import org.teleal.cling.model.message.OutgoingDatagramMessage;
import org.teleal.cling.model.message.UpnpHeaders;
import org.teleal.cling.model.message.UpnpOperation;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.transport.spi.DatagramProcessor;
import org.teleal.cling.transport.spi.UnsupportedDataException;
import org.teleal.common.http.Headers;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class DatagramProcessorImpl implements DatagramProcessor {

    private static Logger log = Logger.getLogger(DatagramProcessor.class.getName());

    public IncomingDatagramMessage read(InetAddress receivedOnAddress, DatagramPacket datagram) throws UnsupportedDataException {

        try {

            if (log.isLoggable(Level.FINER)) {
                log.finer("===================================== DATAGRAM BEGIN ============================================");
                log.finer(new String(datagram.getData()));
                log.finer("-===================================== DATAGRAM END =============================================");
            }

            ByteArrayInputStream is = new ByteArrayInputStream(datagram.getData());

            // Start line
            // TODO: This throws out of memory exception if there is only one line!
            String[] startLine = Headers.readLine(is).split(" ");
            if (!startLine[0].startsWith("HTTP/1.")) {

                return readRequestMessage(receivedOnAddress, datagram, is, startLine[0], startLine[2]);

            } else {
                return readResponseMessage(receivedOnAddress, datagram, is, Integer.valueOf(startLine[1]), startLine[2], startLine[0]);
            }

        } catch (Exception ex) {
            throw new UnsupportedDataException("Could not parse headers: " + ex, ex);
        }
    }

    public DatagramPacket write(OutgoingDatagramMessage message) throws UnsupportedDataException {

        StringBuilder statusLine = new StringBuilder();

        UpnpOperation operation = message.getOperation();

        if (operation instanceof UpnpRequest) {

            UpnpRequest requestOperation = (UpnpRequest) operation;
            statusLine.append(requestOperation.getHttpMethodName()).append(" * ");
            statusLine.append("HTTP/1.").append(operation.getHttpMinorVersion()).append("\r\n");

        } else if (operation instanceof UpnpResponse) {
            UpnpResponse responseOperation = (UpnpResponse) operation;
            statusLine.append("HTTP/1.").append(operation.getHttpMinorVersion()).append(" ");
            statusLine.append(responseOperation.getStatusCode()).append(" ").append(responseOperation.getStatusMessage());
            statusLine.append("\r\n");
        } else {
            throw new UnsupportedDataException(
                    "Message operation is not request or response, don't know how to process: " + message
            );
        }

        // UDA 1.0, 1.1.2: No body but message must have a blank line after header
        StringBuilder messageData = new StringBuilder();
        messageData.append(statusLine);

        messageData.append(message.getHeaders().toString()).append("\r\n");

        if (log.isLoggable(Level.FINER)) {
            log.finer("Writing message data for: " + message);
            log.finer("---------------------------------------------------------------------------------");
            log.finer(messageData.toString().substring(0, messageData.length() - 2)); // Don't print the blank lines
            log.finer("---------------------------------------------------------------------------------");
        }

        try {
            // According to HTTP 1.0 RFC, headers and their values are US-ASCII
            // TODO: Probably should look into escaping rules, too
            byte[] data = messageData.toString().getBytes("US-ASCII");

            log.fine("Writing new datagram packet with " + data.length + " bytes for: " + message);
            return new DatagramPacket(data, data.length, message.getDestinationAddress(), message.getDestinationPort());

        } catch (UnsupportedEncodingException ex) {
            throw new UnsupportedDataException("Can't convert message content to US-ASCII: " + ex.getMessage(), ex);
        }
    }

    protected IncomingDatagramMessage readRequestMessage(InetAddress receivedOnAddress,
                                                         DatagramPacket datagram,
                                                         ByteArrayInputStream is,
                                                         String requestMethod,
                                                         String httpProtocol) throws Exception {

        // Headers
        Headers headers = new Headers(is);

        // Assemble message
        IncomingDatagramMessage requestMessage;
        UpnpRequest upnpRequest = new UpnpRequest(UpnpRequest.Method.getByHttpMethodName(requestMethod));
        upnpRequest.setHttpMinorVersion(httpProtocol.toUpperCase().equals("HTTP/1.1") ? 1 : 0);
        requestMessage = new IncomingDatagramMessage(upnpRequest, datagram.getAddress(), datagram.getPort(), receivedOnAddress);

        requestMessage.setHeaders(new UpnpHeaders(headers));

        return requestMessage;
    }

    protected IncomingDatagramMessage readResponseMessage(InetAddress receivedOnAddress,
                                                          DatagramPacket datagram,
                                                          ByteArrayInputStream is,
                                                          int statusCode,
                                                          String statusMessage,
                                                          String httpProtocol) throws Exception {

        // Headers
        Headers headers = new Headers(is);

        // Assemble the message
        IncomingDatagramMessage responseMessage;
        UpnpResponse upnpResponse = new UpnpResponse(statusCode, statusMessage);
        upnpResponse.setHttpMinorVersion(httpProtocol.toUpperCase().equals("HTTP/1.1") ? 1 : 0);
        responseMessage = new IncomingDatagramMessage(upnpResponse, datagram.getAddress(), datagram.getPort(), receivedOnAddress);

        responseMessage.setHeaders(new UpnpHeaders(headers));

        return responseMessage;
    }


}
