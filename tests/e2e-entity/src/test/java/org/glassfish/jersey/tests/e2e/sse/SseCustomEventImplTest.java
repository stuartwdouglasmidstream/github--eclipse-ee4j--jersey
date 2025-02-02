/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.sse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseEventSink;
import jakarta.ws.rs.sse.SseEventSource;

import jakarta.inject.Singleton;

import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test that {@link org.glassfish.jersey.media.sse.OutboundEventWriter} works with custom
 * {@link jakarta.ws.rs.sse.OutboundSseEvent} implementation.
 *
 * @author Adam Lindenthal
 */
public class SseCustomEventImplTest extends JerseyTest {

    private static final String SSE_EVENT_NAME = "custom-message";

    @Override
    protected Application configure() {
        return new ResourceConfig(SseResource.class);
    }

    @Path("events")
    @Singleton
    public static class SseResource {

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void getServerSentEvents(@Context final SseEventSink eventSink) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                eventSink.send(new MyOutboundEvent("foo"));
                eventSink.send(new MyOutboundEvent("bar"));
                eventSink.send(new MyOutboundEvent("baz"));
            });
        }
    }

    @Test
    public void testWithJaxRsAPI() {
        final WebTarget endpoint = target().path("events");
        final List<InboundSseEvent> results = new ArrayList<>();
        try (final SseEventSource eventSource = SseEventSource.target(endpoint).build()) {
            final CountDownLatch receivedLatch = new CountDownLatch(3);
            eventSource.register((event) -> {
                results.add(event);
                receivedLatch.countDown();
            });

            eventSource.open();
            final boolean allReceived = receivedLatch.await(5000, TimeUnit.MILLISECONDS);
            Assertions.assertTrue(allReceived);
            Assertions.assertEquals(3, results.size());
            Assertions.assertEquals("foo", results.get(0).readData());
            Assertions.assertEquals("bar", results.get(1).readData());
            Assertions.assertEquals("baz", results.get(2).readData());
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWithJerseyAPI() throws InterruptedException {
        final WebTarget endpoint = target().path("events");
        final EventSource eventSource = EventSource.target(endpoint).build();
        final CountDownLatch receiveLatch = new CountDownLatch(3);

        final List<String> results = new ArrayList<>();
        final EventListener listener = inboundEvent -> {
            try {
                results.add(inboundEvent.readData());
                receiveLatch.countDown();
                Assertions.assertEquals(SSE_EVENT_NAME, inboundEvent.getName());
            } catch (ProcessingException ex) {
                throw new RuntimeException("Error when deserializing of data.", ex);
            }
        };
        eventSource.register(listener, SSE_EVENT_NAME);
        eventSource.open();
        Assertions.assertTrue(receiveLatch.await(5000, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(3, results.size());
        Assertions.assertEquals("foo", results.get(0));
        Assertions.assertEquals("bar", results.get(1));
        Assertions.assertEquals("baz", results.get(2));
    }

    static class MyOutboundEvent implements OutboundSseEvent {

        private String data;

        public MyOutboundEvent(String data) {
            this.data = data;
        }

        @Override
        public Class<?> getType() {
            return String.class;
        }

        @Override
        public Type getGenericType() {
            return String.class;
        }

        @Override
        public MediaType getMediaType() {
            return MediaType.TEXT_PLAIN_TYPE;
        }

        @Override
        public String getData() {
            return data;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getName() {
            return SSE_EVENT_NAME;
        }

        @Override
        public String getComment() {
            return "";
        }

        @Override
        public long getReconnectDelay() {
            return 0;
        }

        @Override
        public boolean isReconnectDelaySet() {
            return false;
        }
    }
}
