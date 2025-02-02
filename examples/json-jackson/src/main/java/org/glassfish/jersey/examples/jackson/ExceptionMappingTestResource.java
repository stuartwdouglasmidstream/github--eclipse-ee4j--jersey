/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.jackson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

/**
 * Testing bean that accepts JSON for the PUT method.
 *
 * @author Adam Lindenthal
 */
@Path("parseExceptionTest")
public class ExceptionMappingTestResource {

    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    public DummyBean getAccount(DummyBean bean) {
        return bean;
    }
}
