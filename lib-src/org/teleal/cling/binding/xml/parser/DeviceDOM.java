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

package org.teleal.cling.binding.xml.parser;

import org.teleal.common.xml.DOM;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;

/**
 * @author Christian Bauer
 */
public class DeviceDOM extends DOM {

    public static final String NAMESPACE_URI = "urn:schemas-upnp-org:device-1-0";

    public DeviceDOM(Document dom) {
        super(dom);
    }

    @Override
    public String getRootElementNamespace() {
        return NAMESPACE_URI;
    }

    @Override
    public DeviceElement getRoot(XPath xPath) {
        return new DeviceElement(xPath, getW3CDocument().getDocumentElement());
    }

    @Override
    public DeviceDOM copy() {
        return new DeviceDOM((Document) getW3CDocument().cloneNode(true));
    }

    public DeviceElement createRoot(XPath xpath, ELEMENT el) {
        super.createRoot(el.name());
        return getRoot(xpath);
    }
}
