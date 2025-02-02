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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.PerLookup;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Class testing Resources managed as singletons.
 *
 * @author Miroslav Fuksa
 */
public class SingletonResourceTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(
                SingletonResource.class,
                ChildInheritsParentAnnotation.class,
                ChildImplementsInterfaceAnnotation.class,
                TestResource.class,
                RequestScopeResource.class,
                PerLookupScopeResource.class,
                SingletonScopeResource.class);

        final Resource.Builder resourceBuilder1 = Resource.builder();
        resourceBuilder1.name("resource-programmatic/instance/").path("programmatic/instance/").addMethod("GET")
                .handledBy(new Inflector<ContainerRequestContext, Response>() {
                    private int counter = 1;

                    @Override
                    public Response apply(ContainerRequestContext data) {
                        return Response.ok("prg-instance:" + counter++).build();
                    }
                });
        resourceConfig.registerResources(resourceBuilder1.build());

        final Resource.Builder resourceBuilder2 = Resource.builder();
        resourceBuilder2.name("resource-programmatic/singleton/").path("programmatic/singleton/").addMethod("GET")
                .handledBy(SingletonProgrammatic.class);
        resourceConfig.registerResources(resourceBuilder2.build());

        final Resource.Builder resourceBuilder3 = Resource.builder();
        resourceBuilder3.name("resource-programmatic/reused-singleton/").path("programmatic/reused-singleton/").addMethod("GET")
                .handledBy(SubResourceSingleton.class);
        resourceConfig.registerResources(resourceBuilder3.build());

        final Resource.Builder resourceBuilder4 = Resource.builder();
        resourceBuilder4.name("resource-programmatic/not-singleton/").path("programmatic/not-singleton/").addMethod("GET")
                .handledBy(NotSingletonProgrammatic.class);
        resourceConfig.registerResources(resourceBuilder4.build());

        return resourceConfig;
    }

    @Test
    public void singletonResourceTest() {
        String str;
        str = target().path("singleton").request().get().readEntity(String.class);
        assertEquals("res:1", str);

        str = target().path("singleton").request().get().readEntity(String.class);
        assertEquals("res:2", str);

        str = target().path("singleton/sub").request().get().readEntity(String.class);
        assertEquals("sub:1", str);

        str = target().path("singleton").request().get().readEntity(String.class);
        assertEquals("res:3", str);

        str = target().path("singleton/sub").request().get().readEntity(String.class);
        assertEquals("sub:2", str);

        str = target().path("singleton/sub-not-singleton").request().get().readEntity(String.class);
        assertEquals("not-singleton:1", str);

        str = target().path("singleton/sub-not-singleton").request().get().readEntity(String.class);
        assertEquals("not-singleton:1", str);

        str = target().path("singleton/instance").request().get().readEntity(String.class);
        assertEquals("sub:1", str);

        str = target().path("singleton/instance").request().get().readEntity(String.class);
        assertEquals("sub:1", str);

        str = target().path("singleton/sub").request().get().readEntity(String.class);
        assertEquals("sub:3", str);

        // one instance
        str = target().path("programmatic").path("instance").request().get().readEntity(String.class);
        assertEquals("prg-instance:1", str);

        str = target().path("programmatic").path("instance").request().get().readEntity(String.class);
        assertEquals("prg-instance:2", str);

        // singleton
        str = target().path("programmatic").path("singleton").request().get().readEntity(String.class);
        assertEquals("prg-singleton:1", str);

        str = target().path("programmatic").path("singleton").request().get().readEntity(String.class);
        assertEquals("prg-singleton:2", str);

        // request to the SubResourceSingleton (same class as sub resource on path "singleton/sub")
        str = target().path("programmatic").path("reused-singleton").request().get().readEntity(String.class);
        assertEquals("reused-singleton:4", str);

        // not singleton
        str = target().path("programmatic").path("not-singleton").request().get().readEntity(String.class);
        assertEquals("prg-not-singleton:1", str);

        str = target().path("programmatic").path("not-singleton").request().get().readEntity(String.class);
        assertEquals("prg-not-singleton:1", str);
    }

    @Test
    public void singletonAnnotationInheritedTest() {
        // Singleton annotation is not inherited
        String str;
        str = target().path("inherit").request().get().readEntity(String.class);
        assertEquals("inherit:1", str);

        str = target().path("inherit").request().get().readEntity(String.class);
        assertEquals("inherit:1", str);
    }

    @Test
    public void singletonAnnotationInterfaceTest() {
        // Singleton annotation is not inherited
        String str;
        str = target().path("interface").request().get().readEntity(String.class);
        assertEquals("interface:1", str);

        str = target().path("interface").request().get().readEntity(String.class);
        assertEquals("interface:1", str);

    }

    /**
     * Tests that resources are by default managed in {@link org.glassfish.jersey.process.internal.RequestScope request scope}.
     */
    @Test
    public void testResourceInRequestScope() {
        String str = target().path("testScope/request").request().get().readEntity(String.class);
        assertEquals("same-instances", str);
    }

    @Test
    public void testResourceInPerLookupScope() {
        String str = target().path("testScope/perlookup").request().get().readEntity(String.class);
        assertEquals("different-instances", str);
    }

    @Test
    public void testResourceInSingletonScope() {
        String str = target().path("testScope/singleton").request().get().readEntity(String.class);
        assertEquals("same-instances", str);
    }

    @Path("test-requestScope")
    public static class RequestScopeResource {

        public String get() {
            return "get";
        }
    }

    @Path("test-perlookupScope")
    @PerLookup
    public static class PerLookupScopeResource {

        public String get() {
            return "get";
        }
    }

    @Path("test-singletonScope")
    @Singleton
    public static class SingletonScopeResource {

        public String get() {
            return "get";
        }
    }

    @Path("testScope")
    public static class TestResource {

        @Inject
        InjectionManager injectionManager;

        private String compareInstances(Class<?> clazz) {
            final Object res1 = Injections.getOrCreate(injectionManager, clazz);
            final Object res2 = Injections.getOrCreate(injectionManager, clazz);
            return (res1 == res2) ? "same-instances" : "different-instances";
        }

        @GET
        @Path("request")
        public String compareRequestScopedInstances() {
            return compareInstances(RequestScopeResource.class);
        }

        @GET
        @Path("perlookup")
        public String comparePerLookupScopedInstances() {
            return compareInstances(PerLookupScopeResource.class);
        }

        @GET
        @Path("singleton")
        public String compareSingletonInstances() {
            return compareInstances(SingletonScopeResource.class);
        }
    }

    @Singleton
    public static class Parent {

    }

    @Path("inherit")
    public static class ChildInheritsParentAnnotation extends Parent {

        private int counter = 1;

        @GET
        public String get() {
            return "inherit:" + counter++;
        }
    }

    @Singleton
    public static interface AnnotatedBySingleton {
    }

    @Path("interface")
    public static class ChildImplementsInterfaceAnnotation implements AnnotatedBySingleton {

        private int counter = 1;

        @GET
        public String get() {
            return "interface:" + counter++;
        }
    }

    @Singleton
    public static class SingletonProgrammatic implements Inflector<Request, Response> {

        private int counter = 1;

        @Override
        public Response apply(Request data) {
            return Response.ok("prg-singleton:" + counter++).build();
        }

    }

    public static class NotSingletonProgrammatic implements Inflector<Request, Response> {

        private int counter = 1;

        @Override
        public Response apply(Request data) {
            return Response.ok("prg-not-singleton:" + counter++).build();
        }

    }

    @Singleton
    @Path("singleton")
    public static class SingletonResource {

        private int counter = 1;

        @GET
        @Produces("text/html")
        public String getCounter() {
            return "res:" + (counter++);
        }

        @Path("sub")
        public Class getSubResourceSingleton() {
            return SubResourceSingleton.class;
        }

        @Path("sub-not-singleton")
        public Class getSubResource() {
            return SubResource.class;
        }

        @Path("instance")
        public Object getSubResourceInstance() {
            return new SubResourceSingleton();
        }

        @GET
        @Path("filter")
        public String getCounterFromFilter(@HeaderParam("counter") int counter) {
            return "filter:" + counter;
        }

    }

    @Singleton
    public static class SubResourceSingleton implements Inflector<Request, Response> {

        private int counter = 1;

        @GET
        public String getInternalCounter() {
            return "sub:" + (counter++);
        }

        @Override
        public Response apply(Request request) {
            return Response.ok("reused-singleton:" + counter++).build();
        }
    }

    public static class SubResource {

        private int counter = 1;

        @GET
        public String getInternalCounter() {
            return "not-singleton:" + (counter++);
        }
    }
}
