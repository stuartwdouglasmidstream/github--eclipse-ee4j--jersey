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

package org.glassfish.jersey.tests.integration.jersey1964;

import java.net.ConnectException;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reproducer tests for JERSEY-1964.
 *
 * @author Michal Gajdos
 */
public class Jersey1964ITCase extends JerseyTest {

    @Override
    protected Application configure() {
        return new Jersey1964();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testJackson2JsonPut() throws Exception {
        final Response response = target().request().put(Entity.json(new Issue1964Resource.JsonStringWrapper("foo")));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(Issue1964Resource.JsonStringWrapper.class).getValue(), equalTo("foo"));
    }

    @Test
    public void testJackson2JsonGetInvalidEndpoint() throws Throwable {
        assertThrows(ConnectException.class, () -> {
            try {
                ClientBuilder.newClient()
                        .target("http://localhost:1234")
                        .request()
                        .get();

                fail("End-point shouldn't exist.");
            } catch (final ProcessingException pe) {
                throw pe.getCause();
            }
        });
    }

    @Test
    public void testJackson2JsonPutInvalidEndpoint() throws Throwable {
        assertThrows(ConnectException.class, () -> {
            try {
                ClientBuilder.newClient()
                        .target("http://localhost:1234")
                        .request()
                        .put(Entity.json(new Issue1964Resource.JsonStringWrapper("foo")));

                fail("End-point shouldn't exist.");
            } catch (final ProcessingException pe) {
                throw pe.getCause();
            }
        });
    }
}
