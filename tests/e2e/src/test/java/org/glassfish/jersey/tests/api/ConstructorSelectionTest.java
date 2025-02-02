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

package org.glassfish.jersey.tests.api;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test to verify the proper constructor is selected.
 *
 * @author Marek Potociar
 */
public class ConstructorSelectionTest extends JerseyTest {

    /**
     * A resource with multiple constructors.
     */
    @Path("resource-test")
    public static class MultipleConstructorResource {
        private HttpHeaders headers;
        private UriInfo info;
        private Application application;
        private Request request;
        private Providers provider;

        public MultipleConstructorResource(){
        }

        public MultipleConstructorResource(@Context HttpHeaders headers){
            this.headers = headers;
        }


        public MultipleConstructorResource(@Context HttpHeaders headers,
                                           @Context UriInfo info){
            this.headers = headers;
            this.info = info;
        }

        public MultipleConstructorResource(@Context HttpHeaders headers,
                                           @Context UriInfo info,
                                           @Context Application application){
            this.application = application;
            this.headers = headers;
            this.info = info;
        }

        public MultipleConstructorResource(@Context HttpHeaders headers,
                                           @Context UriInfo info,
                                           @Context Application application,
                                           @Context Request request){
            this.application = application;
            this.headers = headers;
            this.info = info;
            this.request = request;
        }

        protected MultipleConstructorResource(@Context HttpHeaders headers,
                                              @Context UriInfo info,
                                              @Context Application application,
                                              @Context Request request,
                                              @Context Providers provider){
            this.application = application;
            this.headers = headers;
            this.info = info;
            this.request = request;
            this.provider = provider;
        }

        @GET
        public Response isUsedConstructorWithMostAttributes(){
            boolean ok = application != null;
            ok &= headers != null;
            ok &= info != null;
            ok &= request != null;
            ok &= provider == null;
            Response.Status status = ok ? Response.Status.OK : Response.Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).build();
        }
    }

    /**
     * Provider with multiple constructors.
     */
    @Provider
    @Consumes(MediaType.TEXT_PLAIN)
    public static class StringReader implements MessageBodyReader<String> {
        private HttpHeaders headers;
        private UriInfo info;
        private Application application;
        private Request request;
        private Providers providers;

        protected StringReader(@Context HttpHeaders headers, @Context UriInfo info,
                               @Context Application application, @Context Request request,
                               @Context Providers providers) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
            this.providers = providers;
        }

        public StringReader(@Context HttpHeaders headers, @Context UriInfo info,
                            @Context Application application, @Context Request request) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
        }

        public StringReader(@Context HttpHeaders headers, @Context UriInfo info,
                            @Context Application application) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
        }

        public StringReader(@Context HttpHeaders headers, @Context UriInfo info) {
            super();
            this.headers = headers;
            this.info = info;
        }

        public StringReader(@Context HttpHeaders headers) {
            super();
            this.headers = headers;
        }

        @Override
        public boolean isReadable(Class<?> type, Type genericType,
                                  Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public String readFrom(Class<String> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            if (headers == null || info == null || application == null || request == null || providers != null) {
                return "fail";
            }
            return "pass";
        }
    }

    @Path("provider-test")
    public static class ProviderResource {

        @POST
        public String echo(String entity) {
            return entity;
        }

    }

    @Override
    protected Application configure() {
        return new ResourceConfig(MultipleConstructorResource.class, ProviderResource.class, StringReader.class);
    }

    /**
     * JERSEY-1529 reproducer.
     */
    @Test
    public void testResourceConstructorSelection() {
        final Response response = target("resource-test").request().get();

        assertNotNull(response, "Returned response must not be null.");
        assertEquals(200, response.getStatus(), "Resource constructor with most arguments has not been selected.");
    }

    /**
     * JERSEY-1712 reproducer.
     */
    @Test
    public void testProviderConstructorSelection() {
        final Response response = target("provider-test").request().post(Entity.text("echo"));

        assertNotNull(response, "Returned response must not be null.");
        assertEquals(200, response.getStatus());
        assertEquals("pass", response.readEntity(String.class));
    }
}
