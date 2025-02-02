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

package org.glassfish.jersey.jackson.internal.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;

import java.util.Optional;

@Path("/entity/")
public final class Jaxb2ServiceTest {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/simple")
    public final EntityTest simple() {
        return new EntityTest("Hello", "World");
    }

    private static final class EntityTest {

        private final String name;

        private final String value;

        EntityTest(final String name, final String value) {
            this.name = name;
            this.value = value;
        }

        @XmlElement(name = "jaxb")
        @JsonGetter("name")
        public final String getName() {
            return name;
        }

        @JsonGetter("value")
        public final Optional<String> getValue() {
            return Optional.ofNullable(value);
        }
    }
}
