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

package org.glassfish.jersey.tests.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

import javax.xml.transform.stream.StreamSource;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static jakarta.ws.rs.client.Entity.entity;

/**
 * @author Miroslav Fuksa
 */
public class MessageBodyExceptionWrappingTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(TestResource.class);
    }

    /**
     * Tests whether fail of message body writer causes throwing exception. Previously the
     * exception was not thrown and 500 status code was returned in the response.
     */
    @Test
    public void testWrapping() {
        WebTarget resource = target().path("test");
        StreamSource source = new StreamSource() {
            @Override
            public InputStream getInputStream() {
                throw new WebApplicationException(555);
            }
        };
        try {
            Response response = resource.request().post(Entity.entity(source, MediaType.TEXT_XML_TYPE));
            fail("Exception expected, instead response with " + response.getStatus() + " status has been returned.");
        } catch (WebApplicationException e) {
            assertEquals(555, e.getResponse().getStatus());
        }

        try {
            Response response = resource.request().post(Entity.entity(source, MediaType.TEXT_XML_TYPE), Response.class);
            fail("Exception expected, instead response with " + response.getStatus() + " status has been returned.");
        } catch (WebApplicationException e) {
            assertEquals(555, e.getResponse().getStatus());
        }
    }

    @Path("test")
    public static class TestResource {

        @POST
        public String echo(String entity) {
            return entity;
        }
    }

    /**
     * Provider reproducing JERSEY-1990.
     */
    @Produces("text/foo")
    public static class ThrowingUpProvider implements MessageBodyWriter<String>, MessageBodyReader<String> {
        public volatile int counter = 0;

        public int getInvocationCount() {
            return counter;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            counter++;
            final RuntimeException up = new RuntimeException("lunch");
            throw up;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders,
                            OutputStream entityStream) throws IOException, WebApplicationException {
            throw new UnsupportedOperationException("This method should not have ever been called.");
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            counter++;
            final RuntimeException up = new RuntimeException("dinner");
            throw up;
        }

        @Override
        public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders,
                               InputStream entityStream) throws IOException, WebApplicationException {
            throw new UnsupportedOperationException("This method should not have ever been called.");
        }
    }

    /**
     * Reproducer for JERSEY-1990.
     */
    @Test
    public void testMbwHandlingExceptionInIsReadableWritable() {
        ThrowingUpProvider throwingUpProvider = new ThrowingUpProvider();

        final String response =
                target("test").register(throwingUpProvider).request("text/foo").post(entity("hello", "text/foo"), String.class);

        assertEquals("hello", response);
        assertEquals(2, throwingUpProvider.getInvocationCount());
    }

}
