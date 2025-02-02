/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.moxy.json;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.xml.bind.Marshaller;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.eclipse.persistence.oxm.XMLConstants;

/**
 * Configuration class for MOXy JSON provider.
 *
 * @author Michal Gajdos
 */
public final class MoxyJsonConfig {

    private final Map<String, Object> marshallerProperties = new HashMap<>();
    private final Map<String, Object> unmarshallerProperties = new HashMap<>();

    /**
     * Create a new configuration for {@link org.eclipse.persistence.jaxb.rs.MOXyJsonProvider} and initialize default properties.
     *
     * @see #MoxyJsonConfig(boolean)
     */
    public MoxyJsonConfig() {
        this(true);
    }

    /**
     * Create a new configuration for {@link org.eclipse.persistence.jaxb.rs.MOXyJsonProvider}. If the
     * {@code initDefaultProperties} is set to {@code true} then the following values are set:
     * <ul>
     *     <li>{@link jakarta.xml.bind.Marshaller#JAXB_FORMATTED_OUTPUT} - {@code false}</li>
     *     <li>{@link org.eclipse.persistence.jaxb.JAXBContextProperties#JSON_INCLUDE_ROOT} - {@code false}</li>
     *     <li>{@link org.eclipse.persistence.jaxb.MarshallerProperties#JSON_MARSHAL_EMPTY_COLLECTIONS} - {@code true}</li>
     *     <li>{@link org.eclipse.persistence.jaxb.JAXBContextProperties#JSON_NAMESPACE_SEPARATOR} -
     *         {@link org.eclipse.persistence.oxm.XMLConstants#DOT}</li>
     * </ul>
     *
     * @param initDefaultProperties flag to determine whether the default values of properties mentioned above should be set or
     * not.
     */
    public MoxyJsonConfig(final boolean initDefaultProperties) {
        if (initDefaultProperties) {
            // jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT
            setFormattedOutput(false);

            // org.eclipse.persistence.jaxb.JAXBContextProperties.JSON_INCLUDE_ROOT
            setIncludeRoot(false);

            // org.eclipse.persistence.jaxb.MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS
            setMarshalEmptyCollections(true);

            // org.eclipse.persistence.jaxb.JAXBContextProperties.JSON_NAMESPACE_SEPARATOR
            setNamespaceSeparator(XMLConstants.DOT);
        }
    }

    /**
     * Copy constructor.
     *
     * @param that config to make a copy of.
     */
    private MoxyJsonConfig(final MoxyJsonConfig that) {
        this.marshallerProperties.putAll(that.marshallerProperties);
        this.unmarshallerProperties.putAll(that.unmarshallerProperties);
    }

    /**
     * Set the value of the property for both {@code Marshaller} / {@code Unmarshaller}.
     *
     * @param name name of the property.
     * @param value value of the property.
     * @return a {@code MoxyJsonConfig} instance.
     */
    public MoxyJsonConfig property(final String name, final Object value) {
        marshallerProperty(name, value);
        unmarshallerProperty(name, value);
        return this;
    }

    /**
     * Set the value of the property for used {@code Marshaller}.
     *
     * @param name name of the property.
     * @param value value of the property.
     * @return a {@code MoxyJsonConfig} instance.
     */
    public MoxyJsonConfig marshallerProperty(final String name, final Object value) {
        marshallerProperties.put(name, value);
        return this;
    }

    /**
     * Set the value of the property for used {@code Unmarshaller}.
     *
     * @param name name of the property.
     * @param value value of the property.
     * @return a {@code MoxyJsonConfig} instance.
     */
    public MoxyJsonConfig unmarshallerProperty(final String name, final Object value) {
        unmarshallerProperties.put(name, value);
        return this;
    }

    /**
     * Add properties from the given map to the existing marshaller properties.
     *
     * @param marshallerProperties map of marshaller properties.
     * @return a {@code MoxyJsonConfig} instance.
     */
    public MoxyJsonConfig setMarshallerProperties(final Map<String, Object> marshallerProperties) {
        this.marshallerProperties.putAll(marshallerProperties);
        return this;
    }

    /**
     * Add properties from the given map to the existing unmarshaller properties.
     *
     * @param unmarshallerProperties map of unmarshaller properties.
     * @return a {@code MoxyJsonConfig} instance.
     */
    public MoxyJsonConfig setUnmarshallerProperties(final Map<String, Object> unmarshallerProperties) {
        this.unmarshallerProperties.putAll(unmarshallerProperties);
        return this;
    }

    /**
     * Get marshaller properties.
     *
     * @return mutable map of marshaller properties.
     */
    public Map<String, Object> getMarshallerProperties() {
        return marshallerProperties;
    }

