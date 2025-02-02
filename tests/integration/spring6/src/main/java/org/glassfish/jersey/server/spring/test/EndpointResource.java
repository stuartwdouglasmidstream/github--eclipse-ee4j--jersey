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

package org.glassfish.jersey.server.spring.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Konrad Garus (konrad.garus at gmail.com)
 */
@Endpoint
@Path("/spring/endpoint")
public class EndpointResource {

    private String message;

    @PUT
    @Path("message")
    @Consumes(MediaType.TEXT_PLAIN)
    public String setMessage(final String message) {
        this.message = message;
        return message;
    }

    @GET
    @Path("message")
    public String getMessage() {
        return message;
    }
}
