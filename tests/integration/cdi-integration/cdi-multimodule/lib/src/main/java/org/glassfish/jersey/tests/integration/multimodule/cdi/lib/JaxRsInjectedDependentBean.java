/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.multimodule.cdi.lib;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;

/**
 * CDI managed bean, that gets JAX-RS injected. This bean is being consumed
 * by all web apps within an EAR packaged enterprise application.
 *
 * @author Jakub Podlesak
 */
public class JaxRsInjectedDependentBean {

    @Context
    UriInfo uriInfo;

    @HeaderParam("x-test")
    String testHeader;

    /**
     * Get me URI info.
     *
     * @return URI info from the JAX-RS layer.
     */
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    /**
     * Get me the actual request test header.
     *
     * @return actual request URI info.
     */
    public String getTestHeader() {
        return testHeader;
    }

}
