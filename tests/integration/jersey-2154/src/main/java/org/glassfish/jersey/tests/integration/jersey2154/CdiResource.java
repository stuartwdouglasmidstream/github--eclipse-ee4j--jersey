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

package org.glassfish.jersey.tests.integration.jersey2154;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Request scoped CDI bean injected with EJB bean.
 * Part of JERSEY-2154 reproducer. {@link jakarta.ejb.EJBException}
 * thrown in the injected EJB bean should get unwrapped
 * even when no EJB-backed JAX-RS resources have been registered.
 *
 * @author Jakub Podlesak
 */
@RequestScoped
@Path("cdi")
public class CdiResource {

    @EJB EjbBean ejbBean;

    @GET
    public String get() {
        return ejbBean.exceptionDrivenResponse();
    }
}
