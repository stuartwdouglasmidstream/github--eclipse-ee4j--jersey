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

package org.glassfish.jersey.tests.e2e.server;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testing URI template as an e2e test.
 *
 * @author Miroslav Fuksa
 *
 */
public class UriBuilderTemplateTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testNumericResource() {
        Response response = target().path("test/numeric/55").request().get();
        String uri = response.readEntity(String.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("test/numeric/55", uri);
    }

    @Test
    public void testAnyResource() {
        Response response = target().path("test/any/getNumericResource").request().get();
        String uri = response.readEntity(String.class);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("test/numeric/11", uri);
    }

    @Path("test")
    public static class Resource {

        @Path("numeric/{ resourceNo : \\d+}")
        @GET
        public String getNumericResource(@PathParam("resourceNo") int resourceNo) {
            URI anyResourceUri = UriBuilder.fromResource(Resource.class).path(Resource.class,
                    "getNumericResource").build(resourceNo);
            return anyResourceUri.toString();
        }

        @Path("any/{resourceName}")
        @GET
        public String getAnyResource(@PathParam("resourceName") String resourceName) {
            URI numResourceUri = UriBuilder.fromResource(Resource.class).path(Resource.class, "getNumericResource").build(11);
            return numResourceUri.toString();
        }
    }

    @Test
    public void testResolveTemplate() {
        assertThrows(IllegalArgumentException.class, () -> {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("a", "xyz");
            map.put(null, "path");
            UriBuilder builder = UriBuilder.fromPath("").path("{a}/{b}");
            builder.resolveTemplates(map);
        });
    }
}
