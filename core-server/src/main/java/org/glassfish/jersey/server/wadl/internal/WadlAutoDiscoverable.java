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

package org.glassfish.jersey.server.wadl.internal;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.FeatureContext;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;
import org.glassfish.jersey.server.wadl.WadlFeature;

/**
 * {@link AutoDiscoverable} registering {@link org.glassfish.jersey.server.wadl.WadlFeature} if this feature
 * is not already registered.
 *
 * @author Michal Gajdos
 */
@ConstrainedTo(RuntimeType.SERVER)
@Priority(AutoDiscoverable.DEFAULT_PRIORITY)
public final class WadlAutoDiscoverable implements ForcedAutoDiscoverable {

    @Override
    public void configure(final FeatureContext context) {
        if (!context.getConfiguration().isRegistered(WadlFeature.class)) {
            context.register(WadlFeature.class);
        }
    }
}
