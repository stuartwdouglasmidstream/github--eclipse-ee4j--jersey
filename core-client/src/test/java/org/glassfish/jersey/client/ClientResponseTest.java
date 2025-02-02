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

package org.glassfish.jersey.client;

import org.glassfish.jersey.message.internal.InboundMessageContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ReaderInterceptor;
import java.io.InputStream;

public class ClientResponseTest {

    @Test
    public void testHasEntityWhenNoEntity() {
        final InboundMessageContext inboundMessageContext = new InboundMessageContext(new ClientConfig()) {
            @Override
            protected Iterable<ReaderInterceptor> getReaderInterceptors() {
                return null;
            }
        };

        Assertions.assertFalse(inboundMessageContext.hasEntity());

        inboundMessageContext.bufferEntity();
        Assertions.assertFalse(inboundMessageContext.hasEntity());
    }

    @Test
    public void testHasEntity() {
        final ClientRequestFilter abortFilter = requestContext -> requestContext.abortWith(Response.ok("hello").build());
        try (Response r = ClientBuilder.newClient().register(abortFilter).target("http://localhost:8080").request().get()) {
            Assertions.assertTrue(r.hasEntity());

            r.bufferEntity();
            Assertions.assertTrue(r.hasEntity());

            final String s = r.readEntity(String.class);
            Assertions.assertTrue(r.hasEntity());

            final InputStream bufferedEntityStream = r.readEntity(InputStream.class);
            Assertions.assertNotNull(bufferedEntityStream);
            Assertions.assertTrue(r.hasEntity());
        }
    }
}
