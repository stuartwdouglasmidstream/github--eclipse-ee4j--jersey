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

package org.glassfish.jersey.server.model;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test matching of resources with ambiguous templates.
 *
 * @author Miroslav Fuksa
 *
 */
public class AmbiguousTemplateTest {

    @Path("{abc}")
    public static class ResourceABC {

        @PathParam("abc")
        String param;

        @Path("a")
        @GET
        public String getSub() {
            return "a-abc:" + param;
        }

        @GET
        public String get() {
            return "abc:" + param;
        }

    }

    @Path("{xyz}")
    public static class ResourceXYZ {

        @PathParam("xyz")
        String param;

        @POST
        public String post(String post) {
            return "xyz:" + param;
        }

        @Path("x")
        @GET
        public String get() {
            return "x-xyz:" + param;
        }

        @Path("{sub-x}")
        @GET
        public String get(@PathParam("sub-x") String subx) {
            return "subx-xyz:" + param + ":" + subx;
        }
    }

    @Test
    public void testPathParamOnAmbiguousTemplate() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu/a", "GET").build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("a-abc:uuu", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplate2() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/test/x", "GET").build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("x-xyz:test", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplate3() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu", "GET").build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("abc:uuu", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplate4() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/post", "POST")
                .entity("entity").build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("xyz:post", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplate5() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceABC.class,
                ResourceXYZ.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/xxx/foo", "GET").build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("subx-xyz:xxx:foo", response.getEntity());
    }

    @Path("locator")
    public static class SimpleLocator {

        @Path("{resource}")
        public Object locator(@PathParam("resource") String resource) {
            if ("xyz".equals(resource)) {
                return new ResourceXYZ();
            } else if ("abc".equals(resource)) {
                return new ResourceABC();
            }
            throw new WebApplicationException(404);
        }
    }

    @Test
    public void testPathParamOnAmbiguousTemplateTroughSubResourceLocator1() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(SimpleLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/locator/abc/uuu/a", "GET")
                .build()).get();
        Assertions.assertEquals(404, response.getStatus());
    }

    @Test
    public void testPathParamOnAmbiguousTemplateTroughSubResourceLocator2() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(SimpleLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/locator/abc/a", "GET")
                .build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("a-abc:null", response.getEntity());
    }

    @Test
    public void testPathParamOnAmbiguousTemplateTroughSubResourceLocator3() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(SimpleLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/locator/xyz/subxfoo", "GET")
                .build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("subx-xyz:null:subxfoo", response.getEntity());
    }

    @Path("{xyz}")
    public static class ResourceWithLocator {

        @PathParam("xyz")
        String param;

        @Path("/")
        public SubResource locator() {
            return new SubResource(param);
        }

        @Path("{path}")
        public SubResource subLocator(@PathParam("path") String path) {
            return new SubResource(param + ":" + path);
        }
    }

    public static class SubResource {

        private final String str;

        public SubResource(String str) {
            this.str = str;
        }

        @GET
        public String get() {
            return str;
        }
    }

    @Test
    public void testSubResourceLocatorWithPathParams() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceWithLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu", "GET").build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("uuu", response.getEntity());
    }

    @Test
    public void testSubResourceLocatorWithPathParams2() throws ExecutionException, InterruptedException {
        final ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ResourceWithLocator.class));
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/uuu/test", "GET").build()).get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("uuu:test", response.getEntity());
    }

    @Path("{templateA}")
    public static class ResourceA {

        @GET
        public String getA() {
            return "getA";
        }
    }

    @Path("{templateB}")
    public static class ResourceB {

        @POST
        public String postB(String entity) {
            return "postB";
        }
    }

    @Path("resq")
    public static class ResourceQ {

        @GET
        @Path("{path}")
        public String getA() {
            return "getA";
        }

        @PUT
        @Path("{temp}")
        public String put(String str) {
            return "getB";
        }
    }

    @Test
    public void testOptionsOnRoot() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class, ResourceB.class, ResourceQ.class);
        ApplicationHandler app = new ApplicationHandler(resourceConfig);
        final ContainerResponse containerResponse = app.apply(RequestContextBuilder.from("/aaa", "OPTIONS")
                .accept(MediaType.TEXT_PLAIN).build()).get();
        Assertions.assertEquals(200, containerResponse.getStatus());

        final List<String> methods = Arrays.asList(containerResponse.getEntity().toString().split(", "));
        assertThat(methods, hasItems("POST", "GET", "OPTIONS", "HEAD"));
        assertThat(methods.size(), is(4));

    }

    @Test
    public void testGetOnRoot() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class, ResourceB.class, ResourceQ.class);
        ApplicationHandler app = new ApplicationHandler(resourceConfig);
        final ContainerResponse containerResponse = app.apply(RequestContextBuilder.from("/aaa", "GET")
                .accept(MediaType.TEXT_PLAIN).build()).get();
        Assertions.assertEquals(200, containerResponse.getStatus());
        Assertions.assertEquals("getA", containerResponse.getEntity());
    }

    @Test
    public void testOptionsOnChild() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class, ResourceB.class, ResourceQ.class);
        ApplicationHandler app = new ApplicationHandler(resourceConfig);
        final ContainerResponse containerResponse = app.apply(RequestContextBuilder.from("/resq/c", "OPTIONS")
                .accept(MediaType.TEXT_PLAIN).build()).get();
        Assertions.assertEquals(200, containerResponse.getStatus());

        final List<String> methods = Arrays.asList(containerResponse.getEntity().toString().split(", "));
        assertThat(methods, hasItems("PUT", "GET", "OPTIONS", "HEAD"));
        assertThat(methods.size(), is(4));

    }

    @Test
    public void testGetOnChild() throws ExecutionException, InterruptedException {
        ResourceConfig resourceConfig = new ResourceConfig(ResourceA.class, ResourceB.class, ResourceQ.class);
        ApplicationHandler app = new ApplicationHandler(resourceConfig);
        final ContainerResponse containerResponse = app.apply(RequestContextBuilder.from("/resq/a", "GET").build()).get();
        Assertions.assertEquals(200, containerResponse.getStatus());
        Assertions.assertEquals("getA", containerResponse.getEntity());
    }
}
