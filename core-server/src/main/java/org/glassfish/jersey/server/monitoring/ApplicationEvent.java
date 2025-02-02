/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.monitoring;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.ResourceModel;

/**
 * An event informing about application lifecycle changes. The event is created by Jersey runtime and
 * handled by user registered {@link ApplicationEventListener application event listener}.
 * <p/>
 * The event contains the {@link Type} which distinguishes between types of event. There are various
 * properties in the event (accessible by getters) and some of them might be relevant only to specific event types.
 * <p/>
 * Note that internal state of the event must be modified. Even the event is immutable it exposes objects
 * which might be mutable and the code of event listener must not change state of these objects.
 *
 * @author Miroslav Fuksa
 */
public interface ApplicationEvent {

    /**
     * The type of the event that identifies on which lifecycle change the event is triggered.
     */
    public static enum Type {
        /**
         * Initialization of the application has started. In this point no all the event properties
         * are initialized yet.
         */
        INITIALIZATION_START,
        /**
         * Initialization of {@link org.glassfish.jersey.server.ApplicationHandler jersey application} is
         * finished but the server might not be started and ready yet to serve requests (this will be
         * indicated by the {@link #INITIALIZATION_FINISHED} event). This event indicates only that the
         * environment is ready (all providers are registered, application is configured, etc.).
         *
         * @since 2.5
         */
        INITIALIZATION_APP_FINISHED,
        /**
         * Initialization of the application has finished, server is started and application is ready
         * to handle requests now.
         */
        INITIALIZATION_FINISHED,
        /**
         * Application has been destroyed (stopped). In this point the application cannot process any new requests.
         */
        DESTROY_FINISHED,
        /**
         * The application reload is finished. The reload can be invoked by
         * {@link org.glassfish.jersey.server.spi.Container#reload()} method. When this event is triggered
         * the reload is completely finished, which means that the new application is initialized (appropriate
         * events are called) and new reloaded application is ready to server requests.
         */
        RELOAD_FINISHED
    }

    /**
     * Return the type of the event.
     *
     * @return Event type.
     */
    public Type getType();


    /**
     * Get resource config associated with the application. The resource config is set for all event types.
     *
     * @return Resource config on which this application is based on.
     */
    public ResourceConfig getResourceConfig();

    /**
     * Get resource classes registered by the user in the current application. The set contains only
     * user resource classes and not resource classes added by Jersey
     * or by {@link org.glassfish.jersey.server.model.ModelProcessor}.
     * <p/>
     * User resources are resources that
     * were explicitly registered by the configuration, discovered by the class path scanning or that
     * constructs explicitly registered {@link org.glassfish.jersey.server.model.Resource programmatic resource}.
     *
     * @return Resource user registered classes.
     */
    public Set<Class<?>> getRegisteredClasses();

    /**
     * Get resource instances registered by the user in the current application. The set contains only
     * user resources and not resources added by Jersey
     * or by {@link org.glassfish.jersey.server.model.ModelProcessor}.
     * <p/>
     * User resources are resources that
     * were explicitly registered by the configuration, discovered by the class path scanning or that
     * constructs explicitly registered {@link org.glassfish.jersey.server.model.Resource programmatic resource}.
     *
     * @return Resource instances registered by user.
     */
    public Set<Object> getRegisteredInstances();

    /**
     * Get registered providers available in the runtime. The registered providers
     * are providers like {@link org.glassfish.jersey.server.model.MethodList.Filter filters},
     * {@link jakarta.ws.rs.ext.ReaderInterceptor reader} and {@link jakarta.ws.rs.ext.WriterInterceptor writer}
     * interceptors which are explicitly registered by configuration, or annotated by
     * {@link jakarta.ws.rs.ext.Provider @Provider} or registered in META-INF/services. The
     * set does not include providers that are by default built in Jersey.
     *
     * @return Set of provider classes.
     */
    public Set<Class<?>> getProviders();

    /**
     * Get the resource model of the application. The method returns null for
     * {@link Type#INITIALIZATION_START} event type as the resource model is not initialized yet.
     * The returned resource model is the final deployed model including resources enhanced by
     * {@link org.glassfish.jersey.server.model.ModelProcessor model processors}.
     *
     * @return Resource model of the deployed application.
     */
    public ResourceModel getResourceModel();

}
