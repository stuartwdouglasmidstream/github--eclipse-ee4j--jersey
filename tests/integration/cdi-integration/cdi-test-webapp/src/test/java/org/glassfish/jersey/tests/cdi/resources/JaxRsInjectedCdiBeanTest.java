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

package org.glassfish.jersey.tests.cdi.resources;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that a raw CDI managed bean gets JAX-RS injected.
 *
 * @author Jakub Podlesak
 */
public class JaxRsInjectedCdiBeanTest extends CdiTest {

    @Test
    public void testPathAndHeader() {
        _testPathAndHeader(target());
    }

    public static void _testPathAndHeader(final WebTarget webTarget) {
        final WebTarget target = webTarget.path("non-jaxrs-bean-injected");

        final Response pathResponse = target.path("path/1").request().get();
        assertThat(pathResponse.getStatus(), is(200));
        final String path = pathResponse.readEntity(String.class);

        assertThat(path, is("non-jaxrs-bean-injected/path/1"));

        final Response headerResponse = target.path("header/1").request().header("x-test", "bummer").get();
        assertThat(headerResponse.getStatus(), is(200));
        final String header = headerResponse.readEntity(String.class);

        assertThat(header, is("bummer"));
    }
}
