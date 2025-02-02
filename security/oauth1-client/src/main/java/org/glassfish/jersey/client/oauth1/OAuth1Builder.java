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

package org.glassfish.jersey.client.oauth1;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Feature;

/**
 * Builder of OAuth 1 client support. This builder can build {@link OAuth1AuthorizationFlow} using a method
 * {@link #authorizationFlow(String, String, String)} and {@link Feature filter feature} using a method
 * {@link #feature()}. Before one of these methods is invoked, properties common for both features
 * can be defined using methods of this builder. However, for default OAuth configuration there should not be
 * no need to call these {@code set*} methods of this builder and the builder can be directly used to build
 * mentioned features.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public interface OAuth1Builder {

    /**
     * Set the signature method name. The signature methods implement
     * {@link org.glassfish.jersey.oauth1.signature.OAuth1SignatureMethod} and the name is retrieved from
     * {@link org.glassfish.jersey.oauth1.signature.OAuth1SignatureMethod#name()} method. Build-in signature
     * methods are {@code HMAC-SHA1}, {@code RSA-SHA1} and {@code PLAINTEXT}.
     * <p>
     * Default value is {@code HMAC-SHA1}.
     * </p>
     *
     * @param signatureMethod Signature method name.
     * @return This builder instance.
     */
    public OAuth1Builder signatureMethod(String signatureMethod);

    /**
     * Set the realm to which the user wants to authenticate. The parameter
     *              will be sent in Authenticated request and used during Authorization Flow.
     *
     * @param realm Realm on the server to which the user authentication is required.
     * @return This builder instance.
     */
    public OAuth1Builder realm(String realm);

    /**
     * Set the timestamp. The timestamp if defined will be used in {@code Authorization} header. Usually this
     * parameter is not defined explicitly by this method and will be automatically filled with the current
     * time during the request.
     *
     * @param timestamp Timestamp value.
     * @return This builder instance.
     */
    public OAuth1Builder timestamp(String timestamp);

    /**
     * Set the nonce. Nonce (shortcut of "number used once") is used to uniquely identify the request and
     * prevent from multiple usage of the same signed request. The nonce if defined will be used
     * in the {@code Authorization} header if defined. Usually this
     * parameter is not defined explicitly by this method and will be automatically filled with the randomly
     * generated UID during the request.
     *
     * @param nonce Nonce value.
     * @return This builder instance.
     */
    public OAuth1Builder nonce(String nonce);

    /**
     * Set the version of the OAuth protocol. The version, if defined, will be used in the {@code Authorization}
     * header otherwise default value {@code 1.1} will be used. Usually this parameter does not need to be
     * overwritten by this method.
     *
     * @param version OAuth protocol version parameter.
     * @return This builder instance.
     */
    public OAuth1Builder version(String version);


    /**
     * Get the builder of {@link Feature filter feature}.
     *
     * @return The builder that can be used to build {@code OAuth1ClientFeature}.
     */
    public FilterFeatureBuilder feature();


    /**
     * Get the builder of {@link OAuth1AuthorizationFlow}.
     *
     * @param requestTokenUri URI of the endpoint on the Authorization Server where Request Token can be obtained.
     *                        The URI is defined by the Service Provider.
     * @param accessTokenUri URI of the endpoint on the Authorization Server where Access Token can be obtained.
     *                        The URI is defined by the Service Provider.
     * @param authorizationUri URI of the endpoint on the Authorization Server to which the user (resource owner)
     *                         should be redirected in order to grant access to this application (our consumer).
     *                         The URI is defined by the Service Provider.
     * @return The builder that can be used to build {@code OAuth1AuthorizationFlow}.
     */
    public FlowBuilder authorizationFlow(String requestTokenUri, String accessTokenUri, String authorizationUri);


    /**
     * Builder of the {@link jakarta.ws.rs.core.Feature}.
     */
    public static interface FilterFeatureBuilder {
        /**
         * Set the Access Token that will be used for signing the requests. If this method is not called,
         * no access token will be defined in the resulting filter and it will have to be supplied using
         * {@link OAuth1ClientSupport#OAUTH_PROPERTY_ACCESS_TOKEN} request property for each request.
         * The property could be also used to override settings defined by this method.
         *
         * @param accessToken Access token.
         * @return this builder.
         */
        public FilterFeatureBuilder accessToken(AccessToken accessToken);

        /**
         * Build the {@link Feature oauth 1 filter feature}. The feature can be then registered into the
         * client configuration.
         *
         * @return Client OAuth 1 feature.
         */
        public Feature build();
    }

    /**
     * Builder of the {@link OAuth1AuthorizationFlow}.
     */
    public static interface FlowBuilder {

        /**
         * Set the callback URI to which the user (resource owner) should be redirected after he/she
         * grants access to this application. In most cases, the URI is under control of this application
         * and request done on this URI will be used to extract query parameter {@code verifier} that will be used in
         * {@link OAuth1AuthorizationFlow#finish(String)} method.
         * <p>
         * If URI is not defined by this method, the default value {@code oob} will be used in the Authorization
         * Flow which should cause that {@code verifier} will be passed to application in other way than request
         * redirection (for example shown to the user using html page).
         * </p>
         *
         * @param callbackUri URI that should receive authorization response from the Service Provider.
         * @return this builder.
         */
        public FlowBuilder callbackUri(String callbackUri);

        /**
         * Set the client that should be used internally by the {@code OAuth1AuthorizationFlow} to make requests to
         * Authorization Server. If this method is not called, it is up to the implementation to create or get
         * any private client instance to perform these requests. This method could be used mainly for
         * performance reasons to avoid creation of new client instances and have control about created client
         * instances used in the application.
         *
         * @param client Client instance.
         * @return this builder.
         */
        public FlowBuilder client(Client client);

        /**
         * Enable logging (headers and entities) of OAuth requests and responses.
         *
         * @return this builder.
         * @since 2.7
         */
        public FlowBuilder enableLogging();

        /**
         * Build the {@code OAuth1AuthorizationFlow}.
         *
         * @return Authorization flow.
         */
        public OAuth1AuthorizationFlow build();
    }
}
