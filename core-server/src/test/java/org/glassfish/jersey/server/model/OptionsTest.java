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

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.research.ws.wadl.Application;

/**
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak
 * @author Miroslav Fuksa
 */
public class OptionsTest {

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod("patch")
    public @interface PATCH {
    }

    private ApplicationHandler app;

    private void initiateWebApplication(Class<?>... classes) {
        app = new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    public static class ResourceNoOptions {

        @GET
        public String get() {
            return "GET";
        }

        @PUT
        public String put(String e) {
            return "PUT";
        }

        @POST
        public String post(String e) {
            return "POST";
        }

        @DELETE
        public void delete() {
        }

        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testNoOptions() throws Exception {
        initiateWebApplication(ResourceNoOptions.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "OPTIONS").build()).get();
        assertEquals(200, response.getStatus());
        _checkAllowContent(response.getHeaderString("Allow"));

        final MediaType mediaType = MediaType.APPLICATION_XML_TYPE;
        response = app.apply(RequestContextBuilder.from("/", "OPTIONS").accept(mediaType).build()).get();
        assertEquals(200, response.getStatus());
        assertEquals(mediaType, response.getMediaType());
        assertEquals(0, response.getLength());
        _checkAllowContent(response.getHeaderString("Allow"));
    }

    private void _checkAllowContent(String allow) {
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));
    }

    @Path("/")
    public static class ResourceWithOptions {

        @OPTIONS
        public Response options() {
            return Response.ok("OPTIONS")
                    .header("Allow", "OPTIONS, GET, PUT, POST, DELETE, PATCH")
                    .header("X-TEST", "OVERRIDE").build();
        }

        @GET
        public String get() {
            return "GET";
        }

        @PUT
        public String put(String e) {
            return "PUT";
        }

        @POST
        public String post(String e) {
            return "POST";
        }

        @DELETE
        public void delete() {
        }

        @PATCH
        public String patch(String e) {
            return "PATCH";
        }
    }

    @Test
    public void testWithOptions() throws Exception {
        initiateWebApplication(ResourceWithOptions.class);

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "OPTIONS").build()).get();

        assertEquals(200, response.getStatus());

        String allow = response.getHeaderString("Allow");
        assertTrue(allow.contains("OPTIONS"));
        assertTrue(allow.contains("GET"));
        assertTrue(allow.contains("PUT"));
        assertTrue(allow.contains("POST"));
        assertTrue(allow.contains("DELETE"));
        assertTrue(allow.contains("PATCH"));

        assertEquals("OVERRIDE", response.getHeaderString("X-TEST"));
    }

    @Path("resource")
    public static class WadlResource {

        @GET
        public String get() {
            return "get";
        }
    }

    @Test
    public void testRequestNoType() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(WadlResource.class));
        final ContainerRequest request = RequestContextBuilder.from("/resource", "OPTIONS").build();
        final ContainerResponse response = application.apply(request).get();
        Assertions.assertEquals(200, response.getStatus());
        final MediaType type = response.getMediaType();
        Assertions.assertTrue(type.equals(MediaTypes.WADL_TYPE) || type.equals(MediaType.TEXT_HTML_TYPE)
                || type.equals(MediaType.TEXT_PLAIN));

    }

    @Test
    public void testRequestTextPlain() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(WadlResource.class, ResponseTextFilter.class));
        final ContainerResponse response = testOptions(MediaType.TEXT_PLAIN_TYPE, application, "/resource");
        final String entity = (String) response.getEntity();
        Assertions.assertTrue(entity.contains("GET"));
    }

    @Test
    public void testRequestTextHtml() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(WadlResource.class, ResponseHtmlFilter.class));
        final MediaType requestType = MediaType.TEXT_HTML_TYPE;
        final ContainerResponse response = testOptions(requestType, application, "/resource");
        Assertions.assertTrue(response.getAllowedMethods().contains("GET"));
    }

    @Test
    public void testRequestWadl() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(WadlResource.class, ResponseWadlFilter.class));
        final MediaType requestType = MediaTypes.WADL_TYPE;
        final ContainerResponse response = testOptions(requestType, application, "/resource");
    }

    private static class ResponseTextFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            final MediaType type = responseContext.getMediaType();
            Assertions.assertEquals(MediaType.TEXT_PLAIN_TYPE, type);

        }
    }

    private static class ResponseHtmlFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            final MediaType type = responseContext.getMediaType();
            Assertions.assertEquals(MediaType.TEXT_HTML_TYPE, type);

        }
    }

    private static class ResponseWadlFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            final MediaType type = responseContext.getMediaType();
            Assertions.assertEquals(MediaTypes.WADL_TYPE, type);
            responseContext.getEntity().getClass().equals(Application.class);

        }
    }

    @Path("no-get")
    private static class ResourceWithoutGetMethod {

        @POST
        public String post(String entity) {
            return entity;
        }

        @POST
        @Path("sub")
        public String subPost(String entity) {
            return entity;
        }

    }

    @Test
    public void testNoHead() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(ResourceWithoutGetMethod.class));
        final ContainerResponse response = testOptions(MediaType.TEXT_PLAIN_TYPE, application, "/no-get");

        Assertions.assertFalse(((String) response.getEntity()).contains("HEAD"));
    }

    @Test
    public void testNoHeadWildcard() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceWithoutGetMethod.class);
        resourceConfig.property(ServerProperties.WADL_FEATURE_DISABLE, true);
        ApplicationHandler application = new ApplicationHandler(resourceConfig);

        final ContainerRequest request = RequestContextBuilder.from("/no-get", "OPTIONS").accept(MediaType.MEDIA_TYPE_WILDCARD)
                .build();
        final ContainerResponse response = application.apply(request).get();
        Assertions.assertEquals(200, response.getStatus());

        final List<String> strings = response.getStringHeaders().get(HttpHeaders.ALLOW);
        for (String allow : strings) {
            Assertions.assertFalse(allow.contains("HEAD"));
        }
    }

    private ContainerResponse testOptions(MediaType requestType, ApplicationHandler application, String path)
            throws InterruptedException, ExecutionException {
        final ContainerRequest request = RequestContextBuilder.from(path, "OPTIONS").accept(requestType)
                .build();
        final ContainerResponse response = application.apply(request).get();
        Assertions.assertEquals(200, response.getStatus());
        final MediaType type = response.getMediaType();
        Assertions.assertEquals(requestType, type);
        return response;
    }

    @Test
    public void testNoHeadInSub() throws ExecutionException, InterruptedException {
        ApplicationHandler application = new ApplicationHandler(new ResourceConfig(ResourceWithoutGetMethod.class));
        final MediaType requestType = MediaType.TEXT_PLAIN_TYPE;
        final ContainerResponse response = testOptions(requestType, application, "/no-get/sub");
        Assertions.assertFalse(((String) response.getEntity()).contains("HEAD"));
    }
}
