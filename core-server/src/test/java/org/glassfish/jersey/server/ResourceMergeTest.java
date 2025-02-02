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

package org.glassfish.jersey.server;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceTestUtils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test merging of resources and child resources.
 *
 * @author Miroslav Fuksa
 *
 */
public class ResourceMergeTest {

    @Path("a")
    public static class ResourceA {
        @GET
        public String get() {
            return "get";
        }


        @GET
        @Path("child")
        public String childGet() {
            return "child-get";
        }


        @Path("child")
        public ResourceB getLocator() {
            return new ResourceB();
        }

        @GET
        @Path("child2")
        public String child2Get() {
            return "child2-get";
        }
    }


    @Path("a")
    public static class ResourceB {

        @POST
        public String post() {
            return "post";
        }

        @POST
        @Path("child")
        public String childPost() {
            return "child-post";
        }
    }

    @Path("different-path")
    public static class ResourceC {

        @POST
        public String post() {
            return "post";
        }

        @PUT
        @Path("child")
        public String childPut() {
            return "child-put";
        }

        @Path("locator")
        public ResourceA locator() {
            return new ResourceA();
        }
    }


    @Test
    public void testResourceMerge() {
        final List<Resource> rootResources = createRootResources();
        assertEquals(2, rootResources.size());

        final Resource resourceC = ResourceTestUtils.getResource(rootResources, "different-path");
        ResourceTestUtils.containsExactMethods(resourceC, false, "POST");

        final Resource resourceAB = ResourceTestUtils.getResource(rootResources, "a");
        ResourceTestUtils.containsExactMethods(resourceAB, false, "POST", "GET");
    }

    private List<Resource> createRootResources() {
        final Resource resourceA = Resource.from(ResourceA.class);
        final Resource resourceB = Resource.from(ResourceB.class);
        final ResourceBag.Builder builder = new ResourceBag.Builder();
        builder.registerProgrammaticResource(resourceA);
        builder.registerProgrammaticResource(resourceB);
        builder.registerProgrammaticResource(Resource.from(ResourceC.class));
        final ResourceBag bag = builder.build();
        return bag.getRootResources();
    }


    @Test
    public void testChildResourceMerge() {
        final List<Resource> rootResources = createRootResources();
        assertEquals(2, rootResources.size());
        final Resource resourceAB = ResourceTestUtils.getResource(rootResources, "a");
        assertEquals(2, resourceAB.getChildResources().size());
        final Resource child = ResourceTestUtils.getResource(resourceAB.getChildResources(), "child");
        final Resource child2 = ResourceTestUtils.getResource(resourceAB.getChildResources(), "child2");

        ResourceTestUtils.containsExactMethods(child, true, "GET", "POST");
        ResourceTestUtils.containsExactMethods(child2, false, "GET");


        final Resource resourceC = ResourceTestUtils.getResource(rootResources, "different-path");
        final List<Resource> childResourcesC = resourceC.getChildResources();
        assertEquals(2, childResourcesC.size());
        final Resource childC1 = ResourceTestUtils.getResource(childResourcesC, "child");
        ResourceTestUtils.containsExactMethods(childC1, false, "PUT");

        final Resource childC2 = ResourceTestUtils.getResource(childResourcesC, "locator");
        ResourceTestUtils.containsExactMethods(childC2, true);

        child.getResourceMethods().size();
    }

    public static class MyInflector implements Inflector<ContainerRequestContext, Object> {


        @Override
        public Object apply(ContainerRequestContext requestContext) {
            return null;
        }
    }


    @Test
    public void programmaticTest() {
        final List<Resource> rootResources = getResourcesFromProgrammatic();

        assertEquals(1, rootResources.size());
        final Resource root = ResourceTestUtils.getResource(rootResources, "root");
        final List<Resource> childResources = root.getChildResources();
        assertEquals(2, childResources.size());
        final Resource child = ResourceTestUtils.getResource(childResources, "child");
        ResourceTestUtils.containsExactMethods(child, true, "GET", "POST", "DELETE");
        final Resource child2 = ResourceTestUtils.getResource(childResources, "child2");
        ResourceTestUtils.containsExactMethods(child2, false, "PUT");
    }

