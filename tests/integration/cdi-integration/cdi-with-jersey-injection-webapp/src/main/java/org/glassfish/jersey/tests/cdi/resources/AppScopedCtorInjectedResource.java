/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.spi.ExceptionMappers;

/**
 * CDI backed, application scoped, JAX-RS resource to be injected
 * via it's constructor from both CDI and Jersey HK2.
 *
 * @author Jakub Podlesak
 */
@ApplicationScoped
@Path("app-ctor-injected")
public class AppScopedCtorInjectedResource {

    // CDI injected
    EchoService echoService;

    // Jersey injected
    Provider<ContainerRequest> request;
    ExceptionMappers mappers;
    Provider<MonitoringStatistics> stats;

    // Jersey/HK2 custom injected
    MyApplication.MyInjection customInjected;

    // to make weld happy
    public AppScopedCtorInjectedResource() {
    }

    @Inject
    public AppScopedCtorInjectedResource(@AppSpecific final EchoService echoService,
                                         final Provider<ContainerRequest> request,
                                         final ExceptionMappers mappers,
                                         final Provider<MonitoringStatistics> stats,
                                         final MyApplication.MyInjection customInjected) {
        this.echoService = echoService;
        this.request = request;
        this.mappers = mappers;
        this.stats = stats;
        this.customInjected = customInjected;
    }

    @GET
    public String echo(@QueryParam("s") final String s) {
        return echoService.echo(s);
    }

    @GET
    @Path("path/{param}")
    public String getPath() {
        return request.get().getPath(true);
    }

    @GET
    @Path("mappers")
    public String getMappers() {
        return mappers.toString();
    }

    @GET
    @Path("requestCount")
    public String getStatisticsProperty() {
        return String.valueOf(stats.get().snapshot().getRequestStatistics().getTimeWindowStatistics().get(0L).getRequestCount());
    }

    @GET
    @Path("custom")
    public String getCustom() {
        return customInjected.getName();
    }
}
