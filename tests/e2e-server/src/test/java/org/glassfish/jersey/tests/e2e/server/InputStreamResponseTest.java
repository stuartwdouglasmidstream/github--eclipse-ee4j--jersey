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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is to make sure you can just pass an input stream to Jersey,
 * where entity body data would be read from.
 *
 * @author Jakub Podlesak
 */
public class InputStreamResponseTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(InputStreamResource.class, WrappedInputStreamResource.class);
    }

    @Path("/directInputStream")
    public static class InputStreamResource {

        @GET
        public InputStream get() {
            return new ByteArrayInputStream("Josefka".getBytes());
        }
    }

    @Test
    public void testDirectInputStream() throws Exception {
        final String s = target().path("directInputStream").request().get(String.class);

        assertEquals("Josefka", s);
    }

    @Path("/responseWrappedInputStream")
    public static class WrappedInputStreamResource {

        @GET
        public Response get() {
            return Response.ok(new ByteArrayInputStream("Marie".getBytes()), MediaType.TEXT_PLAIN_TYPE).build();
        }
    }

    @Test
    public void testWrappedInputStream() throws Exception {
        final Response response = target().path("responseWrappedInputStream").request().get();

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, response.getMediaType());
        assertEquals("Marie", response.readEntity(String.class));
    }
}
