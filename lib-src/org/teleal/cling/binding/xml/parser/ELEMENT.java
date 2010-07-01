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

import org.w3c.dom.Node;

/**
 * @author Christian Bauer
 */
public enum ELEMENT {
    
    root,
    specVersion,
    major,
    minor,
    URLBase,
    device,
    UDN,
    deviceType,
    friendlyName,
    manufacturer,
    manufacturerURL,
    modelDescription,
    modelName,
    modelNumber,
    modelURL,
    presentationURL,
    UPC,
    serialNumber,
    iconList, icon, width, height, depth, url, mimetype,
    serviceList, service, serviceType, serviceId, SCPDURL, controlURL, eventSubURL,
    deviceList,

    scpd,
    actionList, action, name,
    argumentList, argument, direction, relatedStateVariable, retval,
    serviceStateTable, stateVariable, dataType, defaultValue,
    allowedValueList, allowedValue, allowedValueRange, minimum, maximum, step;

    public static ELEMENT valueOrNullOf(String s) {
        try {
            return valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean equals(Node node) {
        return toString().equals(node.getNodeName());
    }
}