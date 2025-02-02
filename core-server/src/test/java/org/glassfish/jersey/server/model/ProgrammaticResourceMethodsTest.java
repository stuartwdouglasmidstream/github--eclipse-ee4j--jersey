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

package org.glassfish.jersey.server.model;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test of programmatic resource method additions.
 *
 * @author Pavel Bucek
 * @author Marek Potociar
 */
public class ProgrammaticResourceMethodsTest {

    @Test
    public void testGet() throws Exception {
        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("test");
        resourceBuilder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        rc.registerResources(resourceBuilder.build());
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatus(RequestContextBuilder.from("/test", "GET").build(), application);
    }

    @Test
    public void testHead() throws Exception {
        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("test");
        resourceBuilder.addMethod("HEAD").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        rc.registerResources(resourceBuilder.build());
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatus(RequestContextBuilder.from("/test", "HEAD").build(), application);
    }

    @Test
    public void testOptions() throws Exception {
        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("test");
        resourceBuilder.addMethod("OPTIONS").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.ok().build();
            }
        });
        rc.registerResources(resourceBuilder.build());
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatus(RequestContextBuilder.from("/test", "OPTIONS").build(), application);
    }

    @Test
    public void testMultiple() throws Exception {
        Inflector<ContainerRequestContext, Response> inflector = new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.ok().build();
            }
        };

        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("test");

        resourceBuilder.addMethod("GET").handledBy(inflector);
        resourceBuilder.addMethod("OPTIONS").handledBy(inflector);
        resourceBuilder.addMethod("HEAD").handledBy(inflector);

        rc.registerResources(resourceBuilder.build());
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatus(RequestContextBuilder.from("/test", "GET").build(), application);
        checkReturnedStatus(RequestContextBuilder.from("/test", "HEAD").build(), application);
        checkReturnedStatus(RequestContextBuilder.from("/test", "OPTIONS").build(), application);
    }

    @Test
    public void testTwoBindersSamePath() throws Exception {
        final ResourceConfig rc = new ResourceConfig();
        final Resource.Builder resourceBuilder = Resource.builder("/");
        final Resource.Builder childTest1Builder = resourceBuilder.addChildResource("test1");
        childTest1Builder.addMethod("GET").handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.created(URI.create("/foo")).build();
            }
        });
        Inflector<ContainerRequestContext, Response> inflector1 = new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.accepted().build();
            }
        };
        final Resource.Builder childTest2Builder = resourceBuilder.addChildResource("test2");
        childTest2Builder.addMethod("GET").handledBy(inflector1);
        childTest2Builder.addMethod("HEAD").handledBy(inflector1);
        Inflector<ContainerRequestContext, Response> inflector2 = new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                return Response.status(203).build();
            }
        };
        childTest1Builder.addMethod("OPTIONS").handledBy(inflector2);
        childTest1Builder.addMethod("HEAD").handledBy(inflector2);
        final Resource resource = resourceBuilder.build();
        rc.registerResources(resource);
        final ApplicationHandler application = new ApplicationHandler(rc);

        checkReturnedStatusEquals(201, RequestContextBuilder.from("/test1", "GET").build(), application);
//        checkReturnedStatusEquals(203, Requests.from("/test1", "HEAD").build(), application);
//        checkReturnedStatusEquals(203, Requests.from("/test1", "OPTIONS").build(), application);

//        checkReturnedStatusEquals(202, Requests.from("/test2", "GET").build(), application);
//        checkReturnedStatusEquals(202, Requests.from("/test2", "HEAD").build(), application);
//        checkReturnedStatusEquals(202, Requests.from("/test2", "OPTIONS").build(), application);
    }

    @Test
    public void testConsumesProduces() {
        final Resource.Builder builder = Resource.builder("root");
        builder.addMethod("POST").handledBy(new Inflector
                <ContainerRequestContext, Object>() {

            @Override
            public Object apply(ContainerRequestContext requestContext) {
                return null;
            }
        }).consumes(MediaType.APPLICATION_XML).consumes("text/html").consumes("text/plain",
                "application/json").produces(MediaType.TEXT_HTML_TYPE,
                MediaType.APPLICATION_JSON_TYPE);
        final Resource res = builder.build();
        final ResourceMethod method = res.getResourceMethods().get(0);
        final List<MediaType> consumedTypes = method.getConsumedTypes();
        assertEquals(4, consumedTypes.size());
        final List<MediaType> producedTypes = method.getProducedTypes();
        assertEquals(2, producedTypes.size());
        assertTrue(consumedTypes.contains(MediaType.APPLICATION_XML_TYPE));
        assertTrue(consumedTypes.contains(MediaType.TEXT_HTML_TYPE));
        assertTrue(consumedTypes.contains(MediaType.TEXT_PLAIN_TYPE));
        assertTrue(consumedTypes.contains(MediaType.APPLICATION_JSON_TYPE));
        assertTrue(producedTypes.contains(MediaType.TEXT_HTML_TYPE));
        assertTrue(producedTypes.contains(MediaType.APPLICATION_JSON_TYPE));
    }

    private void checkReturnedStatus(ContainerRequest req, ApplicationHandler app) throws Exception {
        checkReturnedStatusEquals(200, req, app);
    }

    private void checkReturnedStatusEquals(int expectedStatus, ContainerRequest req, ApplicationHandler app)
            throws Exception {
        final int responseStatus = app.apply(req).get().getStatus();
        assertEquals(expectedStatus, responseStatus);
    }
}
