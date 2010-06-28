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

package org.teleal.cling.transport.impl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.teleal.cling.transport.Router;
import org.teleal.cling.transport.spi.InitializationException;
import org.teleal.cling.transport.spi.StreamServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Logger;


public class StreamServerImpl implements StreamServer<StreamServerConfigurationImpl> {

    private static Logger log = Logger.getLogger(StreamServer.class.getName());

    protected StreamServerConfigurationImpl configuration;
    protected HttpServer server;

    public StreamServerImpl(StreamServerConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    public void init(InetAddress bindAddress, Router router) throws InitializationException {
        try {
            log.info("Creating socket (for receiving TCP streams) on: " + bindAddress + ":" + configuration.getListenPort());
            InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());

            server = HttpServer.create(socketAddress, configuration.getTcpConnectionBacklog());
            server.createContext("/", new RequestHttpHandler(router));

        } catch (Exception ex) {
            throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
        }
    }

    public StreamServerConfigurationImpl getConfiguration() {
        return configuration;
    }

    public void run() {
        log.fine("Starting StreamServer...");
        server.start();
    }

    public void stop() {
        log.fine("Stopping StreamServer...");
        if (server != null) server.stop(1);
    }

    public static class RequestHttpHandler implements HttpHandler {

        private static Logger log = Logger.getLogger(RequestHttpHandler.class.getName());

        private final Router router;

        public RequestHttpHandler(Router router) {
            this.router = router;
        }

        // This is executed in the request receiving thread!
        public void handle(HttpExchange httpExchange) throws IOException {
            // And we pass control to the service, which will (hopefully) start a new thread immediately so we can return
            // execution to the request receiving thread ASAP
            log.fine("Received HTTP exchange: " + httpExchange.getRequestMethod() + " " + httpExchange.getRequestURI());
            router.received(
                    new HttpExchangeUpnpStream(router.getProtocolFactory(), httpExchange)
            );
        }
    }

}
