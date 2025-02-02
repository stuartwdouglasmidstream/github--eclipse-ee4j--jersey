/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.helloworld.spring;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import jakarta.inject.Singleton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Jersey Spring integration example.
 * Demonstrate how to use Spring managed JAX-RS resource class with singleton scope (+ Spring bean DI).
 *
 * @author Marko Asplund (marko.asplund at gmail.com)
 */
@Path("spring-singleton-hello")
@Component
@Singleton
public class SpringSingletonResource {

    private final AtomicInteger counter = new AtomicInteger();

    @Autowired
    private GreetingService greetingService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHello(@Context HttpHeaders headers, @QueryParam("p1") String p1) {
        if ("foobar".equals(p1)) {
            throw new IllegalArgumentException("foobar is illegal");
        }
        return String.format("%d: %s", counter.incrementAndGet(), greetingService.greet("world"));
    }
}
