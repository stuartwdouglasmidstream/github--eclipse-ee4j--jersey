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

package org.glassfish.jersey.tests.e2e.server.routing;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class ResponseMediaTypeFromProvidersTest extends JerseyTest {

    @Path("response")
    public static class ResponseResource {

        private List<String> getList() {
            // must be an ArrayList. Arrays.asList creates Arrays$ArrayList.
            return Arrays.asList("array", "list").stream().collect(Collectors.toCollection(ArrayList::new));
        }

        @GET
        @Path("list")
        public Response responseList() {
            return Response.ok(getList()).build();
        }
    }

    @Provider
    public static class CollectionMessageBodyWriter implements MessageBodyWriter<Collection<?>> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return type.equals(ArrayList.class) && genericType.equals(ArrayList.class);
        }

        @Override
        public long getSize(final Collection<?> objects, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final Collection<?> objects,
                            final Class<?> type,
                            final Type genericType,
                            final Annotation[] annotations,
                            final MediaType mediaType,
                            final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write("OK".getBytes());
        }
    }

    @Provider
    public static class IncorrectCollectionMessageBodyWriter implements MessageBodyWriter<Collection<?>> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return !new CollectionMessageBodyWriter().isWriteable(type, genericType, annotations, mediaType);
        }

        @Override
        public long getSize(final Collection<?> objects, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final Collection<?> objects,
                            final Class<?> type,
                            final Type genericType,
                            final Annotation[] annotations,
                            final MediaType mediaType,
                            final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write("ERROR".getBytes());
        }
    }

    @Test
    public void testResponseList() throws Exception {
        final Response response = target("response").path("list").request().get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("OK"));
    }

    public static class StringBean {

        private String value;

        public StringBean() {
        }

        public StringBean(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Provider
    public static class StringBeanMessageBodyWriter implements MessageBodyWriter<StringBean> {

        public static MediaType STRING_BEAN_MT = new MediaType("string", "bean");

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return type.equals(StringBean.class) && STRING_BEAN_MT.equals(mediaType);
        }

        @Override
        public long getSize(final StringBean objects, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final StringBean objects,
                            final Class<?> type,
                            final Type genericType,
                            final Annotation[] annotations,
                            final MediaType mediaType,
                            final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            entityStream.write(objects.getValue().getBytes());
        }
    }

    @Path("AcceptableNonWriteableMethodResource")
    public static class AcceptableNonWriteableMethodResource {

        @GET
        public StringBean getStringBean() {
            return new StringBean("getStringBean");
        }

        @GET
        @Produces("text/html")
        public StringBean getTextHtml() {
            return new StringBean("getTextHtml");
        }

        @GET
        @Produces("text/xhtml")
        public StringBean getTextXHtml() {
            return new StringBean("getTextXHtml");
        }

        @POST
        @Consumes("string/bean")
        @Produces("string/bean")
        public StringBean postStringBean(final StringBean stringBean) {
            return stringBean;
        }

        @POST
        @Produces("string/bean")
        public StringBean postStringBean(final String string) {
            return new StringBean("postStringBean_" + string);
        }

        @POST
        @Consumes("string/bean")
        @Path("response")
        public Response postResponse(final StringBean stringBean) {
            return Response.ok(stringBean).type("string/bean").build();
        }

        @POST
        @Path("response")
        public Response postResponse(final String string) {
            return Response.ok(new StringBean("postStringBean_" + string)).type("string/bean").build();
        }
    }

    @Test
    public void testGetMethodRouting() throws Exception {
        final Response response = target("AcceptableNonWriteableMethodResource").request("text/html", "text/xhtml",
                "string/bean;q=0.2").get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getMediaType(), equalTo(StringBeanMessageBodyWriter.STRING_BEAN_MT));
        assertThat(response.readEntity(String.class), equalTo("getStringBean"));
    }

    @Test
    public void testPostMethodRouting() throws Exception {
        final Response response = target("AcceptableNonWriteableMethodResource").request("text/html", "text/xhtml",
                "string/bean;q=0.2").post(Entity.entity("value", "string/bean"));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getMediaType(), equalTo(StringBeanMessageBodyWriter.STRING_BEAN_MT));
        assertThat(response.readEntity(String.class), equalTo("postStringBean_value"));
    }

    @Test
    public void testPostMethodRoutingWildcard() throws Exception {
        final Response response = target("AcceptableNonWriteableMethodResource").request("*/*")
                .post(Entity.entity("value", "string/bean"));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getMediaType(), equalTo(StringBeanMessageBodyWriter.STRING_BEAN_MT));
        assertThat(response.readEntity(String.class), equalTo("postStringBean_value"));
    }

    @Test
    public void testPostMethodRoutingResponse() throws Exception {
        final Response response = target("AcceptableNonWriteableMethodResource").path("response").request()
                .post(Entity.entity("value", "string/bean"));

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.getMediaType(), equalTo(StringBeanMessageBodyWriter.STRING_BEAN_MT));
        assertThat(response.readEntity(String.class), equalTo("postStringBean_value"));
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ResponseResource.class, AcceptableNonWriteableMethodResource.class)
                .register(CollectionMessageBodyWriter.class)
                .register(IncorrectCollectionMessageBodyWriter.class)
                .register(StringBeanMessageBodyWriter.class);
    }
}
