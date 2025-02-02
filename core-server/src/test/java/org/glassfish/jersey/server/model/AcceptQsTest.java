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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jakub Podlesak
 */
public class AcceptQsTest {

    private static class StringReturningInflector implements Inflector<ContainerRequestContext, Response> {

        String entity;

        StringReturningInflector(String entity) {
            this.entity = entity;
        }

        @Override
        public Response apply(ContainerRequestContext data) {
            return Response.ok(entity).build();
        }
    }

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }

    private Inflector<ContainerRequestContext, Response> stringResponse(String s) {
        return new StringReturningInflector(s);
    }

    @Path("/")
    public static class TestResource {

        @Produces("application/foo;qs=0.4")
        @GET
        public String doGetFoo() {
            return "foo";
        }

        @Produces("application/bar;qs=0.5")
        @GET
        public String doGetBar() {
            return "bar";
        }

        @Produces("application/baz")
        @GET
        public String doGetBaz() {
            return "baz";
        }
    }

    @Test
    public void testAcceptGetDeclarative() throws Exception {
        runTestAcceptGet(createApplication(TestResource.class));
    }

    @Test
    public void testAcceptGetProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.4")).handledBy(stringResponse("foo"));
        rb.addMethod("GET").produces(MediaType.valueOf("application/bar;qs=0.5")).handledBy(stringResponse("bar"));
        rb.addMethod("GET").produces(MediaType.valueOf("application/baz")).handledBy(stringResponse("baz"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptGet(new ApplicationHandler(rc));
    }

    private void runTestAcceptGet(ApplicationHandler app) throws Exception {

        String s = (String) app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo").build()).get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=0.1").build()).get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo", "application/bar;q=0.4", "application/baz;q=0.2").build())
                .get().getEntity();
        assertEquals("foo", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar;q=0.4", "application/baz;q=0.2").build())
                .get().getEntity();
        assertEquals("bar", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo", "application/bar", "application/baz;q=0.6").build())
                .get().getEntity();
        assertEquals("bar", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar", "application/baz;q=0.2").build())
                .get().getEntity();
        assertEquals("bar", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar;q=0.2", "application/baz").build())
                .get().getEntity();
        assertEquals("baz", s);

        s = (String) app.apply(RequestContextBuilder.from("/", "GET")
                .accept("application/foo;q=0.4", "application/bar;q=0.2", "application/baz;q=0.4").build())
                .get().getEntity();
        assertEquals("baz", s);
    }

    @Path("/")
    public static class MultipleResource {

        @Produces({"application/foo;qs=0.5", "application/bar"})
        @GET
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testAcceptMultipleDeclarative() throws Exception {
        runTestAcceptMultiple(createApplication(MultipleResource.class));
    }

    @Test
    public void testAcceptMultipleProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");
        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.5"), MediaType.valueOf("application/bar"))
                .handledBy(stringResponse("GET"));
        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptMultiple(new ApplicationHandler(rc));
    }

    private void runTestAcceptMultiple(ApplicationHandler app) throws Exception {

        MediaType foo = MediaType.valueOf("application/foo");
        MediaType bar = MediaType.valueOf("application/bar");

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept(foo).build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("GET", response.getEntity());
        assertEquals(foo, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept(bar).build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("GET", response.getEntity());
        assertEquals(bar, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("*/*").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("GET", response.getEntity());
        assertEquals(bar, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/*").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("GET", response.getEntity());
        assertEquals(bar, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=0.1", "application/bar").build())
                .get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("GET", response.getEntity());
        assertEquals(bar, response.getMediaType());

        response = app
                .apply(RequestContextBuilder.from("/", "GET").accept("application/foo;q=0.5", "application/bar;q=0.1").build())
                .get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("GET", response.getEntity());
        assertEquals(foo, response.getMediaType());
    }

    @Path("/")
    public static class SubTypeResource {

        @Produces("text/*;qs=0.5")
        @GET
        public String getWildcard() {
            return "*";
        }

        @Produces("text/plain;qs=0.6")
        @GET
        public String getPlain() {
            return "plain";
        }

        @Produces("text/html;qs=0.7")
        @GET
        public String getXml() {
            return "html";
        }
    }

    @Test
    public void testAcceptSubTypeDeclarative() throws Exception {
        runTestAcceptSubType(createApplication(SubTypeResource.class));
    }

    @Test
    public void testAcceptSubTypeProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("text/*;qs=0.5")).handledBy(stringResponse("*"));
        rb.addMethod("GET").produces(MediaType.valueOf("text/plain;qs=0.6")).handledBy(stringResponse("plain"));
        rb.addMethod("GET").produces(MediaType.valueOf("text/html;qs=0.7")).handledBy(stringResponse("html"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptSubType(new ApplicationHandler(rc));
    }

    private void runTestAcceptSubType(ApplicationHandler app) throws Exception {

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("image/png, text/plain;q=0.4").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("html", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain, text/html;q=0.5").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/html;q=0.5").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("html", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/*;q=0.5, text/plain;q=0.6").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/gaga;q=0.6").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.valueOf("text/gaga"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/gaga, text/plain").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.valueOf("text/plain"), response.getMediaType());
    }

    @Path("/")
    public static class SubTypeResourceNotIntuitive {

        @Produces("text/*;qs=0.9")
        @GET
        public String getWildcard() {
            return "*";
        }

        @Produces("text/plain;qs=0.7")
        @GET
        public String getPlain() {
            return "plain";
        }

        @Produces("text/html;qs=0.5")
        @GET
        public String getXml() {
            return "html";
        }
    }

    @Test
    public void testAcceptSubTypeNotIntuitiveDeclarative() throws Exception {
        runTestAcceptSubTypeNotIntuitive(createApplication(SubTypeResourceNotIntuitive.class));
    }

    @Test
    public void testAcceptSubTypeNotIntuitiveProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("text/*;qs=0.9")).handledBy(stringResponse("*"));
        rb.addMethod("GET").produces(MediaType.valueOf("text/plain;qs=0.7")).handledBy(stringResponse("plain"));
        rb.addMethod("GET").produces(MediaType.valueOf("text/html;qs=0.5")).handledBy(stringResponse("html"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptSubTypeNotIntuitive(new ApplicationHandler(rc));
    }

    private void runTestAcceptSubTypeNotIntuitive(ApplicationHandler app) throws Exception {

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("image/png, text/plain").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain, text/html;q=0.5").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/html;q=0.5").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/*;q=0.5, text/plain;q=0.6").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/gaga;q=0.6").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.valueOf("text/gaga"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/gaga, text/plain").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("*", response.getEntity());
        assertEquals(MediaType.valueOf("text/gaga"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/*").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/*;q=0.5, text/html;q=0.1").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("plain", response.getEntity());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
    }

    @Path("/")
    public static class NoProducesResource {

        @GET
        public String get() {
            return "GET";
        }
    }

    @Test
    public void testAcceptNoProducesDeclarative() throws Exception {
        runTestAcceptNoProduces(createApplication(NoProducesResource.class));
    }

    @Test
    public void testAcceptNoProducesProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");
        rb.addMethod("GET").handledBy(stringResponse("GET"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestAcceptNoProduces(new ApplicationHandler(rc));
    }

    private void runTestAcceptNoProduces(ApplicationHandler app) throws Exception {

        // media type order in the accept header does not impose output media type!
        ContainerResponse response = app
                .apply(RequestContextBuilder.from("/", "GET").accept("image/png, text/plain;q=0.9").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.valueOf("image/png"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("text/plain;q=0.5, text/html").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("GET", response.getEntity());
        assertEquals(MediaType.TEXT_HTML_TYPE, response.getMediaType());
    }

    @Path("/")
    public static class ProducesOneMethodFooBarResource {

        @GET
        @Produces({"application/foo;qs=0.1", "application/bar"})
        public String get() {
            return "FOOBAR";
        }
    }

    @Test
    public void testProducesOneMethodFooBarResourceDeclarative() throws Exception {
        runTestFooBar(createApplication(ProducesOneMethodFooBarResource.class), "FOOBAR", "FOOBAR");
    }

    @Test
    public void testProducesOneMethodFooBarResourceProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");
        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.1"), MediaType.valueOf("application/bar"))
                .handledBy(stringResponse("FOOBAR"));
        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestFooBar(new ApplicationHandler(rc), "FOOBAR", "FOOBAR");
    }

    @Path("/")
    public static class ProducesTwoMethodsFooBarResource {

        @GET
        @Produces("application/foo;qs=0.1")
        public String getFoo() {
            return "FOO";
        }

        @GET
        @Produces("application/bar")
        public String getBar() {
            return "BAR";
        }
    }

    @Test
    public void testProducesTwoMethodsFooBarResourceProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.1")).handledBy(stringResponse("FOO"));
        rb.addMethod("GET").produces(MediaType.valueOf("application/bar")).handledBy(stringResponse("BAR"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestFooBar(new ApplicationHandler(rc), "FOO", "BAR");
    }

    @Test
    public void testProducesTwoMethodsFooBarResourceDeclarative() throws Exception {
        runTestFooBar(createApplication(ProducesTwoMethodsFooBarResource.class), "FOO", "BAR");
    }

    @Path("/")
    public static class ProducesTwoMethodsBarFooResource {

        @GET
        @Produces("application/bar")
        public String getBar() {
            return "BAR";
        }

        @GET
        @Produces("application/foo;qs=0.1")
        public String getFoo() {
            return "FOO";
        }
    }

    @Test
    public void testProducesTwoMethodsBarFooResourceProgrammatic() throws Exception {
        final Resource.Builder rb = Resource.builder("/");

        rb.addMethod("GET").produces(MediaType.valueOf("application/bar")).handledBy(stringResponse("BAR"));
        rb.addMethod("GET").produces(MediaType.valueOf("application/foo;qs=0.1")).handledBy(stringResponse("FOO"));

        ResourceConfig rc = new ResourceConfig();
        rc.registerResources(rb.build());
        runTestFooBar(new ApplicationHandler(rc), "FOO", "BAR");
    }

    @Test
    public void testProducesTwoMethodsBarFooResourceDeclarative() throws Exception {
        runTestFooBar(createApplication(ProducesTwoMethodsBarFooResource.class), "FOO", "BAR");
    }

    private void runTestFooBar(ApplicationHandler app, String fooContent, String barContent) throws Exception {

        ContainerResponse response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals(fooContent, response.getEntity());
        assertEquals(MediaType.valueOf("application/foo"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/bar").build()).get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals(barContent, response.getEntity());
        assertEquals(MediaType.valueOf("application/bar"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/foo", "application/bar;q=0.5").build())
                .get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals(fooContent, response.getEntity());
        assertEquals(MediaType.valueOf("application/foo"), response.getMediaType());

        response = app.apply(RequestContextBuilder.from("/", "GET").accept("application/bar", "application/foo;q=0.5").build())
                .get();
        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals(barContent, response.getEntity());
        assertEquals(MediaType.valueOf("application/bar"), response.getMediaType());
    }
}
