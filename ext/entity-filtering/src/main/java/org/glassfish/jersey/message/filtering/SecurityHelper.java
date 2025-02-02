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

package org.glassfish.jersey.message.filtering;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.SecurityContext;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.glassfish.jersey.message.filtering.spi.FilteringHelper;

/**
 * Utility methods for security Entity Data Filtering.
 *
 * @author Michal Gajdos
 */
final class SecurityHelper {

    private static final Set<String> roles = new HashSet<>();

    /**
     * Get entity-filtering scopes of security annotations present among given annotations.
     * <p>
     * A scope look like:
     * <ul>
     * <li>&lt;fully qualified annotation class name&gt;, or</li>
     * <li>&lt;fully qualified annotation class name&gt;_&lt;role&gt;</li>
     * </ul>
     * </p>
     *
     * @param annotations a list of annotations (doesn't need to contain only security annotations)
     * @return a set of entity-filtering scopes.
     */
    static Set<String> getFilteringScopes(final Annotation[] annotations) {
        return getFilteringScopes(null, annotations);
    }

    /**
     * Get entity-filtering scopes of security annotations present among given annotations with respect to given
     * {@link SecurityContext}. Resulting set contains only scopes that pass the security context check.
     * <p>
     * A scope look like:
     * <ul>
     * <li>&lt;fully qualified annotation class name&gt;, or</li>
     * <li>&lt;fully qualified annotation class name&gt;_&lt;role&gt;</li>
     * </ul>
     * </p>
     *
     * @param securityContext security context to check whether a user is in specified logical role.
     * @param annotations a list of annotations (doesn't need to contain only security annotations)
     * @return a set of entity-filtering scopes.
     */
    static Set<String> getFilteringScopes(final SecurityContext securityContext, final Annotation[] annotations) {
        if (annotations.length == 0) {
            return Collections.emptySet();
        }

        for (final Annotation annotation : annotations) {
            if (annotation instanceof RolesAllowed) {
                final Set<String> bindings = new HashSet<>();

                for (final String role : ((RolesAllowed) annotation).value()) {
                    if (securityContext == null || securityContext.isUserInRole(role)) {
                        bindings.add(getRolesAllowedScope(role));
                    }
                }

                return bindings;
            } else if (annotation instanceof PermitAll) {
                return FilteringHelper.getDefaultFilteringScope();
            } else if (annotation instanceof DenyAll) {
                return null;
            }
        }

        return Collections.emptySet();
    }

    /**
     * Get entity-filtering scope for {@link RolesAllowed}s role.
     *
     * @param role role to retrieve entity-filtering scope for.
     * @return entity-filtering scope.
     */
    static String getRolesAllowedScope(final String role) {
        roles.add(role);
        return RolesAllowed.class.getName() + "_" + role;
    }

    /**
     * Get authorization roles that has been derived from examining entity classes.
     *
     * @return already processed authorization roles.
     */
    static Set<String> getProcessedRoles() {
        return roles;
    }

    /**
     * Prevent instantiation.
     */
    private SecurityHelper() {
    }
}
