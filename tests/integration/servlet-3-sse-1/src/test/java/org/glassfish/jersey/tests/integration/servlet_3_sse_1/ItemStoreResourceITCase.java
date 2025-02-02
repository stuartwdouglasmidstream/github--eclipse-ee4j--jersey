/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.servlet_3_sse_1;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.grizzly.connector.GrizzlyConnectorProvider;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.describedAs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Item store test.
 *
 * @author Marek Potociar
 */
@Disabled
public class ItemStoreResourceITCase extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(ItemStoreResourceITCase.class.getName());
    private static final int MAX_LISTENERS = 5;
    private static final int MAX_ITEMS = 10;


    @Override
    protected Application configure() {
        return new ItemStoreApp();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.property(ClientProperties.CONNECT_TIMEOUT, 15000)
                .property(ClientProperties.READ_TIMEOUT, 2000)
                .property(ClientProperties.ASYNC_THREADPOOL_SIZE, MAX_LISTENERS + 1)
                .connectorProvider(new GrizzlyConnectorProvider());
    }

    @Override
    protected URI getBaseUri() {
        final UriBuilder baseUriBuilder = UriBuilder.fromUri(super.getBaseUri());
        final boolean externalFactoryInUse = getTestContainerFactory() instanceof ExternalTestContainerFactory;
        return externalFactoryInUse ? baseUriBuilder.path("resources").build() : baseUriBuilder.build();
    }

    /**
     * Test the item addition, addition event broadcasting and item retrieval from {@link ItemStoreResource}.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testItemsStore() throws Exception {
        final List<String> items = Collections.unmodifiableList(Arrays.asList(
                "foo",
                "bar",
                "baz"));
        final WebTarget itemsTarget = target("items");
        final CountDownLatch latch = new CountDownLatch(items.size() * MAX_LISTENERS * 2); // countdown on all events
        final List<Queue<Integer>> indexQueues = new ArrayList<Queue<Integer>>(MAX_LISTENERS);
        final EventSource[] sources = new EventSource[MAX_LISTENERS];
        final AtomicInteger sizeEventsCount = new AtomicInteger(0);

        for (int i = 0; i < MAX_LISTENERS; i++) {
            final int id = i;
            final EventSource es = EventSource.target(itemsTarget.path("events"))
                    .named("SOURCE " + id).build();
            sources[id] = es;

            final Queue<Integer> indexes = new ConcurrentLinkedQueue<Integer>();
            indexQueues.add(indexes);

            es.register(new EventListener() {
                @Override
                @SuppressWarnings("MagicNumber")
                public void onEvent(InboundEvent inboundEvent) {
                    try {
                        if (inboundEvent.getName() == null) {
                            final String data = inboundEvent.readData();
                            LOGGER.info("[-i-] SOURCE " + id + ": Received event id=" + inboundEvent.getId() + " data=" + data);
                            indexes.add(items.indexOf(data));
                        } else if ("size".equals(inboundEvent.getName())) {
                            sizeEventsCount.incrementAndGet();
                        }
                    } catch (ProcessingException ex) {
                        LOGGER.log(Level.SEVERE, "[-x-] SOURCE " + id + ": Error getting event data.", ex);
                        indexes.add(-999);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        try {
            open(sources);

            for (String item : items) {
                postItem(itemsTarget, item);
            }

            assertTrue(latch.await((1000 + MAX_LISTENERS * EventSource.RECONNECT_DEFAULT) * getAsyncTimeoutMultiplier(),
                            TimeUnit.MILLISECONDS),
                    "Waiting to receive all events has timed out.");

            // need to force disconnect on server in order for EventSource.close(...) to succeed with HttpUrlConnection
            sendCommand(itemsTarget, "disconnect");
        } finally {
            close(sources);
        }

        String postedItems = itemsTarget.request().get(String.class);
        for (String item : items) {
            assertTrue(postedItems.contains(item), "Item '" + item + "' not stored on server.");
        }

        int queueId = 0;
        for (Queue<Integer> indexes : indexQueues) {
            for (int i = 0; i < items.size(); i++) {
                assertTrue(indexes.contains(i), "Event for '" + items.get(i) + "' not received in queue " + queueId);
            }
            assertEquals(items.size(), indexes.size(), "Not received the expected number of events in queue " + queueId);
            queueId++;
        }

        assertEquals(items.size() * MAX_LISTENERS, sizeEventsCount.get(), "Number of received 'size' events does not match.");
    }

    /**
     * Test the {@link EventSource} reconnect feature.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testEventSourceReconnect() throws Exception {
        final WebTarget itemsTarget = target("items");
        final CountDownLatch latch = new CountDownLatch(MAX_ITEMS * MAX_LISTENERS * 2); // countdown only on new item events
        final List<Queue<String>> receivedQueues = new ArrayList<Queue<String>>(MAX_LISTENERS);
        final EventSource[] sources = new EventSource[MAX_LISTENERS];

        for (int i = 0; i < MAX_LISTENERS; i++) {
            final int id = i;
            final EventSource es = EventSource.target(itemsTarget.path("events")).named("SOURCE " + id).build();
            sources[id] = es;

            final Queue<String> received = new ConcurrentLinkedQueue<String>();
            receivedQueues.add(received);

            es.register(new EventListener() {
                @Override
                public void onEvent(InboundEvent inboundEvent) {
                    try {
                        if (inboundEvent.getName() == null) {
                            latch.countDown();
                            final String data = inboundEvent.readData();
                            LOGGER.info("[-i-] SOURCE " + id + ": Received event id=" + inboundEvent.getId() + " data=" + data);
                            received.add(data);
                        }
                    } catch (ProcessingException ex) {
                        LOGGER.log(Level.SEVERE, "[-x-] SOURCE " + id + ": Error getting event data.", ex);
                        received.add("[data processing error]");
                    }
                }
            });
        }

        final String[] postedItems = new String[MAX_ITEMS * 2];
        try {
            open(sources);

            for (int i = 0; i < MAX_ITEMS; i++) {
                final String item = String.format("round-1-%02d", i);
                postItem(itemsTarget, item);
                postedItems[i] = item;
                sendCommand(itemsTarget, "disconnect");
                Thread.sleep(100);
            }

            final int reconnectDelay = 1;
            sendCommand(itemsTarget, "reconnect " + reconnectDelay);
            sendCommand(itemsTarget, "disconnect");

            Thread.sleep(reconnectDelay * 1000);

            for (int i = 0; i < MAX_ITEMS; i++) {
                final String item = String.format("round-2-%02d", i);
                postedItems[i + MAX_ITEMS] = item;
                postItem(itemsTarget, item);
            }

            sendCommand(itemsTarget, "reconnect now");

            assertTrue(latch.await((1 + MAX_LISTENERS * (MAX_ITEMS + 1) * reconnectDelay) * getAsyncTimeoutMultiplier(),
                    TimeUnit.SECONDS), "Waiting to receive all events has timed out.");

            // need to force disconnect on server in order for EventSource.close(...) to succeed with HttpUrlConnection
            sendCommand(itemsTarget, "disconnect");
        } finally {
            close(sources);
        }

        final String storedItems = itemsTarget.request().get(String.class);
        for (String item : postedItems) {
            assertThat("Posted item '" + item + "' stored on server", storedItems, containsString(item));
        }

        int sourceId = 0;
        for (Queue<String> queue : receivedQueues) {
            assertThat("Received events in source " + sourceId, queue,
                    describedAs("Collection containing %0", hasItems(postedItems), Arrays.asList(postedItems).toString()));
            assertThat("Size of received queue for source " + sourceId, queue.size(), equalTo(postedItems.length));
            sourceId++;
        }
    }

    private static void postItem(final WebTarget itemsTarget, final String item) {
        final Response response = itemsTarget.request().post(Entity.form(new Form("name", item)));
        assertEquals(204, response.getStatus(), "Posting new item has failed.");
        LOGGER.info("[-i-] POSTed item: '" + item + "'");
    }

    private static void open(final EventSource[] sources) {
        int i = 0;
        for (EventSource source : sources) {
            source.open();
            LOGGER.info("[-->] SOURCE " + i++ + " opened.");
        }
    }

    private static void close(final EventSource[] sources) {
        int i = 0;
        for (EventSource source : sources) {
            if (source.isOpen()) {
                assertTrue(source.close(1, TimeUnit.SECONDS), "Waiting to close a source has timed out.");
//                    source.close(100, TimeUnit.MILLISECONDS);
                LOGGER.info("[<--] SOURCE " + i++ + " closed.");
            }
        }
    }

    private static void sendCommand(final WebTarget itemsTarget, final String command) {
        final Response response = itemsTarget.path("commands").request().post(Entity.text(command));
        assertEquals(200, response.getStatus(), "'" + command + "' command has failed.");
        LOGGER.info("[-!-] COMMAND '" + command + "' has been processed.");
    }
}
