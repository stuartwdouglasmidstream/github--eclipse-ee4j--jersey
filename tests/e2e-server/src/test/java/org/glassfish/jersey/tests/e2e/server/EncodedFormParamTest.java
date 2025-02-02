/*
 * Copyright (c) 2015, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proper encoding of Form params
 *
 * @author Petr Bouda
 */
public class EncodedFormParamTest extends JerseyTest {

    @Path("encoded")
    public static class UrlEncodedResource {

        @Encoded
        @FormParam("name")
        private String name;

        @FormParam("name")
        private String otherName;

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public String get(@FormParam("name") List<String> name)  {
            return name.toString() + " " + this.name;
        }

    }

    @Override
    protected Application configure() {
        return new ResourceConfig(UrlEncodedResource.class);
    }

    @Test
    public void testEncodedParam() {
        Response result = target().path("encoded").request()
                .post(Entity.entity("name&name=George", MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals("[null, George] null", result.readEntity(String.class));
    }
}

