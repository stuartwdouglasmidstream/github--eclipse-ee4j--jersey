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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Petr Janouch
 */
public class ProxyTest extends JerseyTest {

    private static final Charset CHARACTER_SET = Charset.forName("iso-8859-1");

    private static final String PROXY_HOST = "localhost";
    private static final int PROXY_PORT = 8321;
    private static final String PROXY_USER_NAME = "petr";
    private static final String PROXY_PASSWORD = "my secret password";

    @Test
    public void testConnect() throws IOException {
        doTest(Proxy.Authentication.NONE);
    }

    @Test
    public void testBasicAuthentication() throws IOException {
        doTest(Proxy.Authentication.BASIC);
    }

    @Test
    public void testDigestAuthentication() throws IOException {
        doTest(Proxy.Authentication.DIGEST);
    }

    private void doTest(Proxy.Authentication authentication) throws IOException {
        Proxy proxy = new Proxy(authentication);
        try {
            proxy.start();
            jakarta.ws.rs.core.Response response = target("resource").request().get();
            assertEquals(200, response.getStatus());
            assertEquals("OK", response.readEntity(String.class));
            assertTrue(proxy.getProxyHit());
        } finally {
            proxy.stop();
        }
    }

    @Test
    public void authenticationFailTest() throws IOException {
        Proxy proxy = new Proxy(Proxy.Authentication.BASIC);
        try {
            proxy.start();
            proxy.setAuthernticationFail(true);
            try {
                target("resource").request().get();
                fail();
            } catch (Exception e) {
                assertEquals(ProxyAuthenticationException.class, e.getCause().getClass());
            }

            assertTrue(proxy.getProxyHit());
        } finally {
            proxy.stop();
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.property(JdkConnectorProperties.MAX_CONNECTIONS_PER_DESTINATION, 1);
        config.property(ClientProperties.PROXY_URI, "http://" + PROXY_HOST + ":" + PROXY_PORT);
        config.property(ClientProperties.PROXY_USERNAME, PROXY_USER_NAME);
        config.property(ClientProperties.PROXY_PASSWORD, PROXY_PASSWORD);
        config.connectorProvider(new JdkConnectorProvider());
    }

    @Path("/resource")
    public static class Resource {

        @GET
        public String get() {
            return "OK";
        }
    }

    private static class Proxy {

        private final HttpServer server = HttpServer.createSimpleServer("/", PROXY_HOST, PROXY_PORT);
        private volatile String destinationUri = null;
        private final Authentication authentication;
        private volatile boolean proxyHit = false;
        private volatile boolean authenticationFail = false;

        Proxy(Authentication authentication) {
            this.authentication = authentication;
        }

        boolean getProxyHit() {
            return proxyHit;
        }

        void setAuthernticationFail(boolean authenticationFail) {
            this.authenticationFail = authenticationFail;
        }

        void start() throws IOException {
            server.getServerConfiguration().addHttpHandler(new HttpHandler() {
                public void service(Request request, Response response) throws Exception {
                    if (request.getMethod().getMethodString().equals("CONNECT")) {
                        proxyHit = true;

                        String authorizationHeader = request.getHeader("Proxy-Authorization");

                        if (authentication != Authentication.NONE && authorizationHeader == null) {
                            // if we need authentication and receive CONNECT with no Proxy-authorization header, send 407
                            send407(request, response);
                            return;
                        }

                        if (authenticationFail) {
                            send407(request, response);
                            return;
                        }

                        if (authentication == Authentication.BASIC) {
                            if (!verifyBasicAuthorizationHeader(response, authorizationHeader)) {
                                return;
                            }

                            // if success continue

                        } else if (authentication == Authentication.DIGEST) {
                            if (!verifyDigestAuthorizationHeader(response, authorizationHeader)) {
                                return;
                            }

                            // if success continue
                        }

                        // check that both Host header and URI contain host:port
                        String requestURI = request.getRequestURI();
                        String host = request.getHeader("Host");
                        if (!requestURI.equals(host)) {
                            response.setStatus(400);
                            System.out.println("Request URI: " + requestURI);
                            System.out.println("Host header: " + host);
                            return;
                        }

                        // save the destination where a normal proxy would open a connection
                        destinationUri = "http://" + requestURI;
                        response.setStatus(200);
                        hackGrizzlyConnect(request, response);
                        return;
                    }

                    handleTrafficAfterConnect(request, response);
                }
            });

            server.start();
        }

        private void send407(Request request, Response response) {
            response.setStatus(407);

            if (authentication == Authentication.BASIC) {
                response.setHeader("Proxy-Authenticate", "Basic");
            } else {
                response.setHeader("Proxy-Authenticate", "Digest realm=\"my-realm\", domain=\"\", "
                        + "nonce=\"n9iv3MeSNkEfM3uJt2gnBUaWUbKAljxp\", algorithm=MD5, \"\n"
                        + "                            + \"qop=\"auth\", stale=false");
            }
            hackGrizzlyConnect(request, response);
        }

        private boolean verifyBasicAuthorizationHeader(Response response, String authorizationHeader) {
            if (!authorizationHeader.startsWith("Basic")) {
                System.out.println(
                        "Authorization header during Basic authentication does not start with \"Basic\"");
                response.setStatus(400);
                return false;
            }
            String decoded = new String(Base64.getDecoder().decode(authorizationHeader.substring(6).getBytes()),
                    CHARACTER_SET);
            final String[] split = decoded.split(":");
            final String username = split[0];
            final String password = split[1];

            if (!username.equals(PROXY_USER_NAME)) {
                response.setStatus(400);
                System.out.println("Found unexpected username: " + username);
                return false;
            }

            if (!password.equals(PROXY_PASSWORD)) {
                response.setStatus(400);
                System.out.println("Found unexpected password: " + username);
                return false;
            }

            return true;
        }

        private boolean verifyDigestAuthorizationHeader(Response response, String authorizationHeader) {
            if (!authorizationHeader.startsWith("Digest")) {
                System.out.println(
                        "Authorization header during Digest authentication does not start with \"Digest\"");
                response.setStatus(400);
                return false;
            }

            final Matcher match = Pattern.compile("username=\"([^\"]+)\"").matcher(authorizationHeader);
            if (!match.find()) {
                return false;
            }
            final String username = match.group(1);
            if (!username.equals(PROXY_USER_NAME)) {
                response.setStatus(400);
                System.out.println("Found unexpected username: " + username);
                return false;
            }

            return true;
        }

        private void hackGrizzlyConnect(Request request, Response response) {
            // Grizzly does not like CONNECT method and sets keep alive to false
            // This hacks Grizzly, so it will keep the connection open
            response.getResponse().getProcessingState().setKeepAlive(true);
            response.getResponse().setContentLength(0);
            request.setMethod("GET");
        }

        private void handleTrafficAfterConnect(Request request, Response response) throws IOException {
            if (destinationUri == null) {
                // It seems that CONNECT has not been called
                System.out.println("Received non-CONNECT without receiving CONNECT first");
                response.setStatus(400);
                return;
            }

            // create a client and relay the request to the final destination
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.connectorProvider(new JdkConnectorProvider());
            Client client = ClientBuilder.newClient(clientConfig);

            Invocation.Builder destinationRequest = client.target(destinationUri).path(request.getRequestURI()).request();
            for (String headerName : request.getHeaderNames()) {
                destinationRequest.header(headerName, request.getHeader(headerName));
            }

            jakarta.ws.rs.core.Response destinationResponse = destinationRequest
                    .method(request.getMethod().getMethodString());

            // translate the received response into the proxy response
            response.setStatus(destinationResponse.getStatus());
            OutputStream outputStream = response.getOutputStream();
            String body = destinationResponse.readEntity(String.class);
            outputStream.write(body.getBytes());
            client.close();
        }

        void stop() {
            server.shutdown();
        }

        private enum Authentication {
            NONE,
            BASIC,
            DIGEST
        }
    }
}
