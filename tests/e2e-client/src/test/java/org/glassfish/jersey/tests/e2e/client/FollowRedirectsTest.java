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

package org.glassfish.jersey.tests.e2e.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Martin Matula
 */
public class FollowRedirectsTest extends JerseyTest {
    @Path("/test")
    public static class RedirectResource {
        @GET
        public String get() {
            return "GET";
        }

        @GET
        @Path("redirect")
        public Response redirect() {
            return Response.seeOther(UriBuilder.fromResource(RedirectResource.class).build()).build();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(RedirectResource.class);
    }

    @Test
    public void testDoFollow() {
        final Response response = target("test/redirect").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("GET", response.readEntity(String.class));
    }

    @Test
    public void testDontFollow() {
        final WebTarget target = target("test/redirect");
        target.property(ClientProperties.FOLLOW_REDIRECTS, false);
        assertEquals(303, target.request().get().getStatus());
    }
}
