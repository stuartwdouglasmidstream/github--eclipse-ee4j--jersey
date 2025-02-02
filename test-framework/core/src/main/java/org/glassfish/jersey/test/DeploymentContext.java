/*
 * Copyright (c) 2010, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;

import java.util.Optional;

/**
 * Basic application deployment context.
 * <p>
 * An instance of this class is created by creating using {@link Builder}, that allows to configure the deployment
 * context state, and finally building the context by invoking the {@link Builder#build()} method.
 * </p>
 * <p>
 * This deployment context is compatible with basic non-Servlet test containers. The following test container
 * factories support the descriptor:
 * <ul>
 * <li>{@code org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory} for testing with the Grizzly HTTP container.</li>
 * <li>{@code org.glassfish.jersey.test.jetty.JettyTestContainerFactory} for testing with the Jetty HTTP container.</li>
 * <li>{@code org.glassfish.jersey.test.simple.SimpleTestContainerFactory} for testing with the Simple HTTP container.</li>
 * <li>{@code org.glassfish.jersey.test.jdkhttp.JdkHttpServerTestContainerFactory} for testing with the Light Weight HTTP
 * server distributed with Java SE.</li>
 * <li>{@code org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory} for testing in memory without
 * using underlying HTTP client and server side functionality to send requests and receive responses.</li>
 * <li>{@code org.glassfish.jersey.test.external.ExternalTestContainerFactory} for testing Java EE Web
 * applications deployed independently in a separate JVM to that of the tests. For example, the application
 * may be deployed to the GlassFish or WebLogic application server.</li>
 * </ul>
 * </p>
 *
 * @author Paul Sandoz
 * @author Marek Potociar
 * @since 2.8
 */
public class DeploymentContext {

    /**
     * Create a new deployment context builder for a JAX-RS / Jersey application instance.
     *
     * @param application a JAX-RS / Jersey application to be tested.
     * @return new deployment context builder instance associated with the JAX-RS / Jersey application to be tested.
     *
     * @throws java.lang.NullPointerException in case the {@code application} is {@code null}.
     */
    public static Builder builder(final Application application) {
        return new Builder(application);
    }

    /**
     * Create a new deployment context builder for a JAX-RS / Jersey application instance.
     *
     * @param applicationClass a JAX-RS / Jersey application to be tested.
     * @return new deployment context builder instance associated with the JAX-RS / Jersey application to be tested.
     *
     * @throws java.lang.NullPointerException in case the {@code applicationClass} is {@code null}.
     */
    public static Builder builder(final Class<? extends Application> applicationClass) {
        return new Builder(applicationClass);
    }

    /**
     * Create a new deployment context for a JAX-RS / Jersey application instance.
     * <p>
     * The created deployment context will be configured to use default values.
     * </p>
     *
     * @param application a JAX-RS / Jersey application to be tested.
     * @return new deployment context instance associated with the JAX-RS / Jersey application to be tested.
     *
     * @throws java.lang.NullPointerException in case the {@code application} is {@code null}.
     */
    public static DeploymentContext newInstance(final Application application) {
        return new Builder(application).build();
    }

    /**
     * Create a new deployment context for a JAX-RS / Jersey application instance.
     * <p>
     * The created deployment context will be configured to use default values.
     * </p>
     *
     * @param applicationClass a JAX-RS / Jersey application to be tested.
     * @return new deployment context instance associated with the JAX-RS / Jersey application to be tested.
     *
     * @throws java.lang.NullPointerException in case the {@code applicationClass} is {@code null}.
     */
    public static DeploymentContext newInstance(final Class<? extends Application> applicationClass) {
        return new Builder(applicationClass).build();
    }

    /**
     * Deployment context builder for building a basic application deployment context for the JAX-RS / Jersey application
     * defined by the associated {@link jakarta.ws.rs.core.Application} class or instance.
     * <p>
     * If properties of the builder are not modified, default values will be utilized:
     * <ul>
     * <li>Default value for the context path is an empty string</li>
     * </ul>
     * </p>
     * <p>
     * After {@link #build()} has been invoked, the state of the builder will be reset to default values and another
     * context for the associated JAX-RS / Jersey application can be prepared and built.
     * </p>
     */
    public static class Builder {
        private static final String DEFAULT_CONTEXT_PATH = "";

