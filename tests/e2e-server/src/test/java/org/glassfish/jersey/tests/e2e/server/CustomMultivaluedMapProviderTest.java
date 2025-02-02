/*
 * Copyright (c) 2015, 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that the Custom MultivaluedMap provider overrides the default
 * EntityProvider
 *
 * @author Petr Bouda
 */
public class CustomMultivaluedMapProviderTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(
                TestResource.class,
                CustomMultivaluedMapProvider.class);
    }

    @Provider
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public static class CustomMultivaluedMapProvider implements MessageBodyReader<MultivaluedMap<String, String>> {

        @Override
        public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return MultivaluedMap.class.isAssignableFrom(type);
        }

        @Override
        @SuppressWarnings("unchecked")
        public MultivaluedMap readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
                MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
            MultivaluedMap map = new MultivaluedHashMap();
            map.add(getClass().getSimpleName(), getClass().getSimpleName().replace("Provider", "Reader"));
            return map;
        }
    }

    @Path("resource")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public static class TestResource {

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public MultivaluedMap<String, String> map(MultivaluedMap<String, String> map) {
            return map;
        }

    }

    @Test
    public void testNullFormParam() {
        Response response = target("resource").request()
                .post(Entity.entity("map", MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("CustomMultivaluedMapProvider=CustomMultivaluedMapReader", response.readEntity(String.class));
    }

}
