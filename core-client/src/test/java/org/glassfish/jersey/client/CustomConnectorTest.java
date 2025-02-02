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

package org.glassfish.jersey.client;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.concurrent.Future;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Pavel Bucek
 */
public class CustomConnectorTest {

    public static class NullConnector implements Connector, ConnectorProvider {

        @Override
        public ClientResponse apply(ClientRequest request) {
            throw new ProcessingException("test");
        }

        @Override
        public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
            throw new ProcessingException("test-async");
        }

        @Override
        public void close() {
            // do nothing
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Connector getConnector(Client client, Configuration runtimeConfig) {
            return this;
        }
    }

    @Test
    public void testNullConnector() {
        Client client = ClientBuilder.newClient(new ClientConfig().connectorProvider(new NullConnector()).getConfiguration());
        try {
            client.target(UriBuilder.fromUri("/").build()).request().get();
        } catch (ProcessingException ce) {
            assertEquals("test", ce.getMessage());
        }
        try {
            client.target(UriBuilder.fromUri("/").build()).request().async().get();
        } catch (ProcessingException ce) {
            assertEquals("test-async", ce.getMessage());
        }
    }

    /**
     * Loop-back connector provider.
     */
    public static class TestConnectorProvider implements ConnectorProvider {

        @Override
        public Connector getConnector(Client client, Configuration runtimeConfig) {
            return new TestConnector();
        }

    }

    /**
     * Loop-back connector.
     */
    public static class TestConnector implements Connector {
        /**
         * Test loop-back status code.
         */
        public static final int TEST_LOOPBACK_CODE = 600;
        /**
         * Test loop-back status type.
         */
        public final Response.StatusType LOOPBACK_STATUS = new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return TEST_LOOPBACK_CODE;
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.OTHER;
            }

            @Override
            public String getReasonPhrase() {
                return "Test connector loop-back";
            }
        };

        private volatile boolean closed = false;

        @Override
        public ClientResponse apply(ClientRequest request) {
            checkNotClosed();
            final ClientResponse response = new ClientResponse(LOOPBACK_STATUS, request);

            response.setEntityStream(new ByteArrayInputStream(request.getUri().toString().getBytes()));
            return response;
        }

        @Override
        public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
            checkNotClosed();
            throw new UnsupportedOperationException("Async invocation not supported by the test connector.");
        }

        @Override
        public String getName() {
            return "test-loop-back-connector";
        }

        @Override
        public void close() {
            closed = true;
        }

        private void checkNotClosed() {
            if (closed) {
                throw new IllegalStateException("Connector closed.");
            }
        }
    }

    /**
     * Test client request filter that creates new client based on the current runtime configuration
     * and uses the new client to produce a response.
     */
    public static class TestClientFilter implements ClientRequestFilter {

        private static final String INVOKED_BY_TEST_FILTER = "invoked-by-test-filter";

        @Override
        public void filter(ClientRequestContext requestContext) {
            final Configuration config = requestContext.getConfiguration();
            final JerseyClient client = new JerseyClientBuilder().withConfig(config).build();

            try {
                if (requestContext.getPropertyNames().contains(INVOKED_BY_TEST_FILTER)) {
                    return; // prevent the infinite recursion...
                }

                final URI filteredUri = UriBuilder.fromUri(requestContext.getUri()).path("filtered").build();
                requestContext.abortWith(client.target(filteredUri).request().property(INVOKED_BY_TEST_FILTER, true).get());
            } finally {
                client.close();
            }
        }
    }

    /**
     * Reproducer for JERSEY-2318.
     *
     * The test verifies that the {@link org.glassfish.jersey.client.spi.ConnectorProvider} configured
     * on one client instance is transferred to another client instance when the new client instance is
     * created from the original client instance configuration.
     */
    @Test
    public void testConnectorProviderPreservedOnClientConfigCopy() {
        final ClientConfig clientConfig = new ClientConfig().connectorProvider(new TestConnectorProvider());

        final Client client = ClientBuilder.newClient(clientConfig);
        try {
            Response response;

            final WebTarget target = client.target("http://wherever.org/");
            response = target.request().get();
            // let's first verify we are using the test loop-back connector.
            assertThat(response.getStatus(), equalTo(TestConnector.TEST_LOOPBACK_CODE));
            assertThat(response.readEntity(String.class), equalTo("http://wherever.org/"));

            // and now with the filter...
            target.register(TestClientFilter.class);
            response = target.request().get();
            // check if the connector provider has been propagated:
            assertThat(response.getStatus(), equalTo(TestConnector.TEST_LOOPBACK_CODE));
            // check if the filter has been invoked:
            assertThat(response.readEntity(String.class), equalTo("http://wherever.org/filtered"));
        } finally {
            client.close();
        }
    }

}
