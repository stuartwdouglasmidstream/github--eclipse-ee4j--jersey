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

package org.glassfish.jersey.tests.memleaks.beanparam;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.test.memleak.common.AbstractMemoryLeakWebAppTest;
import org.glassfish.jersey.test.memleak.common.MemoryLeakSucceedingTimeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This is an integration test that reproduces JERSEY-2800 by calling RESTful resource {@link BeanParamLeakResource}
 * repetitively.
 *
 * @author Stepan Vavra
 */
public class BeanParamLeakResourceITCase extends AbstractMemoryLeakWebAppTest {

    @Override
    protected Application configure() {
        return new TestApplication();
    }

    @RegisterExtension
    public InvocationInterceptor globalTimeout = new MemoryLeakSucceedingTimeout(300_000);

    @Test
    public void testTheLeakResourceOnce() {
        final Response response = target("beanparam/invoke").queryParam("q", "hello").request().post(null);
        Assertions.assertEquals(200, response.getStatus());
        assertEquals("hello", response.readEntity(String.class));
    }

    @Test
    public void testTheLeakEndless() {

        for (long i = 0;; i++) {
            System.out.print("\rRequests made: " + i);

            final Response response = target("beanparam/invoke").queryParam("q", i).request().post(null);
            if (response.getStatus() != 200) {
                fail("The server was unable to fulfill the request! This may indicate that OutOfMemory exception occurred.");
            }
            assertEquals(Long.toString(i), response.readEntity(String.class));

        }
    }

}
