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

package org.glassfish.jersey.server.mvc.mustache;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

import org.glassfish.jersey.server.mvc.MvcFeature;

/**
 * {@link Feature} used to add support for {@link MvcFeature MVC} and Mustache templates.
 * <p/>
 * Note: This feature also registers {@link MvcFeature}.
 *
 * @author Michal Gajdos
 * @since 2.3
 */
@ConstrainedTo(RuntimeType.SERVER)
public class MustacheMvcFeature implements Feature {

    private static final String SUFFIX = ".mustache";

    /**
     * {@link String} property defining the base path to Mustache templates. If set, the value of the property is added in front
     * of the template name defined in:
     * <ul>
     * <li>{@link org.glassfish.jersey.server.mvc.Viewable Viewable}</li>
     * <li>{@link org.glassfish.jersey.server.mvc.Template Template}, or</li>
     * <li>{@link org.glassfish.jersey.server.mvc.ErrorTemplate ErrorTemplate}</li>
     * </ul>
     * <p/>
     * Value can be absolute providing a full path to a system directory with templates or relative to current
     * {@link jakarta.servlet.ServletContext servlet context}.
     * <p/>
     * There is no default value.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     */
    public static final String TEMPLATE_BASE_PATH = MvcFeature.TEMPLATE_BASE_PATH + SUFFIX;

    /**
     * If {@code true} then enable caching of Mustache templates to avoid multiple compilation.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     *
     * @since 2.5
     */
    public static final String CACHE_TEMPLATES = MvcFeature.CACHE_TEMPLATES + SUFFIX;

    /**
     * Property used to pass user-configured {@link com.github.mustachejava.MustacheFactory factory} able to create
     * {@link com.github.mustachejava.Mustache Mustache templates}.
     * <p/>
     * The default value is not set.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     *
     * @since 2.5
     */
    public static final String TEMPLATE_OBJECT_FACTORY = MvcFeature.TEMPLATE_OBJECT_FACTORY + SUFFIX;

    /**
     * Property defines output encoding produced by {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor}.
     * The value must be a valid encoding defined that can be passed
     * to the {@link java.nio.charset.Charset#forName(String)} method.
     *
     * <p/>
     * The default value is {@code UTF-8}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     * <p/>
     *
     * @since 2.7
     */
    public static final String ENCODING = MvcFeature.ENCODING + SUFFIX;

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        if (!config.isRegistered(MustacheTemplateProcessor.class)) {
            // Template Processor.
            context.register(MustacheTemplateProcessor.class);

            // MvcFeature.
            if (!config.isRegistered(MvcFeature.class)) {
                context.register(MvcFeature.class);
            }

            return true;
        }
        return false;
    }
}
