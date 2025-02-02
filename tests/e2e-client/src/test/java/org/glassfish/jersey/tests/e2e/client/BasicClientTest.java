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

package org.glassfish.jersey.tests.e2e.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import jakarta.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.client.ClientAsyncExecutor;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.spi.ThreadPoolExecutorProvider;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static jakarta.ws.rs.client.Entity.text;

/**
 * Tests sync and async client invocations.
 *
 * @author Miroslav Fuksa
 * @author Marek Potociar
 */
public class BasicClientTest extends JerseyTest {

    @Override
    protected Application configure() {
        //        enable(TestProperties.LOG_TRAFFIC);
        //        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(Resource.class);
    }

    @Test
    public void testCustomExecutorsAsync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register(CustomExecutorProvider.class).register(ThreadInterceptor.class);
        Client client = ClientBuilder.newClient(jerseyConfig);
        runCustomExecutorTestAsync(client);
    }

    @Test
    public void testCustomExecutorsInstanceAsync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register(new CustomExecutorProvider()).register(ThreadInterceptor.class);
        Client client = ClientBuilder.newClient(jerseyConfig);
        runCustomExecutorTestAsync(client);
    }

    @Test
    public void testCustomExecutorsSync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register(CustomExecutorProvider.class).register(ThreadInterceptor.class);
        Client client = ClientBuilder.newClient(jerseyConfig);
        runCustomExecutorTestSync(client);
    }

    @Test
    public void testCustomExecutorsInstanceSync() throws ExecutionException, InterruptedException {
        ClientConfig jerseyConfig = new ClientConfig();
        jerseyConfig.register(new CustomExecutorProvider()).register(ThreadInterceptor.class);
        Client client = ClientBuilder.newClient(jerseyConfig);
        runCustomExecutorTestSync(client);
    }

    private void runCustomExecutorTestAsync(Client client) throws InterruptedException, ExecutionException {
        AsyncInvoker async = client.target(getBaseUri()).path("resource").request().async();

        Future<Response> f1 = async.post(text("post"));
        final Response response = f1.get();
        final String entity = response.readEntity(String.class);
        assertEquals("custom-async-request-post", entity);
    }

    private void runCustomExecutorTestSync(Client client) {
        WebTarget target = client.target(getBaseUri()).path("resource");
        final Response response = target.request().post(text("post"));

        final String entity = response.readEntity(String.class);
        assertNotSame("custom-async-request-post", entity);
    }

    @Test
    public void testAsyncClientInvocation() throws InterruptedException, ExecutionException {
        final WebTarget resource = target().path("resource");

        Future<Response> f1 = resource.request().async().post(text("post1"));
        final Response response = f1.get();
        assertEquals("post1", response.readEntity(String.class));

        Future<String> f2 = resource.request().async().post(text("post2"), String.class);
        assertEquals("post2", f2.get());

        Future<List<JaxbString>> f3 = resource.request().async().get(new GenericType<List<JaxbString>>() {
        });
        assertEquals(
                Arrays.asList("a", "b", "c").toString(),
                f3.get().stream().map(input -> input.value).collect(Collectors.toList()).toString());

        CompletableFuture<String> future1 = new CompletableFuture<>();
        final TestCallback<Response> c1 = new TestCallback<Response>(future1) {

            @Override
            protected String process(Response result) {
                return result.readEntity(String.class);
            }
        };

        resource.request().async().post(text("post"), c1);
        assertEquals("post", future1.get());

        CompletableFuture<String> future2 = new CompletableFuture<>();
        final TestCallback<String> c2 = new TestCallback<String>(future2) {

            @Override
            protected String process(String result) {
                return result;
            }
        };
        resource.request().async().post(text("post"), c2);
        assertEquals("post", future2.get());

        CompletableFuture<String> future3 = new CompletableFuture<>();
        final TestCallback<List<JaxbString>> c3 = new TestCallback<List<JaxbString>>(future3) {

            @Override
            protected String process(List<JaxbString> result) {
                return result.stream().map(jaxbString -> jaxbString.value).collect(Collectors.toList()).toString();
            }
        };
        resource.request().async().get(c3);
        assertEquals(Arrays.asList("a", "b", "c").toString(), future3.get());
    }

    @Test
    public void testAsyncClientInvocationErrorResponse() throws InterruptedException, ExecutionException {
        final WebTarget errorResource = target().path("resource").path("error");

        Future<Response> f1 = errorResource.request().async().get();
        final Response response = f1.get();
        assertEquals(404, response.getStatus());
        assertEquals("error", response.readEntity(String.class));

        Future<String> f2 = errorResource.request().async().get(String.class);
        try {
            f2.get();
            fail("ExecutionException expected.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(WebApplicationException.class.isAssignableFrom(cause.getClass()));
            final Response r = ((WebApplicationException) cause).getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }

        Future<List<String>> f3 = target().path("resource").path("errorlist").request()
                                          .async().get(new GenericType<List<String>>() {
                });
        try {
            f3.get();
            fail("ExecutionException expected.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(WebApplicationException.class.isAssignableFrom(cause.getClass()));
            final Response r = ((WebApplicationException) cause).getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }

        CompletableFuture<String> future1 = new CompletableFuture<>();
        final TestCallback<Response> c1 = new TestCallback<Response>(future1) {

            @Override
            protected String process(Response result) {
                return result.readEntity(String.class);
            }
        };
        errorResource.request().async().get(c1);
        assertEquals("error", future1.get());

        CompletableFuture<String> future2 = new CompletableFuture<>();
        final TestCallback<String> c2 = new TestCallback<String>(future2) {

            @Override
            protected String process(String result) {
                return result;
            }
        };
        errorResource.request().async().get(c2);
        try {
            future2.get();
            fail("ExecutionException expected.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(WebApplicationException.class.isAssignableFrom(cause.getClass()));
            final Response r = ((WebApplicationException) cause).getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }

        CompletableFuture<String> future3 = new CompletableFuture<>();
        final TestCallback<List<JaxbString>> c3 = new TestCallback<List<JaxbString>>(future3) {

            @Override
            protected String process(List<JaxbString> result) {
                return result.stream().map(jaxbString -> jaxbString.value).collect(Collectors.toList()).toString();
            }
        };
        target().path("resource").path("errorlist").request().async().get(c3);
        try {
            future3.get();
            fail("ExecutionException expected.");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            assertTrue(WebApplicationException.class.isAssignableFrom(cause.getClass()));
            final Response r = ((WebApplicationException) cause).getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }
    }

    @Test
    public void testSyncClientInvocation() throws InterruptedException, ExecutionException {
        final WebTarget resource = target().path("resource");

        Response r1 = resource.request().post(text("post1"));
        assertEquals("post1", r1.readEntity(String.class));

        String r2 = resource.request().post(text("post2"), String.class);
        assertEquals("post2", r2);

        List<JaxbString> r3 = resource.request().get(new GenericType<List<JaxbString>>() {
        });
        assertEquals(Arrays.asList("a", "b", "c").toString(),
                     r3.stream().map(input -> input.value).collect(Collectors.toList()).toString());
    }

    @Test
    public void testSyncClientInvocationErrorResponse() throws InterruptedException, ExecutionException {
        final WebTarget errorResource = target().path("resource").path("error");

        Response r1 = errorResource.request().get();
        assertEquals(404, r1.getStatus());
        assertEquals("error", r1.readEntity(String.class));

        try {
            errorResource.request().get(String.class);
            fail("ExecutionException expected.");
        } catch (WebApplicationException ex) {
            final Response r = ex.getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }

        try {
            target().path("resource").path("errorlist").request().get(new GenericType<List<String>>() {
            });
            fail("ExecutionException expected.");
        } catch (WebApplicationException ex) {
            final Response r = ex.getResponse();
            assertEquals(404, r.getStatus());
            assertEquals("error", r.readEntity(String.class));
        }
    }

    @Test
    // JERSEY-1412
    public void testAbortAsyncRequest() throws Exception {
        Invocation invocation = abortingTarget().request().buildPost(text("entity"));
        Future<String> future = invocation.submit(String.class);
        assertEquals("aborted", future.get());
    }

    @Test
    // JERSEY-1412
    public void testAbortSyncRequest() throws Exception {
        Invocation invocation = abortingTarget().request().buildPost(text("entity"));
        String response = invocation.invoke(String.class);
        assertEquals("aborted", response);
    }

    protected WebTarget abortingTarget() {
        Client client = ClientBuilder.newClient();
        client.register(new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext ctx) throws IOException {
                Response r = Response.ok("aborted").build();
                ctx.abortWith(r);
            }
        });
        return client.target("http://any.web:888");
    }

    @ClientAsyncExecutor
    public static class CustomExecutorProvider extends ThreadPoolExecutorProvider {

        /**
         * Create a new instance of the thread pool executor provider.
         */
        public CustomExecutorProvider() {
            super("custom-async-request");
        }
    }

    public static class ThreadInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            final String name = Thread.currentThread().getName(); // e.g. custom-async-request-0
            final int lastIndexOfDash = name.lastIndexOf('-');
            context.setEntity(
                    name.substring(0, lastIndexOfDash < 0 ? name.length() : lastIndexOfDash) + "-" + context.getEntity());
            context.proceed();
        }
    }

    @Path("resource")
    public static class Resource {

        @GET
        @Path("error")
        public String getError() {
            throw new NotFoundException(Response.status(404).type("text/plain").entity("error").build());
        }

        @GET
        @Path("errorlist")
        @Produces(MediaType.APPLICATION_XML)
        public List<JaxbString> getErrorList() {
            throw new NotFoundException(Response.status(404).type("text/plain").entity("error").build());
        }

        @GET
        @Produces(MediaType.APPLICATION_XML)
        public List<JaxbString> get() {
            return Arrays.asList(new JaxbString("a"), new JaxbString("b"), new JaxbString("c"));
        }

        @POST
        public String post(String entity) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return entity;
        }
    }

    private abstract static class TestCallback<T> implements InvocationCallback<T> {

        private final CompletableFuture<String> completableFuture;

        TestCallback(CompletableFuture<String> completableFuture) {
            this.completableFuture = completableFuture;
        }

        @Override
        public void completed(T result) {
            try {
                completableFuture.complete(process(result));
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        }

        protected abstract String process(T result);

        @Override
        public void failed(Throwable error) {
            if (error.getCause() instanceof WebApplicationException) {
                completableFuture.completeExceptionally(error.getCause());
            } else {
                completableFuture.completeExceptionally((error));
            }
        }
    }

    @XmlRootElement
    public static class JaxbString {

        public String value;

        public JaxbString() {
        }

        public JaxbString(String value) {
            this.value = value;
        }
    }
}


