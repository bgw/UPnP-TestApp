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

import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpHeaders;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.transport.Router;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.StreamClient;
import org.teleal.common.http.Headers;
import org.teleal.common.util.URIUtil;
import sun.net.www.protocol.http.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.logging.Logger;

/**
 * It's hard to believe how bad the JDK has gotten in the last 15 years.
 */
public class StreamClientImpl implements StreamClient {

    public static final String HACK_STREAM_HANDLER_SYSTEM_PROPERTY = "hackStreamHandlerProperty";

    private static Logger log = Logger.getLogger(StreamClient.class.getName());

    protected StreamClientConfigurationImpl configuration;

    public StreamClientImpl(StreamClientConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public StreamClientConfigurationImpl getConfiguration() {
        return configuration;
    }

    public void init(Router router) throws InitializationException {

        log.fine("Using persistent HTTP stream client connections: " + configuration.isUsePersistentConnections());
        System.setProperty("http.keepAlive", Boolean.toString(configuration.isUsePersistentConnections()));
        if (System.getProperty(HACK_STREAM_HANDLER_SYSTEM_PROPERTY) == null) {
            log.fine("Setting custom static URLStreamHandlerFactory to work around Sun JDK bugs");
            URL.setURLStreamHandlerFactory(
                    new URLStreamHandlerFactory() {
                        public URLStreamHandler createURLStreamHandler(String protocol) {
                            log.fine("Creating new URLStreamHandler for protocol: " + protocol);
                            if ("http".equals(protocol)) {
                                return new Handler() {

                                    protected java.net.URLConnection openConnection(URL u) throws IOException {
                                        return openConnection(u, null);
                                    }

                                    protected java.net.URLConnection openConnection(URL u, Proxy p) throws IOException {
                                        return new UpnpURLConnection(u, this);
                                    }
                                };
                            } else {
                                return null;
                            }
                        }
                    }
            );
            System.setProperty(HACK_STREAM_HANDLER_SYSTEM_PROPERTY, "alreadyWorkedAroundTheEvilJDK");
        }
    }

    public StreamResponseMessage sendRequest(StreamRequestMessage requestMessage) {

        final UpnpRequest requestOperation = requestMessage.getOperation();
        log.fine("Preparing HTTP request message with method '" + requestOperation.getHttpMethodName() + "': " + requestMessage);

        URL url = URIUtil.toURL(requestOperation.getURI());

        HttpURLConnection urlConnection = null;
        try {

            // TODO: It should be ok to rely on the TCP/IP stack to figure out the right interface
            /*
            urlConnection = (HttpURLConnection) url.openConnection(
                    new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(url.getHost(), url.getPort()))
            );
            */

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod(requestOperation.getHttpMethodName());
            urlConnection.setReadTimeout(configuration.getDataReadTimeoutSeconds() * 1000);
            urlConnection.setConnectTimeout(configuration.getConnectionTimeoutSeconds() * 1000);

            applyRequestProperties(urlConnection, requestMessage);
            applyRequestBody(urlConnection, requestMessage);

            log.fine("Sending HTTP request: " + requestMessage);
            return createResponse(urlConnection, urlConnection.getInputStream());

        } catch (ProtocolException ex) {
            log.fine("Unrecoverable HTTP protocol exception: " + ex);
            return null;
        } catch (IOException ex) {

            if (urlConnection == null) {
                log.info("Could not open URL connection: " + ex.getMessage());
                return null;
            }

            log.fine("Exception occured, trying to read the error stream");
            try {
                return createResponse(urlConnection, urlConnection.getErrorStream());
            } catch (Exception errorEx) {
                log.fine("Could not read error stream: " + errorEx);
                return null;
            }
        } catch (Exception ex) {
            log.info("Unrecoverable exception occured, no error response possible: " + ex);
            return null;

        } finally {

            if (urlConnection != null) {
                // Release any idle persistent connection, or "indicate that we don't want to use this server for a while"
                urlConnection.disconnect();
            }
        }
    }

    protected void applyRequestProperties(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) {

        urlConnection.setInstanceFollowRedirects(false); // Defaults to true but not needed here

        // Let's just add the user-agent header on every request, the UDA 1.0 spec doesn't care and the UDA 1.1 spec says OK
        // TODO: Document this in StreamClient interface requirements
        StringBuilder userAgent = new StringBuilder();
        userAgent.append(getConfiguration().getUserAgentOS()).append(" ");
        userAgent.append("UPnP/").append(requestMessage.getUdaMajorVersion()).append(".").append(requestMessage.getUdaMinorVersion()).append(" ");
        userAgent.append(getConfiguration().getUserAgentProduct());
        log.fine("Setting User-Agent header: " + userAgent.toString());
        urlConnection.setRequestProperty("User-Agent", userAgent.toString());

        // TODO: HttpURLConnection always adds an "Accept" header (which is not needed but shouldn't hurt)
        // TODO: HttpURLConnection always adds a "Host" header (which is needed and should be documented)

        // Other headers
        log.fine("Writing headers on HttpURLConnection: " + requestMessage.getHeaders().size());
        requestMessage.getHeaders().applyTo(urlConnection);
    }

    protected void applyRequestBody(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) throws IOException {

        if (requestMessage.hasBody()) {
            urlConnection.setDoOutput(true);
        } else {
            urlConnection.setDoOutput(false);
            return;
        }

        if (requestMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
            StreamUtil.writeUTF8(urlConnection.getOutputStream(), requestMessage.getBodyString());
        } else if (requestMessage.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
            StreamUtil.writeBytes(urlConnection.getOutputStream(), requestMessage.getBodyBytes());
        }
    }

    protected StreamResponseMessage createResponse(HttpURLConnection urlConnection, InputStream inputStream) throws Exception {

        if (urlConnection.getResponseCode() == -1) {
            log.fine("Did not receive valid HTTP response");
            return null;
        }

        // Status
        UpnpResponse responseOperation = new UpnpResponse(urlConnection.getResponseCode(), urlConnection.getResponseMessage());

        log.fine("Received response: " + responseOperation);

        // Message
        StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);

        // Headers
        responseMessage.setHeaders(
                new UpnpHeaders(
                        new Headers(urlConnection.getHeaderFields())
                )
            );

        // Body
        if (inputStream != null && responseMessage.getHeaders().containsTextContentType()) {
            log.fine("Response contained text content, reading stream...");
            responseMessage.setBody(UpnpMessage.BodyType.STRING, StreamUtil.readBufferedString(inputStream));
        } else if (inputStream != null) {
            log.fine("Response contained binary content, reading stream...");
            responseMessage.setBody(UpnpMessage.BodyType.BYTES, StreamUtil.readBytes(inputStream));
        } else {
            log.fine("Response did not contain entity body");
        }

        log.fine("Response message complete: " + responseMessage);
        return responseMessage;
    }


