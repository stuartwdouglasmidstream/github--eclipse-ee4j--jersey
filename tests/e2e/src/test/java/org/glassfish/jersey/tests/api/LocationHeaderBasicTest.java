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

package org.glassfish.jersey.tests.api;

import java.net.URI;
import java.util.logging.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test if the location relative URI is correctly resolved within basic cases.
 *
 * @author Adam Lindenthal
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LocationHeaderBasicTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(LocationHeaderBasicTest.class.getName());

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(ResponseTest.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        super.configureClient(config);
        config.property(ClientProperties.FOLLOW_REDIRECTS, false);
    }

    /**
     * Test JAX-RS resource
     */
    @Path(value = "/ResponseTest")
    public static class ResponseTest {

        /* injected request URI for assertions in the resource methods */
        @Context
        private UriInfo uriInfo;

        /**
         * Resource method for the basic uri conversion test
         * @return test response with relative location uri
         */
        @GET
        @Path("location")
        public Response locationTest() {
            final URI uri = URI.create("location");
            LOGGER.info("URI Created in the resource method > " + uri);
            return Response.created(uri).build();
        }

        /**
         * Resource method for the test with null location
         * @return test response with null location uri
         */
        @GET
        @Path("locationNull")
        public Response locationTestNull() {
            return Response.created(null).build();
        }

        /**
         * Resource method for the test with entity containing response
         * @return test response with relative uri and with entity
         */
        @GET
        @Path("locationWithBody")
        @Produces("text/plain")
        public Response locationTestWithBody() {
            final URI uri = URI.create("locationWithBody");
            return Response.created(uri).entity("Return from locationWithBody").type("text/plain").build();
        }

        /**
         * Resource method for direct test - location header is checked immediately after calling Response.created() and
         * the result is returned as a boolean response instead of returning the ({@link Response}) type and checking the
         * header in the calling test method. This isolates the influence of absolutization routine performed in the
         * ({@link org.glassfish.jersey.server.ServerRuntime} before closing the stream.
         *
         * @return true if URI is absolutized correctly, false if the URI remains relative (or does not match the expected one).
         */
        @GET
        @Path("locationDirect")
        @Produces("text/plain")
        public Boolean locationDirectTest() {
            final URI uri = getUriBuilder().segment("locationDirect").build();
            final Response response = Response.created(uri).build();
            return response.getLocation().equals(uriInfo.getAbsolutePath());
        }




        /**
         * Resource method for testing correct baseUri and request overwrite in the prematching filter.
         * Should never be called by the test, as {@link ResponseTest#redirectedUri()} should be called instead.
         */
        @GET
        @Path("filterChangedBaseUri")
        public Response locationWithChangedBaseUri() {
            fail("Method should not expected to be called, as prematching filter should have changed the request uri.");
            return Response.created(URI.create("new")).build();
        }

        /**
         * Not called by the test directly, but after prematching filter redirect from
         * {@link ResponseTest#locationWithChangedBaseUri()}.
         *
         * @return {@code 201 Created} response with location resolved against new baseUri.
         */
        @GET
        @Path("newUri")
        public Response redirectedUri() {
            return Response.created(URI.create("newRedirected")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code seeOther} response.
         * @return {@code 303 See Other} response with relative URI
         */
        @POST
        @Path("seeOther")
        @Consumes("text/plain")
        public Response seeOther() {
            return Response.seeOther(URI.create("other")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code seeOther} response.
         * @return {@code 303 See Other} response with relative URI
         */
        @GET
        @Path("seeOtherLeading")
        public Response seeOtherWithLeadingSlash() {
            return Response.seeOther(URI.create("/other")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code seeOther} response.
         * @return {@code 303 See Other} response with relative URI
         */
        @GET
        @Path("seeOtherTrailing")
        public Response seeOtherWithTrailingSlash() {
            return Response.seeOther(URI.create("other/")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code temporaryRedirect} response.
         * @return {@code 307 Temporary Redirect} response with relative URI
         */
        @GET
        @Path("temporaryRedirect")
        public Response temporaryRedirect() {
            return Response.temporaryRedirect(URI.create("redirect")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code temporaryRedirect} response.
         * @return {@code 307 Temporary Redirect} response with relative URI
         */
        @GET
        @Path("temporaryRedirectLeading")
        public Response temporaryRedirectWithLeadingSlash() {
            return Response.temporaryRedirect(URI.create("/redirect")).build();
        }

        /**
         * Resource method for testing relative URI resolution in case of {@code temporaryRedirect} response.
         * @return {@code 307 Temporary Redirect} response with relative URI
         */
        @GET
        @Path("temporaryRedirectTrailing")
        public Response temporaryRedirectWithTrailingSlash() {
            return Response.temporaryRedirect(URI.create("redirect/")).build();
        }

        /** Return UriBuilder with base pre-set {@code /ResponseTest} uri segment for this resource.
         *
         * @return UriBuilder
         */
        private UriBuilder getUriBuilder() {
            return UriBuilder.fromResource(ResponseTest.class);
        }
    }

    /**
     * Basic test; resource methods returns relative uri, test expects uri to be absolute
     */
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testConvertRelativeUriToAbsolute() {
        checkResource("ResponseTest/location", "location");
        // checkResource("ResponseTest/location");
    }

    /**
     * Test with entity; most of the HTTP 201 Created responses do not contain any body, just headers.
     * This test ensures, that the uri conversion works even in case when entity is present.
     */
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testAbsoluteUriWithEntity() {
        final Response response = checkResource("ResponseTest/locationWithBody", "locationWithBody");
        assertNotNull(response.getEntity());
    }


    /**
     * Test with null location;
     * Ensures, that the null location is processed correctly.
     */
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testNullLocation() {
        final Response response = target().path("ResponseTest/locationNull").request(MediaType.TEXT_PLAIN).get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertNull(location, "Location header should be absolute URI");
    }

    /**
     * Tests if the URI is absolutized in the Response directly after Response.Builder.created() is called
     */
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testConversionDirectly() {
        final Boolean result = target().path("ResponseTest/locationDirect").request(MediaType.TEXT_PLAIN).get(Boolean.class);
        assertTrue(result);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testSeeOther() {
        Response response = target().path("ResponseTest/seeOther").request()
                .post(Entity.entity("TEXT", MediaType.TEXT_PLAIN_TYPE));
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "other", location);

        response = target().path("ResponseTest/seeOtherLeading").request(MediaType.TEXT_PLAIN).get(Response.class);
        location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "other", location);

        response = target().path("ResponseTest/seeOtherTrailing").request(MediaType.TEXT_PLAIN).get(Response.class);
        location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "other/", location);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testTemporaryRedirect() {
        Response response = target().path("ResponseTest/temporaryRedirect").request(MediaType.TEXT_PLAIN).get(Response.class);
        String location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "redirect", location);

        response = target().path("ResponseTest/temporaryRedirectLeading").request(MediaType.TEXT_PLAIN).get(Response.class);
        location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "redirect", location);

        response = target().path("ResponseTest/temporaryRedirectTrailing").request(MediaType.TEXT_PLAIN).get(Response.class);
        location = response.getHeaderString(HttpHeaders.LOCATION);
        assertEquals(getBaseUri().toString() + "redirect/", location);
    }

    private Response checkResource(final String resourcePath, final String expectedRelativeUri) {
        final Response response = target().path(resourcePath).request(MediaType.TEXT_PLAIN).get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertEquals(getBaseUri() + expectedRelativeUri, location);
        return response;
    }
}



