/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.servlettests;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author Miroslav Fuksa
 */
@Path("resource404")
public class CustomMediaTypeAnd404Resource {
    @Path("content-type-entity")
    @GET
    public Response getSpecialContentType() {
        return Response.status(Response.Status.NOT_FOUND).type("application/something").entity("not found custom entity").build();
    }

    @Path("content-type-empty-entity")
    @GET
    public Response getSpecialContentTypeWithEmptyEntityString() {
        return Response.status(Response.Status.NOT_FOUND).type("application/something").entity("").build();
    }
}
