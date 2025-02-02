/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jnh.connector;

import jakarta.inject.Singleton;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthTest extends JerseyTest {

        private static final Logger LOGGER = Logger.getLogger(AuthTest.class.getName());
        private static final String PATH = "test";

        @Path("/test")
        @Singleton
        public static class AuthResource {

            int requestCount = 0;

            @GET
            public String get(@Context HttpHeaders h) {
                requestCount++;
                String value = h.getRequestHeaders().getFirst("Authorization");
                if (value == null) {
                    assertEquals(1, requestCount);
                    throw new WebApplicationException(
                            Response.status(401).header("WWW-Authenticate", "Basic realm=\"WallyWorld\"").build());
                } else {
                    assertTrue(requestCount > 1);
                }

                return "GET";
            }

            @GET
            @Path("filter")
            public String getFilter(@Context HttpHeaders h) {
                String value = h.getRequestHeaders().getFirst("Authorization");
                if (value == null) {
                    throw new WebApplicationException(
                            Response.status(401).header("WWW-Authenticate", "Basic realm=\"WallyWorld\"").build());
                }

                return "GET";
            }

            @POST
            public String post(@Context HttpHeaders h, String e) {
                requestCount++;
                String value = h.getRequestHeaders().getFirst("Authorization");
                if (value == null) {
                    assertEquals(1, requestCount);
                    throw new WebApplicationException(
                            Response.status(401).header("WWW-Authenticate", "Basic realm=\"WallyWorld\"").build());
                } else {
                    assertTrue(requestCount > 1);
                }

                return e;
            }

            @POST
            @Path("filter")
            public String postFilter(@Context HttpHeaders h, String e) {
                String value = h.getRequestHeaders().getFirst("Authorization");
                if (value == null) {
                    throw new WebApplicationException(
                            Response.status(401).header("WWW-Authenticate", "Basic realm=\"WallyWorld\"").build());
                }

                return e;
            }

            @DELETE
            public void delete(@Context HttpHeaders h) {
                requestCount++;
                String value = h.getRequestHeaders().getFirst("Authorization");
                if (value == null) {
                    assertEquals(1, requestCount);
                    throw new WebApplicationException(
                            Response.status(401).header("WWW-Authenticate", "Basic realm=\"WallyWorld\"").build());
                } else {
                    assertTrue(requestCount > 1);
                }
            }

            @DELETE
            @Path("filter")
            public void deleteFilter(@Context HttpHeaders h) {
                String value = h.getRequestHeaders().getFirst("Authorization");
                if (value == null) {
                    throw new WebApplicationException(
                            Response.status(401).header("WWW-Authenticate", "Basic realm=\"WallyWorld\"").build());
                }
            }

            @DELETE
            @Path("filter/withEntity")
            public String deleteFilterWithEntity(@Context HttpHeaders h, String e) {
                String value = h.getRequestHeaders().getFirst("Authorization");
                if (value == null) {
                    throw new WebApplicationException(
                            Response.status(401).header("WWW-Authenticate", "Basic realm=\"WallyWorld\"").build());
                }

                return e;
            }
        }

        @Override
        protected Application configure() {
        ResourceConfig config = new ResourceConfig(AuthResource.class);
        config.register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
        return config;
    }

        @Test
        public void testAuthGet() {
        ClientConfig config = new ClientConfig();
        config.property(JavaNetHttpClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("name", "password".toCharArray());
                    }
                });
        config.connectorProvider(new JavaNetHttpConnectorProvider());
        Client client = ClientBuilder.newClient(config);

        Response response = client.target(getBaseUri()).path(PATH).request().get();
        assertEquals("GET", response.readEntity(String.class));
        client.close();
    }

        @Test
        public void testAuthPost() {
        ClientConfig config = new ClientConfig();
            config.property(JavaNetHttpClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION,
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication("name", "password".toCharArray());
                        }
                    });
        config.connectorProvider(new JavaNetHttpConnectorProvider());
        Client client = ClientBuilder.newClient(config);

        Response response = client.target(getBaseUri()).path(PATH).request().post(Entity.text("POST"));
        assertEquals("POST", response.readEntity(String.class));
        client.close();
    }

        @Test
        public void testAuthDelete() {
        ClientConfig config = new ClientConfig();
            config.property(JavaNetHttpClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION,
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication("name", "password".toCharArray());
                        }
                    });
        config.connectorProvider(new JavaNetHttpConnectorProvider());
        Client client = ClientBuilder.newClient(config);

        Response response = client.target(getBaseUri()).path(PATH).request().delete();
        assertEquals(response.getStatus(), 204);
        client.close();
    }

    }
