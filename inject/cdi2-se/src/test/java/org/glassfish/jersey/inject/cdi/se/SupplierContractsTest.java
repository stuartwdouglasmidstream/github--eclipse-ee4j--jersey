/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.inject.cdi.se;

import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Singleton;

import org.glassfish.jersey.internal.inject.InjectionManager;

import org.jboss.weld.exceptions.DeploymentException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that {@link java.util.function.Supplier} can contain multiple contracts.
 *
 * @author Petr Bouda
 */
@Vetoed
public class SupplierContractsTest {

    private InjectionManager injectionManager;

    @BeforeEach
    public void setup() {
        injectionManager = BindingTestHelper.createInjectionManager();
    }

    @AfterEach
    public void teardown() {
        injectionManager.shutdown();
    }


    @Test
    public void testClassFactoryInstanceInterface() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class).to(Greeting.class);
            binder.bindAsContract(Conversation.class);
        });

        Conversation conversation = injectionManager.getInstance(Conversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.greetingSupplier.get());
    }

    @Test
    public void testClassFactoryInstanceImplementation() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class).to(CzechGreeting.class);
            binder.bindAsContract(CzechConversation.class);
        });

        CzechConversation conversation = injectionManager.getInstance(CzechConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.greetingSupplier.get());
    }

    @Test
    public void testInstanceFactoryInstanceInterface() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting()).to(Greeting.class);
            binder.bindAsContract(Conversation.class);
        });

        Conversation conversation = injectionManager.getInstance(Conversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.greetingSupplier.get());
    }

    @Test
    public void testInstanceFactoryInstanceImplementation() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting()).to(CzechGreeting.class);
            binder.bindAsContract(CzechConversation.class);
        });

        CzechConversation conversation = injectionManager.getInstance(CzechConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.greetingSupplier.get());
    }

    @Test
    public void testClassFactoryMultipleContracts() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class)
                    .to(Greeting.class)
                    .to(Printable.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertNotSame(conversation.greeting, conversation.printable);
        assertNotSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testClassFactorySingletonMultipleContracts() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class, Singleton.class)
                    .to(Greeting.class)
                    .to(Printable.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertNotSame(conversation.greeting, conversation.printable);
        assertSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testClassFactoryMultipleContractsSingleton() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(SupplierGreeting.class)
                    .to(Greeting.class)
                    .to(Printable.class)
                    .in(Singleton.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertSame(conversation.greeting, conversation.printable);
        assertNotSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testInstanceFactoryMultipleContracts() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting())
                    .to(Greeting.class)
                    .to(Printable.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertNotSame(conversation.greeting, conversation.printable);
        assertSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testInstanceFactoryMultipleContractsSingleton() {
        BindingTestHelper.bind(injectionManager, binder -> {
            binder.bindFactory(new SupplierGreeting())
                    .to(Greeting.class)
                    .to(Printable.class)
                    .in(Singleton.class);
            binder.bindAsContract(PrintableConversation.class);
        });

        PrintableConversation conversation = injectionManager.getInstance(PrintableConversation.class);
        assertNotNull(conversation.greeting);
        assertNotNull(conversation.printable);
        assertNotNull(conversation.greetingSupplier);
        assertNotNull(conversation.printableSupplier);

        assertSame(conversation.greeting, conversation.printable);
        assertSame(conversation.greetingSupplier, conversation.printableSupplier);
    }

    @Test
    public void testClassFactoryFailedWrongImplementation() {
        assertThrows(DeploymentException.class, () -> {
            BindingTestHelper.bind(injectionManager, binder -> {
                binder.bindFactory(SupplierGreeting.class).to(EnglishGreeting.class);
                binder.bindAsContract(Conversation.class);
            });

            injectionManager.getInstance(Conversation.class);
        });
    }

    @Test
    public void testInstanceFactoryFailsWrongImplementation() {
        assertThrows(DeploymentException.class, () -> {
            BindingTestHelper.bind(injectionManager, binder -> {
                binder.bindFactory(new SupplierGreeting()).to(EnglishGreeting.class);
                binder.bindAsContract(Conversation.class);
            });

            injectionManager.getInstance(Conversation.class);
        });
    }

    @Test
    public void testFailsImplementationButInterfaceExpected() {
        assertThrows(DeploymentException.class, () -> {
            BindingTestHelper.bind(injectionManager, binder -> {
                binder.bindFactory(new SupplierGreeting()).to(CzechGreeting.class);
                binder.bindAsContract(Conversation.class);
            });

            injectionManager.getInstance(Conversation.class);
        });
    }
}
