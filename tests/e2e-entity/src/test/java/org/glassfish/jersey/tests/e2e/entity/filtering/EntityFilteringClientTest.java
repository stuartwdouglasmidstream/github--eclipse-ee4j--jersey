/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity.filtering;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.inject.CustomAnnotationLiteral;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.ManyFilteringsOnClassEntity;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.OneFilteringOnClassEntity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityFilteringClientTest extends EntityFilteringTest {

    public static final MediaType ENTITY_FILTERING = new MediaType("entity", "filtering");

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig()
                // Resources.
                .register(Resource.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(EntityFilteringFeature.class).register(FilteringMessageBodyProvider.class);
    }

    @Path("/")
    @Consumes("entity/filtering")
    @Produces("entity/filtering")
    public static class Resource {

        @POST
        public String post(final String value) {
            return value;
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testEntityAnnotationsPrimaryView() throws Exception {
        final ClientConfig config = new ClientConfig()
                .property(EntityFilteringFeature.ENTITY_FILTERING_SCOPE, PrimaryDetailedView.Factory.get());
        configureClient(config);

        final String fields = ClientBuilder.newClient(config)
                .target(getBaseUri())
                .request()
                .post(Entity.entity(
                                new OneFilteringOnClassEntity(),
                                ENTITY_FILTERING,
                                new Annotation[] {PrimaryDetailedView.Factory.get()}),
                        String.class);

        assertSameFields(fields, "field,accessor,property,subEntities.field2,subEntities.property2,subEntities.property1,"
                + "subEntities.field1,defaultEntities.field,defaultEntities.property");
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testEntityAnnotationsDefaultView() throws Exception {
        final String fields = target()
                .request()
                .post(Entity.entity(new OneFilteringOnClassEntity(),
                                ENTITY_FILTERING,
                                new Annotation[] {new DefaultFilteringScope()}),
                        String.class);

        assertThat(fields, equalTo(""));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testEntityAnnotationsInvalidView() throws Exception {
        final String fields = target()
                .request()
                .post(Entity.entity(
                                new OneFilteringOnClassEntity(),
                                ENTITY_FILTERING,
                                new Annotation[] {CustomAnnotationLiteral.INSTANCE}),
                        String.class);

        assertThat(fields, equalTo(""));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testConfigurationPrimaryView() throws Exception {
        testConfiguration("field,accessor,property,subEntities.field2,subEntities.property2,subEntities.property1,"
                + "subEntities.field1,defaultEntities.field,defaultEntities.property", PrimaryDetailedView.Factory.get());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testConfigurationDefaultView() throws Exception {
        testConfiguration("", new DefaultFilteringScope());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testConfigurationMultipleViews() throws Exception {
        testConfiguration("field,accessor,property,subEntities.field2,subEntities.property2,subEntities.property1,"
                        + "subEntities.field1,defaultEntities.field,defaultEntities.property", PrimaryDetailedView.Factory.get(),
                CustomAnnotationLiteral.INSTANCE);
    }

    private void testConfiguration(final String expected, final Annotation... annotations) {
        final ClientConfig config = new ClientConfig()
                .property(EntityFilteringFeature.ENTITY_FILTERING_SCOPE, annotations.length == 1 ? annotations[0] : annotations);
        configureClient(config);

        final String fields = ClientBuilder.newClient(config)
                .target(getBaseUri())
                .request()
                .post(Entity.entity(new OneFilteringOnClassEntity(), ENTITY_FILTERING), String.class);

        assertSameFields(fields, expected);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testInvalidConfiguration() throws Exception {
        final ClientConfig config = new ClientConfig()
                .property(EntityFilteringFeature.ENTITY_FILTERING_SCOPE, "invalid_value");
        configureClient(config);

        final String fields =
                ClientBuilder.newClient(config)
                        .target(getBaseUri())
                        .request()
                        .post(Entity.entity(new OneFilteringOnClassEntity(), ENTITY_FILTERING), String.class);

        assertThat(fields, equalTo(""));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testEntityAnnotationsOverConfiguration() throws Exception {
        final ClientConfig config = new ClientConfig()
                .property(EntityFilteringFeature.ENTITY_FILTERING_SCOPE, SecondaryDetailedView.Factory.get());
        configureClient(config);

        final String fields = ClientBuilder.newClient(config)
                .target(getBaseUri())
                .request()
                .post(Entity.entity(
                                new ManyFilteringsOnClassEntity(),
                                ENTITY_FILTERING,
                                new Annotation[] {PrimaryDetailedView.Factory.get()}),
                        String.class);

        assertSameFields(fields, "field,accessor,property,manyEntities.property1,manyEntities.field1,oneEntities.field2,"
                + "oneEntities.property2,oneEntities.property1,oneEntities.field1,defaultEntities.field,defaultEntities"
                + ".property");
    }
}
