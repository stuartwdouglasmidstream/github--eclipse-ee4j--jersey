/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests ignoring of client responses in exceptions.
 *
 * @author Santiago Pericas-Geertsen
 */
public class IgnoreExceptionResponseTest extends JerseyTest {

    static String lastAllowSystemProperties;
    static String lastIgnoreExceptionResponse;
    static AtomicReference<URI> baseUri = new AtomicReference<>();

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }

    public IgnoreExceptionResponseTest() {
        baseUri.set(getBaseUri());
    }

    /**
     * Sets ignore exception response as system property after enabling the provider.
     */
    @BeforeAll
    public static void startUp() {
        lastAllowSystemProperties = System.setProperty(CommonProperties.ALLOW_SYSTEM_PROPERTIES_PROVIDER, "true");
        lastIgnoreExceptionResponse = System.setProperty(ClientProperties.IGNORE_EXCEPTION_RESPONSE, "true");
    }

    /**
     * Restores state after completion.
     */
    @AfterAll
    public static void cleanUp() {
        if (lastIgnoreExceptionResponse != null) {
            System.setProperty(ClientProperties.IGNORE_EXCEPTION_RESPONSE, lastIgnoreExceptionResponse);
        }
        if (lastAllowSystemProperties != null) {
            System.setProperty(CommonProperties.ALLOW_SYSTEM_PROPERTIES_PROVIDER, lastAllowSystemProperties);
        }
    }

    @Test
    public void test() {
        Client client = ClientBuilder.newClient();
        Response r = client.target(getBaseUri())
                .path("test")
                .path("first")
                .request()
                .get();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
        assertNull(r.getHeaderString("confidential"));
        assertNull(r.getCookies().get("confidential"));
        assertFalse(r.hasEntity());
    }

    @Path("test")
    public static class TestResource {

        @Path("first")
        @GET
        public String first() {
            Client client = ClientBuilder.newClient();
            String entity = client.target(baseUri.get())
                    .path("test")
                    .path("second")
                    .request()
                    .get(String.class);     // WebApplicationException may be thrown
            return processEntity(entity);
        }

        @Path("second")
        @GET
        public String second() {
            throw new WebApplicationException(
                    "Leaking confidential information",
                    Response.status(500)
                            .header("confidential", "nuke-codes")
                            .cookie(NewCookie.valueOf("confidential=more-nuke-codes"))
                            .entity("even-more-nuke-codes")
                            .build());
        }

        private String processEntity(String entity) {
            return entity;          // filter confidential information
        }
    }
}
