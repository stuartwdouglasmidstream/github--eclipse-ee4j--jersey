/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.util.concurrent.Executors;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Injection E2E tests.
 *
 * @author Marek Potociar
 */
public class InjectionTest extends JerseyTest {

    @Path("injection")
    public static class InjectionTestResource {

        @DELETE
        @Path("delete-path-param/{id}")
        public String deletePathParam(String body, @PathParam("id") String id) {
            return "deleted: " + id + "-" + body;
        }

        @DELETE
        @Path("delete-path-param-async/{id}")
        public void deletePathParam(String body, @PathParam("id") String id, @Suspended AsyncResponse ar) {
            ar.resume("deleted: " + id + "-" + body);
        }

        @GET
        @Path("async")
        public void asyncGet(@Context final UriInfo uriInfo,
                             @Context final Request request,
                             @Context final HttpHeaders headers,
                             @Context final SecurityContext securityContext,
                             @Suspended final AsyncResponse response) {

            // now suspend and resume later on with
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        response.resume(String.format("base uri: %s\nheaders: %s\nmethod: %s\nprincipal: %s",
                                uriInfo.getBaseUriBuilder().build(),
                                headers.getRequestHeaders(),
                                request.getMethod(),
                                securityContext.getUserPrincipal()));
                    } catch (Throwable e) {
                        response.resume(e);
                    }
                }
            });
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(InjectionTestResource.class);
    }

    /**
     * JERSEY-1711 reproducer.
     *
     * The test is ignored as it currently fails on the following:
     * - HttpURLConnection throws a java.net.ProtocolException when trying to send request data with HTTP DELETE
     * - Grizzly container ignores any DELETE request data and does not pass them to Jersey
     *
     * We would need to by-pass these issues in underlying layer to un-ignore the test.
     */
    @Test
    @Disabled
    public void testInjectionIntoDeleteMethod() {
        Response response;

        response = target("injection").path("delete-path-param/test").request()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true)
                .method("DELETE", Entity.text("body"));
        assertNotNull(response, "Response is null.");
        assertEquals(200, response.getStatus(), "Unexpected response status.");
        assertEquals("deleted: test-body", response.readEntity(String.class), "Unexpected response entity.");

        response = target("injection").path("delete-path-param-async/test").request()
                .property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true)
                .method("DELETE", Entity.text("body"));
        assertNotNull(response, "Response is null.");
        assertEquals(200, response.getStatus(), "Unexpected response status.");
        assertEquals("deleted: test-body", response.readEntity(String.class), "Unexpected response entity.");
    }

    /**
     * JERSEY-1761 reproducer.
     *
     * This is to make sure no proxy gets injected into async method parameters.
     */
    @Test
    public void testAsyncMethodParamInjection() {

        Response response = target("injection").path("async").request().get();
        assertEquals(200, response.getStatus(), "Unexpected response status.");
        assertNotNull(response, "Response is null.");
    }
}
