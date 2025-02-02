/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.inject;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import jakarta.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.RequestContextBuilder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Paul Sandoz
 * @author Pavel Bucek
 */
public class FormParamTest extends AbstractTest {

    @Path("/")
    public static class SimpleFormResource {
        @POST
        public String post(
                @FormParam("a") final String a
        ) {
            assertEquals("foo", a);
            return a;
        }
    }

    @Test
    public void testSimpleFormResource() throws ExecutionException, InterruptedException {
        initiateWebApplication(SimpleFormResource.class);

        final Form form = new Form();
        form.param("a", "foo");

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("foo", responseContext.getEntity());
    }


    @Test
    public void testSimpleFormResourceWithCharset() throws ExecutionException, InterruptedException {
        initiateWebApplication(SimpleFormResource.class);

        final Form form = new Form();
        form.param("a", "foo");

        final ContainerResponse responseContext = apply(RequestContextBuilder.from("/", "POST")
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE.withCharset("UTF-8"))
                .entity(form)
                .build()
        );

        assertEquals("foo", responseContext.getEntity());
    }

    @Path("/")
    public static class FormResourceNoConsumes {
        @POST
        public String post(
                @FormParam("a") final String a,
                final MultivaluedMap<String, String> form) {
            assertEquals(a, form.getFirst("a"));
            return a;
        }
    }

    @Test
    public void testFormResourceNoConsumes() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormResourceNoConsumes.class);

        final Form form = new Form();
        form.param("a", "foo");

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("foo", responseContext.getEntity());
    }

    @Path("/")
    public static class FormResourceFormEntityParam {
        @POST
        public String post(
                @FormParam("a") final String a,
                final Form form) {
            assertEquals(a, form.asMap().getFirst("a"));
            return a;
        }
    }

    @Test
    public void testFormResourceFormEntityParam() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormResourceFormEntityParam.class);

        final Form form = new Form();
        form.param("a", "foo");

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("foo", responseContext.getEntity());
    }

    @XmlRootElement(name = "jaxbBean")
    public static class JAXBBean {

        public String value;

        public JAXBBean() {
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof JAXBBean && ((JAXBBean) o).value.equals(value);
        }

        @Override
        public String toString() {
            return "JAXBClass: " + value;
        }
    }

    @Path("/")
    public static class FormResourceX {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String post(
                @FormParam("a") final String a,
                @FormParam("b") final String b,
                final MultivaluedMap<String, String> form,
                @Context final UriInfo ui,
                @QueryParam("a") final String qa) {
            assertEquals(a, form.getFirst("a"));
            assertEquals(b, form.getFirst("b"));
            return a + b;
        }
    }

    @Path("/")
    public static class FormResourceY {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String post(
                @FormParam("a") final String a,
                @FormParam("b") final String b,
                final Form form,
                @Context final UriInfo ui,
                @QueryParam("a") final String qa) {
            assertEquals(a, form.asMap().getFirst("a"));
            assertEquals(b, form.asMap().getFirst("b"));
            return a + b;
        }
    }

    @Test
    public void testFormParamX() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormResourceX.class);

        final Form form = new Form();
        form.param("a", "foo");
        form.param("b", "bar");

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("foobar", responseContext.getEntity());
    }

    @Test
    public void testFormParamY() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormResourceY.class);

        final Form form = new Form();
        form.param("a", "foo");
        form.param("b", "bar");

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("foobar", responseContext.getEntity());
    }

    @Path("/")
    public static class FormParamTypes {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String createSubscription(
                @FormParam("int") final int i,
                @FormParam("float") final float f,
                @FormParam("decimal") final BigDecimal d
        ) {
            return "" + i + " " + f + " " + d;
        }
    }

    @Test
    public void testFormParamTypes() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormParamTypes.class);

        final Form form = new Form();
        form.param("int", "1");
        form.param("float", "3.14");
        form.param("decimal", "3.14");

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("1 3.14 3.14", responseContext.getEntity());
    }

    /**
     * JERSEY-2637 reproducer outside of container (pure server).
     */
    @Test
    public void testFormParamAsQueryParams() throws Exception {
        initiateWebApplication(FormParamTypes.class);

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/?int=2&float=2.71&decimal=2.71", "POST")
                        .type(MediaType.APPLICATION_FORM_URLENCODED)
                        .build()
        );

        assertEquals("0 0.0 null", responseContext.getEntity());
    }

    @Path("/")
    public static class FormDefaultValueParamTypes {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String createSubscription(
                @DefaultValue("1") @FormParam("int") final int i,
                @DefaultValue("3.14") @FormParam("float") final float f,
                @DefaultValue("3.14") @FormParam("decimal") final BigDecimal d
        ) {
            return "" + i + " " + f + " " + d;
        }
    }

    @Test
    public void testFormDefaultValueParamTypes() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormDefaultValueParamTypes.class);

        final Form form = new Form();

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("1 3.14 3.14", responseContext.getEntity());
    }

    public static class TrimmedString {
        private final String string;

        public TrimmedString(final String string) {
            this.string = string.trim();
        }

        @Override
        public String toString() {
            return string;
        }
    }

    @Path("/")
    public static class FormConstructorValueParamTypes {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String createSubscription(
                @DefaultValue("") @FormParam("trim") final TrimmedString s) {
            return s.toString();
        }
    }

    @Test
    public void testFormConstructorValueParamTypes() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormConstructorValueParamTypes.class);

        final Form form = new Form();

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("", responseContext.getEntity());
    }

    @Path("/")
    public static class FormResourceJAXB {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.APPLICATION_XML)
        public JAXBBean post(
                @FormParam("a") final JAXBBean a,
                @FormParam("b") final List<JAXBBean> b) {
            assertEquals("a", a.value);
            assertEquals(2, b.size());
            assertEquals("b1", b.get(0).value);
            assertEquals("b2", b.get(1).value);
            return a;
        }
    }

    @Test
    public void testFormParamJAXB() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormResourceJAXB.class);

        final Form form = new Form();
        form.param("a", "<jaxbBean><value>a</value></jaxbBean>");
        form.param("b", "<jaxbBean><value>b1</value></jaxbBean>");
        form.param("b", "<jaxbBean><value>b2</value></jaxbBean>");


        final ContainerResponse responseContext = apply(RequestContextBuilder.from("/", "POST")
                .accept(MediaType.APPLICATION_XML)
                .type(MediaType.APPLICATION_FORM_URLENCODED)
                .entity(form)
                .build()
        );

        final JAXBBean b = (JAXBBean) responseContext.getEntity();
        assertEquals("a", b.value);
    }

    @Test
    public void testFormParamJAXBError() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormResourceJAXB.class);

        final Form form = new Form();
        form.param("a", "<x><value>a</value></jaxbBean>");
        form.param("b", "<x><value>b1</value></jaxbBean>");
        form.param("b", "<x><value>b2</value></jaxbBean>");

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals(400, responseContext.getStatus());
    }

    @Path("/")
    public static class FormResourceDate {
        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String post(
                @FormParam("a") final Date a,
                @FormParam("b") final Date b,
                @FormParam("c") final Date c) {
            assertNotNull(a);
            assertNotNull(b);
            assertNotNull(c);
            return "POST";
        }
    }

    @Test
    public void testFormParamDate() throws ExecutionException, InterruptedException {
        initiateWebApplication(FormResourceDate.class);

        final String date_RFC1123 = "Sun, 06 Nov 1994 08:49:37 GMT";
        final String date_RFC1036 = "Sunday, 06-Nov-94 08:49:37 GMT";
        final String date_ANSI_C = "Sun Nov  6 08:49:37 1994";

        final Form form = new Form();
        form.param("a", date_RFC1123);
        form.param("b", date_RFC1036);
        form.param("c", date_ANSI_C);

        final ContainerResponse responseContext = apply(
                RequestContextBuilder.from("/", "POST").type(MediaType.APPLICATION_FORM_URLENCODED).entity(form).build()
        );

        assertEquals("POST", responseContext.getEntity());
    }

