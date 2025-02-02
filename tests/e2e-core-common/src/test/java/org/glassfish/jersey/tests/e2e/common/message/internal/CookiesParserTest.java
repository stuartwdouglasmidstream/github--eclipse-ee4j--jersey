/*
 * Copyright (c) 2013, 2023 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common.message.internal;

import java.text.SimpleDateFormat;
import java.util.Locale;

import jakarta.ws.rs.core.NewCookie;

import org.glassfish.jersey.message.internal.CookiesParser;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class CookiesParserTest {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    @Test
    public void testCaseInsensitiveNewCookieParams() throws Exception {
        _testCaseInsensitiveNewCookieParams("expires", "max-age", "path", "domain",
                "comment", "version", "secure", "httponly", "samesite");
        _testCaseInsensitiveNewCookieParams("Expires", "Max-Age", "Path", "Domain",
                "Comment", "Version", "Secure", "HttpOnly", "SameSite");
        _testCaseInsensitiveNewCookieParams("exPires", "max-aGe", "patH", "doMAin",
                "Comment", "vErsion", "secuRe", "httPonly", "samEsite");
    }

    private void _testCaseInsensitiveNewCookieParams(final String expires, final String maxAge, final String path,
                                                     final String domain, final String comment, final String version,
                                                     final String secure, final String httpOnly, final String sameSite)
            throws Exception {

        final String header = "foo=bar;"
                + expires + "=Tue, 15 Jan 2013 21:47:38 GMT;"
                + maxAge + "=42;"
                + path + "=/;"
                + domain + "=.example.com;"
                + comment + "=Testing;"
                + version + "=1;"
                + secure + ";"
                + httpOnly + ";"
                + sameSite + "=STRICT";

        final NewCookie cookie = CookiesParser.parseNewCookie(header);

        assertThat(cookie.getName(), equalTo("foo"));
        assertThat(cookie.getValue(), equalTo("bar"));

        assertThat(cookie.getExpiry(), equalTo(dateFormat.parse("Tue, 15 Jan 2013 21:47:38 GMT")));
        assertThat(cookie.getMaxAge(), equalTo(42));
        assertThat(cookie.getPath(), equalTo("/"));
        assertThat(cookie.getDomain(), equalTo(".example.com"));
        assertThat(cookie.getComment(), equalTo("Testing"));
        assertThat(cookie.getVersion(), equalTo(1));
        assertThat(cookie.isSecure(), is(true));
        assertThat(cookie.isHttpOnly(), is(true));
        assertThat(cookie.getSameSite(), equalTo(NewCookie.SameSite.STRICT));
    }
}
