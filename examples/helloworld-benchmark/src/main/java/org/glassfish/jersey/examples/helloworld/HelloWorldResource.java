/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.helloworld;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * Simple "Hello World" resource with three resource methods and a sub-resource locator (that points again to this class itself).
 *
 * @author Michal Gajdos
 */
@Path("helloworld")
@Produces("text/plain")
public class HelloWorldResource {

    public static final String CLICHED_MESSAGE = "Hello World!";

    @GET
    public String get() {
        return CLICHED_MESSAGE;
    }

    @POST
    public String post(final String entity) {
        return entity;
    }

    @PUT
    public void put(final String entity) {
        // NOOP
    }

    @Path("locator")
    public Class<?> sub() {
        return HelloWorldResource.class;
    }
}
