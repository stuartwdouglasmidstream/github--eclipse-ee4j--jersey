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

package org.glassfish.jersey.tests.cdi.inject;

import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.servlet.ServletProperties;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.SseEventSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SseTest extends JerseyTest {
    private Weld weld;

    @BeforeEach
    public void setup() {
        Assumptions.assumeTrue(Hk2InjectionManagerFactory.isImmediateStrategy());
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        if (Hk2InjectionManagerFactory.isImmediateStrategy()) {
            weld = new Weld();
            weld.initialize();
            super.setUp();
        }
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        if (Hk2InjectionManagerFactory.isImmediateStrategy()) {
            weld.shutdown();
            super.tearDown();
        }
    }

    @Override
    protected Application configure() {
        return new SseAplication();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.builder(configure())
                .initParam(ServletProperties.JAXRS_APPLICATION_CLASS, SseAplication.class.getName())
                .build();
    }

    @Test
    public void testContextSse() throws InterruptedException {
        testSse("contexted");
    }

    @Test
    public void testInjectSse() throws InterruptedException {
        testSse("injected");
    }

    private void testSse(String injectType) throws InterruptedException {
        final String entity = "Everyone !!!";
        final CountDownLatch broadcastLatch = new CountDownLatch(2);
        final CountDownLatch registerLatch = new CountDownLatch(1);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        final WebTarget target = target(InjectionChecker.ROOT).path("register").path(injectType);
        try (SseEventSource source = SseEventSource.target(target).build()) {
            source.register(inboundSseEvent -> {
                try {
                    byteArrayOutputStream.write(inboundSseEvent.readData(String.class).getBytes());
                    registerLatch.countDown();
                    broadcastLatch.countDown();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            source.open();
            registerLatch.await(5000, TimeUnit.MILLISECONDS);
            Assertions.assertEquals(0, registerLatch.getCount());

            try (Response response = target(InjectionChecker.ROOT).path("broadcast").path(injectType)
                    .request()
                    .post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE))) {
                String readEntity = response.readEntity(String.class);
                // System.out.println(readEntity);
                Assertions.assertEquals(response.getStatus(), Response.Status.NO_CONTENT.getStatusCode(), readEntity);

            }
            broadcastLatch.await(5000, TimeUnit.MILLISECONDS);
        }
        Assertions.assertTrue(byteArrayOutputStream.toString().contains(entity));
        Assertions.assertEquals(0, broadcastLatch.getCount());
    }
}
