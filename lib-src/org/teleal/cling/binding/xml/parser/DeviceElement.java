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

import org.teleal.common.xml.DOMElement;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

/**
 * @author Christian Bauer
 */
public class DeviceElement extends DescriptorElement<DeviceElement, DeviceElement> {

    public DeviceElement(XPath xpath, Element element) {
        super(xpath, element);
    }

    @Override
    protected Builder<DeviceElement> createParentBuilder(DOMElement el) {
        return new Builder<DeviceElement>(el) {
            @Override
            public DeviceElement build(Element element) {
                return new DeviceElement(getXpath(), element);
            }
        };
    }

    @Override
    protected ArrayBuilder<DeviceElement> createChildBuilder(DOMElement el) {
        return new ArrayBuilder<DeviceElement>(el) {
            @Override
            public DeviceElement[] newChildrenArray(int length) {
                return new DeviceElement[length];
            }

            @Override
            public DeviceElement build(Element element) {
                return new DeviceElement(getXpath(), element);
            }
        };
    }

    @Override
    protected String getNamespaceURI() {
        return DeviceDOM.NAMESPACE_URI;
    }
}
