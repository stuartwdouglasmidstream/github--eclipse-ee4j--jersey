/*
 * Copyright (c) 2016, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.netty.connector;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Martin Matula
 */
public class TimeoutTest extends JerseyTest {
    @Path("/test")
    public static class TimeoutResource {
        @GET
        public String get() {
            return "GET";
        }

        @GET
        @Path("timeout")
        public String getTimeout() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "GET";
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(TimeoutResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new NettyConnectorProvider());
    }

    @Test
    public void testFast() {
        Response r = target("test").request().get();
        assertEquals(200, r.getStatus());
        assertEquals("GET", r.readEntity(String.class));
    }

    @Test
    public void testSlow() {
        try {
            target("test/timeout").property(ClientProperties.READ_TIMEOUT, 1_000).request().get();
            fail("Timeout expected.");
        } catch (ProcessingException e) {
            assertEquals(e.getMessage(), "Stream closed: read timeout");
            assertThat("Unexpected processing exception cause",
                       e.getCause(), instanceOf(TimeoutException.class));
        }
    }

    @Test
    public void testTimeoutInRequest() {
        try {
            target("test/timeout").request().property(ClientProperties.READ_TIMEOUT, 1_000).get();
            fail("Timeout expected.");
        } catch (ProcessingException e) {
            assertEquals(e.getMessage(), "Stream closed: read timeout");
            assertThat("Unexpected processing exception cause",
                       e.getCause(), instanceOf(TimeoutException.class));
        }
    }

    @Test
    public void testRxSlow() {
        try {
            target("test/timeout").property(ClientProperties.READ_TIMEOUT, 1_000).request()
               .rx().get().toCompletableFuture().join();
            fail("Timeout expected.");
        } catch (CompletionException cex) {
            assertThat("Unexpected async cause",
                       cex.getCause(), instanceOf(ProcessingException.class));
            ProcessingException e = (ProcessingException) cex.getCause();
            assertThat("Unexpected processing exception cause",
                       e.getCause(), instanceOf(TimeoutException.class));
            assertEquals(e.getCause().getMessage(), "Stream closed: read timeout");
        }
    }

    @Test
    public void testRxTimeoutInRequest() {
        try {
            target("test/timeout").request().property(ClientProperties.READ_TIMEOUT, 1_000)
               .rx().get().toCompletableFuture().join();
            fail("Timeout expected.");
        } catch (CompletionException cex) {
            assertThat("Unexpected async cause",
                       cex.getCause(), instanceOf(ProcessingException.class));
            ProcessingException e = (ProcessingException) cex.getCause();
            assertThat("Unexpected processing exception cause",
                       e.getCause(), instanceOf(TimeoutException.class));
            assertEquals(e.getCause().getMessage(), "Stream closed: read timeout");
        }
    }
}
