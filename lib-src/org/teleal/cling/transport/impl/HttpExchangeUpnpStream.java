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

import com.sun.net.httpserver.HttpExchange;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpHeaders;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.transport.spi.UpnpStream;
import org.teleal.cling.protocol.ProtocolFactory;
import org.teleal.common.http.Headers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Logger;


public class HttpExchangeUpnpStream extends UpnpStream {

    private static Logger log = Logger.getLogger(UpnpStream.class.getName());

    private HttpExchange httpExchange;

    public HttpExchangeUpnpStream(ProtocolFactory protocolFactory, HttpExchange httpExchange) {
        super(protocolFactory);
        this.httpExchange = httpExchange;
    }

    public HttpExchange getHttpExchange() {
        return httpExchange;
    }

    public void execute() {

        try {
            log.fine("Processing HTTP request: " + getHttpExchange().getRequestMethod() + " " + getHttpExchange().getRequestURI());

            // Status
            StreamRequestMessage requestMessage =
                    new StreamRequestMessage(
                            UpnpRequest.Method.getByHttpMethodName(getHttpExchange().getRequestMethod()),
                            getHttpExchange().getRequestURI()
                    );

            if (requestMessage.getOperation().getMethod() == null) {
                log.fine("Method not supported by UPnP stack: " + getHttpExchange().getRequestMethod());
                throw new RuntimeException("Method not supported: " + getHttpExchange().getRequestMethod());
            }

            // Protocol
            requestMessage.getOperation().setHttpMinorVersion(
                    getHttpExchange().getProtocol().toUpperCase().equals("HTTP/1.1") ? 1 : 0
            );

            log.fine("Created new request message: " + requestMessage);

            // Headers
            Headers httpHeaders = new Headers(getHttpExchange().getRequestHeaders());
            requestMessage.setHeaders(new UpnpHeaders(httpHeaders));

            // Body
            boolean containsTextBody = requestMessage.getHeaders().containsTextContentType();

            byte[] requestBodyBytes = StreamUtil.readBytes(getHttpExchange().getRequestBody());

            log.fine("Read request body bytes: " + requestBodyBytes.length);

            if (requestBodyBytes.length > 0  && containsTextBody) {

                log.fine("Request contains textual entity body, converting then setting string on message");
                requestMessage.setBody(UpnpMessage.BodyType.STRING, new String(requestBodyBytes));

            } else if (requestBodyBytes.length > 0) {

                log.fine("Request contains binary entity body, setting bytes on message");
                requestMessage.setBody(UpnpMessage.BodyType.BYTES, requestBodyBytes);

            } else {
                log.fine("Request did not contain entity body");
            }

            // Process it
            StreamResponseMessage responseMessage = process(requestMessage);

            // Return the response
            if (responseMessage != null) {
                log.fine("Preparing HTTP response message: " + responseMessage);

                // Headers
                getHttpExchange().getResponseHeaders().putAll(
                        responseMessage.getHeaders().toHttpHeaders()
                );

                // Body
                byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;
                int contentLength = responseBodyBytes != null ? responseBodyBytes.length : -1;

                log.fine("Sending HTTP response message: " + responseMessage + " with content length: " + contentLength);
                getHttpExchange().sendResponseHeaders(responseMessage.getOperation().getStatusCode(), contentLength);

                if (contentLength > 0) {
                    log.fine("Response message has body, writing bytes to stream...");
                    StreamUtil.writeBytes(getHttpExchange().getResponseBody(), responseBodyBytes);
                }

            } else {
                // If it's null, it's 404, everything else needs a proper httpResponse
                // TODO: Document this behavior in interface of StreamServer
                log.fine("Sending HTTP response status: " + HttpURLConnection.HTTP_NOT_FOUND);
                getHttpExchange().sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            }

            responseSent(responseMessage);

        } catch (Throwable t) {

            // You definitely want to catch all Exceptions here, otherwise the server will
            // simply close the socket and you get an "unexpected end of file" on the client.
            // The same is true if you just rethrow an IOException - it is a mystery why it
            // is declared then on the HttpHandler interface if it isn't handled in any
            // way... so we always do error handling here.

            // You don't have to catch Throwable unless, like we do here in unit tests,
            // you might run into Errors as well (assertions).
            log.fine("Exception occured during UPnP stream processing: " + t);
            try {
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            } catch (IOException ex) {
                log.warning("Couldn't send error response: " + ex);
            }

            responseException(t);
        }
    }

}
