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

package org.teleal.cling.model.meta;




import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;


public class DeviceDetails implements Validatable {

    final private URL baseURL;
    final private String friendlyName;
    final private ManufacturerDetails manufacturerDetails;
    final private ModelDetails modelDetails;
    final private String serialNumber;
    final private String upc;
    final private URI presentationURI;

    public DeviceDetails(String friendlyName) {
        this(null, friendlyName, null, null, null, null, null);
    }

    public DeviceDetails(String friendlyName, ManufacturerDetails manufacturerDetails) {
        this(null, friendlyName, manufacturerDetails, null, null, null, null);
    }

    public DeviceDetails(String friendlyName, ManufacturerDetails manufacturerDetails, ModelDetails modelDetails) {
        this(null, friendlyName, manufacturerDetails, modelDetails, null, null, null);
    }

    public DeviceDetails(String friendlyName, ManufacturerDetails manufacturerDetails, ModelDetails modelDetails,
                         String serialNumber, String upc) {
        this(null, friendlyName, manufacturerDetails, modelDetails, serialNumber, upc, null);
    }

    public DeviceDetails(String friendlyName, URI presentationURI) {
        this(null, friendlyName, null, null, null, null, presentationURI);
    }

    public DeviceDetails(String friendlyName, ManufacturerDetails manufacturerDetails, ModelDetails modelDetails, URI presentationURI) {
        this(null, friendlyName, manufacturerDetails, modelDetails, null, null, presentationURI);
    }

    public DeviceDetails(String friendlyName, ManufacturerDetails manufacturerDetails, ModelDetails modelDetails,
                         String serialNumber, String upc, URI presentationURI) {
        this(null, friendlyName, manufacturerDetails, modelDetails, serialNumber, upc, presentationURI);
    }

    public DeviceDetails(String friendlyName, ManufacturerDetails manufacturerDetails, ModelDetails modelDetails,
                         String serialNumber, String upc, String presentationURI)
            throws IllegalArgumentException {
        this(null, friendlyName, manufacturerDetails, modelDetails, serialNumber, upc, URI.create(presentationURI));
    }

    public DeviceDetails(URL baseURL, String friendlyName,
                         ManufacturerDetails manufacturerDetails, ModelDetails modelDetails,
                         String serialNumber, String upc,
                         URI presentationURI) {
        this.baseURL = baseURL;
        this.friendlyName = friendlyName;
        this.manufacturerDetails = manufacturerDetails == null ? new ManufacturerDetails() : manufacturerDetails;
        this.modelDetails = modelDetails == null ? new ModelDetails() : modelDetails;
        this.serialNumber = serialNumber;
        this.upc = upc;
        this.presentationURI = presentationURI;
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public ManufacturerDetails getManufacturerDetails() {
        return manufacturerDetails;
    }

    public ModelDetails getModelDetails() {
        return modelDetails;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getUpc() {
        return upc;
    }

    public URI getPresentationURI() {
        return presentationURI;
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        /* TODO: UPNP VIOLATION: Netgear 834DG DSL upnpService doesn't care about UPC format and just sends its model number...
        if (getUpc() != null) {
            if (getUpc().length() < 12) {
                errors.add(new ValidationError(
                        getClass(),
                        "upc",
                        "UPC must be 12 digits"
                ));
            }
            try {
                Long.parseLong(getUpc());
            } catch (NumberFormatException ex) {
                errors.add(new ValidationError(
                        getClass(),
                        "upc",
                        "UPC must be 12 digits all-numeric, parse exception: " + ex.getMessage()
                ));
            }
        }
        */

        return errors;
    }
}
