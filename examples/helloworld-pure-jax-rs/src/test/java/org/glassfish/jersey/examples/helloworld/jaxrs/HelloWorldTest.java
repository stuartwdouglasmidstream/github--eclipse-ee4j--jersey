/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.helloworld.jaxrs;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;

/**
 * Simple test to confirm the server is running and serving our resource.
 *
 * @author Martin Matula
 */
public class HelloWorldTest {
    @Test
    public void testHelloWorld() throws Exception {
        HttpServer server = App.startServer();

        WebTarget target = ClientBuilder.newClient().target(App.getBaseURI() + "helloworld");
        assertEquals(HelloWorldResource.CLICHED_MESSAGE, target.request(MediaType.TEXT_PLAIN).get(String.class));

        server.stop(0);
    }
}
