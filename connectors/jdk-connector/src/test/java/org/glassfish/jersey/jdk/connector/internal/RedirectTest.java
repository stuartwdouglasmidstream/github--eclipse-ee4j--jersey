/*
 * Copyright (c) 2015, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector.internal;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Petr Janouch
 */
public class RedirectTest extends JerseyTest {

    private static String TARGET_GET_MSG = "You have reached the target";

    @Override
    protected Application configure() {
        return new ResourceConfig(RedirectingResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new JdkConnectorProvider());
    }

    @Test
    public void testDisableRedirect() {
        Response response = target("redirecting/303").property(ClientProperties.FOLLOW_REDIRECTS, false).request().get();
        assertEquals(303, response.getStatus());
    }

    @Test
    public void testGet303() {
        Response response = target("redirecting/303").request().get();
        assertEquals(200, response.getStatus());
        assertEquals(TARGET_GET_MSG, response.readEntity(String.class));
    }

    @Test
    public void testPost303() {
        Response response = target("redirecting/303").request().post(Entity.entity("My awesome message", MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(TARGET_GET_MSG, response.readEntity(String.class));
    }

    @Test
    public void testHead303() {
        Response response = target("redirecting/303").request().head();
        assertEquals(200, response.getStatus());
        assertTrue(response.readEntity(String.class).isEmpty());
    }

    // in this implementation; 301, 307 and 308 work exactly the same
    @Test
    public void testGet307() {
        Response response = target("redirecting/307").request().get();
        assertEquals(200, response.getStatus());
        assertEquals(TARGET_GET_MSG, response.readEntity(String.class));
    }

    // in this implementation; 301, 307 and 308 work exactly the same
    @Test
    public void testPost307() {
        Response response = target("redirecting/307").request().post(Entity.entity("My awesome message", MediaType.TEXT_PLAIN));
        assertEquals(307, response.getStatus());
    }

    // in this implementation; 301, 307 and 308 work exactly the same
    @Test
    public void testHead307() {
        Response response = target("redirecting/307").request().head();
        assertEquals(200, response.getStatus());
        assertTrue(response.readEntity(String.class).isEmpty());
    }

    @Test
    public void testCycle() {
        try {
            target("redirecting/cycle").request().get();
            fail();
        } catch (Throwable t) {
            assertEquals(RedirectException.class.getName(), t.getCause().getClass().getName());
        }
    }

    @Test
    public void testMaxRedirectsSuccess() {
        Response response = target("redirecting/maxRedirect").property(JdkConnectorProperties.MAX_REDIRECTS, 2).request().get();
        assertEquals(200, response.getStatus());
        assertEquals(TARGET_GET_MSG, response.readEntity(String.class));
    }

    @Test
    public void testMaxRedirectsFail() {
        try {
            target("redirecting/maxRedirect").property(JdkConnectorProperties.MAX_REDIRECTS, 1).request().get();
            fail();
        } catch (Throwable t) {
            assertEquals(RedirectException.class.getName(), t.getCause().getClass().getName());
        }
    }

    @Path("/redirecting")
    public static class RedirectingResource {

        private Response get303RedirectToTarget() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("target").build()).build();
        }

        private Response get307RedirectToTarget() {
            return Response.temporaryRedirect(UriBuilder.fromResource(RedirectingResource.class).path("target").build()).build();
        }

        @Path("303")
        @HEAD
        public Response head303() {
            return get303RedirectToTarget();
        }

        @Path("303")
        @GET
        public Response get303() {
            return get303RedirectToTarget();
        }

        @Path("303")
        @POST
        public Response post303(String entity) {
            return get303RedirectToTarget();
        }

        @Path("307")
        @HEAD
        public Response head307() {
            return get307RedirectToTarget();
        }

        @Path("307")
        @GET
        public Response get307() {
            return get307RedirectToTarget();
        }

        @Path("307")
        @POST
        public Response post307(String entity) {
            return get307RedirectToTarget();
        }

        @Path("target")
        @GET
        public String target() {
            return TARGET_GET_MSG;
        }

        @Path("target")
        @POST
        public String target(String entity) {
            return entity;
        }

        @Path("cycle")
        @GET
        public Response cycle() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("cycleNode2").build()).build();
        }

        @Path("cycleNode2")
        @GET
        public Response cycleNode2() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("cycleNode3").build()).build();
        }

        @Path("cycleNode3")
        @GET
        public Response cycleNode3() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("cycle").build()).build();
        }

        @Path("maxRedirect")
        @GET
        public Response maxRedirect() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("maxRedirectNode2").build()).build();
        }

        @Path("maxRedirectNode2")
        @GET
        public Response maxRedirectNode2() {
            return Response.seeOther(UriBuilder.fromResource(RedirectingResource.class).path("target").build()).build();
        }
    }
}
