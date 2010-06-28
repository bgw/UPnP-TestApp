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

import org.teleal.cling.transport.Router;

import java.net.InetAddress;

/**
 * Listens to TCP connections and handles synchronous HTTP requests with UPnP message payloads.
 *
 * <p>
 * Implementations are supposed to call <tt>Router#received(UpnpStream)</tt> with a custom
 * <tt>UpnpStream</tt>. This will start processing of the request and <tt>run()</tt> the
 * <tt>UpnpStream</tt>.
 * </p>
 *
 * <p>
 * The custom <tt>UpnpStream</tt> implementation then passes a <tt>UpnpMessage</tt> to its
 * <tt>process(requestMsg)</tt> method and then sends the returned response back to the client.
 * </p>
 *
 * <p>
 * In pseudo-code:
 * </p>
 *
 * <pre>
 *
 * MyStreamServer implements StreamServer {
 *      run() {
 *          while (not stopped) {
 *              Connection con = listenToSocketAndBlock();
 *              router.received( new MyUpnpStream(con) );
 *
 *          }
 *      }
 * }
 *
 * MyUpnpStream(con) extends UpnpStream {
 *      run() {
 *          UpnpMessage request = readRequest(con);
 *          UpnpMessage response = process(request);
 *          sendResponse(response);
 *      }
 * }
 *
 * </pre>
 *
 *
 */
public interface StreamServer<C extends StreamServerConfiguration> extends Runnable {

    public void init(InetAddress bindAddress, Router router) throws InitializationException;
    public void stop();
    public C getConfiguration();

}
