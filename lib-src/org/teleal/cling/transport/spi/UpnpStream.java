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

package org.teleal.cling.transport.spi;

import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.protocol.ProtocolFactory;
import org.teleal.cling.protocol.ReceivingSync;

import java.util.logging.Logger;


public abstract class UpnpStream implements Runnable {

    private static Logger log = Logger.getLogger(UpnpStream.class.getName());

    protected final ProtocolFactory protocolFactory;
    protected ReceivingSync syncProtocol;

    protected UpnpStream(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    public void run() {
        // TODO: Lock!
        execute();
    }

    public StreamResponseMessage process(StreamRequestMessage requestMsg) throws UnsupportedDataException {
        log.fine("Processing stream request message: " + requestMsg);

        // Try to get a protocol implementation that matches the request message
        syncProtocol = getProtocolFactory().createReceivingSync(requestMsg);

        // Run it
        log.fine("Running protocol for synchronous message processing: " + syncProtocol);
        syncProtocol.run();

        // ... then grab the response
        StreamResponseMessage responseMsg = syncProtocol.getOutputMessage();

        if (responseMsg == null) {
            // That's ok, the caller is supposed to handle this properly (e.g. convert it to HTTP 404)
            log.finer("Protocol did not return any response message");
            return null;
        }
        log.finer("Protocol returned response: " + responseMsg);
        return responseMsg;
    }

    public abstract void execute();

    public void responseSent(StreamResponseMessage responseMessage) {
        syncProtocol.responseSent(responseMessage);
    }

    public void responseException(Throwable t) {
        syncProtocol.responseException(t);
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}
