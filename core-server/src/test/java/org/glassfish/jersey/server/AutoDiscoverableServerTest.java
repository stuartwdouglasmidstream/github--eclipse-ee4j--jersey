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

package org.glassfish.jersey.server;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.Priority;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.internal.util.PropertiesHelper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Note: Auto-discoverables from this test "affects" all other tests in suit.
 *
 * @author Michal Gajdos
 */
public class AutoDiscoverableServerTest {

    private static final String PROPERTY = "AutoDiscoverableTest";

    private static final ContainerRequestFilter component = new ContainerRequestFilter() {
        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.status(400).entity("CommonAutoDiscoverable").build());
        }
    };

    public static class CommonAutoDiscoverable implements AutoDiscoverable {

        @Override
        public void configure(final FeatureContext context) {
            // Return if PROPERTY is not true - applicable for other tests.
            if (!PropertiesHelper.isProperty(context.getConfiguration().getProperty(PROPERTY))) {
                return;
            }

            context.register(component, 1);
        }
    }

    @Priority(10)
    public static class AbortFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.abortWith(Response.status(400).entity("AbortFilter").build());
        }
    }

    @Path("/")
    public static class Resource {

        @GET
        public String doGetFoo(@Context HttpHeaders headers) {
            return Integer.toString(headers.getLength());
        }
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerDefault() throws Exception {
        _test("CommonAutoDiscoverable", null, null);
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerEnabled() throws Exception {
        _test("CommonAutoDiscoverable", null, false);
    }

    @Test
    public void testAutoDiscoverableGlobalDefaultServerDisabled() throws Exception {
        _test("AbortFilter", null, true);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerDefault() throws Exception {
        _test("AbortFilter", true, null);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerEnabled() throws Exception {
        _test("CommonAutoDiscoverable", true, false);
    }

    @Test
    public void testAutoDiscoverableGlobalDisabledServerDisabled() throws Exception {
        _test("AbortFilter", true, true);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerDefault() throws Exception {
        _test("CommonAutoDiscoverable", false, null);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerEnabled() throws Exception {
        _test("CommonAutoDiscoverable", false, false);
    }

    @Test
    public void testAutoDiscoverableGlobalEnabledServerDisabled() throws Exception {
        _test("AbortFilter", false, true);
    }

    private void _test(final String response, final Boolean globalDisable, final Boolean serverDisable) throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(Resource.class, AbortFilter.class)
                .property(PROPERTY, true);

        if (globalDisable != null) {
            resourceConfig.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, globalDisable);
        }
        if (serverDisable != null) {
            resourceConfig.property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, serverDisable);
        }

        final ApplicationHandler app = new ApplicationHandler(resourceConfig);

        assertEquals(response, app.apply(RequestContextBuilder.from("/", "GET").build()).get().getEntity());
    }
}
