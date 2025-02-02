/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link ParamConverter param converters} as e2e test.
 *
 * @author Miroslav Fuksa
 */
public class ParamConverterTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class, MyParamProvider.class, MyStringParamProvider.class);
    }

    @Test
    public void testMyBeanParam() {
        Form form = new Form();
        form.param("form", "formParam");
        final Response response = target()
                .path("resource/myBean").path("pathParam")
                .matrixParam("matrix", "matrixParam")
                .queryParam("query", "queryParam")
                .request()
                .header("header", "headerParam")
                .cookie("cookie", "cookieParam")
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        final String str = response.readEntity(String.class);
        assertEquals("*pathParam*_*matrixParam*_*queryParam*_*headerParam*_*cookieParam*_*formParam*", str);
    }

    @Test
    public void testListOfMyBeanParam() {
        final Response response = target().path("resource/myBean/list")
                .queryParam("q", "A")
                .queryParam("q", "B")
                .queryParam("q", "C")
                .request().get();
        final String str = response.readEntity(String.class);
        assertEquals("*A**B**C*", str);
    }

    @Test
    public void testSetOfMyBeanParam() {
        final Response response = target().path("resource/myBean/set")
                .queryParam("q", "A")
                .queryParam("q", "B")
                .queryParam("q", "C")
                .request().get();
        final String str = response.readEntity(String.class);
        assertThat(str, containsString("*A*"));
        assertThat(str, containsString("*B*"));
        assertThat(str, containsString("*C*"));
    }

    @Test
    public void testSortedSetOfMyBeanParam() {
        final Response response = target().path("resource/myBean/sortedset")
                .queryParam("q", "A")
                .queryParam("q", "B")
                .queryParam("q", "C")
                .request().get();
        final String str = response.readEntity(String.class);
        assertEquals("*A**B**C*", str);
    }

    @Test
    public void testStringParam() {
        Form form = new Form();
        form.param("form", "formParam");
        final Response response = target()
                .path("resource/string").path("pathParam")
                .matrixParam("matrix", "matrixParam")
                .queryParam("query", "queryParam")
                .request()
                .header("header", "headerParam")
                .cookie("cookie", "cookieParam")
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        final String str = response.readEntity(String.class);
        assertEquals("-pathParam-_-matrixParam-_-queryParam-_-headerParam-_-cookieParam-_-formParam-", str);
    }

    @Test
    @Disabled("TODO: ParamConversion not yet implemented in the ResponseBuilder (JERSEY-1385).")
    // TODO: JERSEY-1385: after clarifying with spec the ResponseBuilder paramconversion should be finished (or removed)
    public void testStringParamInResponse() {
        final Response response = target().path("resource/response").request().get();
        assertEquals("-:res-head:-", response.getHeaderString("response-header"));

    }

    @Test
    public void testMyBeanFormParamDefault() {
        Form form = new Form();
        Response response = target().path("resource/myBeanFormDefault")
                .request().post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        String str = response.readEntity(String.class);
        assertEquals("*form-default*", str);
    }

    @Test
    public void testMyBeanQueryParamDefault() {
        final Response response = target().path("resource/myBeanQueryDefault")
                .request().get();
        final String str = response.readEntity(String.class);
        assertEquals("*query-default*", str);
    }

    @Test
    public void testMyBeanMatrixParamDefault() {
        final Response response = target().path("resource/myBeanMatrixDefault")
                .request().get();
        final String str = response.readEntity(String.class);
        assertEquals("*matrix-default*", str);
    }

    @Test
    public void testMyBeanCookieParamDefault() {
        final Response response = target().path("resource/myBeanCookieDefault")
                .request().get();
        final String str = response.readEntity(String.class);
        assertEquals("*cookie-default*", str);
    }

    @Test
    public void testMyBeanHeaderParamDefault() {
        final Response response = target().path("resource/myBeanHeaderDefault")
                .request().get();
        final String str = response.readEntity(String.class);
        assertEquals("*header-default*", str);
    }

    @Path("resource")
    public static class Resource {

        @POST
        @Path("myBean/{path}")
        public String postMyBean(@PathParam("path") MyBean pathParam, @MatrixParam("matrix") MyBean matrix,
                                 @QueryParam("query") MyBean query, @HeaderParam("header") MyBean header,
                                 @CookieParam("cookie") MyBean cookie, @FormParam("form") MyBean form) {
            return pathParam.getValue() + "_" + matrix.getValue() + "_" + query.getValue() + "_" + header.getValue() + "_"
                    + cookie.getValue() + "_" + form.getValue();
        }

        @GET
        @Path("myBean/list")
        public String postMyBean(@QueryParam("q") List<MyBean> query) {
            StringBuilder sb = new StringBuilder();
            for (MyBean bean : query) {
                sb.append(bean.getValue());
            }

            return sb.toString();
        }

        @GET
        @Path("myBean/set")
        public String postMyBean(@QueryParam("q") Set<MyBean> query) {
            StringBuilder sb = new StringBuilder();
            for (MyBean bean : query) {
                sb.append(bean.getValue());
            }

            return sb.toString();
        }

        @GET
        @Path("myBean/sortedset")
        public String postMyBean(@QueryParam("q") SortedSet<MyBean> query) {
            StringBuilder sb = new StringBuilder();
            for (MyBean bean : query) {
                sb.append(bean.getValue());
            }

            return sb.toString();
        }

        @POST
        @Path("myBeanFormDefault")
        public String postMyBeanFormDefault(@DefaultValue("form-default") @FormParam("form") MyBean pathParam) {
            return pathParam.getValue();
        }

        @GET
        @Path("myBeanQueryDefault")
        public String getMyBeanQueryDefault(@DefaultValue("query-default") @QueryParam("q") MyBean queryParam) {
            return queryParam.getValue();
        }

        @GET
        @Path("myBeanMatrixDefault")
        public String getMyBeanMatrixDefault(@DefaultValue("matrix-default") @MatrixParam("m") MyBean matrixParam) {
            return matrixParam.getValue();
        }

        @GET
        @Path("myBeanCookieDefault")
        public String getMyBeanCookieDefault(@DefaultValue("cookie-default") @CookieParam("c") MyBean cookieParam) {
            return cookieParam.getValue();
        }

        @GET
        @Path("myBeanHeaderDefault")
        public String getMyBeanHeaderDefault(@DefaultValue("header-default") @HeaderParam("h") MyBean headerParam) {
            return headerParam.getValue();
        }

        @POST
        @Path("string/{path}")
        public String postString(@PathParam("path") String pathParam, @MatrixParam("matrix") String matrix,
                                 @QueryParam("query") String query, @HeaderParam("header") String header,
                                 @CookieParam("cookie") String cookie, @FormParam("form") String form) {
            return pathParam + "_" + matrix + "_" + query + "_" + header + "_"
                    + cookie + "_" + form;
        }

        @GET
        @Path("q")
        public String get(@QueryParam("query") String query) {
            return query;
        }

        @GET
        @Path("response")
        public Response getResponse() {
            return Response.ok().header("response-header", "res-head").entity("anything").build();
        }
    }

    public static class MyParamProvider implements ParamConverterProvider {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType != MyBean.class) {
                return null;
            }

            return (ParamConverter<T>) new ParamConverter<MyBean>() {

                @Override
                public MyBean fromString(String value) throws IllegalArgumentException {
                    final MyBean myBean = new MyBean();
                    myBean.setValue("*" + value + "*");
                    return myBean;
                }

                @Override
                public String toString(MyBean bean) throws IllegalArgumentException {
                    return "*:" + bean.getValue() + ":*";
                }

            };
        }
    }

    public static class MyStringParamProvider implements ParamConverterProvider {

        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (rawType != String.class) {
                return null;
            }

            return (ParamConverter<T>) new ParamConverter<String>() {

                @Override
                public String fromString(String value) throws IllegalArgumentException {
                    return "-" + value + "-";
                }

                @Override
                public String toString(String str) throws IllegalArgumentException {
                    return "-:" + str + ":-";
                }

            };
        }
    }

    public static class MyBean implements Comparable<MyBean> {

        private String value;

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "MyBean{"
                    + "value='" + value + '\''
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MyBean)) {
                return false;
            }

            MyBean myBean = (MyBean) o;

            return !(value != null ? !value.equals(myBean.value) : myBean.value != null);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @Override
        public int compareTo(MyBean o) {
            return value.compareTo(o.value);
        }
    }
}
