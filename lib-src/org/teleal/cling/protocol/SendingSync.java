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

import org.teleal.cling.model.message.StreamRequestMessage;
import org.teleal.cling.model.message.StreamResponseMessage;
import org.teleal.cling.UpnpService;


public abstract class SendingSync<IN extends StreamRequestMessage, OUT extends StreamResponseMessage> extends SendingAsync {

    protected OUT outputMessage;
    private IN inputMessage;

    protected SendingSync(UpnpService upnpService, IN inputMessage) {
        super(upnpService);
        this.inputMessage = inputMessage;
    }

    protected SendingSync(UpnpService upnpService) {
        super(upnpService);
    }

    public IN getInputMessage() {
        return inputMessage;
    }

    protected void setInputMessage(IN inputMessage) {
        this.inputMessage = inputMessage;
    }

    public OUT getOutputMessage() {
        return outputMessage;
    }

    final protected void execute() {
        outputMessage = executeSync();
    }

    protected abstract OUT executeSync();

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }

}