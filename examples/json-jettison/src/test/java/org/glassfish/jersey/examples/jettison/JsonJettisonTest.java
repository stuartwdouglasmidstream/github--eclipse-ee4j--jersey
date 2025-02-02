/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.jettison;

import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jakub Podlesak
 * @author Marek Potociar
 */
public class JsonJettisonTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);

        return App.createApp();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new JettisonFeature()).register(JaxbContextResolver.class);
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // reset static flights list
        target().path("flights/init").request("application/json").post(null);
    }

    /**
     * Test checks that the application.wadl is reachable.
     * <p/>
     */
    @Test
    public void testApplicationWadl() {
        String applicationWadl = target().path("application.wadl").request().get(String.class);
        assertTrue(applicationWadl.length() > 0, "Something wrong. Returned wadl length is not > 0");
    }

    /**
     * Test check GET on the "flights" resource in "application/json" format.
     */
    @Test
    public void testGetOnFlightsJSONFormat() {
        // get the initial representation
        Flights flights = target().path("flights").request("application/json").get(Flights.class);
        // check that there are two flight entries
        assertEquals(2, flights.getFlight().size(), "Expected number of initial entries not found");
    }

    /**
     * Test checks PUT on the "flights" resource in "application/json" format.
     */
    @Test
    public void testPutOnFlightsJSONFormat() {
        // get the initial representation
        Flights flights = target().path("flights")
                .request("application/json").get(Flights.class);
        // check that there are two flight entries
        assertEquals(2, flights.getFlight().size(), "Expected number of initial entries not found");

        // remove the second flight entry
        if (flights.getFlight().size() > 1) {
            flights.getFlight().remove(1);
        }

        // update the first entry
        flights.getFlight().get(0).setNumber(125);
        flights.getFlight().get(0).setFlightId("OK125");

        // and send the updated list back to the server
        target().path("flights").request().put(Entity.json(flights));

        // get the updated list out from the server:
        Flights updatedFlights = target().path("flights").request("application/json").get(Flights.class);
        //check that there is only one flight entry
        assertEquals(1, updatedFlights.getFlight().size(), "Remaining number of flight entries do not match the expected value");
        // check that the flight entry in retrieved list has FlightID OK!@%
        assertEquals("OK125", updatedFlights.getFlight().get(0).getFlightId(),
                "Retrieved flight ID doesn't match the expected value");
    }

    /**
     * Test checks GET on "flights" resource with mime-type "application/xml".
     */
    @Test
    public void testGetOnFlightsXMLFormat() {
        // get the initial representation
        Flights flights = target().path("flights").request("application/xml").get(Flights.class);
        // check that there are two flight entries
        assertEquals(2, flights.getFlight().size(), "Expected number of initial entries not found");
    }

    /**
     * Test checks PUT on "flights" resource with mime-type "application/xml".
     */
    @Test
    public void testPutOnFlightsXMLFormat() {
        // get the initial representation
        Flights flights = target().path("flights").request("application/XML").get(Flights.class);
        // check that there are two flight entries
        assertEquals(2, flights.getFlight().size(), "Expected number of initial entries not found");

        // remove the second flight entry
        if (flights.getFlight().size() > 1) {
            flights.getFlight().remove(1);
        }

        // update the first entry
        flights.getFlight().get(0).setNumber(125);
        flights.getFlight().get(0).setFlightId("OK125");

        // and send the updated list back to the server
        target().path("flights").request().put(Entity.xml(flights));

        // get the updated list out from the server:
        Flights updatedFlights = target().path("flights").request("application/XML").get(Flights.class);
        //check that there is only one flight entry
        assertEquals(1, updatedFlights.getFlight().size(), "Remaining number of flight entries do not match the expected value");
        // check that the flight entry in retrieved list has FlightID OK!@%
        assertEquals("OK125", updatedFlights.getFlight().get(0).getFlightId(),
                "Retrieved flight ID doesn't match the expected value");
    }

    /**
     * Test check GET on the "aircrafts" resource in "application/json" format.
     */
    @Test
    public void testGetOnAircraftsJSONFormat() {
        GenericType<List<AircraftType>> listOfAircrafts = new GenericType<List<AircraftType>>() {
        };
        // get the initial representation
        List<AircraftType> aircraftTypes = target().path("aircrafts").request("application/json").get(listOfAircrafts);
        // check that there are two aircraft type entries
        assertEquals(2, aircraftTypes.size(), "Expected number of initial aircraft types not found");
    }
}
