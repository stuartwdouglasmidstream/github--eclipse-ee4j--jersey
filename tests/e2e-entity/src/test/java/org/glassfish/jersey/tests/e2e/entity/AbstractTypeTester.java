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

package org.glassfish.jersey.tests.e2e.entity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Abstract entity type tester base class.
 *
 * @author Paul Sandoz
 * @author Martin Matula
 * @author Marek Potociar
 */
public abstract class AbstractTypeTester extends JerseyTest {

    protected static final ThreadLocal localRequestEntity = new ThreadLocal();

    public abstract static class AResource<T> {

        @POST
        public T post(T t) {
            return t;
        }
    }

    public static class RequestEntityInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext) throws IOException, WebApplicationException {
            OutputStream original = writerInterceptorContext.getOutputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writerInterceptorContext.setOutputStream(baos);
            writerInterceptorContext.proceed();
            final byte[] requestEntity = baos.toByteArray();
            writerInterceptorContext.setProperty("requestEntity", requestEntity);
            original.write(requestEntity);
        }
    }

    public static class ResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            localRequestEntity.set(requestContext.getProperty("requestEntity"));
        }
    }

    /**
     * Looks for all resources and providers declared as inner classes of the subclass of this class
     * and adds them to the returned ResourceConfig (unless constrained to client side).
     *
     * @return ResourceConfig instance
     */
    @Override
    protected Application configure() {
        HashSet<Class<?>> classes = new HashSet<Class<?>>();

        for (Class<?> cls : getClass().getDeclaredClasses()) {
            if (cls.getAnnotation(Path.class) != null) {
                classes.add(cls);
            } else if (cls.getAnnotation(Provider.class) != null) {
                final ConstrainedTo constrainedTo = cls.getAnnotation(ConstrainedTo.class);
                if (constrainedTo == null || constrainedTo.value() == RuntimeType.SERVER) {
                    classes.add(cls);
                }
            }
        }

        return new ResourceConfig(classes);
    }

    /**
     * Looks for all providers declared as inner classes of the subclass of this class
     * and adds them to the client configuration (unless constrained to server side).
     */
    @Override
    protected void configureClient(ClientConfig config) {
        config.register(RequestEntityInterceptor.class);
        config.register(ResponseFilter.class);

        for (Class<?> cls : getClass().getDeclaredClasses()) {
            if (cls.getAnnotation(Provider.class) != null) {
                final ConstrainedTo constrainedTo = cls.getAnnotation(ConstrainedTo.class);
                if (constrainedTo == null || constrainedTo.value() == RuntimeType.CLIENT) {
                    config.register(cls);
                }
            }
        }
    }

    protected <T> void _test(T in, Class resource) {
        _test(in, resource, true);
    }

    protected <T> void _test(T in, Class resource, MediaType m) {
        _test(in, resource, m, true);
    }

    protected <T> void _test(T in, Class resource, boolean verify) {
        _test(in, resource, MediaType.TEXT_PLAIN_TYPE, verify);
    }

    protected <T> void _test(T in, Class resource, MediaType m, boolean verify) {
        WebTarget target = target(resource.getSimpleName());
        Response response = target.request().post(Entity.entity(in, m));

        byte[] inBytes = getRequestEntity();
        byte[] outBytes = getEntityAsByteArray(response);

        if (verify) {
            _verify(inBytes, outBytes);
        }
    }

    protected static byte[] getRequestEntity() {
        try {
            return (byte[]) localRequestEntity.get();
        } finally {
            localRequestEntity.set(null);
        }
    }

    protected static void _verify(byte[] in, byte[] out) {
        assertEquals(in.length, out.length);
        for (int i = 0; i < in.length; i++) {
            if (in[i] != out[i]) {
                assertEquals(in[i], out[i], "Index: " + i);
            }
        }
    }

    protected static byte[] getEntityAsByteArray(Response r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ReaderWriter.writeTo(r.readEntity(InputStream.class), baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }
}
