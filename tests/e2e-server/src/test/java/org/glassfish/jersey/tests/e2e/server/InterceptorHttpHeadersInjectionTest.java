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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for JERSEY-1545.
 *
 * @author Jakub Podlesak
 */
public class InterceptorHttpHeadersInjectionTest extends JerseyTest {

    static final String WriterHEADER = "custom-writer-header";
    static final String ReaderHEADER = "custom-reader-header";
    static final String RawCONTENT = "SIMPLE";

    @Provider
    public static class InjectedWriterInterceptor implements WriterInterceptor {

        @Context
        HttpHeaders headers;

        // Replace content with WriterHEADER header value if corresponding header is seen.
        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final String writerHeaderValue = headers.getHeaderString(WriterHEADER);

            if (writerHeaderValue != null) {
                context.getOutputStream().write(writerHeaderValue.getBytes());
            }

            context.proceed();
        }
    }

    @Provider
    public static class InjectedReaderInterceptor implements ReaderInterceptor {

        @Context
        HttpHeaders headers;

        // Replace content with ReaderHEADER header value if corresponding header is seen.
        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            final String readerHeaderValue = headers.getHeaderString(ReaderHEADER);

            if (readerHeaderValue != null) {
                return readerHeaderValue;
            }
            return context.proceed();
        }
    }

    @Path("/")
    public static class SimpleResource {

        @GET
        public String getIt() {
            return RawCONTENT;
        }

        @POST
        public String echo(String message) {
            return message;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(SimpleResource.class, InjectedWriterInterceptor.class, InjectedReaderInterceptor.class);
    }

    // No interceptor should tweak the content if there is not header present.
    private void _checkRawGet() {
        final String result = target().request().get(String.class);
        assertThat(result, containsString(RawCONTENT));
    }

    @Test
    public void testWriter() throws Exception {
        _checkRawGet();
        _checkWriterInterceptor("writer-one");
        _checkWriterInterceptor("writer-two");
    }

    // set WriterHEADER header and check the same value is returned back
    private void _checkWriterInterceptor(final String headerValue) {
        final String result = target().request().header(WriterHEADER, headerValue).get(String.class);
        assertThat(result, containsString(headerValue));
    }

    @Test
    public void testReader() throws Exception {
        _checkRawEcho();
        _checkReaderInterceptor("reader-one");
        _checkReaderInterceptor("reader-two");
    }

    // No interceptor should tweak the content if there is not header present.
    private void _checkRawEcho() {
        final String rawResult = target().request().post(Entity.text(RawCONTENT), String.class);
        assertThat(rawResult, containsString(RawCONTENT));
    }

    // set ReaderHEADER header and check the same value is returned back
    private void _checkReaderInterceptor(String headerValue) {
        final String result = target().request().header(ReaderHEADER, headerValue).post(Entity.text(RawCONTENT), String.class);
        assertThat(result, containsString(headerValue));
    }
}
