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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.test.spi.TestHelper;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class EscapedUriTest {

    private static final String RESPONSE = "CONTENT";

    @Path("x%20y")
    public static class EscapedUriResource {

        private final String context;

        public EscapedUriResource() {
            this("");
        }

        public EscapedUriResource(final String context) {
            this.context = context;
        }

        @GET
        public String get(@Context final UriInfo info) {
            assertEquals(context + "/x%20y", info.getAbsolutePath().getRawPath());
            assertEquals("/", info.getBaseUri().getRawPath());
            assertEquals(context + "/x y", "/" + info.getPath());
            assertEquals(context + "/x%20y", "/" + info.getPath(false));

            return RESPONSE;
        }
    }

    @Path("non/x y")
    public static class NonEscapedUriResource extends EscapedUriResource {

        public NonEscapedUriResource() {
            super("/non");
        }
    }

    @TestFactory
    public Collection<DynamicContainer> generateTests() {
        Collection<DynamicContainer> tests = new ArrayList<>();
        JerseyContainerTest.parameters().forEach(testContainerFactory -> {
            EscapedUriTesmplateTest test = new EscapedUriTesmplateTest(testContainerFactory) {};
            tests.add(TestHelper.toTestContainer(test, testContainerFactory.getClass().getSimpleName()));
        });
        return tests;
    }

    public abstract static class EscapedUriTesmplateTest extends JerseyContainerTest {

        public EscapedUriTesmplateTest(TestContainerFactory testContainerFactory) {
            super(testContainerFactory);
        }

        @Override
        protected Application configure() {
            return new ResourceConfig(EscapedUriResource.class, NonEscapedUriResource.class);
        }

        @Test
        public void testEscaped() {
            assertThat(target("x%20y").request().get(String.class), is(RESPONSE));
        }

        @Test
        public void testNonEscaped() {
            assertThat(target("non/x y").request().get(String.class), is(RESPONSE));
        }
    }
}
