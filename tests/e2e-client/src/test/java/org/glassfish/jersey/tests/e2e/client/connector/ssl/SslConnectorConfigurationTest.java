/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.client.connector.ssl;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SSL connector tests.
 *
 * @author Pavel Bucek
 * @author Arul Dhesiaseelan (aruld at acm.org)
 * @author Marek Potociar
 */
public class SslConnectorConfigurationTest extends AbstractConnectorServerTest {

    /**
     * Test to see that the correct Http status is returned.
     *
     * @throws Exception in case of a test failure.
     */
    @ParameterizedTest
    @MethodSource("testData")
    public void testSSLWithAuth(ConnectorProvider connectorProvider) throws Exception {
        final SSLContext sslContext = getSslContext();

        final ClientConfig cc = new ClientConfig().connectorProvider(connectorProvider);
        final Client client = ClientBuilder.newBuilder()
                .withConfig(cc)
                .sslContext(sslContext)
                .build();

        // client basic auth demonstration
        client.register(HttpAuthenticationFeature.basic("user", "password"));
        final WebTarget target = client.target(Server.BASE_URI).register(LoggingFeature.class);

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(200, response.getStatus());
    }

    /**
     * Test to see that HTTP 401 is returned when client tries to GET without
     * proper credentials.
     *
     * @throws Exception in case of a test failure.
     */
    @ParameterizedTest
    @MethodSource("testData")
    public void testHTTPBasicAuth1(ConnectorProvider connectorProvider) throws Exception {
        final SSLContext sslContext = getSslContext();

        final ClientConfig cc = new ClientConfig().connectorProvider(connectorProvider);
        final Client client = ClientBuilder.newBuilder()
                .withConfig(cc)
                .sslContext(sslContext)
                .build();

        final WebTarget target = client.target(Server.BASE_URI).register(LoggingFeature.class);

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(401, response.getStatus());
    }

    /**
     * Test to see that SSLHandshakeException is thrown when client don't have
     * trusted key.
     *
     * @throws Exception in case of a test failure.
     */
    @ParameterizedTest
    @MethodSource("testData")
    public void testSSLAuth1(ConnectorProvider connectorProvider) throws Exception {
        final SSLContext sslContext = getSslContext();

        final ClientConfig cc = new ClientConfig().connectorProvider(connectorProvider);
        final Client client = ClientBuilder.newBuilder()
                .withConfig(cc)
                .sslContext(sslContext)
                .build();

        WebTarget target = client.target(Server.BASE_URI).register(LoggingFeature.class);

        boolean caught = false;
        try {
            target.path("/").request().get(String.class);
        } catch (Exception e) {
            caught = true;
        }

        assertTrue(caught);
    }

    /**
     * Test that a response to an authentication challenge has the same SSL configuration as the original request.
     */
    @ParameterizedTest
    @MethodSource("testData")
    public void testSSLWithNonPreemptiveAuth(ConnectorProvider connectorProvider) throws Exception {
        final SSLContext sslContext = getSslContext();

        final ClientConfig cc = new ClientConfig().connectorProvider(connectorProvider);
        final Client client = ClientBuilder.newBuilder()
                .withConfig(cc)
                .sslContext(sslContext)
                .build();

        // client basic auth demonstration
        HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials("user", "password")
                .build();

        client.register(authFeature);
        final WebTarget target = client.target(Server.BASE_URI).register(LoggingFeature.class);

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(200, response.getStatus());
    }
}
