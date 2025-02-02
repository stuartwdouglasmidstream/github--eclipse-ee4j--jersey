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

package org.glassfish.jersey.server;

import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.ModelProcessor;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test scope of resources enhanced by model processors.
 *
 * @author Miroslav Fuksa
 *
 */
public class ModelProcessorScopeTest {
    public static class ModelProcessorFeature implements Feature {

        @Override
        public boolean configure(FeatureContext context) {
            context.register(SimpleModelProcessor.class);
            return true;
        }

        @Priority(5000)
        private static class SimpleModelProcessor implements ModelProcessor {

            @Override
            public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration) {
                ResourceModel.Builder builder = new ResourceModel.Builder(resourceModel.getRootResources(), false);
                final Resource singletonResource = Resource.from(SingletonResource.class);
                builder.addResource(singletonResource);

                final Resource requestScopeResource = Resource.from(RequestScopeResource.class);
                builder.addResource(requestScopeResource);

                final Resource.Builder resourceBuilder = Resource.builder("instance");
                resourceBuilder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, String>() {
                    private int counter = 0;

                    @Override
                    public String apply(ContainerRequestContext containerRequestContext) {
                        return String.valueOf("Inflector:" + counter++);
                    }
                });
                final Resource instanceResource = resourceBuilder.build();

                builder.addResource(instanceResource);

                return builder.build();
            }

            @Override
            public ResourceModel processSubResource(ResourceModel subResource, Configuration configuration) {
                final Resource resource = Resource.builder().mergeWith(Resource.from(EnhancedSubResourceSingleton.class))
                        .mergeWith(Resource.from(EnhancedSubResource.class)).mergeWith(subResource.getResources().get(0)).build();

                return new ResourceModel.Builder(true).addResource(resource).build();
            }
        }

        @Path("request-scope")
        public static class RequestScopeResource {
            private int counter = 0;

            @GET
            public String get() {
                return String.valueOf("RequestScopeResource:" + counter++);
            }
        }

        @Path("singleton")
        @Singleton
        public static class SingletonResource {
            private int counter = 0;

            @GET
            public String get() {
                return String.valueOf("SingletonResource:" + counter++);
            }
        }
    }

    @Path("root")
    public static class RootResource {
        @GET
        public String get() {
            return "root";
        }

        @Path("sub-resource-singleton")
        public Class<SubResourceSingleton> getSubResourceSingleton() {
            return SubResourceSingleton.class;
        }

        @Path("sub-resource-instance")
        public SubResourceSingleton getSubResourceSingletonInstance() {
            return new SubResourceSingleton();
        }
    }

    @Singleton
    public static class SubResourceSingleton {
        private int counter = 0;

        @GET
        public String get() {
            return String.valueOf("SubResourceSingleton:" + counter++);
        }
    }

    public static class EnhancedSubResource {
        private int counter = 0;

        @GET
        @Path("enhanced")
        public String get() {
            return String.valueOf("EnhancedSubResource:" + counter++);
        }
    }

    @Singleton
    public static class EnhancedSubResourceSingleton {
        private int counter = 0;

        @GET
        @Path("enhanced-singleton")
        public String get() {
            return "EnhancedSubResourceSingleton:" + String.valueOf(counter++);
        }
    }

    @Path("root-singleton")
    @Singleton
    public static class RootSingletonResource {
        private int counter = 0;

        @GET
        public String get() {
            return "RootSingletonResource:" + String.valueOf(counter++);
        }
    }

    private void _testCounter(ApplicationHandler applicationHandler, String requestUri, final String prefix,
                              final String expectedSecondHit) throws
            InterruptedException, ExecutionException {
        ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from(requestUri,
                "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals(prefix + ":0", response.getEntity());
        response = applicationHandler.apply(RequestContextBuilder.from(requestUri,
                "GET").build()).get();
        assertEquals(prefix + ":" + expectedSecondHit, response.getEntity());
    }

    @Test
    public void testSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(ModelProcessorFeature
                .SingletonResource.class));
        final String requestUri = "/singleton";
        _testCounter(applicationHandler, requestUri, "SingletonResource", "1");
    }

    @Test
    public void testSingletonInModelProcessor() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/singleton";
        _testCounter(applicationHandler, requestUri, "SingletonResource", "1");
    }

    @Test
    public void testSubResourceSingletonInOriginalModel() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/root/sub-resource-singleton";
        _testCounter(applicationHandler, requestUri, "SubResourceSingleton", "1");
    }

    @Test
    public void testSubResourceEnhancedSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/root/sub-resource-singleton/enhanced-singleton";
        _testCounter(applicationHandler, requestUri, "EnhancedSubResourceSingleton", "1");
    }

    @Test
    public void testSubResourceInstanceEnhancedSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/root/sub-resource-instance/enhanced-singleton";
        _testCounter(applicationHandler, requestUri, "EnhancedSubResourceSingleton", "1");
    }

    @Test
    public void testSubResourceInstanceEnhancedSubResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/root/sub-resource-instance/enhanced";
        _testCounter(applicationHandler, requestUri, "EnhancedSubResource", "0");
    }


    @Test
    public void testSubResourceEnhancedSubResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/root/sub-resource-singleton/enhanced";
        _testCounter(applicationHandler, requestUri, "EnhancedSubResource", "0");
    }


    @Test
    public void testInstanceInModelProcessor() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/instance";
        _testCounter(applicationHandler, requestUri, "Inflector", "1");
    }


    @Test
    public void testRootSingleton() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                RootSingletonResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/root-singleton";
        _testCounter(applicationHandler, requestUri, "RootSingletonResource", "1");
    }

    @Test
    public void testRequestScopeResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = new ApplicationHandler(new ResourceConfig(RootResource.class,
                RootSingletonResource.class,
                ModelProcessorFeature.class));
        final String requestUri = "/request-scope";
        _testCounter(applicationHandler, requestUri, "RequestScopeResource", "0");
    }


}
