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

package org.glassfish.jersey.server.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Paul Sandoz
 * @author Jakub Podlesak
 */
public class OptionsSubResourceMethodTest {

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("PATCH")
    public @interface PATCH {
    }

    private ApplicationHandler app;

    private void initiateWebApplication(Class<?>... classes) {
        app = new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    public static class ResourceNoOptions {

        @Path("sub")
        @GET
        public String get() {
            return "GET";
        }

        @Path("sub")
        @PUT
        public String put(String e) {
            return "PUT";
        }

        @Path("sub")
        @POST
        public String post(String e) {
            return "POST";
        }

        @Path("sub")
        @DELETE
        public void delete() {
        }

        @Path("sub")
        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testNoOptions() throws Exception {
        initiateWebApplication(ResourceNoOptions.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());
        _checkAllowContent(response.getHeaderString("Allow"));

        response = app.apply(RequestContextBuilder.from("/sub", "OPTIONS").accept(MediaType.TEXT_HTML).build()).get();

        assertEquals(200, response.getStatus());
        _checkAllowContent(response.getHeaderString("Allow"));
    }

    private void _checkAllowContent(final String allow) {
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));

    }

    @Path("/")
    public static class ResourceWithOptions {

        @Path("sub")
        @OPTIONS
        public Response options() {
            return Response.ok("OPTIONS")
                    .header("Allow", "OPTIONS, GET, PUT, POST, DELETE, PATCH").build();
        }

        @Path("sub")
        @GET
        public String get() {
            return "GET";
        }

        @Path("sub")
        @PUT
        public String put(String e) {
            return "PUT";
        }

        @Path("sub")
        @POST
        public String post(String e) {
            return "POST";
        }

        @Path("sub")
        @DELETE
        public void delete() {
        }

        @Path("sub")
        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testWithOptions() throws Exception {

        initiateWebApplication(ResourceWithOptions.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        String allow = response.getHeaderString("Allow");
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));
    }

    @Path("/")
    public static class ResourceNoOptionsDifferentSub {

        @Path("sub1")
        @GET
        public String getFoo() {
            return "FOO";
        }

        @Path("sub2")
        @PUT
        public String putBar() {
            return "BAR";
        }
    }

    @Test
    public void testNoOptionsDifferentSub() throws Exception {

        initiateWebApplication(ResourceNoOptionsDifferentSub.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/sub1", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        String allow = response.getHeaderString("Allow");
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertFalse(allow.contains("PUT"));

        response = app.apply(RequestContextBuilder.from("/sub2", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        allow = response.getHeaderString("Allow");
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("PUT"));
        assertFalse(allow.contains("GET"));
    }
}
