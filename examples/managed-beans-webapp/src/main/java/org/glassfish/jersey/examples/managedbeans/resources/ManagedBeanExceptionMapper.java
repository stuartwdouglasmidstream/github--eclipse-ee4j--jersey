/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.managedbeans.resources;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import jakarta.annotation.ManagedBean;
import jakarta.annotation.PostConstruct;

/**
 * Custom exception mapper.
 *
 * @author Paul Sandoz
 */
@Provider
@ManagedBean
public class ManagedBeanExceptionMapper implements ExceptionMapper<ManagedBeanException> {

    @Context
    private UriInfo uiFieldInject;

    @Context
    private ResourceContext rc;

    private UriInfo uiMethodInject;

    private UriInfo ui;

    @Context
    public void set(UriInfo ui) {
        this.uiMethodInject = ui;
    }

    @PostConstruct
    public void postConstruct() {
        ensureInjected();
        this.ui = uiMethodInject;
    }

    @Override
    public Response toResponse(ManagedBeanException exception) {
        ensureInjected();
        return Response.serverError().entity(String.format("ManagedBeanException from %s", ui.getPath())).build();
    }

    private void ensureInjected() throws IllegalStateException {
        if (uiFieldInject == null || uiMethodInject == null || rc == null) {
            throw new IllegalStateException();
        }
    }
}
