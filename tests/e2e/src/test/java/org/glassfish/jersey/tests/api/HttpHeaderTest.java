/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.api;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.message.internal.AcceptableLanguageTag;
import org.glassfish.jersey.message.internal.AcceptableToken;
import org.glassfish.jersey.message.internal.HttpDateFormat;
import org.glassfish.jersey.message.internal.HttpHeaderReader;
import org.glassfish.jersey.message.internal.LanguageTag;
import org.glassfish.jersey.message.internal.ParameterizedHeader;
import org.glassfish.jersey.message.internal.Token;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Paul Sandoz
 */
public class HttpHeaderTest {

    @Test
    public void testTokens() throws ParseException {
        final String header = "type  /  content; a = \"asdsd\"";

        final HttpHeaderReader r = HttpHeaderReader.newInstance(header);
        while (r.hasNext()) {
            r.next();
        }
    }

    @Test
    public void testMediaType() throws ParseException {
        final String mimeType = "application/xml;charset=UTF-8";
        MediaType.valueOf(mimeType);
    }

    @Test
    public void testLanguageTag() throws ParseException {
        final String languageTag = "en-US";
        new LanguageTag(languageTag);
    }

    @Test
    public void testAcceptableLanguageTag() throws ParseException {
        final String languageTag = "en-US;q=0.123";
        new AcceptableLanguageTag(languageTag);
    }

    @Test
    public void testAcceptableLanguageTagList() throws Exception {
        final String languageTags = "en-US;q=0.123, fr;q=0.2, en;q=0.3, *;q=0.01";
        final List<AcceptableLanguageTag> l = HttpHeaderReader.readAcceptLanguage(languageTags);
        assertEquals("en", l.get(0).getTag());
        assertEquals("fr", l.get(1).getTag());
        assertEquals("en-US", l.get(2).getTag());
        assertEquals("*", l.get(3).getTag());
    }

    @Test
    public void testToken() throws ParseException {
        final String token = "gzip";
        new Token(token);
    }

    @Test
    public void testAcceptableToken() throws ParseException {
        final String token = "gzip;q=0.123";
        new AcceptableToken(token);
    }

    @Test
    public void testAcceptableTokenList() throws Exception {
        final String tokens = "gzip;q=0.123, compress;q=0.2, zlib;q=0.3, *;q=0.01";
        final List<AcceptableToken> l = HttpHeaderReader.readAcceptToken(tokens);
        assertEquals("zlib", l.get(0).getToken());
        assertEquals("compress", l.get(1).getToken());
        assertEquals("gzip", l.get(2).getToken());
        assertEquals("*", l.get(3).getToken());
    }

    @Test
    public void testDateParsing() throws ParseException {
        final String date_RFC1123 = "Sun, 06 Nov 1994 08:49:37 GMT";
        final String date_RFC1036 = "Sunday, 06-Nov-94 08:49:37 GMT";
        final String date_ANSI_C = "Sun Nov  6 08:49:37 1994";

        HttpHeaderReader.readDate(date_RFC1123);
        HttpHeaderReader.readDate(date_RFC1036);
        HttpHeaderReader.readDate(date_ANSI_C);
    }

    @Test
    public void testDateFormatting() throws ParseException {
        final String date_RFC1123 = "Sun, 06 Nov 1994 08:49:37 GMT";
        final Date date = HttpHeaderReader.readDate(date_RFC1123);

        final String date_formatted = HttpDateFormat.getPreferredDateFormat().format(date);
        assertEquals(date_RFC1123, date_formatted);
    }

    @Test
    public void testParameterizedHeader() throws ParseException {
        ParameterizedHeader ph = new ParameterizedHeader("a");
        assertEquals("a", ph.getValue());

        ph = new ParameterizedHeader("a/b");
        assertEquals("a/b", ph.getValue());

        ph = new ParameterizedHeader("  a  /  b  ");
        assertEquals("a/b", ph.getValue());

        ph = new ParameterizedHeader("");
        assertEquals("", ph.getValue());

        ph = new ParameterizedHeader(";");
        assertEquals("", ph.getValue());
        assertEquals(0, ph.getParameters().size());

        ph = new ParameterizedHeader(";;;");
        assertEquals("", ph.getValue());
        assertEquals(0, ph.getParameters().size());

        ph = new ParameterizedHeader("  ;  ;  ;  ");
        assertEquals("", ph.getValue());
        assertEquals(0, ph.getParameters().size());

        ph = new ParameterizedHeader("a;x=1;y=2");
        assertEquals("a", ph.getValue());
        assertEquals(2, ph.getParameters().size());
        assertEquals("1", ph.getParameters().get("x"));
        assertEquals("2", ph.getParameters().get("y"));

        ph = new ParameterizedHeader("a ;  x=1  ;  y=2  ");
        assertEquals("a", ph.getValue());
        assertEquals(2, ph.getParameters().size());
        assertEquals("1", ph.getParameters().get("x"));
        assertEquals("2", ph.getParameters().get("y"));
    }
}
