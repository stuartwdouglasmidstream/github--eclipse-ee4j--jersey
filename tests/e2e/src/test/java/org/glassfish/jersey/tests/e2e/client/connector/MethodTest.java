/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.client.connector;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the Http methods.
 *
 * @author Stepan Kopriva
 */
@Suite
@SelectClasses({MethodTest.JdkConnectorProviderMethodTest.class,
        MethodTest.GrizzlyConnectorProviderMethodTest.class})
public class MethodTest {

    private static final String PATH = "test";

    @Path("/test")
    public static class HttpMethodResource {

        @GET
        public String get() {
            return "GET";
        }

        @POST
        public String post(String entity) {
            return entity;
        }

        @PUT
        public String put(String entity) {
            return entity;
        }

        @DELETE
        public String delete() {
            return "DELETE";
        }
    }

    public static class GrizzlyConnectorProviderMethodTest extends MethodTemplateTest {
        public GrizzlyConnectorProviderMethodTest() {
            super(new GrizzlyConnectorProvider());
        }
    }

    public static class JdkConnectorProviderMethodTest extends MethodTemplateTest {
        public JdkConnectorProviderMethodTest() {
            super(new JdkConnectorProvider());
        }
    }

    public abstract static class MethodTemplateTest extends JerseyTest {
        private final ConnectorProvider connectorProvider;

        public MethodTemplateTest(ConnectorProvider connectorProvider) {
            this.connectorProvider = connectorProvider;
        }

        @Override
        protected Application configure() {
            return new ResourceConfig(HttpMethodResource.class);
        }

        @Override
        protected void configureClient(ClientConfig config) {
            config.connectorProvider(connectorProvider);
        }

        @Test
        public void testGet() {
            Response response = target(PATH).request().get();
            assertEquals("GET", response.readEntity(String.class));
        }

        @Test
        public void testPost() {
            Response response = target(PATH).request().post(Entity.entity("POST", MediaType.TEXT_PLAIN));
            assertEquals("POST", response.readEntity(String.class));
        }

        @Test
        public void testPut() {
            Response response = target(PATH).request().put(Entity.entity("PUT", MediaType.TEXT_PLAIN));
            assertEquals("PUT", response.readEntity(String.class));
        }

        @Test
        public void testDelete() {
            Response response = target(PATH).request().delete();
            assertEquals("DELETE", response.readEntity(String.class));
        }
    }
}
