/*
 * Copyright (c) 2015, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.java8;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;

import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test usage of Java SE 8 lambdas in JAX-RS resource methods.
 *
 * @author Marek Potociar
 */
public class LambdaResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new Java8Application();
    }

    /**
     * Test that JDK8 lambdas do work in common JAX-RS resource methods.
     */
    @Test
    public void testLambdas() {
        final WebTarget target = target("j8").path("lambdas/{p}");

        // test default method with no @Path annotation
        String response = target.resolveTemplate("p", "test").request().get(String.class);
        assertThat(response, equalTo("test-lambdaized"));
    }

}
