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
import java.lang.annotation.Annotation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import jakarta.inject.Inject;

import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests cases of {@code DynamicFeature} implementation.
 *
 * @author Michal Gajdos
 */
public class DynamicFeatureTest {

    @Path("resource")
    public static class Resource {

        @GET
        public String get() {
            return "get";
        }

        @GET
        @Path("postmatch")
        public String getPostMatch(@Context final HttpHeaders headers) {
            assertEquals("true", headers.getHeaderString("postmatch"));
            return "get";
        }

        @POST
        @Path("providers")
        public String getProviders(@Context final HttpHeaders headers,
                                   @Context final Providers providers,
                                   final String entity) {
            assertNull(providers.getContextResolver(String.class, MediaType.WILDCARD_TYPE));

            assertEquals("ProviderBall", headers.getHeaderString("reader"));
            assertEquals("bar", headers.getHeaderString("foo"));

            return entity;
        }

        @GET
        @Path("providers/error")
        public String getProvidersError() {
            throw new CustomException("error");
        }

        @Path("sub")
        public SubResource subResource() {
            return new SubResource();
        }
    }

    public static class SubResource {

        @GET
        public String get() {
            return "sub-get";
        }
    }

    public static class CustomResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {

            responseContext.setEntity(
                    responseContext.getEntity() + "-filtered",
                    new Annotation[0],
                    responseContext.getMediaType());
        }
    }

    @PreMatching
    public static class PreMatchingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getUriInfo().getMatchedURIs().isEmpty()) {
                fail("Filter executed in PreMatching phase.");
            } else {
                requestContext.getHeaders().add("postmatch", "true");
            }
        }
    }

    public static class PreMatchingDynamicFeature implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            context.register(PreMatchingRequestFilter.class);
        }
    }

    @Test
    public void testPreMatchingFilter() throws Exception {
        final ApplicationHandler application = createApplication(PreMatchingDynamicFeature.class);

        ContainerResponse response;

        response = application.apply(RequestContextBuilder.from("/resource/postmatch", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());
    }

    public static class SubResourceDynamicFeature implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            if (resourceInfo.getResourceClass().equals(SubResource.class)
                    && "get".equals(resourceInfo.getResourceMethod().getName())) {
                context.register(new CustomResponseFilter());
            }
        }
    }

    @Test
    public void testSubResourceFeature() throws Exception {
        final ApplicationHandler application = createApplication(SubResourceDynamicFeature.class);

        ContainerResponse response;

        response = application.apply(RequestContextBuilder.from("/resource/sub", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("sub-get-filtered", response.getEntity());

        response = application.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());
    }

    public static class ProviderBall implements ReaderInterceptor, WriterInterceptor, ContextResolver<String>, ExceptionMapper {

        @Override
        public String getContext(final Class<?> type) {
            return "ProviderBall";
        }

        @Override
        public Response toResponse(final Throwable exception) {
            return Response.ok().entity("ProviderBall").build();
        }

        @Override
        public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("reader", "ProviderBall");
            return context.proceed();
        }

        @Override
        public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("writer", "ProviderBall");

            context.proceed();
        }
    }

    public static class SupportedProvidersDynamicFeature implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            context.register(ProviderBall.class);
            context.register(new ContainerRequestFilter() {
                @Override
                public void filter(final ContainerRequestContext requestContext) throws IOException {
                    requestContext.getHeaders().add("foo", "bar");
                }
            });
            //noinspection unchecked
            context.register(new CustomResponseFilter(), MessageBodyReader.class);
        }
    }

    @Test
    public void testSupportedProvidersFeature() throws Exception {
        final ApplicationHandler application = createApplication(SupportedProvidersDynamicFeature.class);

        ContainerResponse response;

        response = application.apply(RequestContextBuilder.from("/resource/providers", "POST").entity("get").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());
        assertEquals("ProviderBall", response.getHeaderString("writer"));
    }

    public static class CustomException extends RuntimeException {

        public CustomException(final String error) {
            super(error);
        }
    }

    @Test
    public void testNegativeSupportedProvidersFeature() throws Exception {
        final ApplicationHandler application = createApplication(SupportedProvidersDynamicFeature.class);

        try {
            application.apply(RequestContextBuilder.from("/resource/providers/error", "GET").build()).get();
        } catch (Exception e) {
            while (!(e instanceof CustomException)) {
                e = (Exception) e.getCause();
            }
            assertEquals("error", e.getMessage());
        }
    }

    public static class InjectConfigurableProvider implements ContainerResponseFilter {

        private final Configuration configuration;

        @Inject
        public InjectConfigurableProvider(final Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {

            assertNotNull(configuration);
            assertEquals("bar", configuration.getProperty("foo"));
            assertEquals("world", configuration.getProperty("hello"));
        }
    }

    public static class InjectConfigurableDynamicFeature implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            context.register(InjectConfigurableProvider.class);
            context.property("foo", "bar");

            assertEquals("world", context.getConfiguration().getProperty("hello"));
        }
    }

    @Test
    public void testInjectedConfigurable() throws Exception {
        Assumptions.assumeTrue(Hk2InjectionManagerFactory.isImmediateStrategy());

        final ResourceConfig resourceConfig = getTestResourceConfig(InjectConfigurableDynamicFeature.class);
        resourceConfig.property("hello", "world");

        final ApplicationHandler application = createApplication(resourceConfig);

        assertNull(application.getConfiguration().getProperty("foo"));

        final ContainerResponse response = application.apply(RequestContextBuilder.from("/resource", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("get", response.getEntity());

        assertNull(application.getConfiguration().getProperty("foo"));
        assertEquals("world", application.getConfiguration().getProperty("hello"));
    }

    private ApplicationHandler createApplication(final Class<?>... dynamicFeatures) {
        return createApplication(getTestResourceConfig(dynamicFeatures));
    }

    private ResourceConfig getTestResourceConfig(final Class<?>... dynamicFeatures) {
        return new ResourceConfig()
                .registerClasses(Resource.class, SubResource.class)
                .registerClasses(dynamicFeatures);
    }

    private ApplicationHandler createApplication(final ResourceConfig resourceConfig) {
        return new ApplicationHandler(resourceConfig);
    }
}
