/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.inject;

import java.math.BigDecimal;
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Paul Sandoz
 * @author Pavel Bucek
 */
public class QueryParamSortedSetStringConstructorTest extends AbstractTest {

    @Path("/")
    public static class ResourceStringSortedSet {
        @GET
        public String doGetString(@QueryParam("args") SortedSet<BigDecimal> args) {
            assertTrue(args.contains(new BigDecimal("3.145")));
            assertTrue(args.contains(new BigDecimal("2.718")));
            assertTrue(args.contains(new BigDecimal("1.618")));
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSortedSetEmptyDefault {
        @GET
        public String doGetString(@QueryParam("args") SortedSet<BigDecimal> args) {
            assertEquals(0, args.size());
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSortedSetDefault {
        @GET
        public String doGetString(
                @QueryParam("args") @DefaultValue("3.145") SortedSet<BigDecimal> args) {
            assertTrue(args.contains(new BigDecimal("3.145")));
            return "content";
        }
    }

    @Path("/")
    public static class ResourceStringSortedSetDefaultOverride {
        @GET
        public String doGetString(
                @QueryParam("args") @DefaultValue("3.145") SortedSet<BigDecimal> args) {
            assertTrue(args.contains(new BigDecimal("2.718")));
            return "content";
        }
    }

    @Test
    public void testStringConstructorSortedSetGet() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSortedSet.class);

        _test("/?args=3.145&args=2.718&args=1.618", "application/stringSortedSet");
    }

    @Test
    public void testStringConstructorSortedSetNullDefault() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSortedSetEmptyDefault.class);

        _test("/");
    }

    @Test
    public void testStringConstructorSortedSetDefault() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSortedSetDefault.class);

        _test("/");
    }

    @Test
    public void testStringConstructorSortedSetDefaultOverride() throws ExecutionException, InterruptedException {
        initiateWebApplication(ResourceStringSortedSetDefaultOverride.class);

        _test("/?args=2.718");
    }
}
