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

package org.glassfish.jersey.server.modelapi.annotation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import jakarta.inject.Inject;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jakub Podlesak
 */
public class IntrospectionModellerTest {

    static String data;

    @Path("/helloworld")
    @Produces(" a/b, c/d ")
    @Consumes({"e/f,g/h", " i/j"})
    public static class HelloWorldResource {

        @POST
        @Consumes(" a/b, c/d ")
        @Produces({"e/f,g/h", " i/j"})
        public String postA(final String data) {
            return data;
        }

        @POST
        public String postB(final String data) {
            return data;
        }
    }

    @Path("/other/{path}")
    public static class OtherResource {

        private String queryParam;

        public String getQueryParam() {
            return queryParam;
        }

        @QueryParam("q")
        public void setQueryParam(final String queryParam) {
            this.queryParam = queryParam;
        }

        @PathParam("path")
        private String pathParam;

        public String getPathParam() {
            return pathParam;
        }

        public void setPathParam(final String pathParam) {
            this.pathParam = pathParam;
        }

        @Context
        UriInfo uriInfo;

        @Inject
        public String annotatedButNotParam;

        @Inject
        public void setAnnotatedButNotParam(final String annotatedButNotParam) {
            this.annotatedButNotParam = annotatedButNotParam;
        }

        @GET
        public String get(@HeaderParam("h") String headerParam) {
            return headerParam + data;
        }

        @POST
        public String post(final String data) {
            return data;
        }
    }

    public IntrospectionModellerTest() {
    }

    @Test
    /**
     * Test of createResource method, of class IntrospectionModeller.
     */
    public void testCreateResource() {
        Class<?> resourceClass;
        Resource result;
        List<ResourceMethod> resourceMethods;
        ResourceMethod resourceMethod;

        // HelloWorldResource
        resourceClass = HelloWorldResource.class;

        result = Resource.builder(resourceClass).build();
        resourceMethods = result.getResourceMethods();
        assertEquals(2, resourceMethods.size(), "Unexpected number of resource methods in the resource model.");

        resourceMethod = find(resourceMethods, "postA");
        assertEquals(3, resourceMethod.getProducedTypes().size(),
                "Unexpected number of produced media types in the resource method model");
        assertEquals(2, resourceMethod.getConsumedTypes().size(),
                "Unexpected number of consumed media types in the resource method model");
        assertEquals(0, resourceMethod.getInvocable().getHandler().getParameters().size(),
                "Unexpected number of handler parameters");

        resourceMethod = find(resourceMethods, "postB");
        assertEquals(2, resourceMethod.getProducedTypes().size(),
                "Unexpected number of inherited produced media types in the resource method model");
        assertEquals(3, resourceMethod.getConsumedTypes().size(),
                "Unexpected number of inherited consumed media types in the resource method model");
        assertEquals(0, resourceMethod.getInvocable().getHandler().getParameters().size(),
                "Unexpected number of handler parameters");

        // OtherResource
        resourceClass = OtherResource.class;

        result = Resource.builder(resourceClass).build();
        resourceMethods = result.getResourceMethods();
        assertEquals(2, resourceMethods.size(), "Unexpected number of resource methods in the resource model.");

        resourceMethod = find(resourceMethods, "get");
        assertEquals(0, resourceMethod.getProducedTypes().size(),
                "Unexpected number of produced media types in the resource method model");
        assertEquals(0, resourceMethod.getConsumedTypes().size(),
                "Unexpected number of consumed media types in the resource method model");
        assertEquals(5, resourceMethod.getInvocable().getHandler().getParameters().size(),
                "Unexpected number of handler parameters");
        assertSources(resourceMethod.getInvocable().getHandler().getParameters(),
                Parameter.Source.CONTEXT,
                Parameter.Source.PATH,
                Parameter.Source.QUERY,
                Parameter.Source.UNKNOWN, // @Inject on field
                Parameter.Source.UNKNOWN);  // @Inject on setter

        resourceMethod = find(resourceMethods, "post");
        assertEquals(0, resourceMethod.getProducedTypes().size(),
                "Unexpected number of inherited produced media types in the resource method model");
        assertEquals(0, resourceMethod.getConsumedTypes().size(),
                "Unexpected number of inherited consumed media types in the resource method model");
        assertEquals(5, resourceMethod.getInvocable().getHandler().getParameters().size(),
                "Unexpected number of handler parameters");
        assertSources(resourceMethod.getInvocable().getHandler().getParameters(),
                Parameter.Source.CONTEXT,
                Parameter.Source.PATH,
                Parameter.Source.QUERY,
                Parameter.Source.UNKNOWN, // @Inject on field
                Parameter.Source.UNKNOWN);  // @Inject on setter
    }

    private ResourceMethod find(List<ResourceMethod> methods, String javaMethodName) {
        for (ResourceMethod method : methods) {
            if (method.getInvocable().getHandlingMethod().getName().equals(javaMethodName)) {
                return method;
            }
        }

        return null;
    }

    private void assertSources(Collection<Parameter> parameters, Parameter.Source... sources) {
        assertThat("Expected sources not found in the collection",
                parameters.stream().map(Parameter::getSource).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(sources)
        );
    }
}
