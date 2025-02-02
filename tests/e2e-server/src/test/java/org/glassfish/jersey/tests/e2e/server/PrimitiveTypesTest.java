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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests primitive types as entity.
 *
 * @author Miroslav Fuksa
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PrimitiveTypesTest extends JerseyTest {
    @Override
    protected ResourceConfig configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testInteger() {
        WebTarget web = target().path("test");
        Response response = web.path("Integer").request().post(Entity.entity(5, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new Integer(6), response.readEntity(Integer.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveInt() {
        WebTarget web = target().path("test");
        Response response = web.path("int").request().post(Entity.entity(5, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new Integer(6), response.readEntity(Integer.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveIntNull() {
        WebTarget web = target().path("test");
        Response response = web.path("int").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(400, response.getStatus());
    }


    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testLong() {
        WebTarget web = target().path("test");
        Response response = web.path("Long").request().post(Entity.entity(5L, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new Long(6), response.readEntity(Long.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveLong() {
        WebTarget web = target().path("test");
        Response response = web.path("long").request().post(Entity.entity(5L, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(6L, (long) response.readEntity(long.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveLongNull() {
        WebTarget web = target().path("test");
        Response response = web.path("long").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(400, response.getStatus());
    }


    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testShort() {
        WebTarget web = target().path("test");
        Response response = web.path("Short").request().post(Entity.entity((short) 5, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new Short((short) 6), response.readEntity(Short.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveShort() {
        WebTarget web = target().path("test");
        Response response = web.path("short").request().post(Entity.entity((short) 5, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals((short) 6, (short) response.readEntity(short.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveShortNull() {
        WebTarget web = target().path("test");
        Response response = web.path("short").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(400, response.getStatus());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testByte() {
        WebTarget web = target().path("test");
        Response response = web.path("Byte").request().post(Entity.entity((byte) 5, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new Byte((byte) 6), response.readEntity(Byte.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveByte() {
        WebTarget web = target().path("test");
        Response response = web.path("byte").request().post(Entity.entity((byte) 5, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals((byte) 6, (byte) response.readEntity(byte.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveByteNull() {
        WebTarget web = target().path("test");
        Response response = web.path("byte").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(400, response.getStatus());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testFloat() {
        WebTarget web = target().path("test");
        Response response = web.path("Float").request().post(Entity.entity((float) 5, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new Float(6), response.readEntity(Float.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveFloat() {
        WebTarget web = target().path("test");
        Response response = web.path("float").request().post(Entity.entity(5f, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(6f, response.readEntity(float.class), 0.0f);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveFloatNull() {
        WebTarget web = target().path("test");
        Response response = web.path("float").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(400, response.getStatus());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testDouble() {
        WebTarget web = target().path("test");
        Response response = web.path("Double").request().post(Entity.entity((double) 5, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new Double(6), response.readEntity(Double.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveDouble() {
        WebTarget web = target().path("test");
        Response response = web.path("double").request().post(Entity.entity(5d, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(6d, response.readEntity(double.class), 0.0d);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveDoubleNull() {
        WebTarget web = target().path("test");
        Response response = web.path("double").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(400, response.getStatus());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testCharacter() {
        WebTarget web = target().path("test");
        Response response = web.path("Character").request().post(Entity.entity('a', MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new Character('b'), response.readEntity(Character.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveCharacter() {
        WebTarget web = target().path("test");
        Response response = web.path("char").request().post(Entity.entity('a', MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals('b', (char) response.readEntity(char.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveCharacterNull() {
        WebTarget web = target().path("test");
        Response response = web.path("char").request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(400, response.getStatus());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testString() {
        WebTarget web = target().path("test");
        Response response = web.path("String").request().post(Entity.entity("String", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("StringPOST", response.readEntity(String.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testBoolean() {
        WebTarget web = target().path("test");
        Response response = web.path("Boolean").request().post(Entity.entity(Boolean.TRUE, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(Boolean.FALSE, response.readEntity(Boolean.class));
    }


    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testPrimitiveBoolean() {
        WebTarget web = target().path("test");
        Response response = web.path("boolean").request().post(Entity.entity(true, MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertFalse(response.readEntity(boolean.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testBigDecimal() {
        WebTarget web = target().path("test");
        Response response = web.path("bigDecimal").request().post(Entity.entity(new BigDecimal("15"), MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new BigDecimal("16"), response.readEntity(BigDecimal.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testBigInteger() {
        WebTarget web = target().path("test");
        Response response = web.path("bigInteger").request().post(Entity.entity(new BigInteger("15"), MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(new BigInteger("16"), response.readEntity(BigInteger.class));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testAtomicInteger() {
        WebTarget web = target().path("test");
        Response response = web.path("atomicInteger").request().post(Entity.entity(new AtomicInteger(15),
                MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(16, response.readEntity(AtomicInteger.class).get());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testAtomicLong() {
        WebTarget web = target().path("test");
        Response response = web.path("atomicLong").request().post(Entity.entity(new AtomicLong(15),
                MediaType.TEXT_PLAIN_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals(16, response.readEntity(AtomicLong.class).get());
    }


    @Path("test")
    public static class Resource {
        @GET
        @Path("testik")
        public String test(@QueryParam("id") int id) {
            System.out.println(id);
            return String.valueOf(id);
        }

        @POST
        @Path("Integer")
        public Integer postInteger(Integer i) {
            return i + 1;
        }

        @POST
        @Path("int")
        public int postInt(int i) {
            return i + 1;
        }

        @POST
        @Path("Long")
        public Long postLong(Long l) {
            return l + 1;
        }

        @POST
        @Path("long")
        public long postLong(long l) {
            return l + 1;
        }

        @POST
        @Path("Short")
        public long postShort(Short s) {
            return s + 1;
        }


        @POST
        @Path("short")
        public long postPrimitiveShort(short s) {
            return s + 1;
        }


        @POST
        @Path("Byte")
        public Byte postByte(Byte b) {
            return (byte) (b + 1);
        }

        @POST
        @Path("byte")
        public byte postPrimitiveByte(byte b) {
            return (byte) (b + 1);
        }


        @POST
        @Path("Float")
        public Float postFloat(Float f) {
            return f + 1f;
        }


        @POST
        @Path("float")
        public float postPrimitiveFloat(float f) {
            return f + 1;
        }

        @POST
        @Path("Double")
        public Double postDouble(Double d) {
            return d + 1d;
        }


        @POST
        @Path("double")
        public double postPrimitiveDouble(double d) {
            return d + 1;
        }


        @POST
        @Path("Character")
        public Character postCharacter(Character c) {
            byte b = (byte) (c.charValue());
            b = (byte) (b + 1);
            return (char) b;
        }

        @POST
        @Path("char")
        public char postPrimitiveChar(char c) {
            byte b = (byte) c;
            b = (byte) (b + 1);
            return (char) b;
        }

        @POST
        @Path("String")
        public String postString(String str) {
            return str + "POST";
        }

        @POST
        @Path("Boolean")
        public Boolean postBoolean(Boolean b) {
            boolean bool = b;
            return !bool;
        }

        @POST
        @Path("boolean")
        public Boolean postPrimitiveBoolean(boolean b) {
            return !b;
        }

        @Path("bigDecimal")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public BigDecimal number(BigDecimal number) {
            return number.add(new BigDecimal("1"));
        }


        @Path("bigInteger")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public BigInteger bigInteger(BigInteger bigInteger) {
            return bigInteger.add(new BigInteger("1"));
        }

        @Path("atomicInteger")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public AtomicInteger atomicInteger(AtomicInteger atomic) {
            atomic.incrementAndGet();
            return atomic;
        }

        @Path("atomicLong")
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public AtomicLong atomicLong(AtomicLong atomic) {
            atomic.incrementAndGet();
            return atomic;
        }
    }
}
