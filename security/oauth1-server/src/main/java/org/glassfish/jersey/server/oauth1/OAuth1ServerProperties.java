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

package org.glassfish.jersey.server.oauth1;

import java.util.Map;

import org.glassfish.jersey.internal.util.PropertiesClass;
import org.glassfish.jersey.internal.util.PropertiesHelper;

/**
 * Properties used to configure OAuth server module.
 *
 * @author Miroslav Fuksa
 */
@PropertiesClass
public final class OAuth1ServerProperties {
    /**
     * OAuth realm (String property).
     *
     * <p>
     * A default value is {@code 'default'}.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     **/
    public static final String REALM = "jersey.config.server.oauth1.realm";


    /**
     * Property that can be set to a regular expression used to match the path (relative to the base URI) this
     * filter should not be applied to.
     * <p>
     * A default value is {@code null}.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     **/
    public static final String IGNORE_PATH_PATTERN = "jersey.config.server.oauth1.ignorePathPattern";

    /**
     * Property defines maximum age (in milliseconds) of nonces that should be tracked (default = 300000 ms = 5 min).
     *
     * <p>
     * A default value is {@code 300000} which corresponds to 5 minutes.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     **/
    public static final String MAX_AGE = "jersey.config.server.oauth1.maxAge";


    /**
     * Property that can be set to frequency of collecting nonces exceeding max. age (default = 100 = every 100 requests).
     *
     **/
    public static final String GC_PERIOD = "jersey.config.server.oauth1.gcPeriod";


    /**
     * Unit of {@code oauth_timestamp} attribute used in authorization headers. The value must be one of the
     * enum values of {@link java.util.concurrent.TimeUnit} (e.g. {@code SECONDS},
     * {@code MILLISECONDS}, {@code MINUTES}).
     * <p>
     * A default value is {@code SECONDS}.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String TIMESTAMP_UNIT = "jersey.config.server.oauth1.timestamp.unit";


    /**
     * Defines maximum number of nonces that can be stored in the nonce cache. If more nonces will be used,
     * the cache will not store any other nonce and requests will be refused. Note that cache is automatically
     * cleaned as it keeps only nonces delivered with timestamp withing the {@link #MAX_AGE} period.
     * <p>
     * This setting is used to limit the maximum size of internal cache and thanks to this
     * it prevents exhausting of memory and failing of the server.
     * </p>
     *
     * <p>
     * The value must be a long.
     * </p>

     * <p>
     * A default value is {@code 2000000}.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
    */
    public static final String MAX_NONCE_CACHE_SIZE = "jersey.config.server.oauth1.max.nonce.size";

    /**
     * If set to {@code true} makes the correct OAuth authentication optional.
     * Instead of returning the appropriate status code
     * ({@link jakarta.ws.rs.core.Response.Status#BAD_REQUEST}
     * or {@link jakarta.ws.rs.core.Response.Status#UNAUTHORIZED}) the {@link OAuth1ServerFilter OAuth filter}
     * will ignore this request (as if it was not authenticated) and let the web application deal with it.
     *
     * <p>
     * A default value is {@code false}.
     * </p>
     *
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     * */
    public static final String NO_FAIL = "jersey.config.server.oauth1.noFail";

    /**
     * If set to {@code true}, token resources will be exposed. Token resources are JAX-RS resources
     * for retrieving Request Tokens and Access Tokens. If the property is set to {@code false},
     * the resources will not be exposed and it is responsibility of the user custom resource to issue
     * Request Tokens and Access Tokens.
     * <p>
     * URIs of exposed resources can be specified by {@link #REQUEST_TOKEN_URI} and {@link #ACCESS_TOKEN_URI}.
     * </p>
     *
     * <p>
     * A default value is {@code true}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String ENABLE_TOKEN_RESOURCES = "jersey.config.server.oauth1.resource.enabled";

    /**
     * If exposure of token resources is enabled (e.g. by the property {@link #ENABLE_TOKEN_RESOURCES}),
     * this property defines the relative URI of exposed Request Token Resource. The URI must be relative
     * to the base URI of the JAX-RS application.
     * <p/>
     * Request Token resource is the resource which issues a Request Token and Request Token secret
     * during the Authorization process (temporary credentials IDs).
     *
     * <p>
     * The property is undefined by default.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String REQUEST_TOKEN_URI = "jersey.config.server.oauth1.resource.requestToken.uri";

    /**
     * If exposure of token resources is enabled (e.g. by the property {@link #ENABLE_TOKEN_RESOURCES}),
     * this property defines the relative URI of exposed Access Token Resource. The URI must be relative
     * to the base URI of the JAX-RS application.
     * <p/>
     * Access Token resource is the resource which issues a Access Token and Access Token secret
     * during the Authorization process.
     *
     * <p>
     * The property is undefined by default.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String ACCESS_TOKEN_URI = "jersey.config.server.oauth1.resource.accessToken.uri";

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with the specified value type,
     * returns {@code null}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param key  Name of the property.
     * @param type          Type to retrieve the value as.
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(Map<String, ?> properties, String key, Class<T> type) {
        return PropertiesHelper.getValue(properties, key, type, null);
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with {@code defaultValue} type,
     * the specified {@code defaultValue} is returned. Calling this method is equivalent to calling
     * {@code OAuth1ServerProperties.getValue(properties, key, defaultValue, (Class<T>) defaultValue.getClass())}
     *
     * @param properties    Map of properties to get the property value from.
     * @param key  Name of the property.
     * @param defaultValue  Default value if property is not registered
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(Map<String, ?> properties, String key, T defaultValue) {
        return PropertiesHelper.getValue(properties, key, defaultValue, null);
    }

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with the specified value type,
     * returns {@code defaultValue}.
     *
     * @param properties    Map of properties to get the property value from.
     * @param key  Name of the property.
     * @param defaultValue  Default value if property is not registered
     * @param type          Type to retrieve the value as.
     * @param <T>           Type of the property value.
     * @return              Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(Map<String, ?> properties, String key, T defaultValue, Class<T> type) {
        return PropertiesHelper.getValue(properties, key, defaultValue, type, null);
    }
}
