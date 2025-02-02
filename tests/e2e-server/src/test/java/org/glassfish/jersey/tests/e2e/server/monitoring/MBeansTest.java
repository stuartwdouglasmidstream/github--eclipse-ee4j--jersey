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

package org.glassfish.jersey.tests.e2e.server.monitoring;

import java.lang.management.ManagementFactory;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Miroslav Fuksa
 */
public class MBeansTest extends JerseyTest {

    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(TestResource.class, MyExceptionMapper.class);
        resourceConfig.setApplicationName("myApplication");
        resourceConfig.property("very-important", "yes");
        resourceConfig.property("another-property", 48);
        resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
        resourceConfig.register(StatisticsListener.class);
        return resourceConfig;
    }

    public static class MyException extends RuntimeException {

        public MyException(String message) {
            super(message);
        }
    }

    public static class MyExceptionMapper implements ExceptionMapper<MyException> {

        @Override
        public Response toResponse(MyException exception) {
            return Response.ok("mapped").build();
        }
    }

    @Path("resource")
    public static class TestResource {

        @GET
        public String testGet() {
            return "get";
        }

        @GET
        @Path("test/{test: \\d+}")
        public String testGetPathPattern1() {
            return "testGetPathPattern1";
        }

        @GET
        @Path("test2/{test: hell?o}")
        public String testGetPathPattern2() {
            return "testGetPathPattern2";
        }

        @GET
        @Path("test3/{test: abc.* (a)(b)[a,c]?$[1-4]kkx|Y}")
        public String testGetPathPattern3() {
            return "testGetPathPattern2";
        }

        @GET
        @Path("test4/{test: [a,b]:r}")
        public String testGetPathPattern4() {
            return "testGetPathPattern2";
        }

        @POST
        public String testPost() {
            return "post";
        }

        @GET
        @Path("sub")
        public String testSubGet() {
            return "sub";
        }

        @GET
        @Path("exception")
        public String testException() {
            throw new MyException("test");
        }

        @POST
        @Path("sub2")
        @Produces("text/html")
        @Consumes("text/plain")
        public String testSu2bPost(String entity) {
            return "post";
        }

        @Path("locator")
        public SubResource getSubResource() {
            return new SubResource();
        }
    }

    public static class StatisticsListener extends AbstractContainerLifecycleListener implements MonitoringStatisticsListener {

        public static boolean ON_SHUTDOWN_CALLED = false;

        @Override
        public void onStatistics(MonitoringStatistics statistics) {
            // do nothing
        }

        @Override
        public void onShutdown(Container container) {
            StatisticsListener.ON_SHUTDOWN_CALLED = true;
        }
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        Assertions.assertTrue(StatisticsListener.ON_SHUTDOWN_CALLED);

    }

    public static class SubResource {

        @GET
        @Path("in-subresource")
        public String get() {
            return "inSubResource";
        }

        @Path("locator")
        public SubResource getSubResource() {
            return new SubResource();
        }
    }

    @Test
    public void test() throws Exception {
        final String path = "resource";
        assertEquals(200, target().path(path).request().get().getStatus());
        assertEquals(200, target().path(path).request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        assertEquals(200, target().path(path).request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        assertEquals(200, target().path(path).request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        assertEquals(200, target().path(path).request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        assertEquals(200, target().path(path + "/sub2").request().post(Entity.entity("post",
                MediaType.TEXT_PLAIN_TYPE)).getStatus());
        final Response response = target().path(path + "/exception").request().get();
        assertEquals(200, response.getStatus());
        assertEquals("mapped", response.readEntity(String.class));

        assertEquals(200, target().path("resource/sub").request().get().getStatus());
        assertEquals(200, target().path("resource/sub").request().get().getStatus());
        assertEquals(200, target().path("resource/locator/in-subresource").request().get().getStatus());
        assertEquals(200, target().path("resource/locator/locator/in-subresource").request().get().getStatus());
        assertEquals(200, target().path("resource/locator/locator/locator/in-subresource").request().get().getStatus());
        assertEquals(404, target().path("resource/not-found-404").request().get().getStatus());

        // wait until statistics are propagated to mxbeans
        Thread.sleep(1500);

        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name = new ObjectName("org.glassfish.jersey:type=myApplication,subType=Global,global=Configuration");
        final String str = (String) mBeanServer.getAttribute(name, "ApplicationName");
        Assertions.assertEquals("myApplication", str);

        checkResourceMBean("/resource");
        checkResourceMBean("/resource/sub");
        checkResourceMBean("/resource/locator");
        checkResourceMBean("/resource/exception");
        checkResourceMBean("/resource/test/{test: \\\\d+}");
        checkResourceMBean("/resource/test2/{test: hell\\?o}");
        checkResourceMBean("/resource/test3/{test: abc.\\* (a)(b)[a,c]\\?$[1-4]kkx|Y}");
        checkResourceMBean("/resource/test4/{test: [a,b]:r}");
    }

    private void checkResourceMBean(String name) throws MalformedObjectNameException {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = new ObjectName(
                "org.glassfish.jersey:type=myApplication,subType=Uris,resource=\"" + name + "\"");
        ObjectInstance mbean = null;
        try {
            mbean = mBeanServer.getObjectInstance(objectName);
        } catch (InstanceNotFoundException e) {
            Assertions.fail("Resource MBean name '" + name + "' not found.");
        }
        assertNotNull(mbean);
    }

    // this test runs the jersey environments, exposes mbeans and makes requests to
    // the deployed application. The test will never finished. This should be uncommented
    // only for development testing of mbeans in jconsole.
    // Steps: uncomment the test; run it; run jconsole and attach to the process of the tests
    //    @Test
    //    public void testNeverFinishesAndMustBeCommented() throws Exception {
    //        while (true) {
    //            test();
    //        }
    //    }
}
