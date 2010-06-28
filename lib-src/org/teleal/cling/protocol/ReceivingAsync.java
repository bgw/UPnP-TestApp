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

package org.teleal.cling.protocol;

import org.teleal.cling.UpnpService;
import org.teleal.cling.model.message.UpnpMessage;
import org.teleal.cling.model.message.header.UpnpHeader;


public abstract class ReceivingAsync<M extends UpnpMessage> implements Runnable {

    private final UpnpService upnpService;

    private M inputMessage;

    protected ReceivingAsync(UpnpService upnpService, M inputMessage) {
        this.upnpService = upnpService;
        this.inputMessage = inputMessage;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public M getInputMessage() {
        return inputMessage;
    }

    public void run() {
        boolean proceed;
        try {
            proceed = waitBeforeExecution();
        } catch (InterruptedException ex) {
            proceed = false;
        }

        if (proceed) {
            execute();
        }
    }

    protected boolean waitBeforeExecution() throws InterruptedException {
        // Don't wait by default
        return true;
    }

    protected abstract void execute();

    protected <H extends UpnpHeader> H getFirstHeader(UpnpHeader.Type headerType, Class<H> subtype) {
        return getInputMessage().getHeaders().getFirstHeader(headerType, subtype);
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }

}
