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

package org.glassfish.jersey.tests.e2e.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testing {@link Reader} on client and server.
 *
 * @author Miroslav Fuksa
 */
public class ReaderProviderTest extends JerseyTest {

    public static final String GET_READER_RESPONSE = "GET_READER_RESPONSE";
    public static final String GET_POST_RESPONSE = "GET_POST_RESPONSE";

    @Override
    protected Application configure() {
        return new ResourceConfig(ReaderResource.class);
    }

    @Test
    public void testReader() {
        Response response = target().path("test/postReaderGetReader").request().post(Entity.entity(GET_POST_RESPONSE,
                MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(GET_POST_RESPONSE, response.readEntity(String.class));
    }

    @Test
    public void testGetReader() {
        Response response = target().path("test/getReader").request().get();
        assertEquals(200, response.getStatus());
        assertEquals(GET_READER_RESPONSE, response.readEntity(String.class));
    }

    @Test
    public void testEmptyReader() throws IOException {
        Response response = target().path("test/getEmpty").request().get();
        assertEquals(204, response.getStatus());
        final Reader reader = response.readEntity(Reader.class);
        assertNotNull(reader);
        assertEquals(-1, reader.read());
    }

    @Test
    public void testReaderOnClientAsResponseEntity() throws IOException {
        Response response = target().path("test/getReader").request().get();
        assertEquals(200, response.getStatus());
        final Reader reader = response.readEntity(Reader.class);
        assertNotNull(reader);
        BufferedReader br = new BufferedReader(reader);
        assertEquals(GET_READER_RESPONSE, br.readLine());
    }

    @Test
    public void testReaderOnClientAsRequestEntity() throws IOException {
        Response response = target().path("test/postReaderGetReader").request()
                .post(Entity.entity(new StringReader(GET_POST_RESPONSE), MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(GET_POST_RESPONSE, response.readEntity(String.class));
    }

    @Path("test")
    public static class ReaderResource {

        @POST
        @Path("postReaderGetReader")
        public Reader postReader(Reader reader) throws IOException {
            return reader;
        }

        @GET
        @Path("getReader")
        public Reader getReader() throws IOException {
            return new StringReader(GET_READER_RESPONSE);
        }

        @GET
        @Path("getEmpty")
        public String getemptyResponse() throws IOException {
            return null;
        }
    }
}
