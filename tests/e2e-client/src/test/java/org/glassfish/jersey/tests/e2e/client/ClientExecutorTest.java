/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.MessageBodyReader;

import org.glassfish.jersey.client.ClientAsyncExecutor;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Pavel Bucek
 */
public class ClientExecutorTest extends JerseyTest {

    @Path("ClientExecutorTest")
    public static class ClientExecutorTestResource {

        @GET
        @Produces("text/plain")
        public String get() {
            return "get";
        }
    }


    @Override
    protected Application configure() {
        return new ResourceConfig(ClientExecutorTestResource.class);
    }

    private volatile StringBuilder threadName;
    private volatile CountDownLatch latch;

    @BeforeEach
    public void setUpThreadNameHolder() {
        threadName = new StringBuilder();
        latch = new CountDownLatch(1);
    }

    @Test
    public void testDefaultExecutorRx() throws InterruptedException {
        Client client = ClientBuilder.newClient();
        testRx(client, latch);

        latch.await(3, TimeUnit.SECONDS);
        assertThat(threadName.toString(), containsString("jersey-client-async-executor"));
    }

    @Test
    public void testDefaultExecutorAsync() throws InterruptedException {
        Client client = ClientBuilder.newClient();
        testAsync(client, latch);

        latch.await(3, TimeUnit.SECONDS);
        assertThat(threadName.toString(), containsString("jersey-client-async-executor"));
    }

    @Test
    public void testJerseyCustomExecutorAsync() throws InterruptedException {
        Client client = ClientBuilder.newClient();
        client.register(MyExecutorProvider.class);

        testAsync(client, latch);

        latch.await(3, TimeUnit.SECONDS);
        assertThat(threadName.toString(), containsString("MyExecutorProvider"));
    }

    @Test
    public void testCustomExecutorRx() throws InterruptedException {
        ExecutorService clientExecutor =
                Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ClientExecutor-%d").build());

        Client client = ClientBuilder.newBuilder().executorService(clientExecutor).build();
        testRx(client, latch);

        latch.await(3, TimeUnit.SECONDS);
        assertThat(threadName.toString(), containsString("ClientExecutor"));
    }


    private void testRx(Client client, CountDownLatch latch) {
        client.target(UriBuilder.fromUri(getBaseUri()).path("ClientExecutorTest"))
              .register(new ClientExecutorTestReader(threadName, latch))
              .request()
              .rx()
              .get(ClientExecutorTest.class);
    }

    private void testAsync(Client client, CountDownLatch latch) {
        client.target(UriBuilder.fromUri(getBaseUri()).path("ClientExecutorTest"))
              .register(new ClientExecutorTestReader(threadName, latch))
              .request()
              .async()
              .get(ClientExecutorTest.class);
    }

    @ClientAsyncExecutor
    public static class MyExecutorProvider implements ExecutorServiceProvider {

        public final ExecutorService executorService =
                Executors
                        .newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("MyExecutorProvider-%d").build());

        @Override
        public ExecutorService getExecutorService() {
            return executorService;
        }

        @Override
        public void dispose(ExecutorService executorService) {
            executorService.shutdown();
        }
    }

    public static class ClientExecutorTestReader implements MessageBodyReader<ClientExecutorTest> {
        private final StringBuilder threadName;
        private final CountDownLatch countDown;

        public ClientExecutorTestReader(StringBuilder threadName, CountDownLatch countDown) {
            this.threadName = threadName;
            this.countDown = countDown;
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return true;
        }

        @Override
        public ClientExecutorTest readFrom(Class<ClientExecutorTest> type, Type genericType,
                                           Annotation[] annotations,
                                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                           InputStream entityStream) throws IOException, WebApplicationException {

            threadName.append(Thread.currentThread().getName());
            countDown.countDown();

            return new ClientExecutorTest();

        }
    }
}
