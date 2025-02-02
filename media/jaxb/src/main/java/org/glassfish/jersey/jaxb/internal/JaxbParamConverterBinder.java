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

package org.glassfish.jersey.jaxb.internal;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.ext.ParamConverterProvider;

import jakarta.inject.Singleton;

import org.glassfish.jersey.internal.inject.AbstractBinder;

/**
 * Binder for JAXB parameter converter.
 *
 * @author Jakub Podlesak
 */
@ConstrainedTo(RuntimeType.SERVER)
public class JaxbParamConverterBinder extends AbstractBinder {

    @Override
    protected void configure() {
        bind(JaxbStringReaderProvider.RootElementProvider.class).to(ParamConverterProvider.class).in(Singleton.class);
    }
}
