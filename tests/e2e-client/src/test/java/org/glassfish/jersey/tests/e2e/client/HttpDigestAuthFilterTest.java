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

package org.glassfish.jersey.tests.e2e.client;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.uri.UriComponent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Pavel Bucek
 * @author Stefan Katerkamp <stefan at katerkamp.de>
 */
public class HttpDigestAuthFilterTest extends JerseyTest {

    private static final String DIGEST_TEST_LOGIN = "user";
    private static final String DIGEST_TEST_PASS = "password";
    private static final String DIGEST_TEST_INVALIDPASS = "nopass";
    // Digest string expected for OK auth:
    // Digest realm="test", nonce="eDePFNeJBAA=a874814ec55647862b66a747632603e5825acd39",
    //   algorithm=MD5, domain="/auth-digest/", qop="auth"
    private static final String DIGEST_TEST_NONCE = "eDePFNeJBAA=a874814ec55647862b66a747632603e5825acd39";
    private static final String DIGEST_TEST_REALM = "test";
    private static final String DIGEST_TEST_DOMAIN = "/auth-digest/";
    private static int ncExpected = 1;

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(Resource.class);
    }

    @Path("/auth-digest")
    public static class Resource {

        @Context
        private HttpHeaders httpHeaders;
        @Context
        private UriInfo uriInfo;

        @GET
        public Response get1() {
            return verify();
        }


        @GET
        @Path("ěščřžýáíé")
        public Response getEncoding() {
            return verify();
        }

        private Response verify() {
            if (httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION) == null) {
                // the first request has no authorization header, tell filter its 401
                // and send filter back seed for the new to be built header
                ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);
                responseBuilder = responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE,
                        "Digest realm=\"" + DIGEST_TEST_REALM + "\", "
                                + "nonce=\"" + DIGEST_TEST_NONCE + "\", "
                                + "algorithm=MD5, "
                                + "domain=\"" + DIGEST_TEST_DOMAIN + "\", qop=\"auth\"");
                return responseBuilder.build();
            } else {
                // the filter takes the seed and adds the header
                final List<String> authList = httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION);
                if (authList.size() != 1) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                }
                final String authHeader = authList.get(0);

                final String ha1 = md5(DIGEST_TEST_LOGIN, DIGEST_TEST_REALM, DIGEST_TEST_PASS);
                final String requestUri = UriComponent.fullRelativeUri(uriInfo.getRequestUri());
                final String ha2 = md5("GET", requestUri.startsWith("/") ? requestUri : "/" + requestUri);
                final String response = md5(
                        ha1,
                        DIGEST_TEST_NONCE,
                        getDigestAuthHeaderValue(authHeader, "nc="),
                        getDigestAuthHeaderValue(authHeader, "cnonce="),
                        getDigestAuthHeaderValue(authHeader, "qop="),
                        ha2);

                // this generates INTERNAL_SERVER_ERROR if not matching
                Assertions.assertEquals(ncExpected, Integer.parseInt(getDigestAuthHeaderValue(authHeader, "nc=")));

                if (response.equals(getDigestAuthHeaderValue(authHeader, "response="))) {
                    return Response.ok().build();
                } else {
                    return Response.status(Response.Status.UNAUTHORIZED).build();
                }
            }
        }

        private static final Charset CHARACTER_SET = Charset.forName("iso-8859-1");

        /**
         * Colon separated value MD5 hash. Call md5 method of the filter.
         *
         * @param tokens one or more strings
         * @return M5 hash string
         */
        static String md5(final String... tokens) {
            final StringBuilder sb = new StringBuilder(100);
            for (final String token : tokens) {
                if (sb.length() > 0) {
                    sb.append(':');
                }
                sb.append(token);
            }

            final MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (final NoSuchAlgorithmException ex) {
                throw new ProcessingException(ex.getMessage());
            }
            md.update(sb.toString().getBytes(CHARACTER_SET), 0, sb.length());
            final byte[] md5hash = md.digest();
            return bytesToHex(md5hash);
        }

        /**
         * Convert bytes array to hex string.
         *
         * @param bytes array of bytes
         * @return hex string
         */
        private static String bytesToHex(final byte[] bytes) {
            final char[] hexChars = new char[bytes.length * 2];
            int v;
            for (int j = 0; j < bytes.length; j++) {
                v = bytes[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            return new String(hexChars);
        }

        private static final char[] HEX_ARRAY = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


        /**
         * Get a value of the Digest Auth Header.
         *
         * @param authHeader digest auth header string
         * @param keyName key of the value to retrieve
         * @return value string
         */
        static String getDigestAuthHeaderValue(final String authHeader, final String keyName) {
            final int i1 = authHeader.indexOf(keyName);

            if (i1 == -1) {
                return null;
            }

            String value = authHeader.substring(
                    authHeader.indexOf('=', i1) + 1,
                    (authHeader.indexOf(',', i1) != -1
                            ? authHeader.indexOf(',', i1) : authHeader.length()));

            value = value.trim();
            if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                value = value.substring(1, value.length() - 1);
            }

            return value;
        }
    }

    @Test
    public void testHttpDigestAuthFilter() {
        testRequest("auth-digest");
    }

    @Test
    public void testHttpDigestAuthFilterWithEncodedUri() {
        testRequest("auth-digest/ěščřžýáíé");
    }

    @Test
    public void testHttpDigestAuthFilterWithParams() {
        testRequest("auth-digest", true);
    }

    @Test
    public void testHttpDigestAuthFilterWithEncodedUriAndParams() {
        testRequest("auth-digest/ěščřžýáíé", true);
    }

    private void testRequest(final String path) {
        testRequest(path, false);
    }

    private void testRequest(final String path, final boolean addParams) {
        WebTarget resource = target()
                .register(HttpAuthenticationFeature.digest(DIGEST_TEST_LOGIN, DIGEST_TEST_PASS))
                .path(path);

        if (addParams) {
            resource = resource.matrixParam("bar", "foo").queryParam("foo", "bar");
        }

        ncExpected = 1;
        final Response r1 = resource.request().get();
        Assertions.assertEquals(Response.Status.fromStatusCode(r1.getStatus()), Response.Status.OK);
    }


    @Test
    public void testPreemptive() {
        final WebTarget resource = target()
                .register(HttpAuthenticationFeature.digest(DIGEST_TEST_LOGIN, DIGEST_TEST_PASS))
                .path("auth-digest");

        ncExpected = 1;
        final Response r1 = resource.request().get();
        Assertions.assertEquals(Response.Status.fromStatusCode(r1.getStatus()), Response.Status.OK);

        ncExpected = 2;
        final Response r2 = resource.request().get();
        Assertions.assertEquals(Response.Status.fromStatusCode(r2.getStatus()), Response.Status.OK);

        ncExpected = 3;
        final Response r3 = resource.request().get();
        Assertions.assertEquals(Response.Status.fromStatusCode(r3.getStatus()), Response.Status.OK);

    }

    @Test
    public void testAuthentication() {
        final WebTarget resource = target()
                .register(HttpAuthenticationFeature.digest(DIGEST_TEST_LOGIN, DIGEST_TEST_INVALIDPASS))
                .path("auth-digest");

        ncExpected = 1;
        final Response r1 = resource.request().get();
        Assertions.assertEquals(Response.Status.fromStatusCode(r1.getStatus()), Response.Status.UNAUTHORIZED);
    }
}
