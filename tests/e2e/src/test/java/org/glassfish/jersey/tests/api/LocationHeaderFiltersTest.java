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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test if the location response header relative URI is correctly resolved within complex cases with interceptors, filters,
 * exception mappers, etc.
 *
 * @author Adam Lindenthal
 */
public class LocationHeaderFiltersTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(LocationHeaderBasicTest.class.getName());

    static ExecutorService executor;

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        return new ResourceConfig(
                MyTest.class,
                LocationManipulationDynamicBinding.class,
                AbortingPreMatchingRequestFilter.class,
                BaseUriChangingPreMatchingFilter.class,
                TestExceptionMapper.class
        );
    }

    /**
     * Prepare test infrastructure.
     *
     * In this case it prepares executor thread pool of size one and initializes the thread.
     * @throws Exception
     */
    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        /* thread pool for custom executor async test */
        LocationHeaderFiltersTest.executor = Executors.newFixedThreadPool(1);

        // Force the thread to be eagerly instantiated - this prevents the instantiation later and ensures, that the thread
        // will not be a child thread of the request handling thread, so the thread-local baseUri variable will not be inherited.
        LocationHeaderFiltersTest.executor.submit(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Thread pool initialized.");
            }
        });
    }


    /**
     * Test JAX-RS resource
     */
    @SuppressWarnings("VoidMethodAnnotatedWithGET")
    @Path(value = "/ResponseTest")
    public static class MyTest {

        /* injected request URI for assertions in the resource methods */
        @Context
        private UriInfo uriInfo;

        /**
         * Resource method for the test with uri rewritten in the filter
         * @return test response with relative location uri
         */
        @GET
        @Path("locationTestWithFilter")
        public Response locationTestWithFilter() {
            final URI uri = URI.create("location");
            LOGGER.info("URI Created in the resource method > " + uri);
            return Response.created(uri).build();
        }

        /**
         * Resource method for the test with uri rewritten in the interceptor
         * @return test response with relative location uri and with body
         * (write interceptors are not triggered for entity-less responses)
         */
        @GET
        @Path("locationWithInterceptor")
        public Response locationTestWithInterceptor() {
            final URI uri = URI.create("foo");
            return Response.created(uri).entity("Return from locationTestWithInterceptor").type("text/plain").build();
        }


        /**
         * Resource method for testing URI absolutization after the abortion in the post-matching filter.
         * @return dummy response - this string should never be propagated to the client (the processing chain
         * will be aborted in the filter before this resource method is even called.
         * However, it is needed here, because the filter is bound to the resource method name.
         */
        @GET
        @Path("locationAborted")
        public String locationTestAborted() {
            assertTrue(false, "The resource method locationTestAborted() should not have been called. "
                    + "The post-matching filter was not configured correctly.");
            return "DUMMY_RESPONSE"; // this string should never reach the client (the resource method will not be called)
        }

        /**
         * Resource method for testing URI absolutization after the abortion in the pre-matching filter.
         * @return dummy response - this string should never be propagated to the client (the processing chain will be
         * executorComparisonFailed in the filter before this resource method is even called.
         * However, it is needed here, because the filter is bound to the resource method name.
         */
        @GET
        @Path("locationAbortedPreMatching")
        public String locationTestPreMatchingAborted() {
            assertTrue(false, "The resource method locationTestPreMatchingAborted() should not have been called. "
                    + "The pre-matching filter was not configured correctly.");
            return "DUMMY_RESPONSE"; // this string should never reach the client (the resource method will not be called)
        }

        /**
         * Resource method for the test of ResponseFilters in the sync case.
         * Returns response with a relative URI, which is than absolutized by Jersey.
         * Later the {@link UriCheckingResponseFilter} is triggered and checks the URI again - the check itself is done in the
         * filter.
         * Based on the result of the check, the filter returns the original status (201 - Created) or an
         * error status (500 - Internal Server Error) with an error message in the response body.
         */
        @GET
        @Path("responseFilterSync")
        public Response responseFilterSync() {
            return Response.created(URI.create("responseFilterSync")).build();
        }

        /**
         * Resource method for the test of ResponseFilters in the async case. It runs in the separate thread created on request.
         *
         * Returns response with a relative URI, which is than absolutized by Jersey.
         * Later the {@link UriCheckingResponseFilter} is triggered and checks the URI again - the check itself is done in the
         * filter.
         * Based on the result of the check, the filter returns the original status (201 - Created) or an
         * error status (500 - Internal Server Error) with an error message in the response body.
         */
        @GET
        @Path("responseFilterAsync")
        public void responseFilterAsync(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Response result = Response.created(URI.create("responseFilterAsync")).build();
                    asyncResponse.resume(result);
                }
            }).start();
        }

        /**
         * Resource method for the test of ResponseFilters in the async/executor case. It runs in a thread created out of the
         * request scope.
         *
         * Returns response with a relative URI, which is than absolutized by Jersey.
         * Later the {@link UriCheckingResponseFilter} is triggered and checks the URI again - the check itself is done in the
         * filter.
         * Based on the result of the check, the filter returns the original status (201 - Created) or an
         * error status (500 - Internal Server Error) with an error message in the response body.
         */
        @GET
        @Path("responseFilterAsyncExecutor")
        public void responseFilterAsyncExecutor(@Suspended final AsyncResponse asyncResponse) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    final Response result =
                            Response.created(URI.create("responseFilterAsyncExecutor")).build();
                    asyncResponse.resume(result);
                }
            });
        }

        /**
         * Resource method for testing for testing the URI absolutization in the exception mapper in the synchronous case.
         *
         * Method always throws {@link WebApplicationException}, which is defined to be handled by {@link TestExceptionMapper}.
         * The exception mapper then creates the response with relative URI and response is routed
         * into {@link UriCheckingResponseFilter}, which checks if th URI was correctly absolutized.
         * @return does not return any response
         */
        @GET
        @Path("exceptionMapperSync")
        public Response exceptionMapperSync() {
            throw new WebApplicationException();
        }

        /**
         * Resource method for testing for testing the URI absolutization in the exception mapper in the asynchronous case.
         * New thread is started for the resource method processing.
         *
         * Method always "throws" {@link WebApplicationException} (in case of async methods,
         * the exceptions are not thrown directly, but passed to {@link AsyncResponse#resume(Throwable)}),
         * which is defined to be handled by {@link TestExceptionMapper}.         *
         * The exception mapper then creates the response with relative URI and response is routed
         * into {@link UriCheckingResponseFilter}, which checks if th URI was correctly absolutized.
         */
        @GET
        @Path("exceptionMapperAsync")
        public void exceptionMapperAsync(@Suspended final AsyncResponse asyncResponse) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    asyncResponse.resume(new WebApplicationException());
                }
            }).start();
        }

        /**
         * Resource method for testing for testing the URI absolutization in the exception mapper in the asynchronous case.
         * A thread from executor thread pool (created out of request scope) is used for processing the resource method.
         *
         * Method always "throws" {@link WebApplicationException} (in case of async methods,
         * the exceptions are not thrown directly, but passed to {@link AsyncResponse#resume(Throwable)}),
         * which is defined to be handled by {@link TestExceptionMapper}.         *
         * The exception mapper then creates the response with relative URI and response is routed
         * into {@link UriCheckingResponseFilter}, which checks if th URI was correctly absolutized.
         */
        @GET
        @Path("exceptionMapperExecutor")
        public void exceptionMapperExecutor(@Suspended final AsyncResponse asyncResponse) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    asyncResponse.resume(new WebApplicationException());
                }
            });
        }

        /**
         * Resource method for testing correct baseUri and request overwrite in the prematching filter.
         * Should never be called by the test, as {@link MyTest#redirectedUri()} should be called instead.
         */
        @GET
        @Path("filterChangedBaseUri")
        public Response locationWithChangedBaseUri() {
            fail("Method should not expected to be called, as prematching filter should have changed the request uri.");
            return Response.created(URI.create("new")).build();
        }

        /**
         * Not called by the test directly, but after prematching filter redirect from
         * {@link MyTest#locationWithChangedBaseUri()}.
         *
         * @return {@code 201 Created} response with location resolved against new baseUri.
         */
        @GET
        @Path("newUri")
        public Response redirectedUri() {
            return Response.created(URI.create("newRedirected")).build();
        }

    }


    /**
     * Test the URI created in the post-matching request filter.
     */
    @Test
    public void testAbortFilter() {
        checkResponseFilter("ResponseTest/locationAborted", "uriAfterAbortion/SUCCESS");
    }

    /**
     * Test the URI created in the pre-matching request filter.
     */
    @Test
    public void testAbortPreMatchingFilter() {
        checkResource("ResponseTest/locationAbortedPreMatching", "uriAfterPreMatchingAbortion/SUCCESS");
    }


    /**
     * Test with URI Rewritten in the container response filter;
     * Filters do have access to the response headers and can manipulate the location uri so that it contains a relative address.
     * This test incorporates a filter which replaces the uri with a relative one. However we expect to have absolute uri at
     * the end of the chain.
     */
    @Test
    public void testAbsoluteUriWithFilter() {
        checkResource("ResponseTest/locationTestWithFilter", "ResponseTest/UriChangedByFilter");
    }

    /**
     * Test with URI Rewritten in the writer interceptor;
     * Interceptors do have access to the response headers and can manipulate the location uri so that it contains a relative
     * address.
     * This test incorporates an interceptor which replaces the uri with a relative one. However we expect to have absolute uri
     * at the end of the chain.
     */
    @Test
    public void testAbsoluteUriWithInterceptor() {
        checkResource("ResponseTest/locationWithInterceptor", "ResponseTest/UriChangedByInterceptor");
    }


    /**
     * Test, that uri is correct in the response filter when created in the exception mapper (synchronous).
     */
    @Test
    public void testExceptionMapperSync() {
        checkResponseFilter("ResponseTest/exceptionMapperSync", "EXCEPTION_MAPPER");
    }

    /**
     * Test, that uri is correct in the response filter when created in the exception mapper (asynchronous).
     */
    @Test
    public void testExceptionMapperAsync() {
        checkResponseFilter("ResponseTest/exceptionMapperAsync", "EXCEPTION_MAPPER");
    }

    /**
     * Test, that uri is correct in the response filter when created in the exception mapper (asynchronous/executor).
     */
    @Test
    public void testExceptionMapperExecutor() {
        checkResponseFilter("ResponseTest/exceptionMapperExecutor", "EXCEPTION_MAPPER");
    }

    /**
     * Test the baseUri and requestUri change in the prematching filter.
     */
    @Test
    public void testLocationBaseUriChangedByPrematchingFilter() {
        final Response response = target().path("ResponseTest/filterChangedBaseUri").request().get();
        assertEquals("http://www.server.com/newRedirected", response.getHeaderString("Location"));
    }

    /**
     * Test, that uri is correct in the response filter (synchronous).
     */
    @Test
    public void testResponseFilterSync() {
        checkResponseFilter("ResponseTest/responseFilterSync", "responseFilterSync");
    }

    /**
     * Test, that uri is correct in the response filter (asynchronous).
     */
    @Test
    public void testResponseFilterAsync() {
        checkResponseFilter("ResponseTest/responseFilterAsync", "responseFilterAsync");
    }



    /**
     * Test, that uri is correct in the response filter (asynchronous/executor).
     */
    @Test
    public void testResponseFilterAsyncExecutor() {
        checkResponseFilter("ResponseTest/responseFilterAsyncExecutor", "responseFilterAsyncExecutor");
    }



    /**
     * Response filter - replaces the Location header with a relative uri.
     */
    public static class LocationManipulationFilter implements ContainerResponseFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {
            final MultivaluedMap<String, ?> headers = responseContext.getHeaders();
            final List<URI> locations = (List<URI>) headers.get(HttpHeaders.LOCATION);
            locations.set(0, URI.create("ResponseTest/UriChangedByFilter"));
            LOGGER.info("LocationManipulationFilter applied.");
        }
    }

    /**
     * Response filter - check if the URI is absolute. If it is not correctly absolutized,
     * it changes the response to 500 - Internal Server Error and sets the error message into the response body.
     */
    public static class UriCheckingResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
                throws IOException {
            final URI location = responseContext.getLocation();
            if (!location.isAbsolute()) {
                responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                responseContext.setEntity("Response location was not absolute in UriCheckingFilter. Location value: " + location);
            }
        }
    }

    /**
     * Request Filter which aborts the current request calling ContainerRequestContext.abortWith().
     *
     * The returned response is passed to Response.created() and immediately tested for absolutization.
     * This is necessary, as the relative URI would be absolutized later anyway and would reach the calling test method as
     * an absolute URI and there would be no way to determine where the URI conversion was done.
     *
     * The result of the test is propagated back to the test method by separate URI values in case of a success or a failure.
     */
    public static class AbortingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            LOGGER.info("Aborting request in the request filter. Returning status created.");
            final String successRelativeUri = "uriAfterAbortion/SUCCESS";
            Response response = Response.created(URI.create(successRelativeUri)).build();

            if (!response.getLocation().toString().equals(requestContext.getUriInfo().getBaseUri() + successRelativeUri)) {
                response = Response.created(URI.create("uriAfterAbortion/FAILURE")).build();
            }
            requestContext.abortWith(response);
        }
    }

    /**
     * Request Filter which aborts the current request calling ContainerRequestContext.abortWith().
     *
     * The returned response is passed to Response.created() and immediately tested for absolutization.
     * This is necessary, as the relative URI would be absolutized later anyway and would reach the calling test method as
     * an absolute URI and there would be no way to determine where the URI conversion was done.
     *
     * The result of the test is propagated back to the test method by separate URI values in case of a success or a failure.
     */
    @PreMatching
    public static class AbortingPreMatchingRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {

            if (requestContext.getUriInfo().getAbsolutePath().toString().endsWith("locationAbortedPreMatching")) {
                LOGGER.info("Aborting request in the request filter. Returning status created.");
                final String successRelativeUri = "uriAfterPreMatchingAbortion/SUCCESS";
                Response response = Response.created(URI.create(successRelativeUri)).build();
                if (!response.getLocation().toString().equals(requestContext.getUriInfo().getBaseUri() + successRelativeUri)) {
                    response = Response.created(URI.create("uriAfterPreMatchingAbortion/FAILURE")).build();
                }
                requestContext.abortWith(response);
            }
        }
    }

    /**
     * Request prematching filter which changes request URI and base URI.
     *
     * As a result, different resource mathod should be matched and invoked and return location resolved against the new
     * base URI.
     */
    @PreMatching
    public static class BaseUriChangingPreMatchingFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getUriInfo().getAbsolutePath().toString().endsWith("filterChangedBaseUri")) {
                final URI requestUri = requestContext.getUriInfo().getRequestUri();
                // NOTE, that the trailing slash matters, without it, the URI is nod valid and is not correctly resolved by the
                // URI.resolve() method.
                final URI baseUri = URI.create("http://www.server.com/");
                requestContext.setRequestUri(baseUri, requestUri.resolve("newUri"));
            }
        }
    }

    /**
     * Writer interceptor - replaces the Location header with a relative uri.
     */
    public static class LocationManipulationInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(final WriterInterceptorContext context) throws IOException, WebApplicationException {
            final MultivaluedMap<String, ?> headers = context.getHeaders();
            final List<URI> locations = (List<URI>) headers.get(HttpHeaders.LOCATION);
            locations.set(0, URI.create("ResponseTest/UriChangedByInterceptor"));
            LOGGER.info("LocationManipulationInterceptor applied.");
            context.proceed();
        }
    }

    /**
     * Exception mapper which creates a test response with a relative URI.
     */
    public static class TestExceptionMapper implements ExceptionMapper<WebApplicationException> {

        @Override
        public Response toResponse(final WebApplicationException exception) {
            exception.printStackTrace();
            return Response.created(URI.create("EXCEPTION_MAPPER")).build();
        }
    }

    /**
     * Registers the filter and interceptor and binds it to the resource methods of interest.
     */
    public static class LocationManipulationDynamicBinding implements DynamicFeature {

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            if (MyTest.class.equals(resourceInfo.getResourceClass())) {
                final String methodName = resourceInfo.getResourceMethod().getName();
                if (methodName.contains("locationTestWithFilter")) {
                    context.register(LocationManipulationFilter.class);
                    LOGGER.info("LocationManipulationFilter registered.");
                }
                if (methodName.contains("locationTestWithInterceptor")) {
                    context.register(LocationManipulationInterceptor.class);
                    LOGGER.info("LocationManipulationInterceptor registered.");
                }
                if (methodName.contains("locationTestAborted")) {
                    context.register(AbortingRequestFilter.class);
                    LOGGER.info("AbortingRequestFilter registered.");
                }
                if (methodName.contains("responseFilterSync")
                        || methodName.contains("responseFilterAsync")
                        || methodName.contains("locationTestAborted")
                        || methodName.contains("exceptionMapperSync")
                        || methodName.contains("exceptionMapperAsync")
                        || methodName.contains("exceptionMapperExecutor")) {
                    context.register(UriCheckingResponseFilter.class);
                    LOGGER.info("UriCheckingResponseFilter registered.");
                }
                if (methodName.contains("locationWithChangedBaseUri")) {
                    context.register(BaseUriChangingPreMatchingFilter.class);
                    LOGGER.info("BaseUriChangingPreMatchingFilter registered.");
                }
            }
        }
    }

    private Response checkResource(final String resourcePath, final String expectedRelativeUri) {
        final Response response = target().path(resourcePath).request(MediaType.TEXT_PLAIN).get(Response.class);
        final String location = response.getHeaderString(HttpHeaders.LOCATION);
        LOGGER.info("Location resolved from response > " + location);
        assertEquals(getBaseUri() + expectedRelativeUri, location);
        return response;
    }

    private void checkResponseFilter(final String resourcePath, final String expectedRelativeUri) {
        final Response response = target().path(resourcePath).request().get(Response.class);
        assertNotEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Message from response filter: " + response.readEntity(String.class));
        assertEquals(getBaseUri() + expectedRelativeUri, response.getLocation().toString());
    }
}
