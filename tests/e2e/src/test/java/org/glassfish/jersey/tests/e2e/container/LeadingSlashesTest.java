/*
 * Copyright (c) 2015, 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.jetty.JettyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestHelper;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Jersey container implementation of URL resolving.
 * In this test there is no context path that means that
 * slashes in URL are part of Resource address and couldn't
 * be deleted.
 *
 * @author Petr Bouda
 */
public class LeadingSlashesTest {

    public static final String CONTAINER_RESPONSE = "Container-Response";

    @Path("simple")
    public static class SimpleResource {

        @GET
        public String encoded() {
            return CONTAINER_RESPONSE;
        }

    }

    @Path("/")
    public static class EmptyPathParamResource {

        @GET
        @Path("{bar:.*}/{baz:.*}/test")
        public String getHello(@PathParam("bar") final String bar, @PathParam("baz") final String baz) {
            return bar + "-" + baz;
        }

        @GET
        @Path("{bar:.*}/{baz:.*}/testParams")
        public String helloWithQueryParams(@PathParam("bar") final String bar, @PathParam("baz") final String baz,
                                           @QueryParam("bar") final String queryBar, @QueryParam("baz") final String queryBaz) {
            return "PATH PARAM: " + bar + "-" + baz + ", QUERY PARAM " + queryBar + "-" + queryBaz;
        }

        @GET
        @Path("{bar:.*}/{baz:.*}/encoded")
        public String getEncoded(@Encoded @QueryParam("query") String queryParam) {
            return queryParam.equals("%25dummy23%2Ba") + ":" + queryParam;
        }
    }

    @Path("/")
    public static class EmptyResource {

        @GET
        @Path("/test")
        public String getHello() {
            return CONTAINER_RESPONSE;
        }
    }

    @TestFactory
    public Collection<DynamicContainer> generateTests() {
        Collection<DynamicContainer> tests = new ArrayList<>();
        JerseyContainerTest.parameters().forEach(testContainerFactory -> {
            LeadingSlashesTemplateTest test = new LeadingSlashesTemplateTest(testContainerFactory) {};
            tests.add(TestHelper.toTestContainer(test, testContainerFactory.getClass().getSimpleName()));
        });
        return tests;
    }

    public abstract static class LeadingSlashesTemplateTest extends JerseyContainerTest {

        public LeadingSlashesTemplateTest(TestContainerFactory testContainerFactory) {
            super(testContainerFactory);
        }

        @Override
        protected Application configure() {
            ResourceConfig resourceConfig = new ResourceConfig(SimpleResource.class,
                    EmptyResource.class,
                    EmptyPathParamResource.class);

            resourceConfig.property(ServerProperties.REDUCE_CONTEXT_PATH_SLASHES_ENABLED, true);
            return resourceConfig;
        }

        @Test
        public void testSimpleSlashes() {
            Response result = call("/simple");
            assertEquals(CONTAINER_RESPONSE, result.readEntity(String.class));

            result = call("//simple");
            assertNotEquals(CONTAINER_RESPONSE, result.readEntity(String.class));
        }

        @Test
        public void testSlashesWithBeginningEmpty() {
            Response result = call("/test");
            assertEquals(CONTAINER_RESPONSE, result.readEntity(String.class));
        }

        @Test
        public void testSlashesWithBeginningEmptyPathParam() {
            if (JettyTestContainerFactory.class.isInstance(getTestContainerFactory())) {
                return; // since Jetty 11.0.5
            }
            Response result = call("///test");
            assertEquals("-", result.readEntity(String.class));
        }

        @Test
        public void testSlashesWithBeginningEmptyPathParamWithQueryParams() {
            if (JettyTestContainerFactory.class.isInstance(getTestContainerFactory())) {
                return; // since Jetty 11.0.5
            }
            URI hostPort = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
            WebTarget target = client().target(hostPort).path("///testParams")
                    .queryParam("bar", "Container")
                    .queryParam("baz", "Response");

            Response result = target.request().get();
            assertEquals("PATH PARAM: -, QUERY PARAM Container-Response", result.readEntity(String.class));
        }

        @Test
        public void testEncodedQueryParams() {
            if (JettyTestContainerFactory.class.isInstance(getTestContainerFactory())) {
                return; // since Jetty 11.0.5
            }
            URI hostPort = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
            WebTarget target = client().target(hostPort).path("///encoded")
                    .queryParam("query", "%dummy23+a");

            Response response = target.request().get();
            assertEquals(200, response.getStatus());
            assertEquals("true:%25dummy23%2Ba", response.readEntity(String.class));
        }

        private Response call(String path) {
            URI hostPort = UriBuilder.fromUri("http://localhost/").port(getPort()).build();
            return client().target(hostPort).path(path).request().get();
        }
    }
}
