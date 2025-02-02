/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.spring.scope;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.internal.util.JdkVersion;
import org.glassfish.jersey.server.spring.LocalizationMessages;

import java.io.IOException;

/**
 * Spring filter to provide a bridge between JAX-RS and Spring request attributes.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 * @author Jakub Podlesak
 * @author Marek Potociar
 */
@Provider
@PreMatching
public final class RequestContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (JdkVersion.getJdkVersion().getMajor() < 17) {
            throw new IllegalStateException(LocalizationMessages.NOT_SUPPORTED());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (JdkVersion.getJdkVersion().getMajor() < 17) {
            throw new IllegalStateException(LocalizationMessages.NOT_SUPPORTED());
        }
    }
}
