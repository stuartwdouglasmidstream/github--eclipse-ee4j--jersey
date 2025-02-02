/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Adam Lindenthal
 */
public class MultiValueHeaderTest extends JerseyTest {

    @Path("testResource")
    public static class TestResource {
        @GET
        public Response get(@Context HttpHeaders h) {
            final String headerString = h.getHeaderString("tools");
            if ("hammer,axe,shovel,drill".equals(headerString)) {
                return Response.ok(headerString)
                        .header("animals", "mole")
                        .header("animals", "hedgehog")
                        .header("animals", "bat")
                        .header("animals", "rabbit")
                        .build();
            } else {
                return Response.serverError().entity(headerString).build();
            }
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(TestResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new JdkConnectorProvider());
    }

    @Test
    public void testCookieResource() {
        // the default cookie policy does not like cookies from localhost
        Response response = target("testResource").request()
                .header("tools", "hammer")
                .header("tools", "axe")
                .header("tools", "shovel")
                .header("tools", "drill")
                .get();

        Assertions.assertEquals(200, response.getStatus());

        final String values = response.getHeaderString("animals");
        Assertions.assertEquals("mole,hedgehog,bat,rabbit", values);
    }

}