    /**
     * Get unmarshaller properties.
     *
     * @return mutable map of unmarshaller properties.
     */
    public Map<String, Object> getUnmarshallerProperties() {
        return unmarshallerProperties;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#getAttributePrefix()
     */
    public String getAttributePrefix() {
        return (String) marshallerProperties.get(MarshallerProperties.JSON_ATTRIBUTE_PREFIX);
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setAttributePrefix(String)
     */
    public MoxyJsonConfig setAttributePrefix(final String attributePrefix) {
        if (attributePrefix != null) {
            marshallerProperties.put(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, attributePrefix);
            unmarshallerProperties.put(UnmarshallerProperties.JSON_ATTRIBUTE_PREFIX, attributePrefix);
        } else {
            marshallerProperties.remove(MarshallerProperties.JSON_ATTRIBUTE_PREFIX);
            unmarshallerProperties.remove(UnmarshallerProperties.JSON_ATTRIBUTE_PREFIX);
        }
        return this;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#isFormattedOutput()
     */
    public boolean isFormattedOutput() {
        return (Boolean) marshallerProperties.get(Marshaller.JAXB_FORMATTED_OUTPUT);
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setFormattedOutput(boolean)
     */
    public MoxyJsonConfig setFormattedOutput(final boolean formattedOutput) {
        marshallerProperties.put(Marshaller.JAXB_FORMATTED_OUTPUT, formattedOutput);
        return this;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#isIncludeRoot()
     */
    public boolean isIncludeRoot() {
        return (Boolean) marshallerProperties.get(MarshallerProperties.JSON_INCLUDE_ROOT);
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setIncludeRoot(boolean)
     */
    public MoxyJsonConfig setIncludeRoot(final boolean includeRoot) {
        marshallerProperties.put(MarshallerProperties.JSON_INCLUDE_ROOT, includeRoot);
        unmarshallerProperties.put(UnmarshallerProperties.JSON_INCLUDE_ROOT, includeRoot);
        return this;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#isMarshalEmptyCollections()
     */
    public boolean isMarshalEmptyCollections() {
        return (Boolean) marshallerProperties.get(MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS);
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setMarshalEmptyCollections(boolean)
     */
    public MoxyJsonConfig setMarshalEmptyCollections(final boolean marshalEmptyCollections) {
        marshallerProperties.put(MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS, marshalEmptyCollections);
        return this;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#getNamespacePrefixMapper()
     */
    public Map<String, String> getNamespacePrefixMapper() {
        return (Map<String, String>) marshallerProperties.get(MarshallerProperties.NAMESPACE_PREFIX_MAPPER);
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setNamespacePrefixMapper(java.util.Map)
     */
    public MoxyJsonConfig setNamespacePrefixMapper(final Map<String, String> namespacePrefixMapper) {
        if (namespacePrefixMapper != null) {
            marshallerProperties.put(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, namespacePrefixMapper);
            unmarshallerProperties.put(UnmarshallerProperties.JSON_NAMESPACE_PREFIX_MAPPER, namespacePrefixMapper);
        } else {
            marshallerProperties.remove(MarshallerProperties.NAMESPACE_PREFIX_MAPPER);
            unmarshallerProperties.remove(UnmarshallerProperties.JSON_NAMESPACE_PREFIX_MAPPER);
        }
        return this;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#getNamespaceSeparator()
     */
    public char getNamespaceSeparator() {
        return (Character) marshallerProperties.get(MarshallerProperties.JSON_NAMESPACE_SEPARATOR);
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setNamespaceSeparator(char)
     */
    public MoxyJsonConfig setNamespaceSeparator(final char namespaceSeparator) {
        marshallerProperties.put(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, namespaceSeparator);
        unmarshallerProperties.put(UnmarshallerProperties.JSON_NAMESPACE_SEPARATOR, namespaceSeparator);
        return this;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#getValueWrapper()
     */
    public String getValueWrapper() {
        return (String) marshallerProperties.get(MarshallerProperties.JSON_VALUE_WRAPPER);
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setValueWrapper(String)
     */
    public MoxyJsonConfig setValueWrapper(final String valueWrapper) {
        if (valueWrapper != null) {
            marshallerProperties.put(MarshallerProperties.JSON_VALUE_WRAPPER, valueWrapper);
            unmarshallerProperties.put(UnmarshallerProperties.JSON_VALUE_WRAPPER, valueWrapper);
        } else {
            marshallerProperties.remove(MarshallerProperties.JSON_VALUE_WRAPPER);
            unmarshallerProperties.remove(UnmarshallerProperties.JSON_VALUE_WRAPPER);
        }
        return this;
    }

    /**
     * Create a {@link ContextResolver context resolver} for a current state of this {@code MoxyJsonConfig}.
     *
     * @return context resolver for this config.
     */
    public ContextResolver<MoxyJsonConfig> resolver() {
        return new ContextResolver<MoxyJsonConfig>() {

            private final MoxyJsonConfig config = new MoxyJsonConfig(MoxyJsonConfig.this);

            @Override
            public MoxyJsonConfig getContext(final Class<?> type) {
                return config;
            }
        };
    }
}