    /**
     * The Sun genuises restrict the JDK handlers to GET/POST/etc.
     * They do not understand HTTP.
     * It's still not fixed in JDK7!
     */
    public static class UpnpURLConnection extends sun.net.www.protocol.http.HttpURLConnection {

        private static final String[] methods = {
                "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE",
                "SUBSCRIBE", "UNSUBSCRIBE", "NOTIFY" // TODO: M-POST and M-SEARCH?
        };

        protected UpnpURLConnection(URL u, Handler handler) throws IOException {
            super(u, handler);
        }

        public UpnpURLConnection(URL u, String host, int port) throws IOException {
            super(u, host, port);
        }

        public synchronized OutputStream getOutputStream() throws IOException {
            OutputStream os;
            String savedMethod = method;
            // see if the method supports output
            if (method.equals("PUT") || method.equals("POST") || method.equals("NOTIFY")) {
                // fake the method so the superclass method sets its instance variables
                method = "PUT";
            } else {
                // use any method that doesn't support output, an exception will be
                // raised by the superclass
                method = "GET";
            }
            os = super.getOutputStream();
            method = savedMethod;
            return os;
        }

        public void setRequestMethod(String method) throws ProtocolException {
            if (connected) {
                throw new ProtocolException("Cannot reset method once connected");
            }
            // prevent clients from specifying invalid methods. This prevents experimenting
            // with new methods without editing this code, but should be included for
            // security reasons.
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].equals(method)) {
                    this.method = method;
                    return;
                }
            }
            throw new ProtocolException("Invalid UPnP HTTP method: " + method);
        }
    }

}


