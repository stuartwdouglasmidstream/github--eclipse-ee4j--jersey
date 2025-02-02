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

package org.glassfish.jersey.tests.e2e.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestHelper;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class GzipContentEncodingTest {

    @Path("/")
    public static class Resource {

        @GET
        public String get() {
            return "GET";
        }

        @POST
        public String post(final String content) {
            return content;
        }
    }

    @TestFactory
    public Collection<DynamicContainer> generateTests() {
        Collection<DynamicContainer> tests = new ArrayList<>();
        JerseyContainerTest.parameters().forEach(testContainerFactory -> {
            GzipContentEncodingTemplateTest test = new GzipContentEncodingTemplateTest(testContainerFactory) {};
            tests.add(TestHelper.toTestContainer(test, testContainerFactory.getClass().getSimpleName()));
        });
        return tests;
    }

    public abstract static class GzipContentEncodingTemplateTest extends JerseyContainerTest {

        public GzipContentEncodingTemplateTest(TestContainerFactory testContainerFactory) {
            super(testContainerFactory);
        }

        @Override
        protected Application configure() {
            return new ResourceConfig(Resource.class, EncodingFilter.class, GZipEncoder.class);
        }

        @Override
        protected void configureClient(final ClientConfig config) {
            config.register(new ReaderInterceptor() {
                @Override
                public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
                    context.setInputStream(new GZIPInputStream(context.getInputStream()));
                    return context.proceed();
                }
            });
        }

        @Test
        public void testGet() {
            final Response response = target().request()
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                    .get();

            assertThat(response.readEntity(String.class), is("GET"));
        }

        @Test
        public void testPost() {
            final Response response = target().request()
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                    .post(Entity.text("POST"));

            assertThat(response.readEntity(String.class), is("POST"));
        }
    }
}
