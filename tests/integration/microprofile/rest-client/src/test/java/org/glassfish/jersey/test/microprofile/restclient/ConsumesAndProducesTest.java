/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.microprofile.restclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by David Kral.
 */
public class ConsumesAndProducesTest extends JerseyTest {

    static final JsonValue EXPECTED_JSON_VALUE = Json.createObjectBuilder().add("someKey", "Some value").build();

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(ApplicationResourceImpl.class);
    }

    @Test
    public void testWithEntity() throws URISyntaxException {
        ApplicationResource app = RestClientBuilder.newBuilder()
                .baseUri(new URI("http://localhost:9998"))
                .register(new TestClientRequestFilter(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON))
                .build(ApplicationResource.class);

        app.someJsonOperation(Json.createValue(1));
    }

    @Test
    public void testWithoutEntity() throws URISyntaxException {
        ApplicationResource app = RestClientBuilder.newBuilder()
                .baseUri(new URI("http://localhost:9998"))
                .register(new TestClientRequestFilter(MediaType.APPLICATION_JSON, null))
                .build(ApplicationResource.class);

        app.jsonValue();
    }

    @Test
    public void testWithoutEntityActualValue() throws URISyntaxException {
        ApplicationResource app = RestClientBuilder.newBuilder()
                .baseUri(new URI("http://localhost:9998"))
                .build(ApplicationResource.class);

        JsonValue json = app.jsonValue();
        assertEquals(json, EXPECTED_JSON_VALUE);
    }

    @Test
    public void testMethodContentType() throws URISyntaxException {
        ApplicationResource app = RestClientBuilder.newBuilder()
                .baseUri(new URI("http://localhost:9998"))
                .register(new TestClientRequestFilter(MediaType.TEXT_PLAIN, MediaType.TEXT_XML))
                .build(ApplicationResource.class);

        app.methodContentType(MediaType.TEXT_XML_TYPE, "something");
    }

    @Test
    public void testMethodWithRegexPathParam() throws URISyntaxException {
        ApplicationResource app = RestClientBuilder.newBuilder()
            .baseUri(new URI("http://localhost:9998"))
            .build(ApplicationResource.class);

        assertEquals("bar", app.regex("bar"));
    }

    @Test
    public void testMethodWithRegexPathParam0() throws URISyntaxException {
        ApplicationResource app = RestClientBuilder.newBuilder()
            .baseUri(new URI("http://localhost:9998"))
            .build(ApplicationResource.class);

        assertEquals(app.regex0("foo", "1234"), "foo_1234");
    }

    @Test
    public void testMethodWithRegexPathParam0Failure() throws URISyntaxException {
        assertThrows(WebApplicationException.class, () -> {
            ApplicationResource app = RestClientBuilder.newBuilder()
                .baseUri(new URI("http://localhost:9998"))
                .build(ApplicationResource.class);

            app.regex0("foo", "12345");
        });
    }

    private static class TestClientRequestFilter implements ClientRequestFilter {

        private final String expectedAccept;
        private final String expectedContentType;

        TestClientRequestFilter(String expectedAccept, String expectedContentType) {
            this.expectedAccept = expectedAccept;
            this.expectedContentType = expectedContentType;
        }

        @Override
        public void filter(ClientRequestContext requestContext) {
            assertTrue(requestContext.getHeaders().containsKey(HttpHeaders.ACCEPT));
            List<Object> accept = requestContext.getHeaders().get(HttpHeaders.ACCEPT);
            assertTrue(accept.contains(expectedAccept) || accept.contains(MediaType.valueOf(expectedAccept)));

            if (expectedContentType != null) {
                assertTrue(requestContext.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
                List<Object> contentType = requestContext.getHeaders().get(HttpHeaders.CONTENT_TYPE);
                assertEquals(contentType.size(), 1);
                assertTrue(contentType.contains(expectedContentType) || contentType
                        .contains(MediaType.valueOf(expectedContentType)));
            } else {
                assertFalse(requestContext.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
            }

            requestContext.abortWith(Response.ok().build());
        }
    }

}
