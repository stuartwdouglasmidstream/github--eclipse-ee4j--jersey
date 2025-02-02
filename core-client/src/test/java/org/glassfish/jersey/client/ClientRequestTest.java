/*
 * Copyright (c) 2013, 2023 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.URI;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.WriterInterceptor;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import org.glassfish.jersey.message.MessageBodyWorkers;

import org.hamcrest.core.Is;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;

/**
 * {@code ClientRequest} unit tests.
 *
 * @author Marek Potociar
 */
//@RunWith(MockitoJUnitRunner.class)
public class ClientRequestTest {

    @Mock
    private MessageBodyWorkers workers;
    @Mock
    private GenericType<?> entityType;

    private AutoCloseable mockito;

    @BeforeEach
    public void initMocks() {
        mockito = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void closeMocks() throws Exception {
        mockito.close();
    }

    /**
     * Test of resolving properties in the client request.
     */
    @Test
    public void testResolveProperty() {
        JerseyClient client;
        ClientRequest request;

        // test property in neither config nor request
        client = new JerseyClientBuilder().build();

        request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());

        assertFalse(request.getConfiguration().getPropertyNames().contains("name"));
        assertFalse(request.getPropertyNames().contains("name"));

        assertFalse(request.getConfiguration().hasProperty("name"));
        assertFalse(request.hasProperty("name"));

        assertNull(request.getConfiguration().getProperty("name"));
        assertNull(request.getProperty("name"));

        assertNull(request.resolveProperty("name", String.class));
        assertEquals("value-default", request.resolveProperty("name", "value-default"));

        // test property in config only
        client = new JerseyClientBuilder().property("name", "value-global").build();

        request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());

        assertTrue(request.getConfiguration().getPropertyNames().contains("name"));
        assertFalse(request.getPropertyNames().contains("name"));

        assertTrue(request.getConfiguration().hasProperty("name"));
        assertFalse(request.hasProperty("name"));

        assertEquals("value-global", request.getConfiguration().getProperty("name"));
        assertNull(request.getProperty("name"));

        assertEquals("value-global", request.resolveProperty("name", String.class));
        assertEquals("value-global", request.resolveProperty("name", "value-default"));

        // test property in request only
        client = new JerseyClientBuilder().build();

        request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());
        request.setProperty("name", "value-request");

        assertFalse(request.getConfiguration().getPropertyNames().contains("name"));
        assertTrue(request.getPropertyNames().contains("name"));

        assertFalse(request.getConfiguration().hasProperty("name"));
        assertTrue(request.hasProperty("name"));

        assertNull(request.getConfiguration().getProperty("name"));
        assertEquals("value-request", request.getProperty("name"));

        assertEquals("value-request", request.resolveProperty("name", String.class));
        assertEquals("value-request", request.resolveProperty("name", "value-default"));

        // test property in config and request
        client = new JerseyClientBuilder().property("name", "value-global").build();

        request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());
        request.setProperty("name", "value-request");

        assertTrue(request.getConfiguration().getPropertyNames().contains("name"));
        assertTrue(request.getPropertyNames().contains("name"));

        assertTrue(request.getConfiguration().hasProperty("name"));
        assertTrue(request.hasProperty("name"));

        assertEquals("value-global", request.getConfiguration().getProperty("name"));
        assertEquals("value-request", request.getProperty("name"));

        assertEquals("value-request", request.resolveProperty("name", String.class));
        assertEquals("value-request", request.resolveProperty("name", "value-default"));
    }

    private ClientRequest mockThrowing(Exception exception) throws IOException {
        JerseyClient client = new JerseyClientBuilder().build();
        final ClientRequest request = new ClientRequest(
                URI.create("http://example.org"),
                client.getConfiguration(),
                new MapPropertiesDelegate());

        Mockito.doThrow(exception).when(workers)
                .writeTo(any(), same(entityType.getRawType()), same(entityType.getType()),
                        Mockito.<Annotation[]>any(), Mockito.<MediaType>any(),
                        Mockito.<MultivaluedMap<String, Object>>any(), Mockito.<PropertiesDelegate>any(),
                        Mockito.<OutputStream>any(), Mockito.<Iterable<WriterInterceptor>>any());
        return request;
    }

    @Test
    public void testSSLExceptionHandling()
            throws Exception {
        final IOException ioException = new IOException("Test");

        final ClientRequest request = mockThrowing(ioException);

        try {
            request.doWriteEntity(workers, entityType);
            fail("An IOException exception should be thrown.");
        } catch (IOException e) {
            MatcherAssert.assertThat("Detected a un-expected exception! \n" + ExceptionUtils.exceptionStackTraceAsString(e),
                    e, Is.is(ioException));
        }
    }

    @Test
    public void testRuntimeExceptionBeingReThrown()
            throws Exception {

        final RuntimeException runtimeException = new RuntimeException("Test");

        ClientRequest request = mockThrowing(runtimeException);

        try {
            request.doWriteEntity(workers, entityType);
            Assertions.fail("A RuntimeException exception should be thrown.");
        } catch (RuntimeException e) {
            MatcherAssert.assertThat("Detected a un-expected exception! \n" + ExceptionUtils.exceptionStackTraceAsString(e),
                    e, Is.is(runtimeException));
        }
    }

}
