/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.spi.TestContainerFactory;

public class ResourceConfigApplicationPathTest extends JerseyContainerTest {
    public ResourceConfigApplicationPathTest(TestContainerFactory testContainerFactory) {
        super(testContainerFactory);
    }

    @Override
    protected Application configure() {

        return new ResourceConfigApplicationPathTestResourceConfig()
                .register(super.configure().getClasses().iterator().next());
    }

    @ApplicationPath("/applicationpath")
    public static class ResourceConfigApplicationPathTestResourceConfig extends ResourceConfig {

    }

}
