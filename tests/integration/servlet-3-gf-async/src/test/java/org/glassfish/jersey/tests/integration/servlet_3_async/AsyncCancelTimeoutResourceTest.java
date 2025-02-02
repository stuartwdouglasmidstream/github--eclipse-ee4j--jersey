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

package org.glassfish.jersey.tests.integration.servlet_3_async;

import java.util.concurrent.Future;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Asynchronous servlet-deployed resource for testing {@link jakarta.ws.rs.container.AsyncResponse async response} timeouts.
 *
 * @author Michal Gajdos
 */
public class AsyncCancelTimeoutResourceTest extends JerseyTest {

    public AsyncCancelTimeoutResourceTest() {
        enable(TestProperties.LOG_TRAFFIC);
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.builder(new Application())
                .contextPath("servlet-3-gf-async")
                .build();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testTimeout() throws Exception {
        final WebTarget resourceTarget = target("cancel-timeout");
        final Future<Response> suspend = resourceTarget.path("suspend").request().async().get();
        final Future<Response> timeout = resourceTarget.path("timeout").request().async().post(Entity.text(0));

        assertThat(timeout.get().getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        assertThat(suspend.get().getStatus(), is(Response.Status.SERVICE_UNAVAILABLE.getStatusCode()));
    }
}
