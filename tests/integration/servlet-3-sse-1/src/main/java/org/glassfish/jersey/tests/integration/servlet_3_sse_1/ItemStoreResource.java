/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;

/**
 * A resource for storing named items.
 *
 * @author Marek Potociar
 */
@Path("items")
public class ItemStoreResource {
    private static final Logger LOGGER = Logger.getLogger(ItemStoreResource.class.getName());

    private static final ReentrantReadWriteLock storeLock = new ReentrantReadWriteLock();
    private static final LinkedList<String> itemStore = new LinkedList<String>();
    private static final SseBroadcaster broadcaster = new SseBroadcaster();

    private static volatile long reconnectDelay = 0;

    /**
     * List all stored items.
     *
     * @return list of all stored items.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String listItems() {
        try {
            storeLock.readLock().lock();
            return itemStore.toString();
        } finally {
            storeLock.readLock().unlock();
        }
    }

    /**
     * Receive & process commands sent by the test client that control the internal resource state.
     *
     * Following is the list of recognized commands:
     * <ul>
     * <li><b>disconnect</b> - disconnect all registered event streams.</li>
     * <li><b>reconnect now</b> - enable client reconnecting.</li>
     * <li><b>reconnect &lt;seconds&gt;</b> - disable client reconnecting.
     * Reconnecting clients will receive a HTTP 503 response with
     * {@value jakarta.ws.rs.core.HttpHeaders#RETRY_AFTER} set to the amount of
     * milliseconds specified.</li>
     * </ul>
     *
     * @param command command to be processed.
     * @return message about processing result.
     * @throws BadRequestException in case the command is not recognized or not specified.
     */
    @POST
    @Path("commands")
    public String processCommand(String command) {
        if (command == null || command.isEmpty()) {
            throw new BadRequestException("No command specified.");
        }

        if ("disconnect".equals(command)) {
            broadcaster.closeAll();
            return "Disconnected.";
        } else if (command.length() > "reconnect ".length() && command.startsWith("reconnect ")) {
            final String when = command.substring("reconnect ".length());
            try {
                reconnectDelay = "now".equals(when) ? 0 : Long.parseLong(when);
                return "Reconnect strategy updated: " + when;
            } catch (NumberFormatException ignore) {
                // ignored
            }
        }

        throw new BadRequestException("Command not recognized: '" + command + "'");
    }

    /**
     * Connect or re-connect to SSE event stream.
     *
     * @param lastEventId Value of custom SSE HTTP <tt>{@value SseFeature#LAST_EVENT_ID_HEADER}</tt> header.
     *                    Defaults to {@code -1} if not set.
     * @return new SSE event output stream representing the (re-)established SSE client connection.
     * @throws InternalServerErrorException in case replaying missed events to the reconnected output stream fails.
     * @throws ServiceUnavailableException  in case the reconnect delay is set to a positive value.
     */
    @GET
    @Path("events")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput itemEvents(
            @HeaderParam(SseFeature.LAST_EVENT_ID_HEADER) @DefaultValue("-1") int lastEventId) {
        final EventOutput eventOutput = new EventOutput();

        if (lastEventId >= 0) {
            LOGGER.info("Received last event id :" + lastEventId);

            // decide the reconnect handling strategy based on current reconnect delay value.
            final long delay = reconnectDelay;
            if (delay > 0) {
                LOGGER.info("Non-zero reconnect delay [" + delay + "] - responding with HTTP 503.");
                throw new ServiceUnavailableException(delay);
            } else {
                LOGGER.info("Zero reconnect delay - reconnecting.");
                replayMissedEvents(lastEventId, eventOutput);
            }
        }

        if (!broadcaster.add(eventOutput)) {
            LOGGER.severe("!!! Unable to add new event output to the broadcaster !!!");
            // let's try to force a 5s delayed client reconnect attempt
            throw new ServiceUnavailableException(5L);
        }

        return eventOutput;
    }

    private void replayMissedEvents(final int lastEventId, final EventOutput eventOutput) {
        try {
            storeLock.readLock().lock();
            final int firstUnreceived = lastEventId + 1;
            final int missingCount = itemStore.size() - firstUnreceived;
            if (missingCount > 0) {
                LOGGER.info("Replaying events - starting with id " + firstUnreceived);
                final ListIterator<String> it = itemStore.subList(firstUnreceived, itemStore.size()).listIterator();
                while (it.hasNext()) {
                    eventOutput.write(createItemEvent(it.nextIndex() + firstUnreceived, it.next()));
                }
            } else {
                LOGGER.info("No events to replay.");
            }
        } catch (IOException ex) {
            throw new InternalServerErrorException("Error replaying missed events", ex);
        } finally {
            storeLock.readLock().unlock();
        }
    }

    /**
     * Add new item to the item store.
     *
     * Invoking this method will fire 2 new SSE events - 1st about newly added item and 2nd about the new item store size.
     *
     * @param name item name.
     */
    @POST
    public void addItem(@FormParam("name") String name) {
        final int eventId;
        try {
            storeLock.writeLock().lock();
            eventId = itemStore.size();
            itemStore.add(name);
            // Broadcasting an un-named event with the name of the newly added item in data
            broadcaster.broadcast(createItemEvent(eventId, name));
            // Broadcasting a named "size" event with the current size of the items collection in data
            broadcaster.broadcast(new OutboundEvent.Builder().name("size").data(Integer.class, eventId + 1).build());
        } finally {
            storeLock.writeLock().unlock();
        }
    }

    private OutboundEvent createItemEvent(final int eventId, final String name) {
        Logger.getLogger(ItemStoreResource.class.getName()).info("Creating event id [" + eventId + "] name [" + name + "]");
        return new OutboundEvent.Builder().id("" + eventId).data(String.class, name).build();
    }
}
