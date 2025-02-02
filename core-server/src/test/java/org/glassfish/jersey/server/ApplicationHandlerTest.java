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

package org.glassfish.jersey.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.model.ModelValidationException;
import org.glassfish.jersey.server.spi.ResponseErrorMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test basic application behavior.
 *
 * @author Jakub Podlesak
 * @author Marek Potociar
 * @author Michal Gajdos
 */
public class ApplicationHandlerTest {

    private ApplicationHandler createApplication(Class<?>... classes) {
        final ResourceConfig resourceConfig = new ResourceConfig(classes);

        return new ApplicationHandler(resourceConfig);
    }

    @Path("/")
    public static class Resource {

        @GET
        public String doGetFoo(@Context HttpHeaders headers) {

            return Integer.toString(headers.getLength());
        }
    }

    @Path("merged")
    public static class MergedA {

        public static final String RESPONSE = "Got in A";

        @GET
        public String doGet() {
            return RESPONSE;
        }
    }

    @Path("merged")
    public static class MergedA1 {

        public static final String RESPONSE = "Got in A";

        @GET
        public String doGet() {
            return RESPONSE;
        }
    }

    @Path("merged")
    public static class MergedB {

        public static final String RESPONSE = "Posted in B";

        @POST
        public String doPost() {

            return RESPONSE;
        }
    }

    @Test
    public void testReturnBadRequestOnIllHeaderValue() throws Exception {
        ApplicationHandler app = createApplication(Resource.class);

        assertEquals(400,
                app.apply(RequestContextBuilder.from("/", "GET").header(HttpHeaders.CONTENT_LENGTH, "text").build())
                        .get().getStatus());
    }

