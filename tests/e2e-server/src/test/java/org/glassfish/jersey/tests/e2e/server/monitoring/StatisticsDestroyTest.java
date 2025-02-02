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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.monitoring.DestroyListener;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory;
import org.glassfish.jersey.test.jetty.JettyTestContainerFactory;
import org.glassfish.jersey.test.simple.SimpleTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 *
 * @author Miroslav Fuksa
 */
@Suite
@SelectClasses({StatisticsDestroyTest.GrizzlyTestCase.class, StatisticsDestroyTest.JdkServerTestCase.class,
        StatisticsDestroyTest.SimpleHttpServerTestCase.class})
public class StatisticsDestroyTest {

    public static class ParentTest extends JerseyTest {
        @Override
        public Application configure() {
            StatisticsListener.reset();
            final ResourceConfig resourceConfig = new ResourceConfig(TestResource.class);
            resourceConfig.setApplicationName("myApplication");
            resourceConfig.property("very-important", "yes");
            resourceConfig.property("another-property", 48);
            resourceConfig.property(ServerProperties.MONITORING_STATISTICS_MBEANS_ENABLED, true);
            resourceConfig.register(StatisticsListener.class);
            return resourceConfig;
        }

        @Override
        @AfterEach
        public void tearDown() throws Exception {
            super.tearDown();
            assertTrue(StatisticsListener.ON_SHUTDOWN_CALLED);
            assertTrue(StatisticsListener.ON_DESTROY_CALLED);
            assertTrue(StatisticsListener.ON_STATISTICS_CALLED);

            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName("org.glassfish.jersey:type=myApplication,subType=Global,global=Configuration");
            boolean registered = mBeanServer.isRegistered(name);
            int time = 0;
            while (registered && time < 4000) {
                // wait until MBeans are asynchronously exposed
                int waitTime = 300;
                time += waitTime;
                Thread.sleep(waitTime);
                registered = mBeanServer.isRegistered(name);
            }

            Assertions.assertFalse(mBeanServer.isRegistered(name), "MBean should be already unregistered!");
        }

        @Path("resource")
        public static class TestResource {
            @GET
            public String testGet() {
                return "get";
            }
        }

        @Test
        public void test() throws Exception {
            final String path = "resource";
            assertEquals(200, target().path(path).request().get().getStatus());
            final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName("org.glassfish.jersey:type=myApplication,subType=Global,global=Configuration");
            boolean registered = mBeanServer.isRegistered(name);

            // wait (events are processed asynchronously and it might take time to expose mbeans
            int time = 0;
            while (!registered && time < 4000) {
                // wait until MBeans are asynchronously exposed
                int waitTime = 300;
                time += waitTime;
                Thread.sleep(waitTime);
                registered = mBeanServer.isRegistered(name);
            }

            assertTrue(mBeanServer.isRegistered(name), "MBean should be already registered!");
            final String str = (String) mBeanServer.getAttribute(name, "ApplicationName");
            Assertions.assertEquals("myApplication", str);
        }
    }

    public static class GrizzlyTestCase extends ParentTest {
        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            StatisticsListener.reset();
            return new GrizzlyTestContainerFactory();
        }
    }

    public static class JdkServerTestCase extends ParentTest {
        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            StatisticsListener.reset();
            return new JdkHttpServerTestContainerFactory();
        }
    }

    /**
     * Works only with Java 7
     */
    public static class JettyServerTestCase extends ParentTest {
        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            StatisticsListener.reset();
            return new JettyTestContainerFactory();
        }
    }

    public static class SimpleHttpServerTestCase extends ParentTest {
        @Override
        protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
            StatisticsListener.reset();
            return new SimpleTestContainerFactory();
        }
    }

    public static class StatisticsListener extends AbstractContainerLifecycleListener
            implements MonitoringStatisticsListener, DestroyListener {

        public static boolean ON_SHUTDOWN_CALLED = false;
        public static boolean ON_STATISTICS_CALLED = false;
        public static boolean ON_DESTROY_CALLED = false;

        public static void reset() {
            ON_SHUTDOWN_CALLED = false;
            ON_STATISTICS_CALLED = false;
            ON_DESTROY_CALLED = false;
        }

        @Override
        public void onStatistics(MonitoringStatistics statistics) {
            StatisticsListener.ON_STATISTICS_CALLED = true;
        }

        @Override
        public void onShutdown(Container container) {
            StatisticsListener.ON_SHUTDOWN_CALLED = true;
        }

        @Override
        public void onDestroy() {
            StatisticsListener.ON_DESTROY_CALLED = true;
        }
    }
}
