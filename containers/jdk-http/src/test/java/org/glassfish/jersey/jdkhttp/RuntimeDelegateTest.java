/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdkhttp;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * @author Michal Gajdos
 */
public class RuntimeDelegateTest {

    @Path("/")
    public static class Resource {

        @GET
        public String get() {
            return "get";
        }
    }

    @Test
    public void testFetch() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        final HttpHandler handler = RuntimeDelegate.getInstance().createEndpoint(new Application() {

            @Override
            public Set<Class<?>> getClasses() {
                return Collections.<Class<?>>singleton(Resource.class);
            }
        }, HttpHandler.class);

        try {
            server.createContext("/", handler);
            server.start();

            final Response response = ClientBuilder.newClient()
                    .target(UriBuilder.fromUri("http://localhost/").port(server.getAddress().getPort()).build())
                    .request()
                    .get();

            assertThat(response.readEntity(String.class), is("get"));
        } finally {
            server.stop(0);
        }
    }
}
