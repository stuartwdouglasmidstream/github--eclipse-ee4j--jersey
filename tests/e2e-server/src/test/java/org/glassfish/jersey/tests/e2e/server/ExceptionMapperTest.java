/*
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NameBinding;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import org.glassfish.jersey.message.internal.MessageBodyProviderNotFoundException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests throwing exceptions in {@link MessageBodyReader} and {@link MessageBodyWriter}.
 *
 * @author Miroslav Fuksa
 */
public class ExceptionMapperTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(
                Resource.class,
                MyMessageBodyWritter.class,
                MyMessageBodyReader.class,
                ClientErrorExceptionMapper.class,
                MyExceptionMapper.class,
                ThrowableMapper.class,
                MyExceptionMapperCauseAnotherException.class,
                // JERSEY-1515
                TestResource.class,
                VisibilityExceptionMapper.class,
                // JERSEY-1525
                ExceptionTestResource.class,
                ExceptionThrowingFilter.class,
                IOExceptionMapper.class,
                IOExceptionMessageReader.class,
                IOExceptionResource.class,
                MessageBodyProviderNotFoundResource.class,
                ProviderNotFoundExceptionMapper.class,
                // JERSEY-1887
                Jersey1887Resource.class,
                Jersey1887ExceptionMapperImpl.class,
                // JERSEY-2382
                Jersey2382Resource.class,
                Jersey2382ExceptionMapper.class,
                Jersey2382Provider.class
        );
    }

    @Test
    public void testReaderThrowsException() {
        Response res = target().path("test").request("test/test").header("reader-exception", "throw")
                .post(Entity.entity("post", "test/test"));
        assertEquals(200, res.getStatus());
        final String entity = res.readEntity(String.class);
        assertEquals("reader-exception-mapper", entity);
    }

    @Test
    public void testWriterThrowsExceptionBeforeFirstBytesAreWritten() {
        Response res = target().path("test/before").request("test/test").get();
        assertEquals(200, res.getStatus());
        assertEquals("exception-before-first-bytes-exception-mapper", res.readEntity(String.class));
    }

    @Test
    public void testWriterThrowsExceptionAfterFirstBytesAreWritten() throws IOException {
        Response res = target().path("test/after").request("test/test").get();
        assertEquals(200, res.getStatus());
        final InputStream inputStream = res.readEntity(InputStream.class);
        byte b;
        inputStream.read();
        MyMessageBodyWritter.firstBytesReceived = true;
        while ((b = (byte) inputStream.read()) >= 0) {
            assertEquals('a', b);
        }
    }

    @Test
    public void testPreventMultipleExceptionMapping() {
        Response res = target().path("test/exception").request("test/test").get();
        // firstly exception is thrown in the resource method and is correctly mapped. Then it is again thrown in MBWriter but
        // exception can be mapped only once, so second exception in MBWriter cause 500 response code.
        assertEquals(500, res.getStatus());
    }

    @Test
    public void testDefaultExceptionMapper() {
        Response res = target().path("test/defaultExceptionMapper")
                .request("test/test").get();
        assertEquals(200, res.getStatus());
    }

    @Path("test")
    public static class Resource {

        @GET
        @Path("before")
        @Produces("test/test")
        public Response exceptionBeforeFirstBytesAreWritten() {
            return Response.status(200).header("writer-exception", "before-first-byte").entity("ok").build();
        }

        @GET
        @Path("after")
        @Produces("test/test")
        public Response exceptionAfterFirstBytesAreWritten() {
            return Response.status(200).header("writer-exception", "after-first-byte").entity("aaaaa").build();
        }

        @POST
        @Produces("test/test")
        public String post(String str) {
            return "post";
        }

        @GET
        @Path("exception")
        @Produces("test/test")
        public Response throwsException() {
            throw new MyAnotherException("resource");
        }

        @Path("throwable")
        @GET
        public String throwsThrowable() throws Throwable {
            throw new Throwable("throwable",
                    new RuntimeException("runtime-exception",
                            new ClientErrorException("client-error", 499)));
        }

        @GET
        @Path("defaultExceptionMapper")
        public Response isRegisteredDefaultExceptionTest(@Context Providers providers) {
            ExceptionMapper<Throwable> em = providers
                    .getExceptionMapper(Throwable.class);
            return Response.status((em == null) ? 500 : 200).build();
        }
    }

    public static class ClientErrorExceptionMapper implements ExceptionMapper<ClientErrorException> {

        @Override
        public Response toResponse(ClientErrorException exception) {
            return Response.status(Response.Status.OK).entity("mapped-client-error-"
                    + exception.getResponse().getStatus() + "-" + exception.getMessage()).build();
        }
    }

    public static class MyExceptionMapper implements ExceptionMapper<MyException> {

        @Override
        public Response toResponse(MyException exception) {
            return Response.ok().entity(exception.getMessage() + "-exception-mapper").build();
        }
    }

    public static class ThrowableMapper implements ExceptionMapper<Throwable> {

        @Override
        public Response toResponse(Throwable throwable) {
            throwable.printStackTrace();
            return Response.status(Response.Status.OK).entity("mapped-throwable-" + throwable.getMessage()).build();
        }

    }

    public static class MyExceptionMapperCauseAnotherException implements ExceptionMapper<MyAnotherException> {

        @Override
        public Response toResponse(MyAnotherException exception) {
            // the header causes exception to be thrown again in MyMessageBodyWriter
            return Response.ok().header("writer-exception", "before-first-byte").entity(exception.getMessage()
                    + "-another-exception-mapper").build();
        }
    }

    @Produces("test/test")
    public static class MyMessageBodyWritter implements MessageBodyWriter<String> {

        public static volatile boolean firstBytesReceived;

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public long getSize(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return s.length();
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
                WebApplicationException {
            firstBytesReceived = false;

            final List<Object> header = httpHeaders.get("writer-exception");

            if (header != null && header.size() > 0) {
                if (header.get(0).equals("before-first-byte")) {
                    throw new MyException("exception-before-first-bytes");
                } else if (header.get(0).equals("after-first-byte")) {
                    int i = 0;
                    while (!firstBytesReceived && i++ < 500000) {
                        entityStream.write('a');
                        entityStream.flush();
                    }
                    throw new MyException("exception-after-first-bytes");
                }
            } else {
                OutputStreamWriter osw = new OutputStreamWriter(entityStream);
                osw.write(s);
                osw.flush();
            }
        }
    }

    @Consumes("test/test")
    public static class MyMessageBodyReader implements MessageBodyReader<String> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
                WebApplicationException {
            final List<String> header = httpHeaders.get("reader-exception");
            if (header != null && header.size() > 0 && header.get(0).equals("throw")) {
                throw new MyException("reader");
            }
            return "aaa";
        }
    }

    public static class MyException extends RuntimeException {

        public MyException() {
        }

        public MyException(Throwable cause) {
            super(cause);
        }

        public MyException(String message) {
            super(message);
        }

        public MyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MyAnotherException extends RuntimeException {

        public MyAnotherException() {
        }

        public MyAnotherException(Throwable cause) {
            super(cause);
        }

        public MyAnotherException(String message) {
            super(message);
        }

        public MyAnotherException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * BEGIN: JERSEY-1515 reproducer code
     */
    public static class VisibilityException extends WebApplicationException {

        private static final long serialVersionUID = -1159407312691372429L;

    }

    public static class VisibilityExceptionMapper implements ExceptionMapper<VisibilityException> {

        private HttpHeaders headers;
        private UriInfo info;
        private Application application;
        private Request request;
        private Providers provider;

        protected VisibilityExceptionMapper(@Context HttpHeaders headers,
                                            @Context UriInfo info, @Context Application application,
                                            @Context Request request, @Context Providers provider) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
            this.provider = provider;
        }

        public VisibilityExceptionMapper(@Context HttpHeaders headers,
                                         @Context UriInfo info, @Context Application application,
                                         @Context Request request) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
        }

        public VisibilityExceptionMapper(@Context HttpHeaders headers,
                                         @Context UriInfo info, @Context Application application) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
        }

        public VisibilityExceptionMapper(@Context HttpHeaders headers,
                                         @Context UriInfo info) {
            super();
            this.headers = headers;
            this.info = info;
        }

        public VisibilityExceptionMapper(@Context HttpHeaders headers) {
            super();
            this.headers = headers;
        }

        @Override
        public Response toResponse(VisibilityException exception) {
            return Response.ok("visible").build();
        }
    }

    @Path("test/visible")
    public static class TestResource {

        @GET
        public String throwVisibleException() {
            throw new VisibilityException();
        }
    }

    @Test
    public void testJersey1515() {
        Response res = target().path("test/visible").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("visible", res.readEntity(String.class));
    }
    /**
     * END: JERSEY-1515 reproducer code
     */

    /**
     * BEGIN: JERSEY-1525 reproducer code.
     */
    @Path("test/responseFilter")
    public static class ExceptionTestResource {

        @GET
        @ThrowsNPE
        public String getData() {
            return "method";
        }
    }

    @NameBinding
    @Retention(RetentionPolicy.RUNTIME)
    private static @interface ThrowsNPE {
    }

    @ThrowsNPE
    public static class ExceptionThrowingFilter implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            // The if clause prevents throwing exception on a mapped response.
            // Not doing so would result in a second exception being thrown
            // which would not be mapped again; instead, it would be propagated
            // to the hosting container directly.
            if (!"mapped-throwable-response-filter-exception".equals(responseContext.getEntity())) {
                throw new NullPointerException("response-filter-exception");
            }
        }
    }

    @Test
    public void testJersey1525() {
        Response res = target().path("test/responseFilter").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("mapped-throwable-response-filter-exception", res.readEntity(String.class));
    }

    /**
     * END: JERSEY-1525 reproducer code
     */

    @Provider
    public static class IOExceptionMessageReader implements MessageBodyReader<IOBean>, MessageBodyWriter<IOBean> {

        @Override
        public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == IOBean.class;
        }

        @Override
        public IOBean readFrom(Class<IOBean> type,
                               Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            throw new IOException("io-exception");
        }

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == IOBean.class;
        }

        @Override
        public long getSize(IOBean ioBean, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return 0;
        }

        @Override
        public void writeTo(IOBean ioBean, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
                WebApplicationException {
            entityStream.write(ioBean.value.getBytes());

        }
    }

    public static class IOBean {

        private final String value;

        public IOBean(String value) {
            this.value = value;
        }
    }

    public static class IOExceptionMapper implements ExceptionMapper<IOException> {

        @Override
        public Response toResponse(IOException exception) {
            return Response.ok("passed").build();
        }
    }

    @Path("io")
    public static class IOExceptionResource {

        @POST
        public String post(IOBean iobean) {
            return iobean.value;
        }
    }

    @Test
    public void testIOException() {
        final Response response = target().register(IOExceptionMessageReader.class)
                .path("io").request().post(Entity.entity(new IOBean("io-bean"), MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals("passed", response.readEntity(String.class));
    }

    @Test
    public void testThrowableFromResourceMethod() {
        Response res = target().path("test/throwable").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("mapped-throwable-throwable", res.readEntity(String.class));
    }

    @Path("not-found")
    public static class MessageBodyProviderNotFoundResource {

        @GET
        @Produces("aa/bbb")
        public UnknownType get() {
            return new UnknownType();
        }
    }

    public static class UnknownType {

    }

    public static class ProviderNotFoundExceptionMapper implements ExceptionMapper<InternalServerErrorException> {

        @Override
        public Response toResponse(InternalServerErrorException exception) {
            if (exception.getCause() instanceof MessageBodyProviderNotFoundException) {
                return Response.ok("mapped-by-ProviderNotFoundExceptionMapper").build();
            }
            return Response.serverError().entity("Unexpected root cause of InternalServerError").build();
        }
    }

    /**
     * Tests that {@link MessageBodyProviderNotFoundException} wrapped into {@link jakarta.ws.rs.InternalServerErrorException}
     * is correctly mapped using an {@link ExceptionMapper}.
     */
    @Test
    public void testNotFoundResource() {
        final Response response = target().path("not-found").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("mapped-by-ProviderNotFoundExceptionMapper", response.readEntity(String.class));
    }

    public static class Jersey1887Exception extends RuntimeException {
    }

    @Provider
    public static interface Jersey1887ExceptionMapper extends ExceptionMapper<Jersey1887Exception> {
    }

    public static class Jersey1887ExceptionMapperImpl implements Jersey1887ExceptionMapper {

        @Override
        public Response toResponse(final Jersey1887Exception exception) {
            return Response.ok("found").build();
        }
    }

    @Path("jersey1887")
    public static class Jersey1887Resource {

        @GET
        public Response get() {
            throw new Jersey1887Exception();
        }
    }

    /**
     * Test that we're able to use correct exception mapper even when the mapper hierarchy has complex inheritance.
     */
    @Test
    public void testJersey1887() throws Exception {
        final Response response = target().path("jersey1887").request().get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("found"));
    }

    public static class Jersey2382Exception extends RuntimeException {
    }

    public static class Jersey2382Entity {
    }

    @Provider
    public static class Jersey2382Provider implements MessageBodyWriter<Jersey2382Entity> {

        @Override
        public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                   final MediaType mediaType) {
            return true;
        }

        @Override
        public long getSize(final Jersey2382Entity jersey2382Entity, final Class<?> type, final Type genericType,
                            final Annotation[] annotations, final MediaType mediaType) {
            return -1;
        }

        @Override
        public void writeTo(final Jersey2382Entity jersey2382Entity,
                            final Class<?> type,
                            final Type genericType,
                            final Annotation[] annotations,
                            final MediaType mediaType,
                            final MultivaluedMap<String, Object> httpHeaders,
                            final OutputStream entityStream) throws IOException, WebApplicationException {
            if (Jersey2382Entity.class != type) {
                entityStream.write("wrong-type".getBytes());
            } else if (Jersey2382Entity.class != genericType) {
                entityStream.write("wrong-generic-type".getBytes());
            } else {
                entityStream.write("ok".getBytes());
            }
        }
    }

    @Provider
    public static class Jersey2382ExceptionMapper implements ExceptionMapper<Jersey2382Exception> {

        @Override
        public Response toResponse(final Jersey2382Exception exception) {
            return Response.ok(new Jersey2382Entity()).build();
        }
    }

    @Path("jersey2382")
    public static class Jersey2382Resource {

        @GET
        public List<List<Integer>> get() {
            throw new Jersey2382Exception();
        }
    }

    /**
     * Test that we're able to use correct exception mapper even when the mapper hierarchy has complex inheritance.
     */
    @Test
    public void testJersey2382() throws Exception {
        final Response response = target().path("jersey2382").request().get();

        assertThat(response.getStatus(), equalTo(200));
        assertThat(response.readEntity(String.class), equalTo("ok"));
    }
}
