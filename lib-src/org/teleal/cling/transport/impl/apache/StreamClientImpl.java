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

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.model.message.UpnpHeaders;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.UpnpRequest;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.transport.Router;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.StreamClient;
import org.teleal.cling.transport.spi.UnsupportedDataException;

import java.io.IOException;
import java.util.logging.Logger;

public class StreamClientImpl implements StreamClient<StreamClientConfigurationImpl> {

    final private static Logger log = Logger.getLogger(StreamClientImpl.class.getName());

    protected Router router;
    protected StreamClientConfigurationImpl configuration;

    protected ThreadSafeClientConnManager clientConnectionManager;
    protected DefaultHttpClient httpClient;
    protected HttpParams globalParams = new BasicHttpParams();

    public StreamClientImpl(StreamClientConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public StreamClientConfigurationImpl getConfiguration() {
        return configuration;
    }

    public void init(Router router) throws InitializationException {

        ConnManagerParams.setMaxTotalConnections(globalParams, getConfiguration().getMaxTotalConnections());
        HttpConnectionParams.setConnectionTimeout(globalParams, getConfiguration().getConnectionTimeoutSeconds() * 1000);
        HttpConnectionParams.setSoTimeout(globalParams, getConfiguration().getDataReadTimeoutSeconds() * 1000);
        HttpProtocolParams.setContentCharset(globalParams, getConfiguration().getContentCharset());

        // This is a pretty stupid API... https://issues.apache.org/jira/browse/HTTPCLIENT-805
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80)); // The 80 here is... useless
        clientConnectionManager = new ThreadSafeClientConnManager(globalParams, registry);
        httpClient = new DefaultHttpClient(clientConnectionManager, globalParams);