    @Test
    public void testMergedResources() throws Exception {
        ApplicationHandler app = createApplication(MergedA.class, MergedB.class);

        ContainerResponse response;

        response = app.apply(RequestContextBuilder.from("/merged", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals(MergedA.RESPONSE, response.getEntity());

        response = app.apply(RequestContextBuilder.from("/merged", "POST").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals(MergedB.RESPONSE, response.getEntity());
    }

    /**
     * This test ensures that resource validation kicks in AFTER resources are merged.
     */
    @Test
    public void testMergedResourcesValidationFailure() throws Exception {
        try {
            createApplication(MergedA.class, MergedA1.class);
        } catch (ModelValidationException ex) {
            // success
            return;
        }

        fail("Model validation exception was expected but not thrown.");
    }

    public static final class CustomResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) throws
                IOException {
            responseContext.setEntity(
                    responseContext.getEntity() + "-filtered",
                    responseContext.getEntityAnnotations(),
                    responseContext.getMediaType());
        }
    }

    public static final class CustomFeature implements Feature {

        @Override
        public boolean configure(final FeatureContext configuration) {
            configuration.register(CustomResponseFilter.class);
            return true;
        }
    }

    @Path("property")
    public static final class ProviderPropertyResource {

        private final Configuration config;
        private final ResourceConfig application;

        @Inject
        public ProviderPropertyResource(final Application application, final Configuration config) {
            this.config = config;
            this.application = (ResourceConfig) application;
        }

        @GET
        public String get() {
            assertEquals(1, application.getRegisteredClasses().size());
            assertTrue(application.isRegistered(ProviderPropertyResource.class));
            assertEquals(2, application.getClasses().size());
            assertEquals(0, application.getInstances().size());
            assertEquals(0, application.getSingletons().size());
            assertFalse(application.isEnabled(CustomFeature.class));
            assertFalse(application.isRegistered(CustomResponseFilter.class));
            assertTrue(application.getPropertyNames().contains(ServerProperties.PROVIDER_CLASSNAMES));

            assertTrue(config.isEnabled(CustomFeature.class));
            assertTrue(config.isRegistered(ProviderPropertyResource.class));
            assertTrue(config.isRegistered(CustomResponseFilter.class));
            assertTrue(config.isRegistered(CustomFeature.class));
            assertTrue(config.getPropertyNames().contains(ServerProperties.PROVIDER_CLASSNAMES));

            return "get";
        }

    }

    @Test
    public void testProviderAsServerProperty() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(ProviderPropertyResource.class).property(ServerProperties
                .WADL_FEATURE_DISABLE, true);
        resourceConfig.property(ServerProperties.PROVIDER_CLASSNAMES, CustomFeature.class.getName());

        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/property", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get-filtered", response.getEntity());
    }

    @Path("runtimeConfig")
    public static final class RuntimeConfigResource {

        private final Configuration config;
        private final ResourceConfig application;

        @Inject
        public RuntimeConfigResource(final Application application, final Configuration config) {
            this.config = config;
            this.application = (ResourceConfig) application;
        }

        @GET
        public String get() {
            assertEquals(2, application.getRegisteredClasses().size());
            assertTrue(application.isRegistered(RuntimeConfigResource.class));
            assertTrue(application.isRegistered(CustomFeature.class));
            assertEquals(2, application.getClasses().size());
            assertFalse(application.isEnabled(CustomFeature.class));
            assertFalse(application.isRegistered(CustomResponseFilter.class));

            assertTrue(config.isEnabled(CustomFeature.class));
            assertTrue(config.isRegistered(RuntimeConfigResource.class));
            assertTrue(config.isRegistered(CustomResponseFilter.class));
            assertTrue(config.isRegistered(CustomFeature.class));

            return "get";
        }
    }

    @Test
    public void testRuntimeResourceConfig() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(RuntimeConfigResource.class).property(ServerProperties
                .WADL_FEATURE_DISABLE, true);
        resourceConfig.register(CustomFeature.class);

        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/runtimeConfig", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get-filtered", response.getEntity());
    }

    @Path("singleton")
    public static class SingletonResourceAndProvider implements ContainerRequestFilter {
        private static final String FILTER_REF = "FILTER_REF";

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.setProperty(FILTER_REF, this);
        }

        @GET
        public String test(@Context ContainerRequestContext rc) {
            final Object filterRef = rc.getProperty(FILTER_REF);
            if (filterRef == this) {
                return "passed";
            } else {
                return "failed";
            }
        }
    }

    @Test
    public void testSingletonResourceAndProviderClass() throws Exception {
        ApplicationHandler ah = new ApplicationHandler(new ResourceConfig(SingletonResourceAndProvider.class));
        ContainerResponse response = ah.apply(RequestContextBuilder.from("/singleton", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("passed", response.getEntity());
    }

    @Singleton
    @Path("singleton")
    public static class SingletonResource {
        private int counter = 0;

        @GET
        public String test(@Context ContainerRequestContext rc) {
            return ++counter + "";
        }
    }

    @Test
    public void testSingletonResourceClass() throws Exception {
        ApplicationHandler ah = new ApplicationHandler(new ResourceConfig(SingletonResource.class));
        ContainerResponse response = ah.apply(RequestContextBuilder.from("/singleton", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("1", response.getEntity());

        response = ah.apply(RequestContextBuilder.from("/singleton", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("2", response.getEntity());
    }

    @Singleton
    public static class ProgrammaticSingleton implements Inflector<Request, Response> {
        private int counter = 0;

        @Override
        public Response apply(Request data) {
            return Response.ok(++counter + "").build();
        }
    }

    public static class ProgrammaticDefault implements Inflector<ContainerRequestContext, Response> {
        private int counter = 0;

        @Override
        public Response apply(ContainerRequestContext data) {
            return Response.ok(++counter + "").build();
        }
    }

    @Test
    public void testProgrammaticSingletonResourceClass() throws Exception {
        ResourceConfig rc = new ResourceConfig();

        org.glassfish.jersey.server.model.Resource.Builder rb;
        rb = org.glassfish.jersey.server.model.Resource.builder();
        rb.path("singleton").addMethod("GET").handledBy(ProgrammaticSingleton.class);
        rc.registerResources(rb.build());

        rb = org.glassfish.jersey.server.model.Resource.builder();
        rb.path("default").addMethod("GET").handledBy(ProgrammaticDefault.class);
        rc.registerResources(rb.build());

        rb = org.glassfish.jersey.server.model.Resource.builder();
        rb.path("defaultinstance").addMethod("GET").handledBy(new ProgrammaticDefault());
        rc.registerResources(rb.build());

        ApplicationHandler ah = new ApplicationHandler(rc);
        ContainerResponse response = ah.apply(RequestContextBuilder.from("/singleton", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("1", response.getEntity());

        response = ah.apply(RequestContextBuilder.from("/singleton", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("2", response.getEntity());

        response = ah.apply(RequestContextBuilder.from("/default", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("1", response.getEntity());

        response = ah.apply(RequestContextBuilder.from("/default", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("1", response.getEntity());

        response = ah.apply(RequestContextBuilder.from("/defaultinstance", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("1", response.getEntity());

        response = ah.apply(RequestContextBuilder.from("/defaultinstance", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("2", response.getEntity());
    }

    public static class Jersey2402 extends Application {

        @Override
        public Set<Class<?>> getClasses() {
            return new HashSet<Class<?>>() {{
                add(Jersey2402Feature.class);
                add(Jersey2402Resource.class);
            }};
        }

        @Override
        public Map<String, Object> getProperties() {
            return Collections.<String, Object>singletonMap("foo", "bar");
        }
    }

    public static class Jersey2402Feature implements Feature {

        @Override
        public boolean configure(final FeatureContext context) {
            final String property = context.getConfiguration().getProperty("foo") != null
                    ? (String) context.getConfiguration().getProperty("foo") : "baz";

            context.register(new ReaderInterceptor() {
                @Override
                public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
                    context.setInputStream(new ByteArrayInputStream(property.getBytes()));
                    return context.proceed();
                }
            });

            return true;
        }
    }

    @Path("/")
    public static class Jersey2402Resource {

        @POST
        public String post(final String post) {
            return post;
        }
    }

    /**
     * JERSEY-2402 reproducer.
     *
     * Test that property set via Application#getProperties() are available in features.
     */
    @Test
    public void testPropagationOfPropertiesToFeatures() throws Exception {
        final ApplicationHandler handler = new ApplicationHandler(Jersey2402.class);

        final ContainerResponse response = handler.apply(RequestContextBuilder.from("/", "POST").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.getEntity());
    }

    public static class MapResponseErrorApplication extends Application {

        @Override
        public Set<Class<?>> getClasses() {
            return new HashSet<Class<?>>() {{
                add(MapResponseErrorResource.class);
                add(MyResponseErrorMapper.class);
                add(ResponseErrorEntityWriter.class);
            }};
        }

        @Override
        public Map<String, Object> getProperties() {
            return new HashMap<String, Object>() {{
                put(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, true);
            }};
        }
    }

    public static class ResponseErrorEntity {

        private String value;

        public ResponseErrorEntity() {
        }

        public ResponseErrorEntity(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class MyResponseErrorMapper implements ResponseErrorMapper {

        @Override
        public Response toResponse(final Throwable exception) {
            if ((exception instanceof InternalServerErrorException
                         && exception.getCause() instanceof MessageBodyProviderNotFoundException)
                    || (exception instanceof WebApplicationException)) {
                return Response.ok().entity("bar").build();
            } else if (exception instanceof MappableException || exception instanceof RuntimeException) {
                return Response.ok().entity(new ResponseErrorEntity("bar")).type("foo/bar").build();
            }
            return null;
        }
    }

    @Produces("foo/bar")
    public static class ResponseErrorEntityWriter implements MessageBodyWriter<ResponseErrorEntity> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(final ResponseErrorEntity responseErrorEntity, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType) {
            return 0;
        }

        @Override
        public void writeTo(final ResponseErrorEntity responseErrorEntity, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String,
                Object> httpHeaders, final OutputStream entityStream) throws IOException, WebApplicationException {
            throw new RuntimeException("Cannot do that!");
        }
    }

    @Path("/")
    public static class MapResponseErrorResource {

        @GET
        @Produces("application/json")
        public ResponseErrorEntity get() {
            return new ResponseErrorEntity("foo");
        }

        @GET
        @Produces("foo/bar")
        @Path("foobar")
        public ResponseErrorEntity getFooBar() {
            return get();
        }
    }

    /**
     * Test that un-mapped response errors are tried to be processed (MBW).
     */
    @Test
    public void testMapResponseErrorForMbw() throws Exception {
        final ApplicationHandler handler = new ApplicationHandler(MapResponseErrorApplication.class);

        final ContainerRequest context = RequestContextBuilder.from("/", "GET").build();
        final ContainerResponse response = handler.apply(context).get();

        assertEquals(200, response.getStatus());
        assertEquals("bar", response.getEntity());
    }

    /**
     * Test that un-mapped response errors are tried to be processed only once (MBW).
     */
    @Test
    public void testMapCyclicResponseErrorForMbw() throws Exception {
        Assertions.assertThrows(ExecutionException.class, () -> {
            final ApplicationHandler handler = new ApplicationHandler(MapResponseErrorApplication.class);

            final ContainerRequest context = RequestContextBuilder.from("/foobar", "GET").build();

            handler.apply(context).get();
        });
    }
}
