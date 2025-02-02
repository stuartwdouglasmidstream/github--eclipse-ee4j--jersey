/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.osgi.test.basic;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.osgi.test.util.Helper;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * Ensures server localization resource bundle gets loaded fine in OSGi runtime.
 *
 * @author Jakub Podlesak
 */
@RunWith(PaxExam.class)
public class ResourceBundleTest {

    private static final String CONTEXT = "/jersey";

    private static final URI baseUri = UriBuilder
            .fromUri("http://localhost")
            .port(Helper.getPort())
            .path(CONTEXT).build();

    @Configuration
    public static Option[] configuration() {
        List<Option> options = Helper.getCommonOsgiOptions();
        options.addAll(Helper.expandedList(
                // PaxRunnerOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
        ));

        return Helper.asArray(options);
    }

    @Path("/non-deployable")
    public static class BadResource {

        @GET
        private String getMe() {
            return "no way";
        }
    }

    @Test
    public void testBadResource() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(BadResource.class);

        ByteArrayOutputStream logOutput = new ByteArrayOutputStream();
        Handler logHandler = new StreamHandler(logOutput, new SimpleFormatter());

        GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);

        // TODO: there should be a better way to get the log output!
        final Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String name = loggerNames.nextElement();
            if (name.startsWith("org.glassfish")) {
                LogManager.getLogManager().getLogger(Errors.class.getName()).addHandler(logHandler);
            }
        }
        GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);
        logOutput.flush();
        final String logOutputAsString = logOutput.toString();

        Assert.assertFalse(logOutputAsString.contains("[failed to localize]"));
        Assert.assertTrue(logOutputAsString.contains("BadResource"));
    }
}
