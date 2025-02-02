/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.Principal;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NameBinding;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.ExceptionMapper;

import jakarta.annotation.Priority;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JAX-RS name-bound filter tests.
 *
 * @author Martin Matula
 * @author Marek Potociar
 * @author Miroslav.Fuksa
 * @see GloballyNameBoundResourceFilterTest
 */
public class ResourceFilterTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(
                MyDynamicFeature.class,
                BasicTestsResource.class,
                NameBoundRequestFilter.class,
                NameBoundResponseFilter.class,
                PostMatchingFilter.class,
                // exception tests
                ExceptionTestsResource.class,
                ExceptionPreMatchRequestFilter.class,
                ExceptionPostMatchRequestFilter.class,
                ExceptionTestBoundResponseFilter.class,
                ExceptionTestGlobalResponseFilter.class,
                TestExceptionMapper.class,
                AbortResponseResource.class,
                AbortResponseFitler.class,
                AbortRequestFilter.class,
                ExceptionRequestFilter.class
        );
    }

    @Test
    public void testDynamicBinder() {
        test("dynamicBinder");
    }

    @Test
    public void testNameBoundRequest() {
        test("nameBoundRequest");
    }

    @Test
    public void testNameBoundResponse() {
        test("nameBoundResponse");
    }

    @Test
    public void testPostMatching() {
        test("postMatching");
    }

    // See JERSEY-1554
    @Test
    public void testGlobalPostMatchingNotInvokedOn404() {
        Response r = target("basic").path("not-found").request().get();
        assertEquals(404, r.getStatus());
        if (r.hasEntity()) {
            assertThat(r.readEntity(String.class), not(containsString("postMatching")));
        }
    }

    private void test(String name) {
        Response r = target("basic").path(name).request().get();
        assertEquals(200, r.getStatus(), "Unexpected HTTP response status code.");
        assertTrue(r.hasEntity(), "Response does not have entity.");
        assertEquals(name, r.readEntity(String.class), "Unexpected response entity value.");
    }

    @Path("/basic")
    public static class BasicTestsResource {

        @Path("dynamicBinder")
        @GET
        public String getDynamicBinder() {
            return "";
        }

        @Path("nameBoundRequest")
        @GET
        @NameBoundRequest
        public String getNameBoundRequest() {
            return "";
        }

        @Path("nameBoundResponse")
        @GET
        @NameBoundResponse
        public String getNameBoundResponse() {
            return "";
        }

        @Path("postMatching")
        @GET
        public String getPostMatching() {
            return "";
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundRequest {}

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundAbortResponse {}

    @NameBoundRequest
    @Priority(1)
    public static class NameBoundRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok("nameBoundRequest", MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    @NameBoundResponse
    public static class NameBoundResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.setEntity("nameBoundResponse", responseContext.getEntityAnnotations(), MediaType.TEXT_PLAIN_TYPE);
        }
    }

    public static class PostMatchingFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if (requestContext.getUriInfo().getPath().startsWith("basic")) {
                requestContext.abortWith(Response.ok("postMatching", MediaType.TEXT_PLAIN_TYPE).build());
            }
        }
    }

    @Priority(1)
    @PreMatching
    private static class DbFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok("dynamicBinder", MediaType.TEXT_PLAIN_TYPE).build());
        }
    }

    public static class MyDynamicFeature implements DynamicFeature {

        private final DbFilter filter = new DbFilter();

        @Override
        public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
            if ("getDynamicBinder".equals(resourceInfo.getResourceMethod().getName())) {
                context.register(filter);
            }
        }
    }

    @Path("/exception")
    public static class ExceptionTestsResource {

        @Path("matched")
        @GET
        @ExceptionTestBound
        public String getPostMatching() {
            return "method";
        }
    }

    @PreMatching
    public static class ExceptionPreMatchRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            if ("exception/not-found".equals(requestContext.getUriInfo().getPath())) {
                throw new TestException("globalRequest");
            }
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface ExceptionTestBound {}

    @ExceptionTestBound
    public static class ExceptionPostMatchRequestFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            throw new TestException("nameBoundRequest");
        }
    }

    @ExceptionTestBound
    @Priority(10)
    public static class ExceptionTestBoundResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            responseContext.setEntity(
                    (responseContext.hasEntity()) ? responseContext.getEntity() + "-nameBoundResponse" : "nameBoundResponse",
                    responseContext.getEntityAnnotations(),
                    MediaType.TEXT_PLAIN_TYPE);
        }
    }

    @Priority(1)
    public static class ExceptionTestGlobalResponseFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            if (!requestContext.getUriInfo().getPath().startsWith("exception")) {
                return;
            }

            responseContext.setEntity(
                    (responseContext.hasEntity()) ? responseContext.getEntity() + "-globalResponse" : "globalResponse",
                    responseContext.getEntityAnnotations(),
                    MediaType.TEXT_PLAIN_TYPE);
        }
    }

    public static class TestException extends RuntimeException {

        public TestException(String message) {
            super(message);
        }
    }

    public static class TestExceptionMapper implements ExceptionMapper<TestException> {

        public static final String POSTFIX = "-mappedTestException";

        @Override
        public Response toResponse(TestException exception) {
            return Response.ok(exception.getMessage() + POSTFIX).build();
        }
    }

    @Test
    public void testNameBoundResponseFilterNotInvokedOnPreMatchFilterException() {
        Response r = target("exception").path("not-found").request().get();
        assertEquals(200, r.getStatus());
        assertTrue(r.hasEntity());
        assertEquals("globalRequest-mappedTestException-globalResponse", r.readEntity(String.class));
    }

    @Test
    public void testNameBoundResponseFilterInvokedOnPostMatchFilterException() {
        Response r = target("exception").path("matched").request().get();
        assertEquals(200, r.getStatus());
        assertTrue(r.hasEntity());
        assertEquals("nameBoundRequest-mappedTestException-nameBoundResponse-globalResponse", r.readEntity(String.class));
    }

    @NameBoundAbortResponse
    private static class AbortResponseFitler implements ContainerResponseFilter {

        public static final String ABORT_FILTER_TEST_PASSED = "abort-filter-test-passed";

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            boolean passed = true;

            try {
                // checks that IllegalStateException is thrown when setting entity stream
                requestContext.setEntityStream(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return -1;
                    }
                });
                passed = false;
            } catch (IllegalStateException iae) {
                System.out.println(iae.getMessage());
            }

            try {
                // checks that IllegalStateException is thrown when setting security context
                requestContext.setSecurityContext(new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return null;
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return null;
                    }
                });
                passed = false;
            } catch (IllegalStateException iae) {
                System.out.println(iae.getMessage());
            }

            try {
                // checks that IllegalStateException is thrown when aborting request
                requestContext.abortWith(Response.serverError().build());
                passed = false;
            } catch (IllegalStateException iae) {
                System.out.println(iae.getMessage());
            }
            responseContext.getHeaders().add(ABORT_FILTER_TEST_PASSED, passed);
        }
    }

    @NameBoundAbortRequest
    private static class AbortRequestFilter implements ContainerRequestFilter {

        public static final String FILTER_ABORT_ENTITY = "filter-abort-entity";

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.ok(FILTER_ABORT_ENTITY).build());
        }
    }

    @NameBoundExceptionInRequest
    private static class ExceptionRequestFilter implements ContainerRequestFilter {

        public static final String EXCEPTION_IN_REQUEST_FILTER = "exception-in-request-filter";

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            throw new TestException(EXCEPTION_IN_REQUEST_FILTER);
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundResponse {}

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundAbortRequest {}

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface NameBoundExceptionInRequest {}

    @Path("abort")
    public static class AbortResponseResource {

        @Path("response")
        @GET
        @NameBoundAbortResponse
        public String get() {
            return "get";
        }

        @Path("abort-in-filter")
        @GET
        @NameBoundAbortResponse
        @NameBoundAbortRequest
        public String neverCalled() {
            return "This method will never be called. Request will be aborted in a request filter.";
        }

        @Path("exception")
        @GET
        @NameBoundAbortResponse
        @NameBoundExceptionInRequest
        public String exception() {
            return "This method will never be called. Exception will be thrown in a request filter.";
        }
    }

    @Test
    public void testAbortResponseInResponseFilter() {
        Response r = target("abort").path("response").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("get", r.readEntity(String.class));
        assertNotNull(r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
        assertEquals("true", r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
    }

    @Test
    public void testAbortAbortedResponseInResponseFilter() {
        Response r = target("abort").path("abort-in-filter").request().get();
        assertEquals(200, r.getStatus());
        assertEquals(AbortRequestFilter.FILTER_ABORT_ENTITY, r.readEntity(String.class));
        assertNotNull(r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
        assertEquals("true", r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
    }

    @Test
    public void testAbortedResponseFromExceptionResponse() {
        Response r = target("abort").path("exception").request().get();
        assertEquals(200, r.getStatus());
        assertEquals(ExceptionRequestFilter.EXCEPTION_IN_REQUEST_FILTER + TestExceptionMapper.POSTFIX,
                r.readEntity(String.class));
        assertNotNull(r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
        assertEquals("true", r.getHeaderString(AbortResponseFitler.ABORT_FILTER_TEST_PASSED));
    }
}
