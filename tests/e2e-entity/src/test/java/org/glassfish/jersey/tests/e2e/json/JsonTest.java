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

package org.glassfish.jersey.tests.e2e.json;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import jakarta.xml.bind.JAXBContext;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.util.JdkVersion;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.jettison.JettisonConfig;
import org.glassfish.jersey.jettison.JettisonJaxbContext;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common functionality for JSON tests that are using multiple JSON providers (e.g. MOXy, Jackson, Jettison).
 *
 * @author Michal Gajdos
 */
public abstract class JsonTest extends JerseyTest {

    private static final String PKG_NAME = "org/glassfish/jersey/tests/e2e/json/entity/";
    private static final Logger LOGGER = Logger.getLogger(JsonTest.class.getName());

    /**
     * Helper class representing configuration for one test case.
     */
    protected static final class JsonTestSetup {

        private final JsonTestProvider jsonProvider;
        private final Class<?>[] testClasses;

        protected JsonTestSetup(final Class<?> testClass, final JsonTestProvider jsonProvider) {
            this(new Class<?>[] {testClass}, jsonProvider);
        }

        protected JsonTestSetup(final Class<?>[] testClasses, final JsonTestProvider jsonProvider) {
            this.testClasses = testClasses;
            this.jsonProvider = jsonProvider;
        }

        public JsonTestProvider getJsonProvider() {
            return jsonProvider;
        }

        public Set<Object> getProviders() {
            return jsonProvider.getProviders();
        }

        public Class<?> getEntityClass() {
            return testClasses[0];
        }

        public Class<?>[] getTestClasses() {
            return testClasses;
        }

        public Object getTestEntity() throws Exception {
            return getEntityClass().getDeclaredMethod("createTestInstance").invoke(null);
        }
    }

    @Provider
    private static final class JAXBContextResolver implements ContextResolver<JAXBContext> {

        private final JAXBContext context;
        private final Set<Class<?>> types;

        public JAXBContextResolver(final JettisonConfig jsonConfiguration, final Class<?>[] classes,
                                   final boolean forMoxyProvider) throws Exception {
            this.types = new HashSet<>(Arrays.asList(classes));

            if (jsonConfiguration != null) {
                this.context = new JettisonJaxbContext(jsonConfiguration, classes);
            } else {
                this.context = forMoxyProvider
                        ? JAXBContextFactory.createContext(classes, new HashMap()) : JAXBContext.newInstance(classes);
            }
        }

        @Override
        public JAXBContext getContext(final Class<?> objectType) {
            return (types.contains(objectType)) ? context : null;
        }
    }

    private final JsonTestSetup jsonTestSetup;

    /**
     * Creates and configures a JAX-RS {@link Application} for given {@link JsonTestSetup}. The {@link Application} will
     * contain one resource that can be accessed via {@code POST} method at {@code <jsonProviderName>/<entityClassName>} path.
     * The resource also checks whether is the incoming JSON same as the one stored in a appropriate file.
     *
     * @param jsonTestSetup configuration to create a JAX-RS {@link Application} for.
     * @return an {@link Application} instance.
     */
    private static Application configureJaxrsApplication(final JsonTestSetup jsonTestSetup) {
        final Resource.Builder resourceBuilder = Resource.builder();

        final String providerName = getProviderPathPart(jsonTestSetup);
        final String testName = getEntityPathPart(jsonTestSetup);

        resourceBuilder
                .path("/" + providerName + "/" + testName)
                .addMethod("POST")
                .consumes(MediaType.APPLICATION_JSON_TYPE)
                .produces(MediaType.APPLICATION_JSON_TYPE)
                .handledBy(new Inflector<ContainerRequestContext, Response>() {

                    @Override
                    public Response apply(final ContainerRequestContext containerRequestContext) {
                        final ContainerRequest containerRequest = (ContainerRequest) containerRequestContext;

                        // Check if the JSON is the same as in the previous version.
                        containerRequest.bufferEntity();
                        try {
                            String json = JsonTestHelper.getResourceAsString(PKG_NAME,
                                    providerName + "_" + testName + (moxyJaxbProvider() || runningOnJdk7AndLater() ? "_MOXy" : "")
                                            + ".json").trim();

                            final InputStream entityStream = containerRequest.getEntityStream();
                            String retrievedJson = JsonTestHelper.getEntityAsString(entityStream).trim();
                            entityStream.reset();

                            // JAXB-RI and MOXy generate namespace prefixes differently - unify them (ns1/ns2 into ns0)
                            if (jsonTestSetup.getJsonProvider() instanceof JsonTestProvider.JettisonBadgerfishJsonTestProvider) {
                                if (retrievedJson.contains("\"ns1\"")) {
                                    json = json.replace("ns1", "ns0");
                                    retrievedJson = retrievedJson.replace("ns1", "ns0");
                                } else if (retrievedJson.contains("\"ns2\"")) {
                                    json = json.replace("ns2", "ns0");
                                    retrievedJson = retrievedJson.replace("ns2", "ns0");
                                }
                            }

                            if (!json.equals(retrievedJson)) {
                                LOGGER.log(Level.SEVERE, "Expected: " + json);
                                LOGGER.log(Level.SEVERE, "Actual:   " + retrievedJson);

                                return Response.ok("{\"error\":\"JSON values doesn't match.\"}").build();
                            }
                        } catch (final IOException e) {
                            return Response.ok("{\"error\":\"Cannot find original JSON file.\"}").build();
                        }

                        final Object testBean = containerRequest.readEntity(jsonTestSetup.getEntityClass());
                        return Response.ok(testBean).build();
                    }

                });

        final ResourceConfig resourceConfig = new ResourceConfig()
                .registerResources(resourceBuilder.build())
                .register(jsonTestSetup.getJsonProvider().getFeature());

        resourceConfig.registerInstances(getJaxbContextResolver(jsonTestSetup));

        if (jsonTestSetup.getProviders() != null) {
            resourceConfig.registerInstances(jsonTestSetup.getProviders());
        }

        return resourceConfig;
    }

