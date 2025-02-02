/*
 * Copyright (c) 2011, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.helloworld;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HelloWorldTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        return App.create();
    }

    @Test
    public void testHelloWorld() throws Exception {
        Client client = ClientBuilder.newClient();

        assertFalse(App.getMethodCalled);
        Response response = client.target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).build().toString())
                .request("text/plain").get();
        assertTrue(App.getMethodCalled);
        assertEquals(200, response.getStatus());
        assertTrue(response.hasEntity());
        assertTrue("Hello World!".equals(response.readEntity(String.class)));

        String s = client.target(getBaseUri()).path("helloworld").request().get(String.class);
        assertTrue(s.equals("Hello World!"));
    }

    @Test
    public void testHelloWorldOtherMethods() throws Exception {
        Client client = ClientBuilder.newClient();
        assertFalse(App.headMethodCalled);
        Response response = client.target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).build().toString())
                .request("text/plain").head();
        assertTrue(App.headMethodCalled);
        assertEquals(204, response.getStatus());

        response = client.target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).build().toString()).request("text/plain")
                .options();
        assertEquals(204, response.getStatus());
    }
}
