/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.inject.cdi.se.subresources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("root")
public class RootResource {

    @GET
    public String get() {
        return "root";
    }

    @Path("sub-resource-singleton")
    public Class<SubResourceSingleton> getSubResourceSingleton() {
        return SubResourceSingleton.class;
    }

    @Path("sub-resource-instance")
    public SubResourceSingleton getSubResourceSingletonInstance() {
        return new SubResourceSingleton();
    }
}
