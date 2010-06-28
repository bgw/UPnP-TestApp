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

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;


public class UpnpRequest extends UpnpOperation {

    public static enum Method {

        GET("GET"),
        POST("POST"),
        MPOST("M-POST"),
        NOTIFY("NOTIFY"),
        MSEARCH("M-SEARCH"),
        SUBSCRIBE("SUBSCRIBE"),
        UNSUBSCRIBE("UNSUBSCRIBE");

        private String httpMethodName;

        Method(String httpMethodName) {
            this.httpMethodName = httpMethodName;
        }

        public String getHttpMethodName() {
            return httpMethodName;
        }

        public static Method getByHttpMethodName(String httpMethodName) {
            for (Method method: values()) {
                if (method.getHttpMethodName().equals(httpMethodName))
                    return method;
            }
            return null;
        }
    }

    private Method method;
    private URI uri;

    public UpnpRequest(Method method) {
        this.method = method;
    }

    public UpnpRequest(Method method, URI uri) {
        this.method = method;
        this.uri = uri;
    }

    public UpnpRequest(Method method, URL url) {
        this.method = method;
        try {
            if (url != null) {
                this.uri = url.toURI();
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getHttpMethodName() {
        return method.getHttpMethodName();
    }

    public URI getURI() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return getHttpMethodName() + (getURI() != null ? " " + getURI() : "");
    }
}
