/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.util.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Configuration;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

/**
 * The default {@link org.glassfish.jersey.client.spi.ConnectorProvider connector provider} used for testing/benchmarking
 * purposes. The provided connector is {@link org.glassfish.jersey.test.util.client.LoopBackConnector} returns a response that
 * contains the same data (headers, entity) as the processed request.
 *
 * @author Michal Gajdos
 * @since 2.17
 */
public final class LoopBackConnectorProvider implements ConnectorProvider {

    @Override
    public Connector getConnector(final Client client, final Configuration config) {
        return new LoopBackConnector();
    }

    /**
     * Get a client configuration specific to the connector.
     *
     * @return a client configuration specific to the connector.
     */
    public static Configuration getClientConfig() {
        return new ClientConfig().connectorProvider(new LoopBackConnectorProvider());
    }
}
