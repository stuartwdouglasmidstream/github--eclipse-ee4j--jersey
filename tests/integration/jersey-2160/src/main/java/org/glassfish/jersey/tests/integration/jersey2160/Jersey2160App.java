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

package org.glassfish.jersey.tests.integration.jersey2160;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;

/**
 * JAX-RS application for the JERSEY-1960 reproducer test.
 *
 * @author Jakub Podlesak
 */
public class Jersey2160App extends Application {

    @SuppressWarnings({"unchecked"})
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>() {
            {
                add(RequestFilter.class);
                add(Issue2160ReproducerResource.class);
            }
        };
    }
}
