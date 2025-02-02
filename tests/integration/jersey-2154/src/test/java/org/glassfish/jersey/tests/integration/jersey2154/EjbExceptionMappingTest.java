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

package org.glassfish.jersey.tests.integration.jersey2154;

import java.net.URI;
import jakarta.ejb.EJBException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;

/**
 * Reproducer for JERSEY-2154.
 * Test generated {@link WebApplicationException} is propagated
 * via CDI call and mapped to 200 response, even when wrapped with an {@link EJBException}.
 *
 * @author Jakub Podlesak
 */
public class EjbExceptionMappingTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new TestApplication();
    }

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("jersey-2154").build();
    }

    /**
     * The only test needed. Should the original {@link WebApplicationException}
     * not get unwrapped, we would end up with 500 status code.
     */
    @Test
    public void testInjection() {

        final WebTarget cdiResource = target().path("cdi");

        Response response = cdiResource.request().get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), containsString("exception got mapped"));
    }
}
