/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.jsonp;

import java.util.List;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jakub Podlesak
 */
public class JsonWithPaddingTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return App.createApp();
    }

    /**
     * Test checks that the application.wadl is reachable.
     * <p/>
     */
    @Test
    public void testApplicationWadl() {
        WebTarget target = target();
        String applicationWadl = target.path("application.wadl").request().get(String.class);
        assertTrue(applicationWadl.length() > 0, "Something wrong. Returned wadl length is not > 0");
    }

    /**
     * Test check GET on the "changes" resource in "application/json" format.
     */
    @Test
    public void testGetOnChangesJSONFormat() {
        WebTarget target = target();
        GenericType<List<ChangeRecordBean>> genericType = new GenericType<List<ChangeRecordBean>>() {};
        // get the initial representation
        List<ChangeRecordBean> changes = target.path("changes").request("application/json").get(genericType);
        // check that there are two changes entries
        assertEquals(5, changes.size(), "Expected number of initial changes not found");
    }

    /**
     * Test check GET on the "changes" resource in "application/xml" format.
     */
    @Test
    public void testGetOnLatestChangeXMLFormat() {
        WebTarget target = target();
        ChangeRecordBean lastChange = target.path("changes/latest").request("application/xml").get(ChangeRecordBean.class);
        assertEquals(1, lastChange.linesChanged);
    }

    /**
     * Test check GET on the "changes" resource in "application/javascript" format.
     */
    @Test
    public void testGetOnLatestChangeJavascriptFormat() {
        WebTarget target = target();
        String js = target.path("changes").request("application/x-javascript").get(String.class);
        assertTrue(js.startsWith("callback"));
    }

    @Test
    public void testGetOnLatestChangeJavascriptFormatDifferentCallback() {
        WebTarget target = target();
        String js = target.path("changes").queryParam("__callback", "parse").request("application/x-javascript")
                .get(String.class);
        assertTrue(js.startsWith("parse"));
    }
}
