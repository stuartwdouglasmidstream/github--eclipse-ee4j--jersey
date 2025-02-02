/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.opentracing;

import java.util.concurrent.Executors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.opentracing.OpenTracingFeature;
import org.glassfish.jersey.opentracing.OpenTracingUtils;
import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.Uri;

import io.opentracing.Span;


/**
 * OpenTracing example resource.
 * <p>
 * Jersey (with registered {@link OpenTracingFeature} will automatically
 * create and start span for each request ("root" span or "request" span) and a child span to be used in the resource method
 * ("resource" span). The root span is used for Jersey-level event logging (resource matching started, request filters applied,
 * etc). The resource span serves for application-level event logging purposes (used-defined). Both are automatically created
 * and also automatically finished.
 * <p>
 * Resource span is created right before the resource method invocation and finished right after resource method finishes. It
 * can be resolved by calling {@link OpenTracingUtils#getRequestSpan(ContainerRequestContext)}.
 * <p>
 * Application code can also create ad-hoc spans as child spans of the resource span. This can be achieved by calling one of the
 * convenience methods {@link OpenTracingUtils#getRequestChildSpan(ContainerRequestContext)}.
 * <p>
 * {@link ContainerRequestContext} can be obtained via injection.
 * <p>
 * All the ad-hoc created spans MUST be {@link Span#finish() finished} explicitly.
 *
 * @author Adam Lindenthal
 */
@Path(value = "/resource")
public class TracedResource {

    /**
     * Resource method with no explicit tracing.
     * <p>
     * One span (jersey-server) will be created and finished automatically.
     *
     * @return dummy response
     */
    @GET
    @Path("defaultTrace")
    public Response defaultTrace() {
        return Response.ok("foo").build();
    }

    /**
     * Resource method with explicit logging into resource span.
     *
     * @param context injected request context with resource-level span reference
     * @return dummy response
     * @throws InterruptedException if interrupted
     */
    @GET
    @Path("appLevelLogging")
    public Response appLevelLogging(@Context ContainerRequestContext context) throws InterruptedException {
        final Span resourceSpan = OpenTracingUtils
                .getRequestSpan(context)
                .orElseThrow(() -> new RuntimeException("Tracing has failed"));

        resourceSpan.log("Starting expensive operation.");
        // Do the business
        Thread.sleep(200);
        resourceSpan.log("Expensive operation finished.");
        resourceSpan.setTag("expensiveOperationSuccess", true);

        return Response.ok("SUCCESS").build();
    }

    /**
     * Similar as {@link #appLevelLogging(ContainerRequestContext)}, just with {@code POST} method.
     *
     * @param entity  posted entity
     * @param context injected context
     * @return dummy response
     */
    @POST
    @Path("appLevelPost")
    public Response tracePost(String entity, @Context ContainerRequestContext context) {
        final Span resourceSpan = OpenTracingUtils
                .getRequestSpan(context)
                .orElseThrow(() -> new RuntimeException("Tracing has failed"));

        resourceSpan.setTag("result", "42");
        resourceSpan.setBaggageItem("entity", entity);
        return Response.ok("Done!").build();
    }

    /**
     * Resource method with explicit child span creation.
     *
     * @param context injected request context with resource-level (parent) span reference
     * @return dummy response
     * @throws InterruptedException if interrupted
     */
    @GET
    @Path("childSpan")
    public Response childSpan(@Context ContainerRequestContext context) throws InterruptedException {
        final Span childSpan = OpenTracingUtils.getRequestChildSpan(context, "AppCreatedSpan");
        childSpan.log("Starting expensive operation.");
        // Do the business
        Thread.sleep(200);
        childSpan.log("Expensive operation finished.");
        childSpan.setTag("expensiveOperationSuccess", true);

        childSpan.finish();
        return Response.ok("SUCCESS").build();
    }


    /**
     * Resource method with explicit span creation and propagation into injected managed client.
     * <p>
     * Shows how to propagate the server-side span into managed client (or any common Jersey client).
     * This way, the client span will be child of the resource span.
     *
     * @param context injected context
     * @param wt      injected web target
     * @return dummy response
     */
    @GET
    @Path("managedClient")
    public Response traceWithManagedClient(@Context ContainerRequestContext context,
                                           @Uri("resource/appLevelPost") WebTarget wt) {
        final Span providedSpan = OpenTracingUtils
                .getRequestSpan(context)
                .orElseThrow(() -> new RuntimeException("Tracing failed"));

        providedSpan.log("Resource method started.");

        final Response response = wt.request()
                .property(OpenTracingFeature.SPAN_CONTEXT_PROPERTY, providedSpan.context())  // <--- span propagation
                .post(Entity.text("Hello"));

        providedSpan.log("1st Response received from managed client");
        providedSpan.log("Firing 1st request from managed client");

        providedSpan.log("Creating child span");
        final Span childSpan = OpenTracingUtils.getRequestChildSpan(context, "jersey-resource-child-span");


        childSpan.log("Firing 2nd request from managed client");
        final Response response2 = wt.request()
                .property(OpenTracingFeature.SPAN_CONTEXT_PROPERTY, childSpan.context())  // <--- span propagation
                .post(Entity.text("World"));
        childSpan.log("2st Response received from managed client");

        childSpan.finish();
        return Response.ok("Result: " + response.getStatus() + ", " + response2.getStatus()).build();
    }

    @GET
    @Path("async")
    @ManagedAsync
    public void traceWithAsync(@Suspended final AsyncResponse asyncResponse, @Context ContainerRequestContext context) {
        final Span span = OpenTracingUtils.getRequestSpan(context).orElseThrow(() -> new RuntimeException("tracing failed"));
        span.log("In the resource method.");
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                span.log("Interrupted");
                e.printStackTrace();
            }
            span.log("Resuming");
            asyncResponse.resume("OK");
        });
        span.log("Before exiting the resource method");
    }

    @GET
    @Path("error")
    public String failTrace(@Context ContainerRequestContext context) {
        throw new RuntimeException("Failing just for fun.");
    }
}
