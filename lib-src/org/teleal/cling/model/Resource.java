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

package org.teleal.cling.model;

import java.net.URI;
import java.net.URISyntaxException;


public class Resource<M> {

    public enum Type {
        DEVICE_DESCRIPTOR, SERVICE_DESCRIPTOR, ICON, CONTROL, EVENT_SUBSCRIPTION, EVENT_CALLBACK
    }

    private Type type;
    private URI localURI;
    private M model;

    public Resource(URI localURI, M model) {
        this.localURI = localURI;
        this.model = model;
    }

    public Resource(Type type, URI localURI, M model) {
        this.type = type;
        this.localURI = localURI;
        this.model = model;
    }

    public Type getType() {
        return type;
    }

    public URI getLocalURI() {
        return localURI;
    }

    public M getModel() {
        return model;
    }

    public URI getLocalPathQuery() {
        try {
            return new URI(null, null, getLocalURI().getPath(), getLocalURI().getQuery(), null);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean matchesPathQuery(URI pathQuery) {
        return pathQuery.equals(getLocalPathQuery());
    }

    @Override
    public String toString() {
        return getLocalURI() + " (" + getType() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource resource = (Resource) o;

        if (!localURI.equals(resource.localURI)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return localURI.hashCode();
    }
}
