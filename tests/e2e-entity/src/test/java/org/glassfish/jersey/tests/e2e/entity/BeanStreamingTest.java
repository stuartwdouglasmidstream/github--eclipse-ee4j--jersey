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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Paul Sandoz
 * @author Martin Matula
 */
public class BeanStreamingTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(getClass().getDeclaredClasses());
    }

    @Override
    protected void configureClient(ClientConfig config) {
        for (Class<?> c : getClass().getDeclaredClasses()) {
            if (c.getAnnotation(Provider.class) != null) {
                config.register(c);
            }
        }
    }

    @Test
    public void testBean() throws Exception {
        Bean b = new Bean("bean", 123, 3.1415f);

        // the following should work using BeanProvider which
        // supports Bean.class for type application/bean
        WebTarget r = target().path("/bean");
        r.request().post(Entity.entity(b, "application/bean"), Bean.class);

        try {
            r = target().path("/plain");
            r.request().post(Entity.entity(b, "text/plain"), Bean.class);
            assertFalse(false);
        } catch (ProcessingException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testBeanWild() throws Exception {
        Bean b = new Bean("bean", 123, 3.1415f);

        // the following should work using BeanWildProvider which
        // supports Bean.class for type application/*
        target().path("/wild").request().post(Entity.entity(b, "application/wild-bean"), Bean.class);
    }

    @Test
    public void testBean2() throws Exception {
        Bean2 b = new Bean2("bean", 123, 3.1415f);

        target().path("/bean2").request().post(Entity.entity(b, "application/bean"), Bean2.class);

        try {
            target().path("/plain2").request().post(Entity.entity(b, "text/plain"), Bean2.class);
            assertFalse(false);
        } catch (ProcessingException ex) {
            assertTrue(true);
        }
    }

    @Test
    public void testBean2UsingBean() throws Exception {
        Bean2 b = new Bean2("bean", 123, 3.1415f);

        // the following should work using BeanProvider which
        // supports Bean.class for type application/bean
        target().path("/bean").request().post(Entity.entity(b, "application/bean"), Bean2.class);

        try {
            target().path("/plain").request().post(Entity.entity(b, "text/plain"), Bean2.class);
            fail();
        } catch (ProcessingException ex) {
            // good
        }
    }

    @Test
    public void testBean2Wild() throws Exception {
        Bean2 b = new Bean2("bean", 123, 3.1415f);

        // the following should work using BeanWildProvider which
        // supports Bean.class for type application/*
        target().path("/wild2").request().post(Entity.entity(b, "application/wild-bean"), Bean2.class);
    }

    @Test
    public void testBean2WildUsingBean() throws Exception {
        Bean2 b = new Bean2("bean", 123, 3.1415f);

        // the following should work using BeanWildProvider which
        // supports Bean.class for type application/*
        target().path("/wild").request().post(Entity.entity(b, "application/wild-bean"), Bean2.class);
    }

    public static class Bean implements Serializable {
        private String string;
        private int integer;
        private float real;

        public Bean() {
        }

        public Bean(String string, int integer, float real) {
            this.string = string;
            this.integer = integer;
            this.real = real;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }

        public float getReal() {
            return real;
        }

        public void setReal(float real) {
            this.real = real;
        }
    }

    @Provider
    @Produces("application/bean")
    @Consumes("application/bean")
    public static class BeanProvider extends AbstractMessageReaderWriterProvider<Bean> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean.class;
        }

        public Bean readFrom(
                Class<Bean> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders,
                InputStream entityStream) throws IOException {
            ObjectInputStream oin = new ObjectInputStream(entityStream);
            try {
                return (Bean) oin.readObject();
            } catch (ClassNotFoundException cause) {
                IOException effect = new IOException(cause.getLocalizedMessage());
                effect.initCause(cause);
                throw effect;
            }
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean.class;
        }

        public void writeTo(
                Bean t,
                Class<?> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException {
            ObjectOutputStream out = new ObjectOutputStream(entityStream);
            out.writeObject(t);
            out.flush();
        }
    }

    @Provider
    @Produces("application/*")
    @Consumes("application/*")
    public static class BeanWildProvider extends BeanProvider {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean.class;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean.class;
        }
    }

    @Provider
    @Produces("application/bean")
    @Consumes("application/bean")
    public static class Bean2Provider extends AbstractMessageReaderWriterProvider<Bean2> {

        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean2.class;
        }

        public Bean2 readFrom(
                Class<Bean2> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, String> httpHeaders,
                InputStream entityStream) throws IOException {
            ObjectInputStream oin = new ObjectInputStream(entityStream);
            try {
                return (Bean2) oin.readObject();
            } catch (ClassNotFoundException cause) {
                IOException effect = new IOException(cause.getLocalizedMessage());
                effect.initCause(cause);
                throw effect;
            }
        }

        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean2.class;
        }

        public void writeTo(
                Bean2 t,
                Class<?> type,
                Type genericType,
                Annotation annotations[],
                MediaType mediaType,
                MultivaluedMap<String, Object> httpHeaders,
                OutputStream entityStream) throws IOException {
            ObjectOutputStream out = new ObjectOutputStream(entityStream);
            out.writeObject(t);
            out.flush();
        }
    }

    @Provider
    @Produces("application/*")
    @Consumes("application/*")
    public static class Bean2WildProvider extends Bean2Provider {
        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean2.class;
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[], MediaType mt) {
            return type == Bean2.class;
        }
    }

    public static class Bean2 extends Bean {
        public Bean2(String string, int integer, float real) {
            super(string, integer, real);
        }
    }

    @Path("/bean")
    public static class BeanResource {
        @POST
        @Consumes("application/bean")
        @Produces("application/bean")
        public Bean post(Bean t) {
            return t;
        }
    }

    @Path("/bean2")
    public static class Bean2Resource {
        @POST
        @Consumes("application/bean")
        @Produces("application/bean")
        public Bean2 post(Bean2 t) {
            return t;
        }
    }

    @Path("/plain")
    public static class BeanTextPlainResource {
        @POST
        @Consumes("text/plain")
        @Produces("text/plain")
        public Bean post(Bean t) {
            return t;
        }
    }

    @Path("/plain2")
    public static class Bean2TextPlainResource {
        @POST
        @Consumes("text/plain")
        @Produces("text/plain")
        public Bean2 post(Bean2 t) {
            return t;
        }
    }

    @Path("/wild")
    public static class BeanWildResource {
        @POST
        @Consumes("application/*")
        @Produces("application/*")
        public Bean post(Bean t) {
            return t;
        }
    }

    @Path("/wild2")
    public static class Bean2WildResource {
        @POST
        @Consumes("application/*")
        @Produces("application/*")
        public Bean2 post(Bean2 t) {
            return t;
        }
    }
}
