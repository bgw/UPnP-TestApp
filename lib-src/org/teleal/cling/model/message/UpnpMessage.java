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

public abstract class UpnpMessage<O extends UpnpOperation> {

    public static enum BodyType {
        STRING, BYTES
    }

    private int udaMajorVersion = 1;
    private int udaMinorVersion = 0;

    private O operation;
    private UpnpHeaders headers = new UpnpHeaders();
    private Object body;
    private BodyType bodyType;

    protected UpnpMessage(UpnpMessage<O> source) {
        this.operation = source.operation;
        this.headers = source.headers;
        this.body = source.body;
        this.bodyType = source.bodyType;
        this.udaMajorVersion = source.udaMajorVersion;
        this.udaMinorVersion = source.udaMinorVersion;
    }

    protected UpnpMessage(O operation) {
        this.operation = operation;
    }

    protected UpnpMessage(O operation, BodyType bodyType, Object body) {
        this.operation = operation;
        this.bodyType = bodyType;
        this.body = body;
    }

    public int getUdaMajorVersion() {
        return udaMajorVersion;
    }

    public void setUdaMajorVersion(int udaMajorVersion) {
        this.udaMajorVersion = udaMajorVersion;
    }

    public int getUdaMinorVersion() {
        return udaMinorVersion;
    }

    public void setUdaMinorVersion(int udaMinorVersion) {
        this.udaMinorVersion = udaMinorVersion;
    }

    public UpnpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(UpnpHeaders headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(BodyType bodyType, Object body) {
        this.bodyType = bodyType;
        this.body = body;
    }

    public boolean hasBody() {
        return getBody() != null;
    }

    public BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public String getBodyString() {
        return bodyType.equals(BodyType.STRING) ? body.toString() : new String((byte[]) body);
    }

    public byte[] getBodyBytes() {
        return bodyType.equals(BodyType.STRING) ? ((String)body).getBytes() : (byte[]) body;
    }

    public O getOperation() {
        return operation;
    }

    public void setOperation(O operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ", " + getOperation().toString() + ")";
    }
}