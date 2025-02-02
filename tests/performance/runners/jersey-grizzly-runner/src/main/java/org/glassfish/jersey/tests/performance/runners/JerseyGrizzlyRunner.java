/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.tests.performance.runners;

import java.net.URI;

import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * Application class to start performance test web service at http://localhost:8080/ if the base URI
 * is not passed via the second command line argument.
 */
public class JerseyGrizzlyRunner {

    private static final URI BASE_URI = URI.create("http://localhost:8080/");

    private static final int DEFAULT_SELECTORS = 4;
    private static final int DEFAULT_WORKERS = 8;

    public static void main(String[] args) throws Exception {
        System.out.println("Jersey performance test web service application");

        final String jaxRsApp = args.length > 0 ? args[0] : null;
        //noinspection unchecked
        final ResourceConfig resourceConfig = ResourceConfig
                .forApplicationClass((Class<? extends Application>) Class.forName(jaxRsApp));
        URI baseUri = args.length > 1 ? URI.create(args[1]) : BASE_URI;
        int selectors = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_SELECTORS;
        int workers = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_WORKERS;
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);
        final TCPNIOTransport transport = server.getListener("grizzly").getTransport();
        transport.setSelectorRunnersCount(selectors);
        transport.setWorkerThreadPoolConfig(ThreadPoolConfig.defaultConfig().setCorePoolSize(workers).setMaxPoolSize(workers));

        server.start();

        System.out.println(String.format("Application started.\nTry out %s\nHit Ctrl-C to stop it...",
                baseUri));

        while (server.isStarted()) {
            Thread.sleep(600000);
        }
    }
}
