/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common;

import java.io.IOException;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests whether providers are correctly validated in the client runtime (for example if provider constrained to
 * server runtime is skipped in the client).

 * @author Miroslav Fuksa
 *
 */
public class ConstrainedToTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testClientWithProviderClasses() {
        Client client = ClientBuilder.newClient(new ClientConfig(ClientFilterConstrainedToServer.class,
                ClientFilterConstrainedToClient.class, ClientFilter.class));

        _testFilters(client);
    }

    @Test
    public void testClientWithProviderInstances() {
        Client client = ClientBuilder.newClient(new ClientConfig(new ClientFilterConstrainedToServer(),
                new ClientFilterConstrainedToClient(), new ClientFilter()));

        _testFilters(client);
    }

    private void _testFilters(Client client) {
        final Response response = client.target(getBaseUri()).path("resource").request().get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("called", response.getHeaderString("ClientFilterConstrainedToClient"));
        Assertions.assertEquals("called", response.getHeaderString("ClientFilter"));
        Assertions.assertNull(response.getHeaderString("ClientFilterConstrainedToServer"),
                "The ClientFilterConstrainedToServer should not be called as it is constrained to server.");
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class ClientFilterConstrainedToClient implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("ClientFilterConstrainedToClient", "called");
        }
    }

    public static class ClientFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("ClientFilter", "called");
        }
    }

    /**
     * Wrong configuration of Client filter which is constrained to server. This filter will be never called.
     */
    @ConstrainedTo(RuntimeType.SERVER)
    public static class ClientFilterConstrainedToServer implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("ClientFilterConstrainedToServer", "called");
        }
    }

    @Path("resource")
    public static class Resource {
        @GET
        public String get() {
            return "get";
        }
    }
}
