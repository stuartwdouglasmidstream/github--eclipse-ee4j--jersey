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

import java.util.concurrent.Future;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests HTTP methods and entity presence.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar
 */
public class HttpMethodEntityTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testGet() {
        _test("GET", true, true);
        _test("GET", false, false);
    }

    @Test
    public void testPost() {
        _test("POST", true, false);
        _test("POST", false, false);
    }

    @Test
    public void testPut() {
        _test("PUT", true, false);
        _test("PUT", false, true);
    }

    @Test
    public void testDelete() {
        _test("DELETE", true, true);
        _test("DELETE", false, false);
    }

    @Test
    public void testHead() {
        _test("HEAD", true, true);
        _test("HEAD", false, false);
    }

    @Test
    public void testOptions() {
        _test("OPTIONS", true, false);
        _test("OPTIONS", false, false);
    }

    /**
     * Reproducer for JERSEY-2370: Sending POST without body.
     */
    @Test
    public void testEmptyPostWithoutContentType() {
        final WebTarget resource = target().path("resource");
        try {
            final Future<Response> future = resource.request().async().post(null);
            assertEquals(200, future.get().getStatus());

            final Response response = resource.request().post(null);
            assertEquals(200, response.getStatus());
        } catch (Exception e) {
            fail("Sending POST method without entity should not fail.");
        }
    }

    /**
     * Reproducer for JERSEY-2370: Sending POST without body.
     */
    @Test
    public void testEmptyPostWithContentType() {
        final WebTarget resource = target().path("resource");
        try {
            final Future<Response> future = resource.request().async().post(Entity.entity(null, "text/plain"));
            assertEquals(200, future.get().getStatus());

            final Response response = resource.request().post(Entity.entity(null, "text/plain"));
            assertEquals(200, response.getStatus());
        } catch (Exception e) {
            fail("Sending POST method without entity should not fail.");
        }
    }

    public void _test(String method, boolean entityPresent, boolean shouldFail) {
        Entity entity = entityPresent ? Entity.entity("entity", MediaType.TEXT_PLAIN_TYPE) : null;
        _testSync(method, entity, shouldFail);
        _testAsync(method, entity, shouldFail);
    }

    public void _testAsync(String method, Entity entity, boolean shouldFail) {
        try {
            final Future<Response> future = target().path("resource").request().async().method(method, entity);
            if (shouldFail) {
                fail("The method should fail.");
            }
            assertEquals(200, future.get().getStatus());
        } catch (Exception e) {
            if (!shouldFail) {
                fail("The method " + method + " with entity=" + (entity != null) + " should not fail.");
            }
        }
    }

    public void _testSync(String method, Entity entity, boolean shouldFail) {
        try {
            final Response response = target().path("resource").request().method(method, entity);
            assertEquals(200, response.getStatus());
            if (shouldFail) {
                fail("The method should fail.");
            }
        } catch (Exception e) {
            if (!shouldFail) {
                fail("The method " + method + " with entityPresent=" + (entity != null) + " should not fail.");
            }
        }
    }

    @Path("resource")
    public static class Resource {

        @Context
        HttpHeaders httpHeaders;

        @GET
        public String get() {
            return "get";
        }

        @POST
        public String post(String str) {
            // See JERSEY-1455
            assertFalse(httpHeaders.getRequestHeaders().containsKey(HttpHeaders.CONTENT_ENCODING));
            assertFalse(httpHeaders.getRequestHeaders().containsKey(HttpHeaders.CONTENT_LANGUAGE));

            return "post";
        }

        @PUT
        public String put(String str) {
            return "put";
        }

        @HEAD
        public String head() {
            return "head";
        }

        @DELETE
        public String delete() {
            return "delete";
        }
    }
}
