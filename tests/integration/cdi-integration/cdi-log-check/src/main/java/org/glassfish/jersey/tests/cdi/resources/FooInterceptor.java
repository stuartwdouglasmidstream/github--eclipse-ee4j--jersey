/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.resources;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;

@Interceptor
@Fooed
public class FooInterceptor implements Serializable {

    @Inject
    @Intercepted
    private Bean<?> interceptedBean;
    private static final long serialVersionUID = 1L;

    @Inject
    WarningClass warningClass;

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        warningClass.set(interceptedBean.getBeanClass());
        return invocationContext.proceed();
    }
}

