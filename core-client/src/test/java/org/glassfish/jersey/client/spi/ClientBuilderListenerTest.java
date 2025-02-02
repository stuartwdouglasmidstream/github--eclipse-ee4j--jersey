/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.spi;

import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

public class ClientBuilderListenerTest {

    public static final String PROPERTY_NAME = "ClientBuilderListenerProperty";

    @Priority(Priorities.USER + 1000)
    public static class FirstClientBuilderListener implements ClientBuilderListener {
        @Override
        public void onNewBuilder(ClientBuilder builder) {
            builder.withConfig(new ClientConfig().property(PROPERTY_NAME, 60));
        }
    }

    public static class SecondClientBuilderListener implements ClientBuilderListener {
        @Override
        public void onNewBuilder(ClientBuilder builder) {
            builder.withConfig(new ClientConfig().property(PROPERTY_NAME, 50));
        }
    }

    @Priority(Priorities.USER + 2000)
    public static class ThirdClientBuilderListener implements ClientBuilderListener {
        @Override
        public void onNewBuilder(ClientBuilder builder) {
            builder.withConfig(new ClientConfig().property(PROPERTY_NAME, 70));
        }
    }

    @Test
    public void testClientBuilderListener() {
        Client client = ClientBuilder.newClient();
        Assertions.assertEquals(70, client.getConfiguration().getProperty(PROPERTY_NAME));
    }

}
