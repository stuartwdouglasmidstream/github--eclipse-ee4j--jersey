/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.manuallybound;

import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

public class CdiServiceInjectTest extends JerseyTest {
    private Weld weld;

    @BeforeEach
    public void setup() {
        Assumptions.assumeTrue(Hk2InjectionManagerFactory.isImmediateStrategy());
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        if (Hk2InjectionManagerFactory.isImmediateStrategy()) {
            if (!ExternalTestContainerFactory.class.isAssignableFrom(getTestContainerFactory().getClass())) {
                weld = new Weld();
                weld.initialize();
            }
            super.setUp();
        }
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        weld.shutdown();
        super.tearDown();
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(NoBeanDefiningAnnotationContainerFilter.class, Resource.class);
    }

    @Test
    public void testCdiServiceIsInjected() {
        try (Response response = target().request().get()) {
            String header = response.getStringHeaders().getFirst(NoBeanDefiningAnnotationContainerFilter.class.getSimpleName());
            Assertions.assertEquals(CdiServiceImpl.class.getSimpleName(), header);
        }
    }
}
