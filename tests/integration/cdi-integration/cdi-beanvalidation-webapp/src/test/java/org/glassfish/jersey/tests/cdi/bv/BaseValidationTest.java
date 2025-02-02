/*
 * Copyright (c) 2015, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.bv;

import java.net.URI;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Common test for resource validation. The same set of tests is used
 * for the following scenarios: Grizzly based combined deployment with CDI enabled,
 * WAR based combined deployment with CDI enabled, Grizzly based deployment without CDI enabled.
 *
 * @author Jakub Podlesak
 */
public abstract class BaseValidationTest extends JerseyTest {

    public abstract String getAppPath();

    @Override
    protected URI getBaseUri() {
        return UriBuilder.fromUri(super.getBaseUri()).path("cdi-beanvalidation-webapp").path(getAppPath()).build();
    }

    @Test
    public void testParamValidatedResourceNoParam() throws Exception {
        _testParamValidatedResourceNoParam(target());
    }

    public static void _testParamValidatedResourceNoParam(final WebTarget target) throws Exception {

        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("param").path("validate")
                .request().get(Integer.class);

        assertThat(errors, is(1));
    }

    @Test
    public void testParamValidatedResourceParamProvided() throws Exception {
        _testParamValidatedResourceParamProvided(target());
    }

    public static void _testParamValidatedResourceParamProvided(WebTarget target) throws Exception {
        Integer errors = target.register(LoggingFeature.class).path("validated").path("field").path("validate")
                .queryParam("q", "one").request().get(Integer.class);
        assertThat(errors, is(0));
    }

    @Test
    public void testFieldValidatedResourceNoParam() throws Exception {
        _testFieldValidatedResourceNoParam(target());
    }

    public static void _testFieldValidatedResourceNoParam(final WebTarget target) throws Exception {

        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("field").path("validate")
                .request().get(Integer.class);

        assertThat(errors, is(1));
    }

    @Test
    public void testFieldValidatedResourceParamProvided() throws Exception {
        _testFieldValidatedResourceParamProvided(target());
    }

    public static void _testFieldValidatedResourceParamProvided(final WebTarget target) throws Exception {
        Integer errors = target.register(LoggingFeature.class).path("validated").path("field").path("validate")
                .queryParam("q", "one").request().get(Integer.class);
        assertThat(errors, is(0));
    }

    @Test
    public void testPropertyValidatedResourceNoParam() throws Exception {
        _testPropertyValidatedResourceNoParam(target());
    }

    public static void _testPropertyValidatedResourceNoParam(final WebTarget target) throws Exception {

        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("property").path("validate")
                .request().get(Integer.class);

        assertThat(errors, is(1));
    }

    @Test
    public void testPropertyValidatedResourceParamProvided() throws Exception {
        _testPropertyValidatedResourceParamProvided(target());
    }

    public static void _testPropertyValidatedResourceParamProvided(final WebTarget target) throws Exception {
        Integer errors = target.register(LoggingFeature.class).path("validated").path("property").path("validate")
                .queryParam("q", "one").request().get(Integer.class);
        assertThat(errors, is(0));
    }

    @Test
    public void testOldFashionedResourceNoParam() {
        _testOldFashionedResourceNoParam(target());
    }

    public static void _testOldFashionedResourceNoParam(final WebTarget target) {

        Response response = target.register(LoggingFeature.class)
                .path("old").path("fashioned").path("validate")
                .request().get();

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testOldFashionedResourceParamProvided() throws Exception {
        _testOldFashionedResourceParamProvided(target());
    }

    public static void _testOldFashionedResourceParamProvided(final WebTarget target) throws Exception {
        String response = target.register(LoggingFeature.class).path("old").path("fashioned").path("validate")
                .queryParam("q", "one").request().get(String.class);
        assertThat(response, is("one"));
    }

    public static void _testNonJaxRsValidationFieldValidatedResourceNoParam(final WebTarget target) {
        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("field").path("validate").path("non-jaxrs")
                .queryParam("q", "not-important-just-to-get-this-through-jax-rs").request().get(Integer.class);

        assertThat(errors, is(1));
    }

    public static void _testNonJaxRsValidationFieldValidatedResourceParamProvided(final WebTarget target) {
        Integer errors = target.register(LoggingFeature.class)
                .path("validated").path("field").path("validate").path("non-jaxrs")
                .queryParam("q", "not-important-just-to-get-this-through-jax-rs")
                .queryParam("h", "bummer")
                .request().get(Integer.class);

        assertThat(errors, is(0));
    }
}
