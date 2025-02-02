/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.cdi.resources;

import java.net.URI;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.test.JerseyTest;

import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

/**
 * Test for CDI web application resources.
 * Run with:
 * <pre>
 * mvn clean package
 * $AS_HOME/bin/asadmin deploy target/cdi-webapp
 * mvn -DskipTests=false test</pre>
 *
 * @author Jakub Podlesak
 */
public class CdiTest extends JerseyTest {

    Weld weld;

    @BeforeEach
    public void setup() {
        Assumptions.assumeTrue(Hk2InjectionManagerFactory.isImmediateStrategy());
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        weld = new Weld();
        weld.initialize();
        super.setUp();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        weld.shutdown();
        super.tearDown();
    }

    @Override
    protected Application configure() {
        return new MyApplication();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("cdi-webapp").build();
    }
}
