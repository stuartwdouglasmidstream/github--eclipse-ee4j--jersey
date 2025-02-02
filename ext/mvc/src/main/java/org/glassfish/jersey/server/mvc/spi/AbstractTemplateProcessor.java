/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.mvc.spi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import jakarta.servlet.ServletContext;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.internal.LocalizationMessages;
import org.glassfish.jersey.server.mvc.internal.TemplateHelper;

/**
 * Default implementation of {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor template processor} that can be used to
 * implement support for custom templating engines. The class currently recognizes following properties:
 * <ul>
 * <li>{@link org.glassfish.jersey.server.mvc.MvcFeature#TEMPLATE_BASE_PATH}</li>
 * <li>{@link org.glassfish.jersey.server.mvc.MvcFeature#CACHE_TEMPLATES}</li>
 * <li>{@link org.glassfish.jersey.server.mvc.MvcFeature#TEMPLATE_OBJECT_FACTORY}</li>
 * </ul>
 * If any of the properties are not supported by particular template processor then this fact should be mentioned in documentation
 * of the template processor.
 *
 * @author Michal Gajdos
 */
public abstract class AbstractTemplateProcessor<T> implements TemplateProcessor<T> {

    private static final Logger LOGGER = Logger.getLogger(AbstractTemplateProcessor.class.getName());

    private final ConcurrentMap<String, T> cache;

    private final String suffix;
    private final Configuration config;
    private final ServletContext servletContext;

    private final String basePath;
    private final Set<String> supportedExtensions;
    private final Charset encoding;

    /**
     * Create an instance of the processor with injected {@link jakarta.ws.rs.core.Configuration config} and
     * (optional) {@link ServletContext servlet context}.
     *
     * @param config configuration to configure this processor from.
     * @param servletContext (optional) servlet context to obtain template resources from.
     * @param propertySuffix suffix to distinguish properties for current template processor.
     * @param supportedExtensions supported template file extensions.
     */
    public AbstractTemplateProcessor(final Configuration config, final ServletContext servletContext,
                                     final String propertySuffix, final String... supportedExtensions) {
        this.config = config;
        this.suffix = '.' + propertySuffix;

        this.servletContext = servletContext;
        this.supportedExtensions =
                Arrays.stream(supportedExtensions)
                      .map(input -> {
                          input = input.toLowerCase(Locale.ROOT);
                          return input.startsWith(".") ? input : "." + input;
                      })
                      .collect(Collectors.toSet());

        // Resolve property values.
        final Map<String, Object> properties = config.getProperties();

        // Base Path.
        String basePath = PropertiesHelper.getValue(properties, MvcFeature.TEMPLATE_BASE_PATH + suffix, String.class, null);
        if (basePath == null) {
            basePath = PropertiesHelper.getValue(properties, MvcFeature.TEMPLATE_BASE_PATH, "", null);
        }
        this.basePath = basePath;

        // Cache.
        Boolean cacheEnabled = PropertiesHelper.getValue(properties, MvcFeature.CACHE_TEMPLATES + suffix, Boolean.class, null);
        if (cacheEnabled == null) {
            cacheEnabled = PropertiesHelper.getValue(properties, MvcFeature.CACHE_TEMPLATES, false, null);
        }
        this.cache = cacheEnabled ? new ConcurrentHashMap<>() : null;
        this.encoding = TemplateHelper.getTemplateOutputEncoding(config, suffix);
    }

    /**
     * Return base path for current template processor.
     *
     * @return base path or an empty string.
     */
    protected String getBasePath() {
        return basePath;
    }

    /**
     * Return current servlet context, if present.
     *
     * @return servlet context instance or {@code null}.
     */
    protected ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public T resolve(final String name, final MediaType mediaType) {
        // Look into the cache if enabled.
        if (cache != null) {
            if (!cache.containsKey(name)) {
                cache.putIfAbsent(name, resolve(name));
            }
            return cache.get(name);
        }

        return resolve(name);
    }

