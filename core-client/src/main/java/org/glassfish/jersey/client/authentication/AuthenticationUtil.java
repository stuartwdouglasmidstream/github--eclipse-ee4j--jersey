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

package org.glassfish.jersey.client.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.ws.rs.client.ClientRequestContext;

/**
 * Common authentication utilities
 */
class AuthenticationUtil {
   static void discardInputAndClose(InputStream is) {
        byte[] buf = new byte[4096];
        try {
            while (true) {
                if (is.read(buf) <= 0) {
                    break;
                }
            }
        } catch (IOException ex) {
            // ignore
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    static URI getCacheKey(ClientRequestContext request) {
        URI requestUri = request.getUri();
        if (requestUri.getRawQuery() != null) {
            // Return a URI without the query part of the request URI
            try {
                return new URI(
                        requestUri.getScheme(),
                        requestUri.getAuthority(),
                        requestUri.getPath(),
                        null,
                        requestUri.getFragment());
            } catch (URISyntaxException e) {
                // Ignore and fall through
            }
        }
        return requestUri;
    }
}
