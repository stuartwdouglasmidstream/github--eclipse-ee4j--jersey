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

package org.glassfish.jersey.tests.integration.tracing;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

/**
 * @author Libor Kramolis
 */
@Provider
@Priority(14)
public class ReaderInterceptor14 implements ReaderInterceptor {
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        //System.out.println("*** ReaderInterceptor14.aroundReadFrom: BEFORE");
        try {
            Thread.sleep(42);
        } catch (InterruptedException e) {
        }
        try {
            return context.proceed();
        } finally {
            try {
                Thread.sleep(42);
            } catch (InterruptedException e) {
            }
            //System.out.println("*** ReaderInterceptor14.aroundReadFrom: AFTER");
        }
    }
}
