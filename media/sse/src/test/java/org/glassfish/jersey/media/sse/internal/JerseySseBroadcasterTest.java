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

package org.glassfish.jersey.media.sse.internal;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseEventSink;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link jakarta.ws.rs.sse.SseBroadcaster} test.
 *
 * @author Adam Lindenthal
 */
public class JerseySseBroadcasterTest {

    private static final String TEST_EXCEPTION_MSG = "testException";

    @Test
    public void testOnErrorNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            try (JerseySseBroadcaster broadcaster = new JerseySseBroadcaster()) {

                broadcaster.onError(null);
            }
        });
    }

    @Test
    public void testOnCloseNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            try (JerseySseBroadcaster jerseySseBroadcaster = new JerseySseBroadcaster()) {

                jerseySseBroadcaster.onClose(null);
            }
        });
    }

    @Test
    public void testOnErrorFromOnNext() throws InterruptedException {
        try (JerseySseBroadcaster broadcaster = new JerseySseBroadcaster()) {

            final CountDownLatch latch = new CountDownLatch(1);


            broadcaster.onError((subscriber, throwable) -> {
                if (TEST_EXCEPTION_MSG.equals(throwable.getMessage())) {
                    latch.countDown();
                }
            });

            broadcaster.register(new SseEventSink() {
                @Override
                public boolean isClosed() {
                    return false;
                }

                @Override
                public CompletionStage<?> send(OutboundSseEvent event) {
                    throw new RuntimeException(TEST_EXCEPTION_MSG);
                }

                @Override
                public void close() {

                }
            });

            broadcaster.broadcast(new JerseySse().newEvent("ping"));
            Assertions.assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnClose() throws InterruptedException {
        try (JerseySseBroadcaster broadcaster = new JerseySseBroadcaster()) {

            final CountDownLatch latch = new CountDownLatch(1);

            final SseEventSink eventSink = new SseEventSink() {
                @Override
                public boolean isClosed() {
                    return false;
                }

                @Override
                public CompletionStage<?> send(OutboundSseEvent event) {
                    return null;
                }

                @Override
                public void close() {

                }
            };
            broadcaster.register(eventSink);

            broadcaster.onClose((s) -> {
                if (s.equals(eventSink)) {
                    latch.countDown();
                }
            });

            broadcaster.close();
            Assertions.assertTrue(latch.await(2000, TimeUnit.MILLISECONDS));
        }
    }

}
