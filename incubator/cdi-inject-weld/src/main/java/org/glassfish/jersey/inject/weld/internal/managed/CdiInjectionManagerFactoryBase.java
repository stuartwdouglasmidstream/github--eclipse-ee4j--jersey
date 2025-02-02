/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.inject.weld.internal.managed;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.ws.rs.RuntimeType;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;

/**
 * CDI Injection Manager Factory base class that holds the current bean manager.
 */
public abstract class CdiInjectionManagerFactoryBase implements InjectionManagerFactory {
    private static BeanManager beanManager;

    /* package */ static void setBeanManager(BeanManager bm) {
        beanManager = bm;
    }

    protected static InjectionManager getInjectionManager(RuntimeType runtimeType) {
        return beanManager.getExtension(BinderRegisterExtension.class).getInjectionManager(runtimeType);
    }
}
