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

import java.util.Arrays;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link BeanParam bean param injections}.
 *
 * @author Miroslav Fuksa
 */
public class BeanParamTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class,
                ResourceInitializedBySetter.class);
    }

    @Test
    public void compareBeanWithStandardParams() {
        FullBean bean = getFullBean();
        Response response = doRequest(bean, "resource/compareBean");
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("true", response.readEntity(String.class));
    }

    @Test
    public void testSingleFullBean() {
        FullBean bean = getFullBean();
        Response response = doRequest(bean, "resource/singleBean");
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(bean.toString(), response.readEntity(String.class));
    }

    @Test
    public void testSingleConstructorInitializedBean() {
        FullBean bean = getFullBean();
        Response response = doRequest(bean, "resource/constructorBean");
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(bean.toString(), response.readEntity(String.class));
    }

    @Test
    public void testTwoFullBeans() {
        FullBean bean = getFullBean();
        Response response = doRequest(bean, "resource/twoBeans");
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(bean.toString() + " / " + bean.toString(), response.readEntity(String.class));
    }

    @Test
    public void testTwoDifferentBeans() {
        FullBean fullBean = getFullBean();
        SmallBean smallBean = new SmallBean(fullBean);
        Response response = doRequest(fullBean, "resource/differentBeans");
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(fullBean.toString() + " / " + smallBean.toString(), response.readEntity(String.class));
    }

    @Test
    public void testEncodedBean() {
        FullBean fullBean = getFullBean();
        fullBean.setQueryParam("encoded/a?&&+./?");
        fullBean.setMatrixParam("not-encoded/a?&&+./?");

        Response response = doRequest(fullBean, "resource/encodedBean");
        Assertions.assertEquals(200, response.getStatus());

        EncodedBean bean = new EncodedBean("not-encoded/a?&&+./?", "encoded%2Fa%3F%26%26%2B.%2F%3F");
        Assertions.assertEquals(bean.toString(), response.readEntity(String.class));
    }

    private Response doRequest(FullBean bean, String path) {
        final Form form = new Form();
        form.asMap().put("form", Arrays.asList(bean.getFormParam()));

        return target().path(path).path(bean.getPathParam()).matrixParam("matrix",
                bean.getMatrixParam()).queryParam("query",
                bean.getQueryParam()).request().header("header", bean.getHeaderParam()).cookie("cookie",
                bean.getCookie()).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    private FullBean getFullBean() {
        FullBean bean = new FullBean();
        bean.setPathParam("pathParameter");
        bean.setMatrixParam("matrixParameter");
        bean.setQueryParam("queryParameter");
        bean.setHeaderParam("headerParameter");
        bean.setCookie("cookieParameter");
        bean.setFormParam("formParameter");
        bean.setOverrideRequestNull(true);
        return bean;
    }

    @Path("resource")
    public static class Resource {

        @POST
        @Path("singleBean/{path}")
        public String postBeanParam(@BeanParam FullBean bean) {
            return bean == null ? "fail: bean param is null!!!" : bean.toString();
        }

        @POST
        @Path("constructorBean/{path}")
        public String constructorBeanParam(@BeanParam ConstructorInitializedBean bean) {
            return bean == null ? "fail: bean param is null!!!" : bean.toString();
        }

        @POST
        @Path("compareBean/{path}")
        public String compareBeanParam(@BeanParam ConstructorInitializedBean bean, @CookieParam("cookie") String cookie,
                                       @FormParam("form") String formParam,
                                       @HeaderParam("header") String headerParam, @MatrixParam("matrix") String matrixParam,
                                       @QueryParam("query") String queryParam, @PathParam("path") String pathParam,
                                       @Context Request request) {
            ConstructorInitializedBean newBean = new ConstructorInitializedBean(cookie, formParam,
                    headerParam, matrixParam,
                    queryParam, pathParam, request);

            return String.valueOf(bean.toString().equals(newBean.toString()));
        }

        @POST
        @Path("twoBeans/{path}")
        public String postTwoSameBeans(@BeanParam FullBean bean1, @BeanParam FullBean bean2) {
            if (bean1 == null) {
                return "fail: bean1 param is null!!!";
            }
            if (bean2 == null) {
                return "fail: bean2 param is null!!!";
            }
            return bean1.toString() + " / " + bean2.toString();
        }

        @POST
        @Path("differentBeans/{path}")
        public String postTwoDifferentBeans(@BeanParam FullBean bean1, @BeanParam SmallBean bean2) {
            if (bean1 == null) {
                return "fail: bean1 param is null!!!";
            }
            if (bean2 == null) {
                return "fail: bean2 param is null!!!";
            }
            return bean1.toString() + " / " + bean2.toString();
        }

        @POST
        @Path("encodedBean/{path}")
        public String postEncodedParam(@BeanParam EncodedBean bean) {
            return bean == null ? "fail: bean param is null!!!" : bean.toString();
        }

    }

    public static class SmallBean {

        @HeaderParam("header")
        private String headerParam;

        @PathParam("path")
        private String pathParam;

        public SmallBean(FullBean bean) {
            headerParam = bean.getHeaderParam();
            pathParam = bean.getPathParam();
        }

        public SmallBean() {
        }

        @Override
        public String toString() {
            return "SmallBean{"
                    + "headerParam='" + headerParam + '\''
                    + ", pathParam='" + pathParam + '\''
                    + '}';
        }
    }

    public static class EncodedBean {

        @MatrixParam("matrix")
        private String matrixParam;

        @Encoded
        @QueryParam("query")
        private String queryParam;

        public EncodedBean(String matrixParam, String queryParam) {
            this.matrixParam = matrixParam;
            this.queryParam = queryParam;
        }

        public EncodedBean() {
        }

        @Override
        public String toString() {
            return "EncodedBean{"
                    + "matrixParam='" + matrixParam + '\''
                    + ", queryParam='" + queryParam + '\''
                    + '}';
        }
    }

    public static class FullBean {

        @HeaderParam("header")
        private String headerParam;

        @PathParam("path")
        private String pathParam;

        @MatrixParam("matrix")
        private String matrixParam;

        @QueryParam("query")
        private String queryParam;

        @CookieParam("cookie")
        private String cookie;

        @FormParam("form")
        private String formParam;

        @Context
        private Request request;

        private boolean overrideRequestNull;

        public FullBean() {
        }

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }

        public String getFormParam() {
            return formParam;
        }

        public void setFormParam(String formParam) {
            this.formParam = formParam;
        }

        public String getHeaderParam() {
            return headerParam;
        }

        public void setHeaderParam(String headerParam) {
            this.headerParam = headerParam;
        }

        public String getMatrixParam() {
            return matrixParam;
        }

        public void setMatrixParam(String matrixParam) {
            this.matrixParam = matrixParam;
        }

        public String getPathParam() {
            return pathParam;
        }

        public void setPathParam(String pathParam) {
            this.pathParam = pathParam;
        }

        public String getQueryParam() {
            return queryParam;
        }

        public void setQueryParam(String queryParam) {
            this.queryParam = queryParam;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public boolean isOverrideRequestNull() {
            return overrideRequestNull;
        }

        public void setOverrideRequestNull(boolean overrideRequestNull) {
            this.overrideRequestNull = overrideRequestNull;
        }

        private String requestToString() {
            if (overrideRequestNull) {
                return "not-null";
            } else {
                return request == null ? "null" : "not-null";
            }
        }

        @Override
        public String toString() {
            return "Bean{"
                    + "cookie='" + cookie + '\''
                    + ", formParam='" + formParam + '\''
                    + ", headerParam='" + headerParam + '\''
                    + ", matrixParam='" + matrixParam + '\''
                    + ", pathParam='" + pathParam + '\''
                    + ", queryParam='" + queryParam + '\''
                    + ", request='" + requestToString() + "'"
                    + '}';
        }
    }

    public static class ConstructorInitializedBean {

        private String headerParam;
        private String pathParam;
        private String matrixParam;
        private String queryParam;
        private String cookie;
        private String formParam;
        private Request request;

        public ConstructorInitializedBean(@CookieParam("cookie") String cookie, @FormParam("form") String formParam,
                                          @HeaderParam("header") String headerParam, @MatrixParam("matrix") String matrixParam,
                                          @QueryParam("query") String queryParam, @PathParam("path") String pathParam,
                                          @Context Request request) {
            this.cookie = cookie;
            this.formParam = formParam;
            this.headerParam = headerParam;
            this.matrixParam = matrixParam;
            this.queryParam = queryParam;
            this.pathParam = pathParam;
            this.request = request;
        }

        private boolean overrideRequestNull;

        public String getCookie() {
            return cookie;
        }

        public void setCookie(String cookie) {
            this.cookie = cookie;
        }

        public String getFormParam() {
            return formParam;
        }

        public void setFormParam(String formParam) {
            this.formParam = formParam;
        }

        public String getHeaderParam() {
            return headerParam;
        }

        public void setHeaderParam(String headerParam) {
            this.headerParam = headerParam;
        }

        public String getMatrixParam() {
            return matrixParam;
        }

        public void setMatrixParam(String matrixParam) {
            this.matrixParam = matrixParam;
        }

        public String getPathParam() {
            return pathParam;
        }

        public void setPathParam(String pathParam) {
            this.pathParam = pathParam;
        }

        public String getQueryParam() {
            return queryParam;
        }

        public void setQueryParam(String queryParam) {
            this.queryParam = queryParam;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public boolean isOverrideRequestNull() {
            return overrideRequestNull;
        }

        public void setOverrideRequestNull(boolean overrideRequestNull) {
            this.overrideRequestNull = overrideRequestNull;
        }

        private String requestToString() {
            if (overrideRequestNull) {
                return "not-null";
            } else {
                return request == null ? "null" : "not-null";
            }
        }

        @Override
        public String toString() {
            return "Bean{"
                    + "cookie='" + cookie + '\''
                    + ", formParam='" + formParam + '\''
                    + ", headerParam='" + headerParam + '\''
                    + ", matrixParam='" + matrixParam + '\''
                    + ", pathParam='" + pathParam + '\''
                    + ", queryParam='" + queryParam + '\''
                    + ", request='" + requestToString() + "'"
                    + '}';
        }
    }

    @Path("resource-setter")
    public static class ResourceInitializedBySetter {

        private FullBean fullBean;

        @BeanParam
        public void setFullBean(FullBean fullBean) {
            this.fullBean = fullBean;
        }

        @POST
        @Path("{path}")
        public String post() {
            return fullBean.toString();
        }
    }

    @Test
    public void testResourceInitializedBySetter() {
        FullBean bean = getFullBean();
        final Response response = doRequest(bean, "resource-setter");
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(bean.toString(), response.readEntity(String.class));
    }

}