//    @InjectParam replace with @Inject?

//    public static class ParamBean {
//        @FormParam("a") String a;
//
//        @FormParam("b") String b;
//
//        @Context
//        UriInfo ui;
//
//        @QueryParam("a") String qa;
//    }
//
//    @Path("/")
//    public static class FormResourceBean {
//        @POST
//        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//        public String post(
//                @InjectParam ParamBean pb,
//                @FormParam("a") String a,
//                @FormParam("b") String b,
//                Form form) {
//            assertEquals(pb.a, form.getFirst("a"));
//            assertEquals(pb.b, form.getFirst("b"));
//            return pb.a + pb.b;
//        }
//    }
//
//    public void testFormParamBean() {
//        initiateWebApplication(FormResourceBean.class);
//
//        WebResource r = resource("/");
//
//        Form form = new Form();
//        form.add("a", "foo");
//        form.add("b", "bar");
//
//        String s = r.post(String.class, form);
//        assertEquals("foobar", s);
//    }
//
//    @Path("/")
//    public static class FormResourceBeanNoFormParam {
//        @POST
//        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//        public String post(@InjectParam ParamBean pb) {
//            return pb.a + pb.b;
//        }
//    }
//
//    public void testFormParamBeanNoFormParam() {
//        initiateWebApplication(FormResourceBeanNoFormParam.class);
//
//        WebResource r = resource("/");
//
//        Form form = new Form();
//        form.add("a", "foo");
//        form.add("b", "bar");
//
//        String s = r.post(String.class, form);
//        assertEquals("foobar", s);
//    }
//
//    @Path("/")
//    public static class FormResourceBeanConstructor {
//        private final ParamBean pb;
//
//        public FormResourceBeanConstructor(@InjectParam ParamBean pb) {
//            this.pb = pb;
//        }
//
//        @GET
//        public String get() {
//            return "GET";
//        }
//
//        @POST
//        @Consumes(MediaType.TEXT_PLAIN)
//        public String postText(String s) {
//            return s;
//        }
//
//        @POST
//        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//        public String post(String s) {
//            assertTrue(s.contains("a=foo"));
//            assertTrue(s.contains("b=bar"));
//
//            return pb.a + pb.b;
//        }
//    }
//
//    public void testFormParamBeanConstructor() {
//        initiateWebApplication(FormResourceBeanConstructor.class);
//
//        WebResource r = resource("/");
//
//        Form form = new Form();
//        form.add("a", "foo");
//        form.add("b", "bar");
//
//        String s = r.post(String.class, form);
//        assertEquals("foobar", s);
//    }
//
//    public void testFormParamBeanConstructorIllegalState() {
//        initiateWebApplication(FormResourceBeanConstructor.class);
//
//        WebResource r = resource("/");
//
//        boolean caught = false;
//        try {
//            ClientResponse cr = r.get(ClientResponse.class);
//        } catch (ContainerException ex) {
//            assertEquals(IllegalStateException.class, ex.getCause().getCause().getClass());
//            caught = true;
//        }
//        assertTrue(caught);
//
//
//        caught = false;
//        try {
//            ClientResponse cr = r.post(ClientResponse.class, "text");
//        } catch (ContainerException ex) {
//            assertEquals(IllegalStateException.class, ex.getCause().getCause().getClass());
//            caught = true;
//        }
//        assertTrue(caught);
//    }
//
//
//    @Path("/")
//    public static class FormResourceBeanConstructorFormParam {
//        private final ParamBean pb;
//
//        public FormResourceBeanConstructorFormParam(@InjectParam ParamBean pb) {
//            this.pb = pb;
//        }
//
//        @POST
//        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//        public String post(
//                @FormParam("a") String a,
//                @FormParam("b") String b,
//                Form form) {
//            assertEquals(a, form.getFirst("a"));
//            assertEquals(b, form.getFirst("b"));
//            return a + b;
//        }
//    }
//
//    public void testFormParamBeanConstructorFormParam() {
//        initiateWebApplication(FormResourceBeanConstructorFormParam.class);
//
//        WebResource r = resource("/");
//
//        Form form = new Form();
//        form.add("a", "foo");
//        form.add("b", "bar");
//
//        String s = r.post(String.class, form);
//        assertEquals("foobar", s);
//    }
}
