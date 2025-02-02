/*
 * Copyright (c) 2015, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector.internal;

import java.net.CookieManager;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdk.connector.JdkConnectorProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.CLOSED;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.CONNECTING;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.CONNECT_TIMEOUT;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.ERROR;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.IDLE;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.IDLE_TIMEOUT;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.RECEIVED;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.RECEIVING_BODY;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.RECEIVING_HEADER;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.RESPONSE_TIMEOUT;
import static org.glassfish.jersey.jdk.connector.internal.HttpConnection.State.SENDING_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Petr Janouch
 */
public class HttpConnectionTest extends JerseyTest {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Throwable testError = new Throwable();

    @AfterAll
    public static void cleanUp() {
        scheduler.shutdownNow();
    }

    @Test
    public void testBasic() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, RECEIVING_BODY, RECEIVED, IDLE};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        doTest(ERROR_STATE.NONE, expectedStates, request);
    }

    @Test
    public void testMultipleRequests() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, RECEIVING_BODY, RECEIVED, IDLE, SENDING_REQUEST, RECEIVING_HEADER, RECEIVING_BODY, RECEIVED,
                IDLE};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        doTest(ERROR_STATE.NONE, expectedStates, request, request);
    }

    @Test
    public void testErrorSending() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST, ERROR, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        doTest(ERROR_STATE.SENDING, expectedStates, request);
    }

    @Test
    public void testErrorReceiving() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, ERROR, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        doTest(ERROR_STATE.RECEIVING_HEADER, expectedStates, request);
    }

    @Test
    public void testTimeoutConnecting() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, CONNECT_TIMEOUT, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        ConnectorConfiguration configuration = new ConnectorConfiguration(client(), client().getConfiguration()) {
            @Override
            int getConnectTimeout() {
                return 100;
            }
        };
        doTest(ERROR_STATE.LOST_CONNECT, configuration, expectedStates, request);
    }

    @Test
    public void testResponseTimeout() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, RESPONSE_TIMEOUT, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        ConnectorConfiguration configuration = new ConnectorConfiguration(client(), client().getConfiguration()) {

            @Override
            int getResponseTimeout() {
                return 100;
            }
        };

        doTest(ERROR_STATE.LOST_REQUEST, configuration, expectedStates, request);
    }

    @Test
    public void testIdleTimeout() {
        HttpConnection.State[] expectedStates = new HttpConnection.State[] {CONNECTING, IDLE, SENDING_REQUEST,
                RECEIVING_HEADER, RECEIVING_BODY, RECEIVED, IDLE, IDLE_TIMEOUT, CLOSED};
        HttpRequest request = HttpRequest.createBodyless("GET", target("hello").getUri());
        ConnectorConfiguration configuration = new ConnectorConfiguration(client(), client().getConfiguration()) {

            @Override
            int getConnectionIdleTimeout() {
                return 500;
            }
        };

        doTest(ERROR_STATE.NONE, configuration, expectedStates, request);
    }

    private void doTest(ERROR_STATE errorState,
                        ConnectorConfiguration configuration,
                        HttpConnection.State[] expectedStates,
                        HttpRequest... httpRequests) {
        CountDownLatch latch = new CountDownLatch(1);
        TestStateListener stateListener = new TestStateListener(expectedStates, latch, httpRequests);
        HttpConnection connection = createConnection(httpRequests[0].getUri(), stateListener, errorState, configuration);
        connection.connect();

        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (Throwable t) {
            // continue
        }

        assertEquals(Arrays.asList(expectedStates), stateListener.getObservedStates());

        if (errorState == ERROR_STATE.SENDING || errorState == ERROR_STATE.CONNECTING
                || errorState == ERROR_STATE.RECEIVING_HEADER) {
            assertTrue(testError == connection.getError());
        }
    }

    private void doTest(ERROR_STATE errorState, HttpConnection.State[] expectedStates, HttpRequest... httpRequests) {
        ConnectorConfiguration configuration = new ConnectorConfiguration(client(), client().getConfiguration());
        doTest(errorState, configuration, expectedStates, httpRequests);
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(EchoResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.property(JdkConnectorProperties.CONNECTION_IDLE_TIMEOUT, 30_000);
        config.connectorProvider(new JdkConnectorProvider());
    }

    @Path("/hello")
    public static class EchoResource {

        @GET
        public String getHello() {
            return "Hello";
        }
    }

    private HttpConnection createConnection(URI uri,
                                            TestStateListener stateListener,
                                            final ERROR_STATE errorState,
                                            ConnectorConfiguration configuration) {
        return new HttpConnection(uri, new CookieManager(), configuration, scheduler, stateListener) {
            @Override
            protected Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> createFilterChain(URI uri,
                                                                                                     ConnectorConfiguration
                                                                                                             configuration) {
                Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> filterChain = super
                        .createFilterChain(uri, configuration);
                return new InterceptorFilter(filterChain, errorState);
            }
        };
    }

    private static class TestStateListener implements HttpConnection.StateChangeListener {

        private final List<HttpConnection.State> observedStates = new ArrayList<>();
        private final HttpRequest[] httpRequests;
        private final AtomicInteger sentRequests = new AtomicInteger(0);
        private final CountDownLatch latch;
        private final Queue<HttpConnection.State> expectedStates;

        public TestStateListener(HttpConnection.State[] expectedStates, CountDownLatch latch, HttpRequest... httpRequests) {
            this.httpRequests = httpRequests;
            this.latch = latch;
            this.expectedStates = new LinkedList<>(Arrays.asList(expectedStates));
        }

        @Override
        public void onStateChanged(HttpConnection connection, HttpConnection.State oldState, HttpConnection.State newState) {
            System.out.printf("Connection [%s] state change: %s -> %s\n", connection, oldState, newState);

            observedStates.add(newState);

            HttpConnection.State expectedState = expectedStates.poll();
            if (expectedState != newState) {
                latch.countDown();
            }

            if (newState == IDLE && httpRequests.length > sentRequests.get()) {
                connection.send(httpRequests[sentRequests.get()]);
                sentRequests.incrementAndGet();
            }

            if (expectedStates.peek() == null) {
                latch.countDown();
            }
        }

        public List<HttpConnection.State> getObservedStates() {
            return observedStates;
        }
    }

    private static class InterceptorFilter extends Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> {

        private final ERROR_STATE errorState;

        InterceptorFilter(Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> downstreamFilter, ERROR_STATE errroState) {
            super(downstreamFilter);
            this.errorState = errroState;
        }

        @Override
        void write(HttpRequest data, final CompletionHandler<HttpRequest> completionHandler) {
            if (errorState == ERROR_STATE.LOST_REQUEST) {
                completionHandler.completed(data);
                return;
            }

            if (errorState == ERROR_STATE.SENDING) {
                completionHandler.failed(testError);
                return;
            }

            if (errorState == ERROR_STATE.RECEIVING_HEADER) {
                downstreamFilter.write(data, new CompletionHandler<HttpRequest>() {
                    @Override
                    public void completed(HttpRequest result) {
                        completionHandler.completed(result);
                    }
                });
                downstreamFilter.onError(testError);
                return;
            }

            downstreamFilter.write(data, completionHandler);
        }

        @Override
        void connect(SocketAddress address, Filter<?, ?, HttpRequest, HttpResponse> upstreamFilter) {
            if (errorState == ERROR_STATE.LOST_CONNECT) {
                return;
            }

            if (errorState == ERROR_STATE.CONNECTING) {
                return;
            }

            super.connect(address, upstreamFilter);
        }
    }

    private enum ERROR_STATE {
        NONE,
        CONNECTING,
        SENDING,
        RECEIVING_HEADER,
        LOST_REQUEST,
        LOST_CONNECT
    }
}
