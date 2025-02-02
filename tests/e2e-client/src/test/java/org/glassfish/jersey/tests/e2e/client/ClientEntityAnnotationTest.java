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

package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.lang.annotation.Annotation;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests annotations of entity on the client side.
 *
 * @author Miroslav Fuksa
 *
 */
public class ClientEntityAnnotationTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(ClientFilter.class);
    }

    @Test
    public void test() {
        Annotation[] annotations = MyProvider.class.getAnnotations();
        Entity<String> post = Entity.entity("test", MediaType.WILDCARD_TYPE,
                annotations);
        final Response response = target().path("resource").request().post(post);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("test", response.readEntity(String.class));
    }


    public static class ClientFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            final Annotation[] entityAnnotations = requestContext.getEntityAnnotations();
            Assertions.assertEquals(1, entityAnnotations.length);
            Assertions.assertEquals(MyProvider.class.getAnnotation(Provider.class), entityAnnotations[0]);
        }
    }

    @Path("resource")
    public static class Resource {
        @POST
        public String post(String entity) {
            return entity;
        }
    }

    @Provider
    public static class MyProvider {
    }
}
