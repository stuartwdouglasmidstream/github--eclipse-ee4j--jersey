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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.Sse;
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
 * JAX-RS {@link SseEventSource} and {@link SseEventSink} test.
 *
 * @author Adam Lindenthal
 */
public class SseEventSinkToEventSourceTest extends JerseyTest {

    private static final String INTEGER_SSE_NAME = "integer-message";
    private static final Logger LOGGER = Logger.getLogger(SseEventSinkToEventSourceTest.class.getName());

    @Override
    protected Application configure() {
        return new ResourceConfig(SseResource.class);
    }

    private static final int MSG_COUNT = 10;
    private static volatile CountDownLatch transmitLatch;

    @Path("events")
    @Singleton
    public static class SseResource {

        @GET
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public void getServerSentEvents(@Context final SseEventSink eventSink, @Context final Sse sse) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                int i = 0;
                while (transmitLatch.getCount() > 0) {
                    eventSink.send(sse.newEventBuilder()
                            .name(INTEGER_SSE_NAME)
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .data(Integer.class, i)
                            .build());

                    // send another event with name "foo" -> should be ignored by the client
                    eventSink.send(sse.newEventBuilder()
                            .name("foo")
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .data(String.class, "bar")
                            .build());

                    // send another unnamed event -> should be ignored by the client
                    eventSink.send(sse.newEventBuilder()
                            .mediaType(MediaType.TEXT_PLAIN_TYPE)
                            .data(String.class, "baz")
                            .build());
                    transmitLatch.countDown();
                    i++;
                }
            });
        }
    }

    @Test
    public void testWithSimpleSubscriber() {
        transmitLatch = new CountDownLatch(MSG_COUNT);
        final WebTarget endpoint = target().path("events");
        final List<InboundSseEvent> results = new ArrayList<>();
        try (final SseEventSource eventSource = SseEventSource.target(endpoint).build()) {
            final CountDownLatch receivedLatch = new CountDownLatch(3 * MSG_COUNT);
            eventSource.register((event) -> {
                results.add(event);
                receivedLatch.countDown();
            });

            eventSource.open();
            final boolean allTransmitted = transmitLatch.await(5000, TimeUnit.MILLISECONDS);
            final boolean allReceived = receivedLatch.await(5000, TimeUnit.MILLISECONDS);
            Assertions.assertTrue(allTransmitted);
            Assertions.assertTrue(allReceived);
            Assertions.assertEquals(30, results.size());
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWithJerseyApi() throws InterruptedException {
        final WebTarget endpoint = target().path("events");
        final EventSource eventSource = EventSource.target(endpoint).build();
        transmitLatch = new CountDownLatch(MSG_COUNT);
        final CountDownLatch receiveLatch = new CountDownLatch(MSG_COUNT);

        final List<Integer> results = new ArrayList<>();
        final EventListener listener = inboundEvent -> {
            try {
                results.add(inboundEvent.readData(Integer.class));
                receiveLatch.countDown();
                Assertions.assertEquals(INTEGER_SSE_NAME, inboundEvent.getName());
            } catch (ProcessingException ex) {
                throw new RuntimeException("Error when deserializing of data.", ex);
            }
        };
        eventSource.register(listener, INTEGER_SSE_NAME);
        eventSource.open();
        Assertions.assertTrue(transmitLatch.await(5000, TimeUnit.MILLISECONDS));
        Assertions.assertTrue(receiveLatch.await(5000, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(10, results.size());
    }


    @Test
    public void testWithEventSource() throws InterruptedException {
        transmitLatch = new CountDownLatch(2 * MSG_COUNT);
        final WebTarget endpoint = target().path("events");
        final SseEventSource eventSource = SseEventSource.target(endpoint).build();

        final CountDownLatch count1 = new CountDownLatch(3 * MSG_COUNT);
        final CountDownLatch count2 = new CountDownLatch(3 * MSG_COUNT);

        eventSource.register(new InboundHandler("consumer1", count1));
        eventSource.register(new InboundHandler("consumer2", count2));

        eventSource.open();
        final boolean sent = transmitLatch.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        Assertions.assertTrue(sent, "Awaiting for SSE message has timeout. Not all message were sent.");

        final boolean handled2 = count2.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        Assertions.assertTrue(handled2, "Awaiting for SSE message has timeout. Not all message were handled by eventSource2.");

        final boolean handled1 = count1.await(5 * getAsyncTimeoutMultiplier(), TimeUnit.SECONDS);
        Assertions.assertTrue(handled1, "Awaiting for SSE message has timeout. Not all message were handled by eventSource1.");

    }

    private class InboundHandler implements Consumer<InboundSseEvent> {
        private final CountDownLatch latch;
        private final String name;

        InboundHandler(final String name, final CountDownLatch latch) {
            this.latch = latch;
            this.name = name;
        }

        @Override
        public void accept(final InboundSseEvent inboundSseEvent) {
            try {
                if (INTEGER_SSE_NAME.equals(inboundSseEvent.getName())) {
                    final Integer data = inboundSseEvent.readData(Integer.class);
                    LOGGER.info(String.format("[%s] Integer data received: [id=%s name=%s comment=%s reconnectDelay=%d value=%d]",
                            name,
                            inboundSseEvent.getId(),
                            inboundSseEvent.getName(), inboundSseEvent.getComment(), inboundSseEvent.getReconnectDelay(), data));
                } else {
                    final String data = inboundSseEvent.readData();
                    LOGGER.info(String.format("[%s] String data received: [id=%s name=%s comment=%s reconnectDelay=%d value=%s]",
                            name,
                            inboundSseEvent.getId(),
                            inboundSseEvent.getName(), inboundSseEvent.getComment(), inboundSseEvent.getReconnectDelay(), data));
                }
                latch.countDown();
            } catch (final ProcessingException ex) {
                throw new RuntimeException("Error when deserializing the data.", ex);
            }
        }
    }
}
