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

package org.glassfish.jersey.client;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;

import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity test for {@link Invocation.Builder#rx()} methods.
 *
 * @author Pavel Bucek
 */
public class ClientRxTest {

    private static final ExecutorService EXECUTOR_SERVICE = new ClientRxExecutorServiceWrapper(
            Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("rxTest-%d").build())
    );

    private final Client CLIENT;
    private final Client CLIENT_WITH_EXECUTOR;

    public ClientRxTest() {
        CLIENT = ClientBuilder.newClient();
        CLIENT_WITH_EXECUTOR = ClientBuilder.newBuilder().executorService(EXECUTOR_SERVICE).build();
    }

    @AfterEach
    public void afterTest() {
        CLIENT.close();
        CLIENT_WITH_EXECUTOR.close();
    }

    @AfterAll
    public static void afterClass() {
        EXECUTOR_SERVICE.shutdownNow();
    }

    @Test
    public void testRxInvoker() {
        // explicit register is not necessary, but it can be used.
        CLIENT.register(TestRxInvokerProvider.class, RxInvokerProvider.class);

        String s = target(CLIENT).request().rx(TestRxInvoker.class).get();

        assertTrue(s.startsWith("rxTestInvoker"), "Provided RxInvoker was not used.");
    }

    @Test
    public void testRxInvokerWithExecutor() {
        // implicit register (not saying that the contract is RxInvokerProvider).
        String s = target(CLIENT_WITH_EXECUTOR).register(TestRxInvokerProvider.class).request().rx(TestRxInvoker.class).get();

        assertTrue(s.startsWith("rxTestInvoker"), "Provided RxInvoker was not used.");
        assertTrue(s.contains("rxTest-"), "Executor Service was not passed to RxInvoker");
    }

    @Test
    public void testDefaultRxInvokerWithExecutor() throws ExecutionException, InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        ClientRequestFilter threadFilter = (f) -> { threadName.set(Thread.currentThread().getName()); };
        ClientRequestFilter abortFilter = (f) -> { f.abortWith(Response.ok().build()); };
        try (Response r = target(CLIENT_WITH_EXECUTOR)
                .register(threadFilter, 100)
                .register(abortFilter, 200)
                .request().rx().get().toCompletableFuture().get()) {

            assertEquals(200, r.getStatus());
            assertTrue(threadName.get().contains("rxTest-"), "Executor Service was not passed to RxInvoker");
        }
    }

    @Test
    public void testRxInvokerWithExecutorServiceProvider() {
        AtomicReference<String> threadName = new AtomicReference<>();
        String s = target(CLIENT)
                .register(TestRxInvokerProvider.class, 200)
                .register(TestExecutorServiceProvider.class)
                .request().rx(TestRxInvoker.class).get();

        assertTrue(s.startsWith("rxTestInvoker"), "Provided RxInvoker was not used.");
        assertTrue(s.contains("rxTest-"), "Executor Service was not passed to RxInvoker");
    }

    @Test
    public void testDefaultRxInvokerWithExecutorServiceProvider() throws ExecutionException, InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        ClientRequestFilter threadFilter = (f) -> { threadName.set(Thread.currentThread().getName()); };
        ClientRequestFilter abortFilter = (f) -> { f.abortWith(Response.ok().build()); };
        try (Response r = target(CLIENT)
                .register(threadFilter, 100)
                .register(abortFilter, 200)
                .register(TestExecutorServiceProvider.class)
                .request().rx().get().toCompletableFuture().get()) {

            assertEquals(200, r.getStatus());
            assertTrue(threadName.get().contains("rxTest-"), "Executor Service was not passed to RxInvoker");
        }
    }

    @Test
    public void testRxInvokerInvalid() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Invocation.Builder request = target(CLIENT).request();
            request.rx(null).get();
        });
        String message = exception.getMessage();
        Assertions.assertTrue(AllOf.allOf(new StringContains("null"), new StringContains("clazz")).matches(message));
    }

    @Test
    public void testRxInvokerNotRegistered() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            Invocation.Builder request = target(CLIENT).request();
            request.rx(TestRxInvoker.class).get();
        });
        String message = exception.getMessage();
        Assertions.assertTrue(AllOf.allOf(
                 new StringContains("TestRxInvoker"),
                 new StringContains("not registered"),
                 new StringContains("RxInvokerProvider"))
             .matches(message));

    }

    @Test
    public void testConnectorIsReusedWhenRx() throws ExecutionException, InterruptedException {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        HttpUrlConnectorProvider provider = new HttpUrlConnectorProvider() {
            @Override
            public Connector getConnector(Client client, Configuration config) {
                atomicInteger.incrementAndGet();
                return super.getConnector(client, config);
            }
        };

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(provider);

        ClientRequestFilter abortFilter = (f) -> { f.abortWith(Response.ok().build()); };
        Client client = ClientBuilder.newClient(clientConfig).register(abortFilter);

        AtomicReference<String> threadName = new AtomicReference<>();
        for (int cnt = 0; cnt != 5; cnt++) {
            try (Response r = target(client)
                    .request().rx().get().toCompletableFuture().get()) {

                assertEquals(200, r.getStatus());
                assertEquals(1, atomicInteger.get());
            }
        }

    }

    private WebTarget target(Client client) {
        // Uri is not relevant, the call won't be ever executed.
        return client.target("http://localhost:9999");
    }

    @Provider
    public static class TestRxInvokerProvider implements RxInvokerProvider<TestRxInvoker> {
        @Override
        public TestRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
            return new TestRxInvoker(syncInvoker, executorService);
        }

        @Override
        public boolean isProviderFor(Class<?> clazz) {
            return TestRxInvoker.class.equals(clazz);
        }
    }

    private static class TestRxInvoker extends AbstractRxInvoker<String> {

        private TestRxInvoker(SyncInvoker syncInvoker, ExecutorService executor) {
            super(syncInvoker, executor);
        }

        @Override
        public <R> String method(String name, Entity<?> entity, Class<R> responseType) {
            return "rxTestInvoker" + (getExecutorService() == null ? "" : " rxTest-");
        }

        @Override
        public <R> String method(String name, Entity<?> entity, GenericType<R> responseType) {
            return "rxTestInvoker" + (getExecutorService() == null ? "" : " rxTest-");
        }
    }

    private static class TestExecutorServiceProvider implements ExecutorServiceProvider {

        @Override
        public ExecutorService getExecutorService() {
            return EXECUTOR_SERVICE;
        }

        @Override
        public void dispose(ExecutorService executorService) {
            //@After
        }
    }

    // -----------------------------------------------------------------------------------------------------

    @Test
    public void testRxInvokerWithPriorityExecutorServiceProvider() {
        AtomicReference<String> threadName = new AtomicReference<>();
        String s = target(CLIENT)
                .register(PriorityTestRxInvokerProvider.class)
                .register(TestExecutorServiceProvider.class)
                .register(PriorityTestExecutorServiceProvider.class)
                .request().rx(PriorityTestRxInvoker.class).get();

        assertTrue(s.startsWith("PriorityTestRxInvoker"), "Provided RxInvoker was not used.");
        assertTrue(s.contains("TRUE"), "@ClientAsyncExecutor Executor Service was not passed to RxInvoker");
    }

    @ClientAsyncExecutor
    private static class PriorityTestExecutorServiceProvider extends TestExecutorServiceProvider {
        @Override
        public ExecutorService getExecutorService() {
            return new ClientRxExecutorServiceWrapper(EXECUTOR_SERVICE) {
                //new class
            };
        }
    }

    @Provider
    public static class PriorityTestRxInvokerProvider implements RxInvokerProvider<PriorityTestRxInvoker> {
        @Override
        public PriorityTestRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
            return new PriorityTestRxInvoker(syncInvoker, executorService);
        }

        @Override
        public boolean isProviderFor(Class<?> clazz) {
            return PriorityTestRxInvoker.class.equals(clazz);
        }
    }

    private static class PriorityTestRxInvoker extends AbstractRxInvoker<String> {

        private PriorityTestRxInvoker(SyncInvoker syncInvoker, ExecutorService executor) {
            super(syncInvoker, executor);
        }

        @Override
        public <R> String method(String name, Entity<?> entity, Class<R> responseType) {
            return "PriorityTestRxInvoker " + (getExecutorService() != null
                    && !ClientRxExecutorServiceWrapper.class.equals(getExecutorService().getClass())
                    && ClientRxExecutorServiceWrapper.class.isInstance(getExecutorService()) ? "TRUE" : "FALSE");
        }

        @Override
        public <R> String method(String name, Entity<?> entity, GenericType<R> responseType) {
            return method(null, null, (Class<?>) null);
        }
    }

    // -----------------------------------------------------------------------------------------------------

    /**
     * Wrap the executor service to distinguish the executor service obtained from the Injection Manager by class name
     */
    private static class ClientRxExecutorServiceWrapper implements ExecutorService {
        private final ExecutorService executorService;

        private ClientRxExecutorServiceWrapper(ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public void shutdown() {
            executorService.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return executorService.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return executorService.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return executorService.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return executorService.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return executorService.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return executorService.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return executorService.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return executorService.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            executorService.execute(command);
        }
    }
}
