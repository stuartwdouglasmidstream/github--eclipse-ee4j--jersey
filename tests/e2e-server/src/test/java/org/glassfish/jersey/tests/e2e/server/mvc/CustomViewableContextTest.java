/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.mvc;

import java.io.InputStream;
import java.util.Properties;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.CustomViewableContext;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.TestViewProcessor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Michal Gajdos
 */
public class CustomViewableContextTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(ExplicitTemplate.class, ImplicitTemplate.class)
                .register(MvcFeature.class)
                .register(TestViewProcessor.class)
                .register(CustomViewableContext.class);
    }

    @Path("/explicit")
    public static class ExplicitTemplate {

        @GET
        @Template
        public Viewable getViewable() {
            return new Viewable("", "method");
        }

        @GET
        @Path("annotation")
        @Template
        public String getTemplate() {
            return "annotation";
        }
    }

    @Template
    @Path("/implicit")
    public static class ImplicitTemplate {

        @Override
        public String toString() {
            return "implicit";
        }
    }

    @Test
    public void testExplicitMethod() throws Exception {
        testResource("explicit", "method");
    }

    @Test
    public void testExplicitAnnotation() throws Exception {
        testResource("explicit/annotation", "annotation");
    }

    @Test
    public void testImplicit() throws Exception {
        testResource("implicit", "implicit");
    }

    private void testResource(final String path, final String modelValue) throws Exception {
        final Properties p = new Properties();
        p.load(target(path).request().get(InputStream.class));

        assertEquals("/CustomViewableContext/index.testp", p.getProperty("path"));
        assertEquals(modelValue, p.getProperty("model"));
        assertEquals("TestViewProcessor", p.getProperty("name"));
    }
}