    private List<Resource> getResourcesFromProgrammatic() {
        final Resource.Builder root = Resource.builder("root");
        root.addChildResource("child").addMethod("GET").handledBy(new MyInflector());
        root.addChildResource("child").addMethod("POST").handledBy(new MyInflector());
        root.addChildResource("child2").addMethod("PUT").handledBy(new MyInflector());

        final Resource.Builder root2 = Resource.builder("root");
        root2.addChildResource("child").addMethod("DELETE").handledBy(new MyInflector());
        root2.addChildResource("child").addMethod((String) null).handledBy(new MyInflector());

        final ResourceBag.Builder builder = new ResourceBag.Builder();
        builder.registerProgrammaticResource(root.build());
        builder.registerProgrammaticResource(root2.build());
        final ResourceBag bag = builder.build();
        return bag.getRootResources();
    }


    @Test
    public void mergeTwoLocatorsTest() {
        final Resource.Builder root = Resource.builder("root");
        root.addChildResource("child").addMethod().handledBy(new MyInflector()).consumes(MediaType.APPLICATION_XML_TYPE);
        root.addChildResource("child").addMethod().handledBy(new MyInflector()).consumes(MediaType.APPLICATION_JSON_TYPE);
        final ResourceBag.Builder builder = new ResourceBag.Builder();
        try {
            builder.registerProgrammaticResource(root.build());
            final ResourceBag bag = builder.build();
            fail("Should fail - two locators on the same path.");
        } catch (Exception e) {
            // ok - should fail
        }
    }


    @Path("root/{a}")
    public static class ResourceTemplateA {
        @GET
        @Path("{q}")
        public String get() {
            return "get";
        }

        @PUT
        @Path("{q}")
        public String put() {
            return "put";
        }

        @POST
        @Path("{post}")
        public String post() {
            return "post";
        }


    }

    @Path("root/{b}")
    public static class ResourceTemplateB {
        @GET
        public String getB() {
            return "get-B";
        }
    }


    @Test
    public void testMergingOfTemplates() {
        final List<Resource> resources = createResources(ResourceTemplateA.class, ResourceTemplateB.class);
        testMergingTemplateResources(resources);
    }

    @Test
    public void testMergingOfTemplatesProgrammatic() {
        final List<Resource> resources = getResourcesTemplatesProgrammatic();
        testMergingTemplateResources(resources);
    }

    private void testMergingTemplateResources(List<Resource> resources) {
        assertEquals(2, resources.size());
        final Resource resB = ResourceTestUtils.getResource(resources, "root/{b}");
        ResourceTestUtils.containsExactMethods(resB, false, "GET");
        final Resource resA = ResourceTestUtils.getResource(resources, "root/{a}");

        assertTrue(resA.getResourceMethods().isEmpty());
        final List<Resource> childResources = resA.getChildResources();
        assertEquals(2, childResources.size());
        final Resource childQ = ResourceTestUtils.getResource(childResources, "{q}");
        ResourceTestUtils.containsExactMethods(childQ, false, "GET", "PUT");
        final Resource childPost = ResourceTestUtils.getResource(childResources, "{post}");
        ResourceTestUtils.containsExactMethods(childPost, false, "POST");
    }

    private List<Resource> createResources(Class<?>... resourceClass) {
        final ResourceBag.Builder builder = new ResourceBag.Builder();
        for (Class<?> clazz : resourceClass) {
            final Resource resource = Resource.from(clazz);
            builder.registerProgrammaticResource(resource);
        }
        final ResourceBag bag = builder.build();
        return bag.getRootResources();
    }

    private List<Resource> getResourcesTemplatesProgrammatic() {
        final Resource.Builder root = Resource.builder("root/{a}");
        root.addChildResource("{q}").addMethod("GET").handledBy(new MyInflector());
        root.addChildResource("{q}").addMethod("PUT").handledBy(new MyInflector());
        root.addChildResource("{post}").addMethod("POST").handledBy(new MyInflector());

        final Resource.Builder root2 = Resource.builder("root/{b}");
        root2.addMethod("GET").handledBy(new MyInflector());

        final ResourceBag.Builder builder = new ResourceBag.Builder();
        builder.registerProgrammaticResource(root.build());
        builder.registerProgrammaticResource(root2.build());
        final ResourceBag bag = builder.build();
        return bag.getRootResources();
    }

}



