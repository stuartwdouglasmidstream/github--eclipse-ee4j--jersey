/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.spring.parameterinjection;

import org.glassfish.jersey.server.spring.SpringTestConfiguration;
import org.glassfish.jersey.server.spring.fieldinjection.SpringFieldInjectionJerseyTestConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import jakarta.ws.rs.core.Application;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpringParameterInjectionTest extends JerseyTest {
    @Override
    protected Application configure() {
        ApplicationContext context = new AnnotationConfigApplicationContext(SpringTestConfiguration.class);
        return new SpringParameterInjectionJerseyTestConfig()
                .property("contextConfig", context);
    }

    @Test
    public void testInjectionOfSingleBean() {
        String result = target("test1").request().get(String.class);
        assertEquals("test ok", result);
    }

    @Test
    public void testInjectionOfListOfBeans() {
        String result = target("test2").request().get(String.class);
        assertEquals("test ok", result);
    }

    @Test
    public void testInjectionOfSetOfBeans() {
        String result = target("test3").request().get(String.class);
        assertEquals("test ok", result);
    }
}
