/*
 * Copyright (c) 2015, 2023 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdkhttp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Jdk Https Server tests.
 *
 * @author Adam Lindenthal
 */
public class JdkHttpsServerTest extends AbstractJdkHttpServerTester {

    private static final String TRUSTSTORE_CLIENT_FILE = "./truststore_client";
    private static final String TRUSTSTORE_CLIENT_PWD = "asdfgh";
    private static final String KEYSTORE_CLIENT_FILE = "./keystore_client";
    private static final String KEYSTORE_CLIENT_PWD = "asdfgh";

    private static final String KEYSTORE_SERVER_FILE = "./keystore_server";
    private static final String KEYSTORE_SERVER_PWD = "asdfgh";
    private static final String TRUSTSTORE_SERVER_FILE = "./truststore_server";
    private static final String TRUSTSTORE_SERVER_PWD = "asdfgh";

    private final ResourceConfig rc = new ResourceConfig(TestResource.class);

    @Path("/testHttps")
    public static class TestResource {
        @GET
        public String get() {
            return "test";
        }
    }

    /**
     * Test, that {@link HttpsServer} instance is returned when providing empty SSLContext (but not starting).
     * @throws Exception
     */
    @Test
    public void testCreateHttpsServerNoSslContext() throws Exception {
        HttpServer server = startServer(getHttpsUri(), rc, null, false);
        assertInstanceOf(HttpsServer.class, server);
    }

    /**
     * Test, that exception is thrown when attempting to start a {@link HttpsServer} with empty SSLContext.
     * @throws Exception
     */
    @Test
    public void testStartHttpServerNoSslContext() throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> startServer(getHttpsUri(), rc, null, true));
    }

    /**
     * Test, that {@link javax.net.ssl.SSLHandshakeException} is thrown when attempting to connect to server with client
     * not configured correctly.
     * @throws Exception
     */
    @Test
    public void testCreateHttpsServerDefaultSslContext() throws Throwable {
        assertThrows(SSLHandshakeException.class, () -> {
            final HttpServer server = startServer(getHttpsUri(), rc, SSLContext.getDefault(), true);
            assertInstanceOf(HttpsServer.class, server);

            // access the https server with not configured client
            final Client client = ClientBuilder.newBuilder().newClient();
            try {
                client.target(getHttpsUri()).path("testHttps").request().get(String.class);
            } catch (final ProcessingException e) {
                throw e.getCause();
            }
        });
    }

    /**
     * Test, that {@link HttpsServer} can be manually started even with (empty) SSLContext, but will throw an exception
     * on request.
     * @throws Exception
     */
    @Test
    public void testHttpsServerNoSslContextDelayedStart() throws Throwable {
        assertThrows(IOException.class, () -> {
            final HttpServer server = startServer(getHttpsUri(), rc, null, false);
            assertInstanceOf(HttpsServer.class, server);
            server.start();

            final Client client = ClientBuilder.newBuilder().newClient();
           try {
                client.target(getHttpsUri()).path("testHttps").request().get(String.class);
            } catch (final ProcessingException e) {
                throw e.getCause();
            }
        });
    }

    /**
     * Test, that {@link HttpsServer} cannot be configured with {@link HttpsConfigurator} after it has started.
     * @throws Exception
     */
    @Test
    public void testConfigureSslContextAfterStart() throws Throwable {
        assertThrows(IllegalStateException.class, () -> {
            final HttpServer server = startServer(getHttpsUri(), rc, null, false);
            assertInstanceOf(HttpsServer.class, server);
            server.start();
            ((HttpsServer) server).setHttpsConfigurator(new HttpsConfigurator(getServerSslContext()));
        });
    }

    /**
     * Tests a client to server roundtrip with correctly configured SSL on both sides.
     * @throws IOException
     */
    @Test
    public void testCreateHttpsServerRoundTrip() throws IOException {
        final SSLContext serverSslContext = getServerSslContext();

        HttpServer server = startServer(getHttpsUri(), rc, serverSslContext, true);

        final SSLContext foundContext = ((HttpsServer) server).getHttpsConfigurator().getSSLContext();
        assertEquals(serverSslContext, foundContext);

        final SSLContext clientSslContext = getClientSslContext();
        final Client client = ClientBuilder.newBuilder().sslContext(clientSslContext).build();
        final String response = client.target(UriBuilder.fromUri("https://localhost/").port(getPort())).path("testHttps").request().get(String.class);

        assertEquals("test", response);
    }

    /**
     * Test, that if URI uses http scheme instead of https, SSLContext is ignored.
     * @throws IOException
     */
    @Test
    public void testHttpWithSsl() throws IOException {
        HttpServer server = startServer(getBaseUri(), rc, getServerSslContext(), true);
        assertInstanceOf(HttpServer.class, server);
        assertThat(server, not(instanceOf(HttpsServer.class)));
    }

    private SSLContext getClientSslContext() throws IOException {
        final InputStream trustStore = JdkHttpsServerTest.class.getResourceAsStream(TRUSTSTORE_CLIENT_FILE);
        final InputStream keyStore = JdkHttpsServerTest.class.getResourceAsStream(KEYSTORE_CLIENT_FILE);


        final SslConfigurator sslConfigClient = SslConfigurator.newInstance()
                .trustStoreBytes(IOUtils.toByteArray(trustStore))
                .trustStorePassword(TRUSTSTORE_CLIENT_PWD)
                .keyStoreBytes(IOUtils.toByteArray(keyStore))
                .keyPassword(KEYSTORE_CLIENT_PWD);

        return sslConfigClient.createSSLContext();
    }

    private SSLContext getServerSslContext() throws IOException {
        final InputStream trustStore = JdkHttpsServerTest.class.getResourceAsStream(TRUSTSTORE_SERVER_FILE);
        final InputStream keyStore = JdkHttpsServerTest.class.getResourceAsStream(KEYSTORE_SERVER_FILE);

        final SslConfigurator sslConfigServer = SslConfigurator.newInstance()
                .keyStoreBytes(IOUtils.toByteArray(keyStore))
                .keyPassword(KEYSTORE_SERVER_PWD)
                .trustStoreBytes(IOUtils.toByteArray(trustStore))
                .trustStorePassword(TRUSTSTORE_SERVER_PWD);

        return sslConfigServer.createSSLContext();
    }

    private URI getHttpsUri() {
        return UriBuilder.fromUri("https://localhost/").port(getPort()).build();
    }
}