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
import org.teleal.common.xml.ParserException;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

/**
 * @author Christian Bauer
 */
public abstract class DescriptorElement<CHILD extends DescriptorElement, PARENT extends DescriptorElement>
        extends DOMElement<CHILD, PARENT> {

    public static final String XPATH_PREFIX = "d";

    public DescriptorElement(XPath xpath, Element element) {
        super(xpath, element);
    }

    @Override
    protected String prefix(String localName) {
        return XPATH_PREFIX + ":" + localName;
    }

    @Override
    public DescriptorElement setContent(String content) {
        super.setContent(content);
        return this;
    }

    public DescriptorElement getRequiredChild(ELEMENT el) throws ParserException {
        return super.getRequiredChild(el.name());
    }

    public boolean equals(ELEMENT element) {
        return element.name().equals(getElementName());
    }

    public CHILD createChild(ELEMENT el) {
        return super.createChild(el.name(), getNamespaceURI());
    }

    public void createChildIfNotNull(ELEMENT el, Object content) {
        if (content != null) {
            super.createChild(el.name(), getNamespaceURI()).setContent(content.toString());
        }
    }

    protected abstract String getNamespaceURI();
}
