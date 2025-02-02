/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.jsonmoxy;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Pavel Bucek
 * @author Michal Gajdos
 */
public class JsonResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return App.createApp();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(App.createMoxyJsonResolver());
    }

    @Test
    public void testGet() {
        final WebTarget target = target("test");
        final TestBean testBean = target.request(MediaType.APPLICATION_JSON_TYPE).get(TestBean.class);

        assertEquals(testBean, new TestBean("a", 1, 1L));
    }

    @Test
    public void roundTripTest() {
        final WebTarget target = target("test");
        final TestBean testBean = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(new TestBean("a", 1, 1L), MediaType.APPLICATION_JSON_TYPE), TestBean.class);

        assertEquals(testBean, new TestBean("a", 1, 1L));
    }
}
