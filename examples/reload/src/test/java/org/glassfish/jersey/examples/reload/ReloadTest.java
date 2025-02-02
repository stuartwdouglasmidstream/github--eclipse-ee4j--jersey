/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.reload;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is to test the reload feature without updating the resources text file.
 *
 * @author Jakub Podlesak
 */
public class ReloadTest extends JerseyTest {

    private static Container container;

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);

        final ResourceConfig result = new ResourceConfig(ArrivalsResource.class);

        result.registerInstances(new AbstractContainerLifecycleListener() {
            @Override
            public void onStartup(Container container) {
                ReloadTest.container = container;
            }
        });

        return result;
    }

    @Test
    public void testReload() {

        // hit arrivals
        Response response = target().path("arrivals").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());

        // make sure stats resource is not found
        response = target().path("stats").request(MediaType.TEXT_PLAIN).get();
        assertEquals(404, response.getStatus());

        // add stats resource
        container.reload(new ResourceConfig(ArrivalsResource.class, StatsResource.class));

        // check stats
        response = target().path("stats").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("1"), "1 expected as number of arrivals hits in stats");

        // another arrivals hit
        response = target().path("arrivals").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());

        // check updated stats
        response = target().path("stats").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("2"), "2 expected as number of arrivals hits in stats");

        // remove stats
        container.reload(new ResourceConfig(ArrivalsResource.class));

        // make sure stats resource is not found
        response = target().path("stats").request(MediaType.TEXT_PLAIN).get();
        assertEquals(404, response.getStatus());
    }
}
