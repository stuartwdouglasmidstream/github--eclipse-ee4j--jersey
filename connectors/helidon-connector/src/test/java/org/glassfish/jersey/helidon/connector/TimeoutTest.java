/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.helidon.connector;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        config.connectorProvider(new HelidonConnectorProvider());
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
            assertTimeoutException(e);
        }
    }

    @Disabled
    // TODO - WebClient change request
    public void testTimeoutInRequest() {
        try {
            target("test/timeout").request().property(ClientProperties.READ_TIMEOUT, 1_000).get();
            fail("Timeout expected.");
        } catch (ProcessingException e) {
            assertTimeoutException(e);
        }
    }

    private void assertTimeoutException(Exception e) {
        String exceptionName = "TimeoutException"; // check netty or JDK TimeoutException
        Throwable t = e.getCause();
        while (t != null) {
            if (t.getClass().getSimpleName().contains(exceptionName)) {
                break;
            }
            t = t.getCause();
        }
        if (t == null) {
            if (e.getCause() != null) {
                if (e.getCause().getCause() != null) {
                    fail("Unexpected processing exception cause" + e.getCause().getCause().getMessage());
                } else {
                    fail("Unexpected processing exception cause" + e.getCause().getMessage());
                }
            } else {
                fail("Unexpected processing exception cause" + e.getMessage());
            }
        }
    }
}
