/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 Markus KARG. All rights reserved.
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

package org.glassfish.jersey.server.spi;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.spi.Contract;

/**
 * Service-provider interface for creating server instances.
 *
 * If supported by the provider, a server instance of the requested Java type
 * will be created.
 * <p>
 * The created server uses an internally created {@link Container} which is
 * responsible for listening on a communication channel provided by the server
 * for new client requests, dispatching these requests to the registered
 * {@link ApplicationHandler Jersey application handler} using the handler's
 * {@link ApplicationHandler#handle(org.glassfish.jersey.server.ContainerRequest)
 * handle(requestContext)} method and sending the responses provided by the
 * application back to the client.
 * </p>
 * <p>
 * A provider shall support a one-to-one mapping between a type, provided the
 * type is not {@link Object}. A provider may also support mapping of sub-types
 * of a type (provided the type is not {@code Object}). It is expected that each
 * provider supports mapping for distinct set of types and subtypes so that
 * different providers do not conflict with each other. In addition, a provider
 * SHOULD support the super type {@link WebServer} to participate in auto-selection
 * of providers (in this case the <em>first</em> supporting provider found is
 * used).
 * </p>
 * <p>
 * An implementation can identify itself by placing a Java service provider
 * configuration file (if not already present) -
 * {@code org.glassfish.jersey.server.spi.WebServerProvider} - in the resource
 * directory {@code META-INF/services}, and adding the fully qualified
 * service-provider-class of the implementation in the file.
 * </p>
 *
 * @author Markus KARG (markus@headcrashing.eu)
 * @since 3.1.0
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface WebServerProvider {

    /**
     * Creates a server of a given type which runs the given application using the
     * given bootstrap configuration.
     *
     * @param <T>
     *            the type of the web server.
     * @param type
     *            the type of the web server. Providers SHOULD support at least
     *            {@link WebServer}.
     * @param application
     *            The application to host.
     * @param configuration
     *            The configuration (host, port, etc.) to be used for bootstrapping.
     * @return the server, otherwise {@code null} if the provider does not support
     *         the requested {@code type}.
     * @throws ProcessingException
     *             if there is an error creating the server.
     */
    <T extends WebServer> T createServer(Class<T> type,
                                         Application application,
                                         SeBootstrap.Configuration configuration) throws ProcessingException;

    /**
     * Creates a server of a given type which runs the given application using the
     * given bootstrap configuration.
     *
     * @param <T>
     *            the type of the web server.
     * @param type
     *            the type of the web server. Providers SHOULD support at least
     *            {@link WebServer}.
     * @param applicationClass
     *            The class of application to host.
     * @param configuration
     *            The configuration (host, port, etc.) to be used for bootstrapping.
     * @return the server, otherwise {@code null} if the provider does not support
     *         the requested {@code type}.
     * @throws ProcessingException
     *             if there is an error creating the server.
     */
    <T extends WebServer> T createServer(Class<T> type,
                                         Class<? extends Application> applicationClass,
                                         SeBootstrap.Configuration configuration) throws ProcessingException;


    /**
     * Utility function that matches {@code WebServerProvider} supported type with the user type passed either
     * as {@link ServerProperties#WEBSERVER_CLASS} property (higher priority) or by the {@code userType} argument
     * (lower priority).
     * @param supportedType The type supported by the {@code WebServerProvider} implementation
     * @param userType The user type passed in by the user, usually {@link WebServer} class.
     * @param configuration The configuration to check {@link ServerProperties#WEBSERVER_CLASS} property
     * @param <T> The {@link WebServer} subtype
     * @return @{code true} if the user provided type matches the supported type.
     */
    static <T extends WebServer> boolean isSupportedWebServer(
            Class<? extends WebServer> supportedType, Class<T> userType, SeBootstrap.Configuration configuration) {
        final Object webServerObj = configuration.property(ServerProperties.WEBSERVER_CLASS);
        final Class<? extends WebServer> webServerCls = webServerObj == null || WebServer.class.equals(webServerObj)
                ? null : (Class<? extends WebServer>) webServerObj;
        // WebServer.class.equals(webServerObj) is the default, and then we want userType
        return (webServerCls != null  && webServerCls.isAssignableFrom(supportedType))
                || (webServerCls == null && userType.isAssignableFrom(supportedType));
    }
}
