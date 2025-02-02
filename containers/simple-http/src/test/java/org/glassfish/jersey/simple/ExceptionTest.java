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

package org.glassfish.jersey.simple;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Paul Sandoz
 */
public class ExceptionTest extends AbstractSimpleServerTester {
    @Path("{status}")
    public static class ExceptionResource {
        @GET
        public String get(@PathParam("status") int status) {
            throw new WebApplicationException(status);
        }

    }

    @Test
    public void test400StatusCode() {
        startServer(ExceptionResource.class);
        Client client = ClientBuilder.newClient();
        WebTarget r = client.target(getUri().path("400").build());
        assertEquals(400, r.request().get(Response.class).getStatus());
    }

    @Test
    public void test500StatusCode() {
        startServer(ExceptionResource.class);
        Client client = ClientBuilder.newClient();
        WebTarget r = client.target(getUri().path("500").build());

        assertEquals(500, r.request().get(Response.class).getStatus());
    }
}