        /*
        // TODO: Ugh! And it turns out that by default it doesn't even use persistent connections properly!
        @Override
        protected ConnectionReuseStrategy createConnectionReuseStrategy() {
            return new NoConnectionReuseStrategy();
        }

        @Override
        protected ConnectionKeepAliveStrategy createConnectionKeepAliveStrategy() {
            return new ConnectionKeepAliveStrategy() {
                public long getKeepAliveDuration(HttpResponse httpResponse, HttpContext httpContext) {
                    return 0;
                }
            };
        }
        httpClient.removeRequestInterceptorByClass(RequestConnControl.class);
        */
    }

    public StreamResponseMessage sendRequest(StreamRequestMessage requestMessage) {

        final UpnpRequest requestOperation = requestMessage.getOperation();
        log.fine("Preparing HTTP request message with method '" + requestOperation.getHttpMethodName() + "': " + requestMessage);

        try {

            // Create the right HTTP request
            HttpUriRequest httpRequest = createHttpRequest(requestMessage, requestOperation);

            // Set all the headers on the request
            httpRequest.setParams(getRequestParams(requestMessage));
            HeaderUtil.add(httpRequest, requestMessage.getHeaders().toHttpHeaders());

            log.fine("Sending HTTP request: " + httpRequest.getURI());
            ResponseMetadata httpResponseMetadata = new ResponseMetadata();
            Object httpResponseBody = httpClient.execute(httpRequest, createResponseHandler(httpResponseMetadata));

            // Decide what the response is supposed to be
            return createResponseMessage(httpResponseMetadata, httpResponseBody);

        } catch (ClientProtocolException ex) {
            log.info("HTTP protocol exception executing request: " + ex.toString());
            return null;
        } catch (IOException ex) {
            log.info("Client connection was aborted: " + ex.getMessage()); // Don't log stacktrace
            return null;
        }
    }

    protected HttpUriRequest createHttpRequest(UpnpMessage upnpMessage, UpnpRequest upnpRequestOperation) throws IOException {

        if (upnpRequestOperation.getMethod().equals(UpnpRequest.Method.GET)) {

            return new HttpGet(upnpRequestOperation.getURI());

        } else if (upnpRequestOperation.getMethod().equals(UpnpRequest.Method.POST)) {

            // TODO: M-POST and M-SEARCH?

            HttpEntityEnclosingRequest postRequest = new HttpPost(upnpRequestOperation.getURI());

            if (upnpMessage.getBodyType().equals(UpnpMessage.BodyType.BYTES)) {
                log.fine("Preparing HTTP request entity as byte[]");
                postRequest.setEntity(new ByteArrayEntity(upnpMessage.getBodyBytes()));
            } else {
                log.fine("Preparing HTTP request entity as string");
                postRequest.setEntity(new StringEntity(upnpMessage.getBodyString()));
            }

            return (HttpUriRequest) postRequest;

        } else {
            throw new IOException("Don't know how to create HTTP message of method '" + upnpRequestOperation.getHttpMethodName() + "': " + upnpMessage);
        }
    }

    protected StreamResponseMessage createResponseMessage(ResponseMetadata httpResponseMetadata, Object httpResponseBody) throws ClientProtocolException {

        // Status
        UpnpResponse responseOperation =
                new UpnpResponse(httpResponseMetadata.getStatusCode(), httpResponseMetadata.getStatusMessage());

        // Message and body
        StreamResponseMessage responseMessage;

        if (httpResponseBody != null) {
            if (httpResponseMetadata.getHeaders().containsTextContentType()) {
                responseMessage = new StreamResponseMessage(responseOperation, (String) httpResponseBody);
            } else {
                responseMessage = new StreamResponseMessage(responseOperation, (byte[]) httpResponseBody);
            }
        } else {
            responseMessage = new StreamResponseMessage(responseOperation);
        }

        // Headers
        responseMessage.setHeaders(httpResponseMetadata.getHeaders());

        return responseMessage;
    }

    protected ResponseHandler createResponseHandler(final ResponseMetadata metadata) {
        return new ResponseHandler() {
            public Object handleResponse(final HttpResponse httpResponse) throws IOException {

                StatusLine statusLine = httpResponse.getStatusLine();
                log.fine("Received HTTP response: " + statusLine);

                metadata.setStatusCode(statusLine.getStatusCode());
                metadata.setStatusMessage(statusLine.getReasonPhrase());

                try {
                    // Set response headers
                    metadata.setHeaders(
                            new UpnpHeaders(HeaderUtil.get(httpResponse))
                    );

                    // Try to figure out what body we are dealing with
                    HttpEntity entity = httpResponse.getEntity();
                    if (entity == null) return null;

                    if (metadata.getHeaders().containsTextContentType()) {
                        return EntityUtils.toString(entity);
                    } else {
                        return EntityUtils.toByteArray(entity);
                    }

                } catch (Exception ex) {
                    throw new UnsupportedDataException("Generic response processing exception, re-throwing: " + ex.getMessage(), ex);
                }

            }
        };
    }

    protected class ResponseMetadata {
        private int statusCode;
        private String statusMessage;
        private UpnpHeaders headers;

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public void setStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
        }

        public UpnpHeaders getHeaders() {
            return headers;
        }

        public void setHeaders(UpnpHeaders headers) {
            this.headers = headers;
        }
    }

    protected HttpParams getRequestParams(StreamRequestMessage requestMessage) {
        HttpParams localParams = new BasicHttpParams();

        localParams.setParameter(
                CoreProtocolPNames.PROTOCOL_VERSION,
                requestMessage.getOperation().getHttpMinorVersion() == 0 ? HttpVersion.HTTP_1_0 : HttpVersion.HTTP_1_1
        );

        // Let's just add the user-agent header on every request, the UDA 1.0 spec doesn't care and the UDA 1.1 spec says OK
        // TODO: Document this in StreamClient interface requirements
        StringBuilder userAgent = new StringBuilder();
        userAgent.append(getConfiguration().getUserAgentOS()).append(" ");
        userAgent.append("UPnP/").append(requestMessage.getUdaMajorVersion()).append(".").append(requestMessage.getUdaMinorVersion()).append(" ");
        userAgent.append(getConfiguration().getUserAgentProduct());
        HttpProtocolParams.setUserAgent(localParams, userAgent.toString());

        // DefaultHttpClient adds HOST header automatically in its default processor

        return new DefaultedHttpParams(localParams, globalParams);
    }


}
