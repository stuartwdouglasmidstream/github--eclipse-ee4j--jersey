/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.json;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Priorities;

public class JsonWithoutExceptionMapperTest extends JerseyTest {

    public static class BooleanEntity {
        public boolean data;
    }

    @Priority(2 * Priorities.USER)
    public static class LowPriorityExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception exception) {
            return Response.accepted().entity(getClass().getSimpleName()).build();
        }
    }

    @Path("/")
    public static class Resource {
        @POST
        public Boolean value(BooleanEntity entity) {
            return entity.data;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, LowPriorityExceptionMapper.class)
                .register(JacksonFeature.withoutExceptionMappers());
    }

    @Test
    public void testZullBooleanValue() {
        String response = target().request(MediaType.APPLICATION_JSON)
                .buildPost(Entity.entity("zull", MediaType.APPLICATION_JSON_TYPE)).invoke().readEntity(String.class);
        Assertions.assertFalse(response.contains("zull"));
        Assertions.assertTrue(response.equals(LowPriorityExceptionMapper.class.getSimpleName()));
    }
}
