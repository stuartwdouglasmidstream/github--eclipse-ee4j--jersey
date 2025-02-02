/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.entityfiltering.security.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.glassfish.jersey.examples.entityfiltering.security.domain.RestrictedEntity;

/**
 * Resource not restricted with security annotations leaving security restrictions solely to {@link RestrictedEntity} and
 * {@link jakarta.ws.rs.core.SecurityContext}.
 *
 * @author Michal Gajdos
 */
@Path("unrestricted-resource")
@Produces("application/json")
public class UnrestrictedResource {

    @GET
    public RestrictedEntity getRestrictedEntity() {
        return RestrictedEntity.instance();
    }
}
