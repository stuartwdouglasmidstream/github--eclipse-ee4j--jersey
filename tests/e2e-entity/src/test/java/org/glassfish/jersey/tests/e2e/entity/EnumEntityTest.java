/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EnumEntityTest extends JerseyTest {

    public enum SimpleEnum {
        VALUE1,
        VALUE2
    }

    public enum ValueEnum {
        VALUE100(100),
        VALUE200(200);

        private final int value;

        ValueEnum(int value) {
            this.value = value;
        }
    }

    @Path("/")
    public static class EnumResource {
        @POST
        @Path("/simple")
        public String postSimple(SimpleEnum simpleEnum) {
            return simpleEnum.name();
        }

        @POST
        @Path("/value")
        public String postValue(ValueEnum valueEnum) {
            return valueEnum.name();
        }

        @POST
        @Path("/echo")
        public String echo(String value) {
            return value;
        }

        @PUT
        @Path("/simple")
        public SimpleEnum putSimple(String simple) {
            return SimpleEnum.valueOf(simple);
        }

        @PUT
        @Path("value")
        public ValueEnum putValue(String value) {
            return ValueEnum.valueOf(value);
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(EnumResource.class);
    }

    // Server side tests

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testSimpleEnumServerReader() {
        for (SimpleEnum value : SimpleEnum.values()) {
            try (Response r = target("simple").request(MediaType.TEXT_PLAIN_TYPE)
                    .post(Entity.entity(value.name(), MediaType.TEXT_PLAIN_TYPE))) {
                Assertions.assertEquals(value.name(), r.readEntity(String.class));
            }
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testValueEnumServerReader() {
        for (ValueEnum value : ValueEnum.values()) {
            try (Response r = target("value").request(MediaType.TEXT_PLAIN_TYPE)
                    .post(Entity.entity(value.name(), MediaType.TEXT_PLAIN_TYPE))) {
                Assertions.assertEquals(value.name(), r.readEntity(String.class));
            }
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testSimpleEnumServerWriter() {
        for (SimpleEnum value : SimpleEnum.values()) {
            try (Response r = target("simple").request(MediaType.TEXT_PLAIN_TYPE)
                    .put(Entity.entity(value.name(), MediaType.TEXT_PLAIN_TYPE))) {
                Assertions.assertEquals(value.name(), r.readEntity(String.class));
            }
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testValueEnumServerWriter() {
        for (ValueEnum value : ValueEnum.values()) {
            try (Response r = target("value").request(MediaType.TEXT_PLAIN_TYPE)
                    .put(Entity.entity(value.name(), MediaType.TEXT_PLAIN_TYPE))) {
                Assertions.assertEquals(value.name(), r.readEntity(String.class));
            }
        }
    }

    // Client side tests

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testSimpleEnumClientReader() {
        for (SimpleEnum value : SimpleEnum.values()) {
            try (Response r = target("simple").request(MediaType.TEXT_PLAIN_TYPE)
                    .post(Entity.entity(value.name(), MediaType.TEXT_PLAIN_TYPE))) {
                Assertions.assertEquals(value, r.readEntity(SimpleEnum.class));
            }
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testValueEnumClientReader() {
        for (ValueEnum value : ValueEnum.values()) {
            try (Response r = target("value").request(MediaType.TEXT_PLAIN_TYPE)
                    .post(Entity.entity(value.name(), MediaType.TEXT_PLAIN_TYPE))) {
                Assertions.assertEquals(value, r.readEntity(ValueEnum.class));
            }
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testSimpleEnumClientWriter() {
        for (SimpleEnum value : SimpleEnum.values()) {
            try (Response r = target("echo").request(MediaType.TEXT_PLAIN_TYPE)
                    .post(Entity.entity(value, MediaType.TEXT_PLAIN_TYPE))) {
                Assertions.assertEquals(value.name(), r.readEntity(String.class));
            }
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testValueEnumClientWriter() {
        for (ValueEnum value : ValueEnum.values()) {
            try (Response r = target("echo").request(MediaType.TEXT_PLAIN_TYPE)
                    .post(Entity.entity(value, MediaType.TEXT_PLAIN_TYPE))) {
                Assertions.assertEquals(value.name(), r.readEntity(String.class));
            }
        }
    }

}
