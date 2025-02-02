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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of fix for issues JERSEY-1088 and JERSEY-1089.
 *
 * @author Marek Potociar
 */
public class AsyncContentAndEntityTypeTest {

    private ApplicationHandler createApplication(Class<?>... classes) {
        return new ApplicationHandler(new ResourceConfig(classes));
    }

    @Path("/")
    public static class AsyncResource {
        static BlockingQueue<AsyncResponse> ctxQueue = new ArrayBlockingQueue<AsyncResponse>(1);

        @Produces("application/foo")
        @GET
        public void getFoo(@Suspended AsyncResponse ar) throws InterruptedException {
            AsyncResource.ctxQueue.put(ar);
        }

        @POST
        @Consumes("application/foo")
        public void postFoo(String foo) throws InterruptedException {
            AsyncResource.ctxQueue.take().resume(foo);
        }
    }

    @Test
    public void testAsyncContentType() throws Exception {
        final ApplicationHandler app = createApplication(AsyncResource.class);

        MediaType foo = MediaType.valueOf("application/foo");

        Future<ContainerResponse> responseFuture =
                Executors.newFixedThreadPool(1).submit(new Callable<ContainerResponse>() {
                    @Override
                    public ContainerResponse call() throws Exception {
                        return app.apply(RequestContextBuilder.from("/", "GET").accept("*/*").build()).get();
                    }
                });


        ContainerResponse response;
        // making sure the JVM optimization does not swap the order of the calls.
        synchronized (this) {
            app.apply(RequestContextBuilder.from("/", "POST").entity("Foo").build());
            response = responseFuture.get();
        }

        assertTrue(response.getStatus() < 300, "Status: " + response.getStatus());
        assertEquals("Foo", response.getEntity());
        assertEquals(foo, response.getMediaType());

        final GenericType stringType = new GenericType(String.class);
        assertEquals(stringType.getRawType(), response.getEntityClass());
        assertEquals(stringType.getType(), response.getEntityType());
    }

}
