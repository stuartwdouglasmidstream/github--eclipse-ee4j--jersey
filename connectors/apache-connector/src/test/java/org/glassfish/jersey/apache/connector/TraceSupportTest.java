/*
 * Copyright (c) 2011, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.apache.connector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This very basic resource showcases support of a HTTP TRACE method,
 * not directly supported by JAX-RS API.
 *
 * @author Marek Potociar
 */
public class TraceSupportTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(TraceSupportTest.class.getName());

    /**
     * Programmatic tracing root resource path.
     */
    public static final String ROOT_PATH_PROGRAMMATIC = "tracing/programmatic";

    /**
     * Annotated class-based tracing root resource path.
     */
    public static final String ROOT_PATH_ANNOTATED = "tracing/annotated";

    @HttpMethod(TRACE.NAME)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TRACE {
        public static final String NAME = "TRACE";
    }

    @Path(ROOT_PATH_ANNOTATED)
    public static class TracingResource {

        @TRACE
        @Produces("text/plain")
        public String trace(Request request) {
            return stringify((ContainerRequest) request);
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(TracingResource.class);
        config.register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
        final Resource.Builder resourceBuilder = Resource.builder(ROOT_PATH_PROGRAMMATIC);
        resourceBuilder.addMethod(TRACE.NAME).handledBy(new Inflector<ContainerRequestContext, Response>() {

            @Override
            public Response apply(ContainerRequestContext request) {
                if (request == null) {
                    return Response.noContent().build();
                } else {
                    return Response.ok(stringify((ContainerRequest) request), MediaType.TEXT_PLAIN).build();
                }
            }
        });

        return config.registerResources(resourceBuilder.build());

    }

    private String[] expectedFragmentsProgrammatic = new String[]{
            "TRACE http://localhost:" + this.getPort() + "/tracing/programmatic"
    };
    private String[] expectedFragmentsAnnotated = new String[]{
            "TRACE http://localhost:" + this.getPort() + "/tracing/annotated"
    };

    private WebTarget prepareTarget(String path) {
        final WebTarget target = target();
        target.register(LoggingFeature.class);
        return target.path(path);
    }

    @Test
    public void testProgrammaticApp() throws Exception {
        Response response = prepareTarget(ROOT_PATH_PROGRAMMATIC).request("text/plain").method(TRACE.NAME);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());

        String responseEntity = response.readEntity(String.class);
        for (String expectedFragment : expectedFragmentsProgrammatic) {
            assertTrue(// toLowerCase - http header field names are case insensitive
                    responseEntity.contains(expectedFragment),
                    "Expected fragment '" + expectedFragment + "' not found in response:\n" + responseEntity);
        }
    }

    @Test
    public void testAnnotatedApp() throws Exception {
        Response response = prepareTarget(ROOT_PATH_ANNOTATED).request("text/plain").method(TRACE.NAME);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());

        String responseEntity = response.readEntity(String.class);
        for (String expectedFragment : expectedFragmentsAnnotated) {
            assertTrue(// toLowerCase - http header field names are case insensitive
                    responseEntity.contains(expectedFragment),
                    "Expected fragment '" + expectedFragment + "' not found in response:\n" + responseEntity);
        }
    }

    @Test
    public void testTraceWithEntity() throws Exception {
        _testTraceWithEntity(false, false);
    }

    @Test
    public void testAsyncTraceWithEntity() throws Exception {
        _testTraceWithEntity(true, false);
    }

    @Test
    public void testTraceWithEntityApacheConnector() throws Exception {
        _testTraceWithEntity(false, true);
    }

    @Test
    public void testAsyncTraceWithEntityApacheConnector() throws Exception {
        _testTraceWithEntity(true, true);
    }

    private void _testTraceWithEntity(final boolean isAsync, final boolean useApacheConnection) throws Exception {
        try {
            WebTarget target = useApacheConnection ? getApacheClient().target(target().getUri()) : target();
            target = target.path(ROOT_PATH_ANNOTATED);

            final Entity<String> entity = Entity.entity("trace", MediaType.WILDCARD_TYPE);

            Response response;
            if (!isAsync) {
                response = target.request().method(TRACE.NAME, entity);
            } else {
                response = target.request().async().method(TRACE.NAME, entity).get();
            }

            fail("A TRACE request MUST NOT include an entity. (response=" + response + ")");
        } catch (Exception e) {
            // OK
        }
    }

    private Client getApacheClient() {
        return ClientBuilder.newClient(new ClientConfig().connectorProvider(new ApacheConnectorProvider()));
    }


    public static String stringify(ContainerRequest request) {
        StringBuilder buffer = new StringBuilder();

        printRequestLine(buffer, request);
        printPrefixedHeaders(buffer, request.getHeaders());

        if (request.hasEntity()) {
            buffer.append(request.readEntity(String.class)).append("\n");
        }

        return buffer.toString();
    }

    private static void printRequestLine(StringBuilder buffer, ContainerRequest request) {
        buffer.append(request.getMethod()).append(" ").append(request.getUriInfo().getRequestUri().toASCIIString()).append("\n");
    }

    private static void printPrefixedHeaders(StringBuilder buffer, Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            List<String> val = e.getValue();
            String header = e.getKey();

            if (val.size() == 1) {
                buffer.append(header).append(": ").append(val.get(0)).append("\n");
            } else {
                StringBuilder sb = new StringBuilder();
                boolean add = false;
                for (String s : val) {
                    if (add) {
                        sb.append(',');
                    }
                    add = true;
                    sb.append(s);
                }
                buffer.append(header).append(": ").append(sb.toString()).append("\n");
            }
        }
    }
}
