/*
 * Copyright (c) 2014, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.oauth;

import java.lang.reflect.Field;
import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.client.oauth1.AccessToken;
import org.glassfish.jersey.client.oauth1.ConsumerCredentials;
import org.glassfish.jersey.client.oauth1.OAuth1AuthorizationFlow;
import org.glassfish.jersey.client.oauth1.OAuth1Builder;
import org.glassfish.jersey.client.oauth1.OAuth1ClientSupport;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureFeature;
import org.glassfish.jersey.oauth1.signature.PlaintextMethod;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

public class OauthClientAuthorizationFlowTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(AccessTokenResource.class, PhotosResource.class, RequestTokenResource.class,
                OAuth1SignatureFeature.class);
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyTestContainerFactory();
    }

    /**
     * Tests mainly the client functionality. The test client registers
     * {@link org.glassfish.jersey.client.oauth1.OAuth1ClientFilter} and uses the filter only to sign requests. So, it does not
     * use the filter to perform authorization flow. However, each request that this test performs is actually a request used
     * during the authorization flow.
     * <p/>
     * The server side of this test extracts header authorization values and tests that signatures are
     * correct for each request type.
     */
    @Test
    public void testOAuthClientFeature() {
        final URI baseUri = getBaseUri();

        // baseline for requests
        final OAuth1Builder oAuth1Builder = OAuth1ClientSupport.builder(new ConsumerCredentials("dpf43f3p2l4k3l03",
                "kd94hf93k423kf44")).timestamp("1191242090").nonce("hsu94j3884jdopsl").signatureMethod(PlaintextMethod.NAME)
                .version("1.0");
        final Feature feature = oAuth1Builder.feature().build();

        final Client client = client();
        client.register(LoggingFeature.class);
        final WebTarget target = client.target(baseUri);

        // simulate request for Request Token (temporary credentials)
        String responseEntity = target.path("request_token").register(feature).request().post(Entity.entity("entity",
                MediaType.TEXT_PLAIN_TYPE), String.class);
        assertEquals(responseEntity, "oauth_token=hh5s93j4hdidpola&oauth_token_secret=hdhd0244k9j7ao03");

        final Feature feature2 = oAuth1Builder.timestamp("1191242092").nonce("dji430splmx33448").feature().accessToken(new
                AccessToken("hh5s93j4hdidpola", "hdhd0244k9j7ao03")).build();

        // simulate request for Access Token
        responseEntity = target.path("access_token").register(feature2).request().post(Entity.entity("entity",
                MediaType.TEXT_PLAIN_TYPE), String.class);
        assertEquals(responseEntity, "oauth_token=nnch734d00sl2jdk&oauth_token_secret=pfkkdhi9sl3r4s00");

        final Feature feature3 = oAuth1Builder.nonce("kllo9940pd9333jh").signatureMethod("HMAC-SHA1").timestamp("1191242096")
                .feature().accessToken(new AccessToken("nnch734d00sl2jdk", "pfkkdhi9sl3r4s00")).build();

        // based on Access Token
        responseEntity = target.path("/photos").register(feature3).queryParam("file", "vacation.jpg").queryParam("size",
                "original").request().get(String.class);

        assertEquals(responseEntity, "PHOTO");
    }

    @Test
    public void testOAuthClientFlow() throws Exception {
        final String uri = getBaseUri().toString();

        final OAuth1AuthorizationFlow authFlow = OAuth1ClientSupport
                .builder(new ConsumerCredentials("dpf43f3p2l4k3l03", "kd94hf93k423kf44"))
                .timestamp("1191242090")
                .nonce("hsu94j3884jdopsl")
                .signatureMethod("PLAINTEXT")
                .authorizationFlow(
                        uri + "request_token",
                        uri + "access_token",
                        uri + "authorize")
                .enableLogging()
                .build();

        // Check we have correct authorization URI.
        final String authorizationUri = authFlow.start();
        assertThat(authorizationUri, containsString("authorize?oauth_token=hh5s93j4hdidpola"));

        // For the purpose of the test I need parameters (and there is no way how to do it now).
        final Field paramField = authFlow.getClass().getDeclaredField("parameters");
        paramField.setAccessible(true);
        final OAuth1Parameters params = (OAuth1Parameters) paramField.get(authFlow);

        // Update parameters.
        params.timestamp("1191242092").nonce("dji430splmx33448");

        final AccessToken accessToken = authFlow.finish();
        assertThat(accessToken, equalTo(new AccessToken("nnch734d00sl2jdk", "pfkkdhi9sl3r4s00")));

        // Update parameters before creating a feature (i.e. changing signature method).
        params.nonce("kllo9940pd9333jh").signatureMethod("HMAC-SHA1").timestamp("1191242096");

        // Check Authorized Client.
        final Client flowClient = authFlow.getAuthorizedClient().register(LoggingFeature.class);

        String responseEntity = flowClient.target(uri).path("/photos")
                .queryParam("file", "vacation.jpg")
                .queryParam("size", "original")
                .request()
                .get(String.class);

        assertThat("Flow Authorized Client", responseEntity, equalTo("PHOTO"));

        // Check Feature.
        final Client featureClient = ClientBuilder.newClient()
                .register(authFlow.getOAuth1Feature()).register(LoggingFeature.class);

        responseEntity = featureClient.target(uri).path("/photos")
                .queryParam("file", "vacation.jpg")
                .queryParam("size", "original")
                .request()
                .get(String.class);

        assertThat("Feature Client", responseEntity, equalTo("PHOTO"));
    }
}
