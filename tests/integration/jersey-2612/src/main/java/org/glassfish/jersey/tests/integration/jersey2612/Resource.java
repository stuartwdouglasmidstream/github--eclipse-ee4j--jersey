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

package org.glassfish.jersey.tests.integration.jersey2612;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import java.util.Optional;

/**
 * Test resource.
 */
@Path("/")
public class Resource {

    @Path("hello")
    @GET
    @Produces("text/plain")
    public String hello(@QueryParam("name") final Optional<String> name) {
        return "Hello " + name.orElse("World") + "!";
    }

    @Path("square")
    @GET
    @Produces("text/plain")
    public int echo(@QueryParam("value") final Optional<Integer> value) {

        return value.map(integer -> integer * integer).orElse(0);
    }

}
