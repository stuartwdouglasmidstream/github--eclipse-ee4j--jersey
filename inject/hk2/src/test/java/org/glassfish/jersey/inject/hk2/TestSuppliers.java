/*
 * Copyright (c) 2017, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.inject.hk2;

import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Set of suppliers to inject.
 */
public class TestSuppliers {

    static final String TEST = "Test";
    static final String OTHER_TEST = "OtherTest";

    public static class TargetSupplierBean {
        @Inject
        @Named(OTHER_TEST)
        public String obj;
    }

    public static class TargetSupplier {
        @Inject
        @Named(OTHER_TEST)
        public Supplier<String> supplier;
    }

    public static class TestSupplier implements Supplier<String> {
        @Override
        public String get() {
            return TEST;
        }
    }

    public static class OtherTestSupplier implements Supplier<String> {
        @Override
        public String get() {
            return OTHER_TEST;
        }
    }
}
