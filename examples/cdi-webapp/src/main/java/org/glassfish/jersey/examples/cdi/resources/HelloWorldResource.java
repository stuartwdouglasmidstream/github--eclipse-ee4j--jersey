/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.cdi.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;

import jakarta.annotation.ManagedBean;

/**
 * A managed bean with no use of injection whatsoever.
 *
 * @author Roberto Chinnici.
 */
@ManagedBean
@Path("/helloworld")
public class HelloWorldResource {

    @GET
    @Produces("text/plain")
    public String getClichedMessage() {
        return "Hello World";
    }
}
