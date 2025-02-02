/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Paul Sandoz
 * @author Martin Matula
 */
@Suite
@SelectClasses({
        ContextResolverMediaTypeTest.ProduceTest.class,
        ContextResolverMediaTypeTest.ProducesSeparateTest.class,
        ContextResolverMediaTypeTest.ProducesTest.class,
        ContextResolverMediaTypeTest.ProducesXXXTest.class,
})
public class ContextResolverMediaTypeTest {

    @Produces("text/plain")
    @Provider
    @Disabled("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class TextPlainContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "text/plain";
        }
    }

    @Produces("text/*")
    @Provider
    @Disabled("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class TextContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "text/*";
        }
    }

    @Produces("*/*")
    @Provider
    @Disabled("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class WildcardContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "*/*";
        }
    }

    @Produces({"text/plain", "text/html"})
    @Provider
    @Disabled("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class TextPlainHtmlContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "text/plain/html";
        }

    }

    @Produces("text/html")
    @Provider
    @Disabled("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class TextHtmlContextResolver implements ContextResolver<String> {

        public String getContext(Class<?> objectType) {
            return "text/html";
        }

    }

    @Path("/")
    @Disabled("This class is not a test class & must be ignored by the Enclosed test runner.")
    public static class ContextResource {

        @Context
        Providers p;

        @Context
        ContextResolver<String> cr;

        @GET
        @Path("{id: .+}")
        public String get(@PathParam("id") MediaType m) {
            ContextResolver<String> cr = p.getContextResolver(String.class, m);

            // Verify cache is working
            ContextResolver<String> cachedCr = p.getContextResolver(String.class, m);
            assertEquals(cr, cachedCr);

            if (cr == null) {
                return "NULL";
            } else {
                return cr.getContext(null);
            }
        }
    }

    @Nested
    public static class ProduceTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ContextResource.class,
                    TextPlainContextResolver.class,
                    TextContextResolver.class,
                    WildcardContextResolver.class);
        }

        @Test
        public void testProduce() throws IOException {

            WebTarget target = target();

            assertEquals("text/plain", target.path("text/plain").request().get(String.class));
            assertEquals("text/*", target.path("text/*").request().get(String.class));
            assertEquals("*/*", target.path("*/*").request().get(String.class));

            assertEquals("text/*", target.path("text/html").request().get(String.class));

            assertEquals("*/*", target.path("application/xml").request().get(String.class));
            assertEquals("*/*", target.path("application/*").request().get(String.class));
        }
    }

    @Nested
    public static class ProducesTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ContextResource.class,
                    TextPlainHtmlContextResolver.class,
                    TextContextResolver.class,
                    WildcardContextResolver.class);
        }

        @Test
        public void testProduces() throws IOException {
            WebTarget target = target();

            assertEquals("text/plain/html", target.path("text/plain").request().get(String.class));
            assertEquals("text/plain/html", target.path("text/html").request().get(String.class));
            assertEquals("text/*", target.path("text/*").request().get(String.class));
            assertEquals("*/*", target.path("*/*").request().get(String.class));

            assertEquals("text/*", target.path("text/csv").request().get(String.class));

            assertEquals("*/*", target.path("application/xml").request().get(String.class));
            assertEquals("*/*", target.path("application/*").request().get(String.class));
        }
    }

    @Nested
    public static class ProducesSeparateTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ContextResource.class,
                    TextPlainContextResolver.class,
                    TextHtmlContextResolver.class,
                    TextContextResolver.class,
                    WildcardContextResolver.class);
        }

        @Test
        public void testProducesSeparate() throws IOException {
            WebTarget target = target();

            assertEquals("text/plain", target.path("text/plain").request().get(String.class));
            assertEquals("text/html", target.path("text/html").request().get(String.class));
            assertEquals("text/*", target.path("text/*").request().get(String.class));
            assertEquals("*/*", target.path("*/*").request().get(String.class));

            assertEquals("text/*", target.path("text/csv").request().get(String.class));

            assertEquals("*/*", target.path("application/xml").request().get(String.class));
            assertEquals("*/*", target.path("application/*").request().get(String.class));
        }
    }

    @Nested
    public static class ProducesXXXTest extends JerseyTest {

        @Override
        protected Application configure() {
            return new ResourceConfig(ContextResource.class,
                    TextPlainContextResolver.class,
                    TextHtmlContextResolver.class);
        }

        @Test
        public void testProducesXXX() throws IOException {
            WebTarget target = target();

            assertEquals("text/plain", target.path("text/plain").request().get(String.class));
            assertEquals("text/html", target.path("text/html").request().get(String.class));
            assertEquals("NULL", target.path("text/*").request().get(String.class));
            assertEquals("NULL", target.path("*/*").request().get(String.class));

            assertEquals("NULL", target.path("text/csv").request().get(String.class));

            assertEquals("NULL", target.path("application/xml").request().get(String.class));
            assertEquals("NULL", target.path("application/*").request().get(String.class));
        }
    }
}
