/*
 * Copyright (c) 2011, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.Priority;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for JAX-RS filters.
 *
 * @author Pavel Bucek
 * @author Marek Potociar
 */
public class ApplicationFilterTest {

    /**
     * Utility Injection binder that may be used for registering provider instances of provider
     * type {@code T} in HK2.
     */
    static class ProviderInstanceBindingBinder<T> extends AbstractBinder {

        private final Iterable<? extends T> providers;
        private final Class<T> providerType;

        /**
         * Create an injection binder for the supplied collection of provider instances.
         *
         * @param providers list of provider instances.
         * @param providerType registered provider contract type.
         */
        public ProviderInstanceBindingBinder(final Iterable<? extends T> providers, final Class<T> providerType) {
            this.providers = providers;
            this.providerType = providerType;
        }

        @Override
        protected void configure() {
            for (final T provider : providers) {
                bind(provider).to(providerType);
            }
        }
    }

    @Test
    public void testSingleRequestFilter() throws Exception {

        final AtomicInteger called = new AtomicInteger(0);

        final List<ContainerRequestFilter> requestFilters = new ArrayList<>();
        requestFilters.add(new ContainerRequestFilter() {
            @Override
            public void filter(final ContainerRequestContext context) throws IOException {
                called.incrementAndGet();
            }
        });

        final ResourceConfig resourceConfig = new ResourceConfig()
                .register(new ProviderInstanceBindingBinder<>(requestFilters, ContainerRequestFilter.class));

        final Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(final ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        resourceConfig.registerResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(RequestContextBuilder.from("/test", "GET").build()).get().getStatus());
        assertEquals(1, called.intValue());
    }

    @Test
    public void testSingleResponseFilter() throws Exception {
        final AtomicInteger called = new AtomicInteger(0);

        final List<ContainerResponseFilter> responseFilterList = new ArrayList<>();
        responseFilterList.add(new ContainerResponseFilter() {
            @Override
            public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                    throws IOException {
                called.incrementAndGet();
            }
        });

        final ResourceConfig resourceConfig = new ResourceConfig()
                .register(new ProviderInstanceBindingBinder<>(responseFilterList, ContainerResponseFilter.class));

        final Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(final ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        resourceConfig.registerResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(RequestContextBuilder.from("/test", "GET").build()).get().getStatus());
        assertEquals(1, called.intValue());
    }

    @Test
    public void testFilterCalledOn200() throws Exception {
        final SimpleFilter simpleFilter = new SimpleFilter();
        final ResourceConfig resourceConfig = new ResourceConfig(SimpleResource.class).register(simpleFilter);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/simple", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertTrue(simpleFilter.called);
    }

    @Test
    public void testFilterNotCalledOn404() throws Exception {
        // not found
        final SimpleFilter simpleFilter = new SimpleFilter();
        final ResourceConfig resourceConfig = new ResourceConfig(SimpleResource.class).register(simpleFilter);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/NOT-FOUND", "GET").build()).get();
        assertEquals(404, response.getStatus());
        Assertions.assertFalse(simpleFilter.called);
    }

    @Test
    public void testFilterNotCalledOn405() throws Exception {
        // method not allowed
        final SimpleFilter simpleFilter = new SimpleFilter();
        final ResourceConfig resourceConfig = new ResourceConfig(SimpleResource.class).register(simpleFilter);
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        final ContainerResponse response = application.apply(RequestContextBuilder.from("/simple", "POST").entity("entity")
                .build()).get();
        assertEquals(405, response.getStatus());
        Assertions.assertFalse(simpleFilter.called);
    }

    @Path("simple")
    public static class SimpleResource {

        @GET
        public String get() {
            return "get";
        }
    }

    public abstract class CommonFilter implements ContainerRequestFilter {

        public boolean called = false;

        @Override
        public void filter(final ContainerRequestContext context) throws IOException {
            verify();
            called = true;
        }

        protected abstract void verify();
    }

    public class SimpleFilter extends CommonFilter {

        @Override
        protected void verify() {
        }
    }

    @Priority(1)
    public class Filter1 extends CommonFilter {

        private Filter10 filter10;
        private Filter100 filter100;

        public void setFilters(final Filter10 filter10, final Filter100 filter100) {
            this.filter10 = filter10;
            this.filter100 = filter100;
        }

        @Override
        protected void verify() {
            assertTrue(filter10.called == false);
            assertTrue(filter100.called == false);
        }
    }

    @Priority(10)
    public class Filter10 extends CommonFilter {

        private Filter1 filter1;
        private Filter100 filter100;

        public void setFilters(final Filter1 filter1, final Filter100 filter100) {
            this.filter1 = filter1;
            this.filter100 = filter100;
        }

        @Override
        protected void verify() {
            assertTrue(filter1.called == true);
            assertTrue(filter100.called == false);
        }
    }

    @Priority(100)
    public class Filter100 extends CommonFilter {

        private Filter1 filter1;
        private Filter10 filter10;

        public void setFilters(final Filter1 filter1, final Filter10 filter10) {
            this.filter1 = filter1;
            this.filter10 = filter10;
        }

        @Override
        protected void verify() {
            assertTrue(filter1.called);
            assertTrue(filter10.called);
        }
    }

    @Test
    public void testMultipleFiltersWithBindingPriority() throws Exception {

        final Filter1 filter1 = new Filter1();
        final Filter10 filter10 = new Filter10();
        final Filter100 filter100 = new Filter100();

        filter1.setFilters(filter10, filter100);
        filter10.setFilters(filter1, filter100);
        filter100.setFilters(filter1, filter10);

        final List<ContainerRequestFilter> requestFilterList = new ArrayList<>();
        requestFilterList.add(filter100);
        requestFilterList.add(filter1);
        requestFilterList.add(filter10);

        final ResourceConfig resourceConfig = new ResourceConfig()
                .register(new ProviderInstanceBindingBinder<>(requestFilterList, ContainerRequestFilter.class));

        final Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(final ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        resourceConfig.registerResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);

        assertEquals(200, application.apply(RequestContextBuilder.from("/test", "GET").build()).get().getStatus());
    }

    public class ExceptionFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext context) throws IOException {
            throw new IOException("test");
        }
    }

    @Test
    public void testFilterExceptionHandling() throws Exception {

        final List<ContainerRequestFilter> requestFilterList = new ArrayList<>();
        requestFilterList.add(new ExceptionFilter());

        final ResourceConfig resourceConfig = new ResourceConfig()
                .register(new ProviderInstanceBindingBinder<>(requestFilterList, ContainerRequestFilter.class));

        final Resource.Builder rb = Resource.builder("test");
        rb.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(final ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        resourceConfig.registerResources(rb.build());
        final ApplicationHandler application = new ApplicationHandler(resourceConfig);
        try {
            application.apply(RequestContextBuilder.from("/test", "GET").build()).get().getStatus();
            Assertions.fail("should throw an exception");
        } catch (final Exception e) {
            // ok
        }
    }
}
