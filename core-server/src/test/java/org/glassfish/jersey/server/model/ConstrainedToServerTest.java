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

package org.glassfish.jersey.server.model;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests whether providers are correctly validated in the server runtime (for example if provider constrained to
 * client runtime is skipped on the server).
 * @author Miroslav Fuksa
 *
 */
public class ConstrainedToServerTest {


    @Test
    public void testFiltersAnnotated() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(MyServerFilter.class, MyClientFilter.class,
                MyServerWrongFilter.class, MyServerFilterWithoutConstraint.class, Resource.class);
        resourceConfig.registerInstances(new MyServerWrongFilter2(), new MyServerFilter2());
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals("called", response.getHeaderString("MyServerFilter"));
        assertEquals("called", response.getHeaderString("MyServerFilter2"));
        assertEquals("called", response.getHeaderString("MyServerFilterWithoutConstraint"));
    }

    @Test
    public void testMyClientUnConstrainedFilter() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(MyClientUnConstrainedFilter.class, Resource.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testResourceAndProviderConstrainedToClient() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceAndProviderConstrainedToClient.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource-and-provider",
                "GET").build()).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testResourceAndProviderConstrainedToServer() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(ResourceAndProviderConstrainedToServer.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource-and-provider-server",
                "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("called", response.getHeaderString("ResourceAndProviderConstrainedToServer"));
    }

    @Test
    public void testClientAndServerProvider() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, MyServerAndClientFilter.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("called", response.getHeaderString("MyServerAndClientFilter"));
    }

    @Test
    public void testClientAndServerProvider2() throws ExecutionException, InterruptedException {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, MyServerAndClientContrainedToClientFilter.class);
        ApplicationHandler handler = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testSpecificApplication() throws ExecutionException, InterruptedException {
        Application app = new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                final HashSet<Class<?>> classes = new HashSet<>();
                classes.add(Resource.class);
                classes.add(MyClientFilter.class);
                classes.add(MyServerWrongFilter.class);
                return classes;
            }
        };
        ApplicationHandler handler = new ApplicationHandler(app);
        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
    }

    @ConstrainedTo(RuntimeType.SERVER)
    public static class MyServerFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("MyServerFilter", "called");
        }
    }

    public static class MyServerFilterWithoutConstraint implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("MyServerFilterWithoutConstraint", "called");
        }
    }

    @ConstrainedTo(RuntimeType.SERVER)
    public static class MyServerFilter2 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("MyServerFilter2", "called");
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class MyServerWrongFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            fail("This filter should never be called.");
        }
    }


    @ConstrainedTo(RuntimeType.SERVER)
    public static class MyServerAndClientFilter implements ContainerResponseFilter, ClientResponseFilter {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("MyServerAndClientFilter", "called");
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class MyServerAndClientContrainedToClientFilter implements ContainerResponseFilter, ClientResponseFilter,
            MessageBodyWriter<String> {
        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            fail("This MyServerAndClientContrainedToClientFilter filter should never be called.");
        }

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return false;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return 0;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
                WebApplicationException {
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class MyServerWrongFilter2 implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            fail("This filter should never be called.");
        }
    }

    @ConstrainedTo(RuntimeType.CLIENT)
    public static class MyClientFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            fail("This filter should never be called.");
        }
    }


    public static class MyClientUnConstrainedFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            fail("This filter should never be called.");
        }
    }

    @Path("resource")
    public static class Resource {

        @GET
        public String get() {
            return "get";
        }
    }

    @Path("resource-and-provider")
    @ConstrainedTo(RuntimeType.CLIENT)
    public static class ResourceAndProviderConstrainedToClient implements ContainerResponseFilter {

        @GET
        public Response get() {
            return Response.ok().entity("ok").build();
        }

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            fail("This filter method should never be called.");
        }
    }

    @Path("resource-and-provider-server")
    @ConstrainedTo(RuntimeType.SERVER)
    public static class ResourceAndProviderConstrainedToServer implements ContainerResponseFilter {

        @GET
        public Response get() {
            return Response.ok().entity("ok").build();
        }


        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.getHeaders().add("ResourceAndProviderConstrainedToServer", "called");
        }
    }
}
