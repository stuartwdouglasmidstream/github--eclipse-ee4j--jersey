/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.jersey_ejb.test;

import java.net.URI;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.examples.jersey_ejb.resources.MyApplication;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Basic test for adding/removing messages.
 *
 * Tests currently disabled in pom.xml file.
 *
 * To test the app, mvn clean package and asadmin deploy target/jersey-ejb
 * and then run the tests using extenrnal test container factory:
 * mvn -Prun-external-tests test
 *
 * @author Pavel Bucek
 */
public class MessageBoardTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new MyApplication();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("jersey-ejb").build();
    }

    /**
     * Regression test for JERSEY-2541.
     */
    @Test
    public void testListMessages() {
        Response response = target().path("app/messages").request(MediaType.TEXT_HTML).get();

        assertEquals(200, response.getStatus(),
                String.format("Response status should be 200. Current value is %d.", response.getStatus()));
    }

    @Test
    public void testAddMessage() {

        Response response = target().path("app/messages").request(MediaType.TEXT_PLAIN)
                .post(Entity.entity("hello world!", MediaType.TEXT_PLAIN));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                "Response status should be CREATED. Current value is \"" + response.getStatus() + "\"");

        client().target(response.getLocation()).request().delete(); // remove added message
    }

    @Test
    public void testDeleteMessage() {

        URI u = null;

        Response response = target().path("app/messages").request().post(Entity.entity("toDelete", MediaType.TEXT_PLAIN));
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            u = response.getLocation();
        } else {
            fail();
        }

        String s = client().target(u).request().get(String.class);

        assertTrue(s.contains("toDelete"));

        Response firstDeleteResponse = client().target(u).request().delete();
        final int successfulDeleteResponseStatus = firstDeleteResponse.getStatus();
        assertTrue((200 <= successfulDeleteResponseStatus) && (successfulDeleteResponseStatus < 300),
                "First DELETE request should return with a 2xx status code");

        Response nonExistentGetResponse = client().target(u).request().get();
        assertEquals(404, nonExistentGetResponse.getStatus(), "GET request to a non existent resource should return 404");

        Response nonExistentDeleteResponse = client().target(u).request().delete();
        assertEquals(404, nonExistentDeleteResponse.getStatus(), "DELETE request to a non existent resource should return 404");
    }

}
