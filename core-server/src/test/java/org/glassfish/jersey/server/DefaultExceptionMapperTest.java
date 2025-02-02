/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.CompletionCallback;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultExceptionMapperTest {
    public static final String MESSAGE = "DefaultExceptionMapperTest I/O Exception";
    @Test
    public void testIOException() {
        IOException ioe = new IOException(MESSAGE);
        DefaultExceptionMapper mapper = new DefaultExceptionMapper();
        Response response = mapper.toResponse(ioe);
        assertThat(response.getEntity().toString().contains(MESSAGE)).isFalse();
    }

    @Test
    public void testCompletionCallback() {
        AtomicInteger counter = new AtomicInteger();
        CompletionCallback hitOnceCallback = new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                counter.incrementAndGet();
            }
        };
        ResourceConfig resourceConfig = new ResourceConfig().register(new IOExThrowingResource(hitOnceCallback));
        ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);
        try {
            applicationHandler.apply(RequestContextBuilder.from("/", "GET").build()).get();
        } catch (Exception e) {
            // expected
        }

        assertEquals(1, counter.get());
    }

    @Test
    public void testDefaultExceptionMapperNotRegisteredInExceptionMapperFactory() {
        ResourceConfig resourceConfig = new ResourceConfig();
        ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);

        // use InjectionManager from ApplicationHandler to set up the default bindings
        ExceptionMapperFactory exceptionMapperFactory = new ExceptionMapperFactory(applicationHandler.getInjectionManager());

        assertThat(exceptionMapperFactory.find(Throwable.class)).isNull();
    }

    @Path("/")
    public static class IOExThrowingResource {
        private final CompletionCallback callback;

        public IOExThrowingResource(CompletionCallback callback) {
            this.callback = callback;
        }

        @GET
        public String doGet(@Suspended AsyncResponse asyncResponse) throws IOException {
            asyncResponse.register(callback);
            throw new IOException(MESSAGE);
        }
    }
}
