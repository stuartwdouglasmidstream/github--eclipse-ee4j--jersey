/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.ext.cdi1x.internal;

import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Abstraction layer to separate client and server dependent implementation.
 */
interface CdiComponentProviderRuntimeSpecifics {
    boolean containsJaxRsParameterizedCtor(final AnnotatedType annotatedType);

    AnnotatedParameter<?> getAnnotatedParameter(AnnotatedParameter<?> ap);

    Set<Class<? extends Annotation>> getJaxRsInjectAnnotations();

    boolean isAcceptableResource(Class<?> resource);

    boolean isJaxRsResource(Class<?> resource);

    void clearJaxRsResource(ClassLoader loader);
}
