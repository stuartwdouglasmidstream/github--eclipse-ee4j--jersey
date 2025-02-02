/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.header;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import static org.glassfish.jersey.tests.e2e.header.HeaderDelegateProviderTest.HEADER_NAME;

public class RuntimeDelegateProviderEnabledTest extends JerseyTest {
    @Override
    protected Application configure() {
        return new ResourceConfig(HeaderDelegateProviderTest.HeaderSettingResource.class,
                HeaderDelegateProviderTest.HeaderContainerResponseFilter.class);
    }

    @Test
    public void testClientResponseHeaders() {
        try (Response response = target("/simple").request().get()) {
            Assertions.assertEquals(
                    HeaderDelegateProviderTest.BeanForHeaderDelegateProviderTest.getValue(),
                    response.getHeaderString(HeaderDelegateProviderTest.HeaderContainerResponseFilter.class.getSimpleName())
            );
            Assertions.assertEquals(
                    HeaderDelegateProviderTest.BeanForHeaderDelegateProviderTest.getValue(),
                    response.getStringHeaders().getFirst(HEADER_NAME)
            );
        }
    }

    @Test
    public void testContainerResponseFilter() {
        try (Response response = target("/simple").request().get()) {
            Assertions.assertEquals(
                    HeaderDelegateProviderTest.BeanForHeaderDelegateProviderTest.getValue(),
                    response.getHeaderString(HEADER_NAME)
            );
        }
    }

    @Test
    public void testProviderOnClient() {
        try (Response response = target("/headers").request()
                .header(HEADER_NAME, new HeaderDelegateProviderTest.BeanForHeaderDelegateProviderTest()).get()) {
            Assertions.assertEquals(
                    HeaderDelegateProviderTest.BeanForHeaderDelegateProviderTest.getValue(),
                    response.getHeaderString(HeaderDelegateProviderTest.HeaderSettingResource.class.getSimpleName())
            );
        }
    }

    @Test
    public void testProviderOnClientFilter() {
        try (Response response = target("/clientfilter")
                .register(HeaderDelegateProviderTest.HeaderClientRequestFilter.class)
                .request().get()) {
            Assertions.assertEquals(
                    HeaderDelegateProviderTest.BeanForHeaderDelegateProviderTest.getValue(),
                    response.readEntity(String.class)
            );
        }
    }
}
