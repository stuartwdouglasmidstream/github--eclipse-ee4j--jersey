/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.inject;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.servlet.WebConfig;

import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Providers;

public class ParentInject implements ParentChecker {
    @Context
    protected Application contextApplication;

    @Inject
    protected Application injectApplication;

    @Context
    protected Configuration contextConfiguration;

    @Inject
    protected Configuration injectConfiguration;

    @Context
    protected HttpHeaders contextHttpHeaders;

    @Inject
    protected HttpHeaders injectHttpHeaders;

//    @Context
//    protected ParamConverterProvider contextParamConverterProvider;
//
//    @Inject
//    protected ParamConverterProvider injectParamConverterProvider;

    @Context
    protected PropertiesDelegate contextPropertiesDelegate;

    @Inject
    protected PropertiesDelegate injectPropertiesDelegate;

    @Context
    protected Providers contextProviders;

    @Inject
    protected Providers injectProviders;

    @Context
    protected ResourceContext contextResourceContext;

    @Inject
    protected ResourceContext injectResourceContext;

    @Context
    protected Request contextRequest;

    @Inject
    protected Request injectRequest;

    @Context
    protected ResourceInfo contextResourceInfo;

    @Inject
    protected ResourceInfo injectResourceInfo;

    @Context
    protected SecurityContext contextSecurityContext;

    @Inject
    protected SecurityContext injectSecurityContext;

    @Context
    protected UriInfo contextUriInfo;

    @Inject
    protected UriInfo injectUriInfo;

    @Context
    protected HttpServletRequest contextHttpServletRequest;

    @Context
    protected WebConfig contextWebConfig;

    @Inject
    protected WebConfig injectWebConfig;

    @Context
    protected HttpServletResponse contextHttpServletResponse;

    @Context
    protected ServletConfig contextServletConfig;

    @Context
    protected ServletContext contextServletContext;

    @Override
    public boolean checkInjected(StringBuilder stringBuilder) {
        boolean injected = true;
        injected &= checkApplication(injectApplication, stringBuilder);
        injected &= checkConfiguration(injectConfiguration, stringBuilder);
        injected &= InjectionChecker.checkHttpHeaders(injectHttpHeaders, stringBuilder);
        injected &= checkPropertiesDelegate(injectPropertiesDelegate, stringBuilder);
//        injected &= InjectionChecker.checkParamConverterProvider(injectParamConverterProvider, stringBuilder);
        injected &= InjectionChecker.checkProviders(injectProviders, stringBuilder);
        injected &= InjectionChecker.checkRequest(injectRequest, stringBuilder);
        injected &= InjectionChecker.checkResourceContext(injectResourceContext, stringBuilder);
        injected &= InjectionChecker.checkResourceInfo(injectResourceInfo, stringBuilder);
        injected &= InjectionChecker.checkSecurityContext(injectSecurityContext, stringBuilder);
        injected &= InjectionChecker.checkUriInfo(injectUriInfo, stringBuilder);

        injected &= InjectionChecker.checkWebConfig(injectWebConfig, stringBuilder);

        return injected;
    }

    @Override
    public boolean checkContexted(StringBuilder stringBuilder) {
        boolean injected = true;
        injected &= checkApplication(contextApplication, stringBuilder);
        injected &= checkConfiguration(contextConfiguration, stringBuilder);
        injected &= InjectionChecker.checkHttpHeaders(contextHttpHeaders, stringBuilder);
//        injected &= InjectionChecker.checkParamConverterProvider(contextParamConverterProvider, stringBuilder);
        injected &= checkPropertiesDelegate(contextPropertiesDelegate, stringBuilder);
        injected &= InjectionChecker.checkProviders(contextProviders, stringBuilder);
        injected &= InjectionChecker.checkRequest(contextRequest, stringBuilder);
        injected &= InjectionChecker.checkResourceContext(contextResourceContext, stringBuilder);
        injected &= InjectionChecker.checkResourceInfo(contextResourceInfo, stringBuilder);
        injected &= InjectionChecker.checkSecurityContext(contextSecurityContext, stringBuilder);
        injected &= InjectionChecker.checkUriInfo(contextUriInfo, stringBuilder);

        injected &= InjectionChecker.checkHttpServletRequest(contextHttpServletRequest, stringBuilder);
        injected &= InjectionChecker.checkHttpServletResponse(contextHttpServletResponse, stringBuilder);
        injected &= InjectionChecker.checkWebConfig(contextWebConfig, stringBuilder);
        injected &= InjectionChecker.checkServletConfig(contextServletConfig, stringBuilder);
        injected &= InjectionChecker.checkServletContext(contextServletContext, stringBuilder);

        return injected;
    }

    protected boolean checkApplication(Application application, StringBuilder stringBuilder) {
        return InjectionChecker.checkApplication(application, stringBuilder);
    }

    protected boolean checkConfiguration(Configuration configuration, StringBuilder stringBuilder) {
        return InjectionChecker.checkConfiguration(configuration, stringBuilder);
    }

    protected boolean checkPropertiesDelegate(PropertiesDelegate propertiesDelegate, StringBuilder stringBuilder) {
        return InjectionChecker.checkPropertiesDelegate(propertiesDelegate, stringBuilder);
    }
}