        private final ResourceConfig resourceConfig;
        private String contextPath = DEFAULT_CONTEXT_PATH;
        private SSLContext sslContext;
        private SSLParameters sslParameters;

        /**
         * Create new deployment context builder instance not explicitly bound to the JAX-RS / Jersey application class.
         * <p>
         * The constructor is provided to support different subclass initialization scenarios.
         * </p>
         */
        protected Builder() {
            this.resourceConfig = null;
        }

        /**
         * Create new deployment context builder instance and bind it to the JAX-RS / Jersey application instance.
         *
         * @param app JAX-RS / Jersey application instance.
         */
        protected Builder(final Application app) {
            if (app == null) {
                throw new NullPointerException("Application must not be null.");
            }
            this.resourceConfig = ResourceConfig.forApplication(app);
        }

        /**
         * Create new deployment context builder instance and bind it to the JAX-RS / Jersey application class.
         *
         * @param appClass JAX-RS / Jersey application class.
         */
        protected Builder(final Class<? extends Application> appClass) {
            if (appClass == null) {
                throw new NullPointerException("Application class must not be null.");
            }

            this.resourceConfig = ResourceConfig.forApplicationClass(appClass);
        }

        /**
         * Set the application deployment context path.
         *
         * @param contextPath application context path.
         * @return this application deployment context builder.
         *
         * @throws NullPointerException if {@code contextPath} is {@code null}.
         */
        public Builder contextPath(final String contextPath) {
            if (contextPath == null) {
                throw new NullPointerException("The context path must not be null");
            }

            this.contextPath = contextPath;
            return this;
        }

        /**
         * Set the application ssl properties.
         *
         * @param sslContext application ssl context.
         * @param sslParameters application ssl parameters
         * @return this application deployment context builder.
         *
         * @throws NullPointerException if {@code sslContext} is {@code null}.
         */
        public Builder ssl(final SSLContext sslContext, SSLParameters sslParameters) {
            if (sslContext == null && sslParameters == null) {
                throw new NullPointerException("The sslContext and sslParameters must not be null");
            }
            this.sslContext = sslContext;
            this.sslParameters = sslParameters;
            return this;
        }

        /**
         * Build a new application deployment context configured by the current state of this
         * application deployment context builder.
         *
         * @return this application deployment context builder.
         */
        public DeploymentContext build() {
            final DeploymentContext lld = new DeploymentContext(this);

            reset();

            return lld;
        }

        /**
         * Reset the application deployment context builder values to defaults.
         *
         * Note that the builder still remains bound to the same JAX-RS / Jersey application instance.
         */
        protected void reset() {
            this.contextPath = "";
        }
    }

    private final ResourceConfig resourceConfig;
    private final String contextPath;
    private final SSLContext sslContext;
    private final SSLParameters sslParameters;

    /**
     * Create new application deployment context.
     *
     * @param b {@link Builder} instance.
     */
    protected DeploymentContext(final Builder b) {
        this.contextPath = b.contextPath;
        this.resourceConfig = b.resourceConfig;
        this.sslContext = b.sslContext;
        this.sslParameters = b.sslParameters;
    }

    /**
     * Get the deployed application resource configuration.
     * <p>
     * This method can be overridden in a subclass to support conversion of non-resource config
     * initialization scenario (e.g. via Servlet init parameters) into a resource configuration.
     * </p>
     *
     * @return the deployed application resource configuration.
     */
    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    /**
     * Get the deployed application context path.
     *
     * @return the deployed application context path.
     */
    public final String getContextPath() {
        return contextPath;
    }

    /**
     * Get the deployed application ssl context.
     *
     * @return the deployed application ssl context.
     */
    public Optional<SSLContext> getSslContext() {
        return Optional.ofNullable(sslContext);
    }

    /**
     * Get the deployed application ssl parameters.
     *
     * @return the deployed application ssl parameters.
     */
    public Optional<SSLParameters> getSslParameters() {
        return Optional.ofNullable(sslParameters);
    }

}

