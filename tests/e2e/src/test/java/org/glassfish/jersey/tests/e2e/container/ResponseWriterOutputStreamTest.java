/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.container;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestHelper;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is really weird approach and test.
 *
 * @author Michal Gajdos
 */
public class ResponseWriterOutputStreamTest {

    private static final String CHECK_STRING = "RESOURCE";

    @Path("/")
    public static class Resource {

        @GET
        @Produces("text/plain")
        public void get(final ContainerRequest context) throws IOException {
            assertThat(context.getMethod(), is("GET"));

            final ContainerResponse response = new ContainerResponse(context, Response.ok().build());
            final OutputStream os = context.getResponseWriter()
                    .writeResponseStatusAndHeaders(CHECK_STRING.getBytes().length, response);
            os.write(CHECK_STRING.getBytes());
            os.close();
        }

        @POST
        @Produces("text/plain")
        public void post(final ContainerRequest context) throws IOException {
            assertThat(context.getMethod(), is("POST"));

            final String s = context.readEntity(String.class);
            assertEquals(CHECK_STRING, s);

            final ContainerResponse response = new ContainerResponse(context, Response.ok().build());
            final OutputStream os = context.getResponseWriter()
                    .writeResponseStatusAndHeaders(s.getBytes().length, response);
            os.write(s.getBytes());
            os.close();
        }
    }

    @TestFactory
    public Collection<DynamicContainer> generateTests() {
        Collection<DynamicContainer> tests = new ArrayList<>();
        JerseyContainerTest.parameters().forEach(testContainerFactory -> {
            ResponseWriterOutputStreamTemplateTest test = new ResponseWriterOutputStreamTemplateTest(testContainerFactory) {};
            tests.add(TestHelper.toTestContainer(test, testContainerFactory.getClass().getSimpleName()));
        });
        return tests;
    }

    public abstract static class ResponseWriterOutputStreamTemplateTest extends JerseyContainerTest {

        public ResponseWriterOutputStreamTemplateTest(TestContainerFactory testContainerFactory) {
            super(testContainerFactory);
        }

        @Override
        protected Application configure() {
            return new ResourceConfig(Resource.class);
        }

        @Test
        public void testGet() {
            assertThat(target().request().get(String.class), is(CHECK_STRING));
        }

        @Test
        public void testPost() throws InterruptedException, ExecutionException {
            final CountDownLatch controlLatch = new CountDownLatch(1);
            final Invocation invocation = target().request().buildPost(Entity.text(CHECK_STRING));
            final Future<Response> resp = invocation.submit(new InvocationCallback<Response>() {
                @Override
                public void completed(Response response) {
                    controlLatch.countDown();
                }

                @Override
                public void failed(Throwable throwable) {
                    controlLatch.countDown();

                }
            });
            controlLatch.await();
            final String response = resp.get().readEntity(String.class);
            assertThat(response, is(CHECK_STRING));
        }

        @Test
        public void testAll() throws InterruptedException, ExecutionException {
            testGet();
            testPost();
        }
    }
}
