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

package org.teleal.cling.transport.impl.apache;

import org.apache.http.ConnectionClosedException;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpHeaders;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.UpnpOperation;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.header.ContentTypeHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.protocol.ProtocolFactory;
import org.teleal.cling.transport.spi.UnsupportedDataException;
import org.teleal.cling.transport.spi.UpnpStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.logging.Logger;

public class HttpServerConnectionUpnpStream extends UpnpStream {

    final private static Logger log = Logger.getLogger(HttpServerConnectionUpnpStream.class.getName());

    protected final HttpServerConnection connection;
    protected final BasicHttpProcessor httpProcessor = new BasicHttpProcessor();
    protected final HttpService httpService;
    protected final HttpParams params;

    protected HttpServerConnectionUpnpStream(ProtocolFactory protocolFactory,
                                       HttpServerConnection connection,
                                       final HttpParams params) {
        super(protocolFactory);
        this.connection = connection;
        this.params = params;

        // The Date header is recommended in UDA, need to document the requirement in StreamServer interface
        httpProcessor.addInterceptor(new ResponseDate());

        // The Server header is only required for Control so callers have to add it to UPnPMessage
        // httpProcessor.addInterceptor(new ResponseServer());

        httpProcessor.addInterceptor(new ResponseContent());
        httpProcessor.addInterceptor(new ResponseConnControl());

        httpService =
                new UpnpHttpService(
                        httpProcessor,
                        new DefaultConnectionReuseStrategy(),
                        new DefaultHttpResponseFactory()
                );
        httpService.setParams(params);
    }

    public HttpServerConnection getConnection() {
        return connection;
    }

    @Override
    public void execute() {


        try {
            while (!Thread.interrupted() && connection.isOpen()) {
                log.fine("Handling request on open connection...");
                HttpContext context = new BasicHttpContext(null);
                httpService.handleRequest(connection, context);
            }
        } catch (ConnectionClosedException ex) {
            log.fine("Client closed connection");
        } catch (SocketTimeoutException ex) {
            log.fine("Server-side closed socket (this is 'normal' behavior of Apache HTTP Core!): " + ex.getMessage());
        } catch (IOException ex) {
            log.warning("I/O exception during HTTP request processing: " + ex.getMessage());
        } catch (HttpException ex) {
            throw new UnsupportedDataException("Request malformed: " + ex.getMessage(), ex);
        } finally {
            try {
                connection.shutdown();
            } catch (IOException ex) {
                log.fine("Error closing connection: " + ex.getMessage());
            }
        }
    }

    /**
     * A thread-safe custom service implementation that creates a UPnP message from the request,
     * then passes it to <tt>UpnpStream#process()</tt>, finally sends the response back to the
     * client.
     */
    protected class UpnpHttpService extends HttpService {

        public UpnpHttpService(HttpProcessor processor, ConnectionReuseStrategy reuse, HttpResponseFactory responseFactory) {
            super(processor, reuse, responseFactory);
        }

        @Override
        protected void doService(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext ctx)
                throws HttpException, IOException {

            log.fine("Processing HTTP request: " + httpRequest.getRequestLine().toString());

            // Extract what we need from the HTTP httpRequest
            String requestMethod = httpRequest.getRequestLine().getMethod();
            String requestURI = httpRequest.getRequestLine().getUri();

            StreamRequestMessage requestMessage =
                    new StreamRequestMessage(
                            UpnpRequest.Method.getByHttpMethodName(requestMethod),
                            URI.create(requestURI)
                    );

            if (requestMessage.getOperation().getMethod() == null) {
                log.fine("Method not supported by UPnP stack: " + requestMethod);
                throw new MethodNotSupportedException("Method not supported: " + requestMethod);
            }

            log.fine("Created new request message: " + requestMessage);

            // HTTP version
            int requestHttpMinorVersion = httpRequest.getProtocolVersion().getMinor();
            requestMessage.getOperation().setHttpMinorVersion(requestHttpMinorVersion);

            // Headers
            requestMessage.setHeaders(
                    new UpnpHeaders(
                            HeaderUtil.get(httpRequest)
                    )
            );

            // Body
            if (httpRequest instanceof HttpEntityEnclosingRequest) {
                log.fine("Request contains entity body, setting on UPnP message");
                HttpEntityEnclosingRequest entityEnclosingHttpRequest = (HttpEntityEnclosingRequest) httpRequest;

                ContentTypeHeader contentTypeHeader = requestMessage.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class);

                if (contentTypeHeader != null && contentTypeHeader.isText()) {
                    log.fine("HTTP request content type is text");
                    try {
                        requestMessage.setBody(UpnpMessage.BodyType.STRING, readBuffered(entityEnclosingHttpRequest.getEntity().getContent()));
                    } catch (IOException ex) {
                        log.warning("Couldn't read HTTP request entity: " + ex.getMessage());
                    }
                } else {
                    // TODO: Handle binary? UPnP has no binary entity body in any request!
                    throw new HttpException("Request with unknown entity body received: " + contentTypeHeader);
                }
            } else {
                log.fine("Request did not contain entity body");
            }

            // Finally process it
            StreamResponseMessage responseMsg = process(requestMessage);

            if (responseMsg != null) {
                log.fine("Sending HTTP response message: " + responseMsg);

                // Status line
                httpResponse.setStatusLine(
                        new BasicStatusLine(
                                new ProtocolVersion("HTTP", 1, responseMsg.getOperation().getHttpMinorVersion()),
                                responseMsg.getOperation().getStatusCode(),
                                responseMsg.getOperation().getStatusMessage()
                        )
                );

                log.fine("Response status line: " + httpResponse.getStatusLine());

                // Headers
                httpResponse.setParams(getResponseParams(requestMessage.getOperation()));
                HeaderUtil.add(httpResponse, responseMsg.getHeaders().toHttpHeaders());

                if (responseMsg.hasBody() && responseMsg.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
                    httpResponse.setEntity(new ByteArrayEntity(responseMsg.getBodyBytes()));
                } else {
                    StringEntity responseEntity = new StringEntity(responseMsg.getBodyString(), "UTF-8");
                    httpResponse.setEntity(responseEntity);
                }

            } else {
                // If it's null, it's 404, everything else needs a proper httpResponse
                // TODO: Document this behavior in interface of StreamServer
                log.fine("Sending HTTP response: " + HttpStatus.SC_NOT_FOUND);
                httpResponse.setStatusCode(HttpStatus.SC_NOT_FOUND);
            }
        }

        // Will return null if there is a problem with the input stream!
        protected String readBuffered(InputStream inputStream) throws IOException {
            assert inputStream != null;

            BufferedReader inputReader = null;
            try {
                inputReader = new BufferedReader(
                        new InputStreamReader(inputStream)
                );

                StringBuilder input = new StringBuilder();
                String inputLine;
                while ((inputLine = inputReader.readLine()) != null) {
                    input.append(inputLine).append("\n");
                }
                if (input.length() > 0) {
                    input.deleteCharAt(input.length() - 1);
                }

                return input.length() > 0 ? input.toString() : null;
            } finally {
                if (inputReader != null) {
                    try {
                        inputReader.close();
                    } catch (IOException ex) {
                        // Ignore this, it's thrown for example if the stream is already closed!
                    }
                }
            }
        }

        protected HttpParams getResponseParams(UpnpOperation operation) {
            HttpParams localParams = new BasicHttpParams();
            return new DefaultedHttpParams(localParams, params);
        }

    }

}