    private static JAXBContextResolver getJaxbContextResolver(final JsonTestSetup jsonTestSetup) {
        try {
            return createJaxbContextResolver(jsonTestSetup.getJsonProvider(), jsonTestSetup.getTestClasses());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean runningOnJdk7AndLater() {
        final String javaVersion = AccessController.doPrivileged(PropertiesHelper.getSystemProperty("java.version"));
        final JdkVersion jdkVersion = JdkVersion.parseVersion(javaVersion);
        return (jdkVersion.getMajor() == 1 && jdkVersion.getMinor() >= 7) || (jdkVersion.getMajor() > 8);
    }

    private static boolean moxyJaxbProvider() {
        return "org.eclipse.persistence.jaxb.JAXBContextFactory".equals(
                AccessController.doPrivileged(PropertiesHelper.getSystemProperty("jakarta.xml.bind.JAXBContext")));
    }

    /**
     * Returns entity path part for given {@link JsonTestSetup} (based on the name of the entity).
     *
     * @return entity path part.
     */
    protected static String getEntityPathPart(final JsonTestSetup jsonTestSetup) {
        return jsonTestSetup.getEntityClass().getSimpleName();
    }

    /**
     * Creates new {@link ContextResolver} of {@link JAXBContext} instance for given {@link JsonTestProvider} and an entity
     * class.
     *
     * @param jsonProvider provider to create a context resolver for.
     * @param clazz        JAXB element class for JAXB context.
     * @return an instance of JAXB context resolver.
     * @throws Exception if the creation of {@code JAXBContextResolver} fails.
     */
    protected static JAXBContextResolver createJaxbContextResolver(final JsonTestProvider jsonProvider,
                                                                   final Class<?> clazz) throws Exception {
        return createJaxbContextResolver(jsonProvider, new Class<?>[] {clazz});
    }

    /**
     * Creates new {@link ContextResolver} of {@link JAXBContext} instance for given {@link JsonTestProvider} and an entity
     * classes.
     *
     * @param jsonProvider provider to create a context resolver for.
     * @param classes      JAXB element classes for JAXB context.
     * @return an instance of JAXB context resolver.
     * @throws Exception if the creation of {@code JAXBContextResolver} fails.
     */
    protected static JAXBContextResolver createJaxbContextResolver(final JsonTestProvider jsonProvider, final Class<?>[] classes)
            throws Exception {
        return new JAXBContextResolver(jsonProvider.getConfiguration(), classes,
                jsonProvider instanceof JsonTestProvider.MoxyJsonTestProvider);
    }

    JsonTest(final JsonTestSetup jsonTestSetup) throws Exception {
        super(configureJaxrsApplication(jsonTestSetup));
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        this.jsonTestSetup = jsonTestSetup;
    }

    /**
     * Returns entity path part for current test case (based on the name of the entity).
     *
     * @return entity path part.
     */
    protected String getEntityPathPart() {
        return getEntityPathPart(jsonTestSetup);
    }

    /**
     * Returns provider path part for current test case (based on the name of the {@link JsonTestProvider}).
     *
     * @return provider path part.
     */
    protected String getProviderPathPart() {
        return getProviderPathPart(jsonTestSetup);
    }

    /**
     * Returns provider path part for given {@link JsonTestSetup} (based on the name of the {@link JsonTestProvider}).
     *
     * @return provider path part.
     */
    protected static String getProviderPathPart(final JsonTestSetup jsonTestSetup) {
        return jsonTestSetup.jsonProvider.getClass().getSimpleName();
    }

    protected JsonTestSetup getJsonTestSetup() {
        return jsonTestSetup;
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(getJsonTestSetup().getJsonProvider().getFeature());

        config.register(getJaxbContextResolver(jsonTestSetup));

        // Register additional providers.
        if (getJsonTestSetup().getProviders() != null) {
            for (final Object provider : getJsonTestSetup().getProviders()) {
                config.register(provider);
            }
        }
    }

    @Test
    public void test() throws Exception {
        final Object entity = getJsonTestSetup().getTestEntity();

        final Object receivedEntity = target()
                .path(getProviderPathPart())
                .path(getEntityPathPart())
                .request("application/json; charset=UTF-8")
                .post(Entity.entity(entity, "application/json; charset=UTF-8"), getJsonTestSetup().getEntityClass());

        // Print out configuration for this test case as there is no way to rename generated JUnit tests at the moment.
        // TODO remove once JUnit supports parameterized tests with custom names
        // TODO (see http://stackoverflow.com/questions/650894/change-test-name-of-parameterized-tests
        // TODO or https://github.com/KentBeck/junit/pull/393)
        assertEquals(entity, receivedEntity,
                String.format("%s - %s: Received JSON entity content does not match expected JSON entity content.",
                getJsonTestSetup().getJsonProvider().getClass().getSimpleName(),
                getJsonTestSetup().getEntityClass().getSimpleName()));
    }
}
