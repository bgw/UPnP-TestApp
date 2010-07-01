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

package org.teleal.cling.model.message.header;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class UpnpHeader<T> {

    final private static Logger log = Logger.getLogger(UpnpHeader.class.getName());

    public static enum Type {

        USN("USN",
                USNRootDeviceHeader.class,
                DeviceUSNHeader.class,
                ServiceUSNHeader.class,
                UDNHeader.class
        ),
        NT("NT",
                RootDeviceHeader.class,
                UDADeviceTypeHeader.class,
                UDAServiceTypeHeader.class,
                DeviceTypeHeader.class,
                ServiceTypeHeader.class,
                UDNHeader.class,
                NTEventHeader.class
        ),
        NTS("NTS", NTSHeader.class),
        HOST("HOST", HostHeader.class),
        SERVER("SERVER", ServerHeader.class),
        LOCATION("LOCATION", LocationHeader.class),
        MAX_AGE("CACHE-CONTROL", MaxAgeHeader.class),
        USER_AGENT("USER-AGENT", UserAgentHeader.class),
        CONTENT_TYPE("CONTENT-TYPE", ContentTypeHeader.class),
        MAN("MAN", MANHeader.class),
        MX("MX", MXHeader.class),
        ST("ST",
                STAllHeader.class,
                RootDeviceHeader.class,
                UDADeviceTypeHeader.class,
                UDAServiceTypeHeader.class,
                DeviceTypeHeader.class,
                ServiceTypeHeader.class,
                UDNHeader.class
        ),
        EXT("EXT", EXTHeader.class),
        SOAPACTION("SOAPACTION", SoapActionHeader.class),
        TIMEOUT("TIMEOUT", TimeoutHeader.class),
        CALLBACK("CALLBACK", CallbackHeader.class),
        SID("SID", SubscriptionIdHeader.class),
        SEQ("SEQ", EventSequenceHeader.class),

        EXT_IFACE_MAC("X-CLING-IFACE-MAC", InterfaceMacHeader.class);

        private String httpName;
        private Class<? extends UpnpHeader>[] headerClasses;

        private Type(String httpName, Class<? extends UpnpHeader>... headerClass) {
            this.httpName = httpName;
            this.headerClasses = headerClass;
        }

        public String getHttpName() {
            return httpName;
        }

        public Class<? extends UpnpHeader>[] getHeaderClasses() {
            return headerClasses;
        }

        public boolean isValidHeaderClass(Class<? extends UpnpHeader> clazz) {
            for (Class<? extends UpnpHeader> permissibleClass : getHeaderClasses()) {
                if (permissibleClass.isAssignableFrom(clazz)) {
                    return true;
                }
            }
            return false;
        }

        public static Type getByHttpName(String httpName) {
            Type type = null;
            for (Type t : Type.values()) {
                if (t.getHttpName().equals(httpName)) {
                    type = t;
                }
            }
            return type;
        }
    }

    private T value;

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public abstract void setString(String s) throws InvalidHeaderException;

    public abstract String getString();

    public static UpnpHeader newInstance(UpnpHeader.Type type, String headerValue) {

        // Try all the UPnP headers and see if one matches our value parsers
        UpnpHeader upnpHeader = null;
        for (int i = 0; i < type.getHeaderClasses().length && upnpHeader == null; i++) {
            Class<? extends UpnpHeader> headerClass = type.getHeaderClasses()[i];
            try {
                log.finer("Trying to parse '" + type + "' with class: " + headerClass.getSimpleName());
                upnpHeader = headerClass.newInstance();
                if (headerValue != null) {
                    upnpHeader.setString(headerValue);
                }
            } catch (InvalidHeaderException ex) {
                log.finer("Invalid header value for tested type: " + headerClass.getSimpleName() + " - " + ex.getMessage());
                upnpHeader = null;
            } catch (Exception ex) {
                log.severe("Error instantiating header of type '" + type + "' with value: " + headerValue);
                log.log(Level.SEVERE, "Cause: " + ex.toString(), ex);
            }

        }
        return upnpHeader;
    }


    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") '" + getValue() + "'";
    }
}