    /**
     * Resolve a template name to a template reference.
     *
     * @param name the template name.
     * @return the template reference, otherwise {@code null} if the template name cannot be resolved.
     */
    private T resolve(final String name) {
        for (final String template : getTemplatePaths(name)) {
            Reader reader = null;

            // ServletContext.
            if (servletContext != null) {
                //"The path must begin with a "/"".
                final String path = template.startsWith("/") ? template : "/" + template;
                final InputStream stream = servletContext.getResourceAsStream(path);
                reader = stream != null ? new InputStreamReader(stream) : null;
            }

            // Classloader.
            if (reader == null) {
                InputStream stream = getClass().getResourceAsStream(template);
                if (stream == null) {
                    stream = getClass().getClassLoader().getResourceAsStream(template);
                }
                reader = stream != null ? new InputStreamReader(stream) : null;
            }

            // File-system path.
            if (reader == null) {
                try {
                    reader = new InputStreamReader(new FileInputStream(template), encoding);
                } catch (final FileNotFoundException fnfe) {
                    // NOOP.
                }
            }

            if (reader != null) {
                try {
                    return resolve(template, reader);
                } catch (final Exception e) {
                    LOGGER.log(Level.WARNING, LocalizationMessages.TEMPLATE_RESOLVE_ERROR(template), e);
                } finally {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        LOGGER.log(Level.WARNING, LocalizationMessages.TEMPLATE_ERROR_CLOSING_READER(), e);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve given template path and/or reader to a template reference object.
     *
     * @param templatePath resolved template path (incl. base path and suffix).
     * @param reader reader containing template character stream.
     * @return non-{@code null} template reference object.
     * @throws Exception if an exception occurred during resolving.
     */
    protected abstract T resolve(final String templatePath, final Reader reader) throws Exception;

    /**
     * Return collection of possible template paths (included basePath and suffix).
     *
     * @param name the template name.
     * @return collection of possible template paths.
     */
    private Collection<String> getTemplatePaths(final String name) {
        final String lowerName = name.toLowerCase(Locale.ROOT);
        final String templatePath = basePath.endsWith("/") ? basePath + name.substring(1) : basePath + name;

        // Check whether the given name ends with supported suffix.
        for (final String extension : supportedExtensions) {
            if (lowerName.endsWith(extension)) {
                return Collections.singleton(templatePath);
            }
        }

        return supportedExtensions.stream().map(input -> templatePath + input).collect(Collectors.toSet());
    }

    /**
     * Retrieve a template object factory. The factory is, at first, looked for in
     * {@link jakarta.ws.rs.core.Configuration configuration} and if not found, given default value is used.
     *
     * @param createInstance function that delegates a creation and an initialization to injection manager.
     * @param type           type of requested template object factory.
     * @param defaultValue   default value to be used if no factory reference is present in configuration.
     * @param <F> type of requested template object factory.
     * @return non-{@code null} template object factory.
     */
    protected <F> F getTemplateObjectFactory(Function<Class<?>, ?> createInstance, Class<F> type, Value<F> defaultValue) {
        final Object objectFactoryProperty = config.getProperty(MvcFeature.TEMPLATE_OBJECT_FACTORY + suffix);

        if (objectFactoryProperty != null) {
            if (type.isAssignableFrom(objectFactoryProperty.getClass())) {
                return type.cast(objectFactoryProperty);
            } else {
                Class<?> factoryClass = null;

                if (objectFactoryProperty instanceof String) {
                    factoryClass = ReflectionHelper.classForNamePA((String) objectFactoryProperty).run();
                } else if (objectFactoryProperty instanceof Class<?>) {
                    factoryClass = (Class<?>) objectFactoryProperty;
                }

                if (factoryClass != null) {
                    if (type.isAssignableFrom(factoryClass)) {
                        return type.cast(createInstance.apply(factoryClass));
                    } else {
                        LOGGER.log(Level.CONFIG, LocalizationMessages.WRONG_TEMPLATE_OBJECT_FACTORY(factoryClass, type));
                    }
                }
            }
        }

        return defaultValue.get();
    }

    /**
     * Set the {@link HttpHeaders#CONTENT_TYPE} header to the {@code httpHeaders} based on {@code mediaType} and
     * {@link #getEncoding() default encoding} defined in this processor. If {@code mediaType} defines encoding
     * then this encoding will be used otherwise the default processor encoding is used. The chosen encoding
     * is returned from the method.
     *
     * @param mediaType Media type of the entity.
     * @param httpHeaders Http headers.
     * @return Selected encoding.
     */
    protected Charset setContentType(final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders) {
        final Charset encoding;

        final String charset = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
        final MediaType finalMediaType;
        if (charset == null) {
            encoding = getEncoding();
            final HashMap<String, String> params = new HashMap<>(mediaType.getParameters());
            params.put(MediaType.CHARSET_PARAMETER, encoding.name());
            finalMediaType = new MediaType(mediaType.getType(), mediaType.getSubtype(), params);
        } else {
            encoding = Charset.forName(charset);
            finalMediaType = mediaType;
        }
        final ArrayList<Object> typeList = new ArrayList<>(1);
        typeList.add(finalMediaType.toString());
        httpHeaders.put(HttpHeaders.CONTENT_TYPE, typeList);
        return encoding;
    }

    /**
     * Get the output encoding.
     *
     * @return Not-{@code null} encoding.
     */
    protected Charset getEncoding() {
        return encoding;
    }
}
