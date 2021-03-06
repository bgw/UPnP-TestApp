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

package org.teleal.cling.model.message;

import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.model.message.header.ContentTypeHeader;

import java.net.URI;
import java.net.URL;


public class StreamRequestMessage extends UpnpMessage<UpnpRequest> {

    public StreamRequestMessage(StreamRequestMessage source) {
        super(source);
    }

    public StreamRequestMessage(UpnpRequest operation) {
        super(operation);
    }

    public StreamRequestMessage(UpnpRequest.Method method, URI uri) {
        super(new UpnpRequest(method, uri));
    }

    public StreamRequestMessage(UpnpRequest.Method method, URL url) {
        super(new UpnpRequest(method, url));
    }


    public StreamRequestMessage(UpnpRequest operation, String body) {
        super(operation, BodyType.STRING, body);
    }

    public StreamRequestMessage(UpnpRequest.Method method, URI uri, String body) {
        super(new UpnpRequest(method, uri), BodyType.STRING, body);
    }

    public StreamRequestMessage(UpnpRequest.Method method, URL url, String body) {
        super(new UpnpRequest(method, url), BodyType.STRING, body);
    }


    public StreamRequestMessage(UpnpRequest operation, byte[] body) {
        super(operation, BodyType.BYTES, body);
    }

    public StreamRequestMessage(UpnpRequest.Method method, URI uri, byte[] body) {
        super(new UpnpRequest(method, uri), BodyType.BYTES, body);
    }

    public StreamRequestMessage(UpnpRequest.Method method, URL url, byte[] body) {
        super(new UpnpRequest(method, url), BodyType.BYTES, body);
    }


    public boolean hasUDATextContentType() {
        ContentTypeHeader contentTypeHeader =
                getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class);
        return contentTypeHeader != null && contentTypeHeader.isUDACompliant();
    }

    public boolean hasHostHeader() {
        return getHeaders().getFirstHeader(UpnpHeader.Type.HOST) != null;
    }

    public URI getUri() {
        return getOperation().getURI();
    }
    
}