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

package org.glassfish.jersey.tests.e2e.entity.filtering.json;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Feature;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.entity.filtering.PrimaryDetailedView;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.ComplexEntity;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.ComplexSubEntity;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.ComplexSubSubEntity;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
@Suite
@SelectClasses({
        JsonEntityFilteringScopesTest.JacksonFeatureJsonEntityFilteringScopesTest.class,
        JsonEntityFilteringScopesTest.MoxyJsonFeatureJsonEntityFilteringScopesTest.class
})
public class JsonEntityFilteringScopesTest {

    public static class MoxyJsonFeatureJsonEntityFilteringScopesTest extends JsonEntityFilteringScopesTemplateTest {
        public MoxyJsonFeatureJsonEntityFilteringScopesTest() {
            super(MoxyJsonFeature.class);
        }
    }

    public static class JacksonFeatureJsonEntityFilteringScopesTest extends JsonEntityFilteringScopesTemplateTest {
        public JacksonFeatureJsonEntityFilteringScopesTest() {
            super(JacksonFeature.class);
        }
    }

    public abstract static class JsonEntityFilteringScopesTemplateTest extends JerseyTest {
        public JsonEntityFilteringScopesTemplateTest(Class<? extends Feature> filteringProvider) {
            super(new ResourceConfig(Resource.class, EntityFilteringFeature.class).register(filteringProvider));

            enable(TestProperties.DUMP_ENTITY);
            enable(TestProperties.LOG_TRAFFIC);
        }

        /**
         * Primary -> Default -> Primary.
         */
        @Test
        public void testEntityFilteringScopes() throws Exception {
            final ComplexEntity entity = target().request().get(ComplexEntity.class);

            // ComplexEntity
            assertThat(entity.accessorTransient, is("propertyproperty"));
            assertThat(entity.getProperty(), is("property"));

            // ComplexSubEntity
            final ComplexSubEntity subEntity = entity.field;
            assertThat(subEntity, notNullValue());
            assertThat(subEntity.accessorTransient, is("fieldfield"));
            assertThat(subEntity.field, is("field"));

            // ComplexSubSubEntity
            final ComplexSubSubEntity subSubEntity = entity.field.getProperty();
            assertThat(subSubEntity.accessorTransient, is("fieldfield"));
            assertThat(subSubEntity.getProperty(), is("property"));
            assertThat(subSubEntity.field, nullValue());
        }
    }

    @Path("/")
    @Consumes("application/json")
    @Produces("application/json")
    public static class Resource {

        @GET
        @PrimaryDetailedView
        public ComplexEntity get() {
            return ComplexEntity.INSTANCE;
        }
    }
}
