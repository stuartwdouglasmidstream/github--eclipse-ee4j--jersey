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

package org.glassfish.jersey.server.model.internal;

import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.container.ContainerRequestContext;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.uri.PathPattern;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Miroslav Fuksa
 *
 */
public class ChildResourceTest {

    @Test
    public void testRootResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = createApplication();
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/root", "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("root-get", response.getEntity());
    }

    @Test
    public void testChildResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = createApplication();
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/root/child",
                "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("sub-get", response.getEntity());
    }

    @Test
    public void testAnotherChildResource() throws ExecutionException, InterruptedException {
        ApplicationHandler applicationHandler = createApplication();
        final ContainerResponse response = applicationHandler.apply(RequestContextBuilder.from("/root/another-child",
                "GET").build()).get();
        assertEquals(200, response.getStatus());
        assertEquals("another-child-get", response.getEntity());
    }

    private ApplicationHandler createApplication() {
        final Resource.Builder rootBuilder = Resource.builder("root");

        rootBuilder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, String>() {
            @Override
            public String apply(ContainerRequestContext requestContext) {
                return "root-get";
            }
        });

        rootBuilder.addChildResource("child").addMethod("GET").handledBy(new Inflector<ContainerRequestContext, String>() {
            @Override
            public String apply(ContainerRequestContext requestContext) {
                return "sub-get";
            }
        }).build();

        Resource.Builder anotherChildBuilder = Resource.builder("another-child");
        anotherChildBuilder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, String>() {
            @Override
            public String apply(ContainerRequestContext requestContext) {
                return "another-child-get";
            }
        });
        rootBuilder.addChildResource(anotherChildBuilder.build());



        Resource resource = rootBuilder.build();
        ResourceConfig resourceConfig = new ResourceConfig().registerResources(resource);

        return new ApplicationHandler(resourceConfig);
    }


    @Test
    public void test() {
        process("http://localhost/{adas}/aa/f");
        process("http://localhost/{aaa}/aa/f");


    }

    private void process(String str) {
        PathPattern pattern = new PathPattern(str);

        System.out.println("template: " + pattern.getTemplate().toString());
        System.out.println("pattern: " + pattern.toString());
    }


}
