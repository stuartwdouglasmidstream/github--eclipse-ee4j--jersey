/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.sseitemstore.jaxrs;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.sse.SseEventSource;

import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
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
public class JaxrsItemStoreResourceTest extends JerseyTest {

    private static final int RECONNECT_DEFAULT = 500;
    private static final Logger LOGGER = Logger.getLogger(JaxrsItemStoreResourceTest.class.getName());
    private static final int MAX_LISTENERS = 5;
    private static final int MAX_ITEMS = 10;

    private final ExecutorService executorService =
            Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("JaxrsItemStoreResourceTest-%d").build());
    private final AtomicReference<Client> client = new AtomicReference<>(null);

    @Override
    protected Application configure() {
        return new JaxrsItemStoreApp();
    }

    protected void configureClient(ClientConfig config) {
        // using AHC as a test client connector to avoid issues with HttpUrlConnection socket management.
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        // adjusting max. connections just to be safe - the testEventSourceReconnect is quite greedy...
        cm.setMaxTotal(MAX_LISTENERS * MAX_ITEMS);
        cm.setDefaultMaxPerRoute(MAX_LISTENERS * MAX_ITEMS);

        config.property(ApacheClientProperties.CONNECTION_MANAGER, cm)
                .property(ClientProperties.READ_TIMEOUT, 2000)
                .connectorProvider(new ApacheConnectorProvider());
    }

    @Override
    protected Client getClient() {
        if (client.get() == null) {
            ClientConfig clientConfig = new ClientConfig();
            configureClient(clientConfig);
            client.compareAndSet(null,
                                 ClientBuilder.newBuilder()
                                              .withConfig(clientConfig)
                                              .executorService(executorService).build());
        }

        return client.get();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        client.get().close();
        executorService.shutdown();
    }

    @Override
    protected URI getBaseUri() {
        final UriBuilder baseUriBuilder = UriBuilder.fromUri(super.getBaseUri()).path("sse-item-store-jaxrs-webapp");
        final boolean externalFactoryInUse = getTestContainerFactory() instanceof ExternalTestContainerFactory;
        return externalFactoryInUse ? baseUriBuilder.path("resources").build() : baseUriBuilder.build();
    }

    /**
     * Test the item addition, addition event broadcasting and item retrieval from {@link JaxrsItemStoreResource}.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testItemsStore() throws Exception {
        final List<String> items = Collections.unmodifiableList(Arrays.asList("foo", "bar", "baz"));
        final WebTarget itemsTarget = target("resources").path("items");
        final CountDownLatch latch = new CountDownLatch(items.size() * MAX_LISTENERS * 2); // countdown on all events
        final List<Queue<Integer>> indexQueues = new ArrayList<>(MAX_LISTENERS);
        final SseEventSource[] sources = new SseEventSource[MAX_LISTENERS];
        final AtomicInteger sizeEventsCount = new AtomicInteger(0);

        for (int i = 0; i < MAX_LISTENERS; i++) {
            final int id = i;
            final SseEventSource es = SseEventSource.target(itemsTarget.path("events")).build();
            sources[id] = es;

            final Queue<Integer> indexes = new ConcurrentLinkedQueue<>();
            indexQueues.add(indexes);

            es.register(inboundEvent -> {
                try {
                    if (null == inboundEvent.getName()) {
                        final String data = inboundEvent.readData();
                        LOGGER.info("[-i-] SOURCE " + id + ": Received event id=" + inboundEvent.getId() + " data=" + data);
                        indexes.add(items.indexOf(data));
                    } else if ("size".equals(inboundEvent.getName())) {
                        sizeEventsCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "[-x-] SOURCE " + id + ": Error getting event data.", ex);
                    indexes.add(-999);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            open(sources);
            items.forEach((item) -> postItem(itemsTarget, item));

            assertTrue(latch.await((1000 + MAX_LISTENERS * RECONNECT_DEFAULT) * getAsyncTimeoutMultiplier(),
                    TimeUnit.MILLISECONDS), "Waiting to receive all events has timed out.");

            // need to force disconnect on server in order for EventSource.close(...) to succeed with HttpUrlConnection
            sendCommand(itemsTarget, "disconnect");
        } finally {
            close(sources);
        }

        String postedItems = itemsTarget.request().get(String.class);
        items.forEach((item) -> assertTrue(postedItems.contains(item), "Item '" + item + "' not stored on server."));

        final AtomicInteger queueId = new AtomicInteger(0);
        indexQueues.forEach((indexes) -> {
            for (int i = 0; i < items.size(); i++) {
                assertTrue(indexes.contains(i), "Event for '" + items.get(i) + "' not received in queue " + queueId.get());
            }
            assertEquals(items.size(), indexes.size(), "Not received the expected number of events in queue " + queueId.get());
            queueId.incrementAndGet();
        });

        assertEquals(items.size() * MAX_LISTENERS, sizeEventsCount.get(), "Number of received 'size' events does not match.");
    }

    /**
     * Test the {@link SseEventSource} reconnect feature.
     *
     * @throws Exception in case of a test failure.
     */
    @Test
    public void testEventSourceReconnect() throws Exception {
        final WebTarget itemsTarget = target("resources").path("items");
        final CountDownLatch latch = new CountDownLatch(MAX_ITEMS * MAX_LISTENERS * 2); // countdown only on new item events
        final List<Queue<String>> receivedQueues = new ArrayList<>(MAX_LISTENERS);
        final SseEventSource[] sources = new SseEventSource[MAX_LISTENERS];

        for (int i = 0; i < MAX_LISTENERS; i++) {
            final int id = i;
            final SseEventSource es = SseEventSource.target(itemsTarget.path("events"))
                    .reconnectingEvery(1, TimeUnit.MILLISECONDS).build();
            sources[id] = es;

            final Queue<String> received = new ConcurrentLinkedQueue<>();
            receivedQueues.add(received);

            es.register(inboundEvent -> {
                try {
                    if (null == inboundEvent.getName()) {
                        final String data = inboundEvent.readData();
                        LOGGER.info("[-i-] SOURCE " + id + ": Received event id=" + inboundEvent.getId() + " data=" + data);
                        received.add(data);
                        latch.countDown();
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "[-x-] SOURCE " + id + ": Error getting event data.", ex);
                    received.add("[data processing error]");
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
                Thread.sleep(200);
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

    private static void open(final SseEventSource[] sources) {
        Arrays.stream(sources).forEach(SseEventSource::open);
    }

    private static void close(final SseEventSource[] sources) {
        int i = 0;
        for (SseEventSource source : sources) {
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
