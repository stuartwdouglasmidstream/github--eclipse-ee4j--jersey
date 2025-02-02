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

package org.glassfish.jersey.server;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NameBinding;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.Suspended;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link CompletionCallback}.
 *
 * @author Miroslav Fuksa
 */
public class AsyncCallbackServerTest {

    private static class Flags {
        public volatile boolean onResumeCalled;
        public volatile boolean onCompletionCalled;
        public volatile boolean onCompletionCalledWithError;
        public volatile boolean onResumeFailedCalled;
    }

    @Test
    public void testCompletionCallback() throws ExecutionException, InterruptedException {
        final Flags flags = new Flags();

        ApplicationHandler app = new ApplicationHandler(
                new ResourceConfig().register(new CompletionResource(flags))
                .register(new CheckingCompletionFilter(flags)));
        ContainerRequest req = RequestContextBuilder.from(
                "/completion/onCompletion", "GET").build();

        final ContainerResponse response = app.apply(req).get();
        assertEquals(200, response.getStatus());
        assertTrue(flags.onCompletionCalled, "onComplete() was not called.");
    }

    @Test
    public void testCompletionFail() throws ExecutionException, InterruptedException {
        final Flags flags = new Flags();

        ApplicationHandler app = new ApplicationHandler(
                new ResourceConfig().register(new CompletionResource(flags))
                        .register(new CheckingCompletionFilter(flags)));

        try {
            final ContainerResponse response = app.apply(RequestContextBuilder.from(
                    "/completion/onError", "GET").build()).get();
            fail("should fail");
        } catch (Exception e) {
            // ok - should throw an exception
        }
        assertTrue(flags.onCompletionCalledWithError, "onError().");
    }

    @Test
    public void testRegisterNullClass() throws ExecutionException, InterruptedException {
        final ApplicationHandler app = new ApplicationHandler(new ResourceConfig(NullCallbackResource.class));
        final ContainerRequest req = RequestContextBuilder.from("/null-callback/class", "GET").build();

        final ContainerResponse response = app.apply(req).get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRegisterNullObject() throws ExecutionException, InterruptedException {
        final ApplicationHandler app = new ApplicationHandler(new ResourceConfig(NullCallbackResource.class));
        final ContainerRequest req = RequestContextBuilder.from("/null-callback/object", "GET").build();

        final ContainerResponse response = app.apply(req).get();
        assertEquals(200, response.getStatus());
    }

    @CompletionBinding
    public static class CheckingCompletionFilter implements ContainerResponseFilter {

        private final Flags flags;

        public CheckingCompletionFilter(Flags flags) {
            this.flags = flags;
        }

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
            assertFalse(flags.onCompletionCalled,
                    "onComplete() callback has already been called.");
        }
    }

    public static class MyCompletionCallback implements CompletionCallback {

        private final Flags flags;

        public MyCompletionCallback(Flags flags) {
            this.flags = flags;
        }

        @Override
        public void onComplete(Throwable throwable) {
            assertFalse(flags.onCompletionCalled, "onComplete() has already been called.");
            assertFalse(flags.onCompletionCalledWithError, "onComplete() has already been called with error.");
            if (throwable == null) {
                flags.onCompletionCalled = true;
            } else {
                flags.onCompletionCalledWithError = true;
            }
        }
    }

    @Path("completion")
    public static class CompletionResource {

        private final Flags flags;

        public CompletionResource(Flags flags) {
            this.flags = flags;
        }

        @GET
        @Path("onCompletion")
        @CompletionBinding
        public void onComplete(@Suspended AsyncResponse asyncResponse) {
            assertFalse(flags.onCompletionCalled);
            asyncResponse.register(new MyCompletionCallback(flags));
            asyncResponse.resume("ok");
            assertTrue(flags.onCompletionCalled);
        }

        @GET
        @Path("onError")
        @CompletionBinding
        public void onError(@Suspended AsyncResponse asyncResponse) {
            assertFalse(flags.onCompletionCalledWithError);
            asyncResponse.register(new MyCompletionCallback(flags));
            asyncResponse.resume(new RuntimeException("test-exception"));
            assertTrue(flags.onCompletionCalledWithError);
        }
    }

    @Path("null-callback")
    @Singleton
    public static class NullCallbackResource {

        @GET
        @Path("class")
        @CompletionBinding
        public void registerClass(@Suspended AsyncResponse asyncResponse) {
            try {
                asyncResponse.register(null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(null, MyCompletionCallback.class);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(MyCompletionCallback.class, null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(MyCompletionCallback.class, MyCompletionCallback.class, null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            asyncResponse.resume("ok");
        }

        @GET
        @Path("object")
        @CompletionBinding
        public void registerObject(@Suspended AsyncResponse asyncResponse) {
            try {
                asyncResponse.register((Object) null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(null, new MyCompletionCallback(new Flags()));
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(new MyCompletionCallback(new Flags()), null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            try {
                asyncResponse.register(new MyCompletionCallback(new Flags()), new MyCompletionCallback(new Flags()), null);
                fail("NullPointerException expected.");
            } catch (NullPointerException npe) {
                // Expected.
            }

            asyncResponse.resume("ok");
        }
    }

    @NameBinding
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(value = RetentionPolicy.RUNTIME)
    public @interface CompletionBinding {
    }
}
