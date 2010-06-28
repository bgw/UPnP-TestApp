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

package org.teleal.cling.protocol.async;

import org.teleal.cling.model.message.discovery.OutgoingSearchRequest;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.message.header.UpnpHeader;
import org.teleal.cling.UpnpService;
import org.teleal.cling.protocol.SendingAsync;

import java.util.logging.Logger;


public class SendingSearch extends SendingAsync {

    private static Logger log = Logger.getLogger(SendingSearch.class.getName());

    private UpnpHeader searchTarget;

    public SendingSearch(UpnpService upnpService) {
        this(upnpService, new STAllHeader());
    }

    public SendingSearch(UpnpService upnpService, UpnpHeader searchTarget) {
        super(upnpService);

        if (!UpnpHeader.Type.ST.isValidHeaderClass(searchTarget.getClass())) {
            throw new IllegalArgumentException(
                    "Given search target instance is not a valid header class for type ST: " + searchTarget.getClass()
            );
        }
        this.searchTarget = searchTarget;
    }

    public UpnpHeader getSearchTarget() {
        return searchTarget;
    }

    protected void execute() {

        log.fine("Executing search for target: " + searchTarget.getString());

        OutgoingSearchRequest msg = new OutgoingSearchRequest(searchTarget);

        for (int i = 0; i < getBulkRepeat(); i++) {
            try {

                getUpnpService().getRouter().send(msg);

                // UDA 1.0 is silent about this but UDA 1.1 recomments "a few hundred milliseconds"
                log.finer("Sleeping "+ getBulkIntervalMilliseconds()+" milliseconds");
                Thread.sleep(getBulkIntervalMilliseconds());

            } catch (InterruptedException ex) {
                log.warning("Search sending thread was interrupted: " + ex);
            }
        }
    }

    public int getBulkRepeat() {
        return 2; // UDA 1.0 says "repeat more than once", so we do it twice
    }

    public int getBulkIntervalMilliseconds() {
        return 100; // That should be plenty on an ethernet LAN
    }

}
