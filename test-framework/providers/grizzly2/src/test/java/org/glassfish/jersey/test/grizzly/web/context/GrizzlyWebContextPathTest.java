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

package org.glassfish.jersey.test.grizzly.web.context;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;

import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test support for context path in {@link org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory}.
 *
 * @author Paul Sandoz
 * @author Marek Potociar
 */
public class GrizzlyWebContextPathTest extends JerseyTest {

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    @Path("contextroot")
    public static class TestResource {
        @GET
        public String get() {
            return "GET";
        }

        @Path("sub")
        @GET
        public String getSub() {
            return "sub";
        }
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.forPackages(this.getClass().getPackage().getName())
                .contextPath("context")
                .build();
    }

    @Test
    public void testGet() {
        WebTarget target = target("contextroot");

        String s = target.request().get(String.class);
        assertEquals("GET", s);

        assertEquals("/context/contextroot", target.getUri().getRawPath());
    }

    @Test
    public void testGetSub() {
        WebTarget target = target("contextroot/sub");

        String s = target.request().get(String.class);
        assertEquals("sub", s);

        assertEquals("/context/contextroot/sub", target.getUri().getRawPath());
    }
}
