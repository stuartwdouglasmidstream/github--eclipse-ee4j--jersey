/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity;

import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for JERSEY-1579.
 *
 * @author Jakub Podlesak
 */
public class EmptyRequestToEntityParamTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(EntityResource.class);
    }

    @Path("/")
    public static class EntityResource {

        @DELETE
        public void delete(Map<String, String> entity) {
            Assertions.assertNull(entity);
        }
    }

    @Test
    public void testEmptyRequest() {
        Response response = target().request().delete(Response.class);
        Assertions.assertEquals(204, response.getStatus());
    }
}
