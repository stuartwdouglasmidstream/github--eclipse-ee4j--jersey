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

package org.glassfish.jersey.server.mvc;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

import org.glassfish.jersey.server.mvc.internal.ErrorTemplateExceptionMapper;
import org.glassfish.jersey.server.mvc.internal.MvcBinder;

/**
 * {@code MvcFeature} used to add MVC support to the server.
 *
 * @author Michal Gajdos
 */
@ConstrainedTo(RuntimeType.SERVER)
public final class MvcFeature implements Feature {

    /**
     * {@link String} property defining the base path to MVC templates. If set, the value of the property is added in front
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
    public static final String TEMPLATE_BASE_PATH = "jersey.config.server.mvc.templateBasePath";

    /**
     * If {@code true} then enable caching of template objects, i.e. to avoid multiple compilations of a template.
     * <p/>
     * The default value is {@code false}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     * <p/>
     * Note: This property is used as common prefix for specific
     * {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor template processors} properties and might not be supported by
     * all template processors.
     *
     * @since 2.5
     */
    public static final String CACHE_TEMPLATES = "jersey.config.server.mvc.caching";

    /**
     * Property used to pass user-configured factory able to create template objects. Value of the property is supposed to be an
     * instance of "templating engine"-specific factory, a class of the factory or class-name of the factory.
     * <p/>
     * The default value is not set.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     * <p/>
     * Note: This property is used as common prefix for specific
     * {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor template processors} properties and might not be supported by
     * all template processors.
     *
     * @since 2.5
     */
    public static final String TEMPLATE_OBJECT_FACTORY = "jersey.config.server.mvc.factory";

    /**
     * Property defines output encoding produced by {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor}. The value
     * must be a valid encoding defined that can be passed to the {@link java.nio.charset.Charset#forName(String)} method.
     * <p/>
     * The default value is {@code UTF-8}.
     * <p/>
     * The name of the configuration property is <tt>{@value}</tt>.
     * <p/>
     * Note: This property is used as common prefix for specific
     * {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor template processors} properties and might not be supported by
     * all template processors.
     *
     * @since 2.7
     */
    public static final String ENCODING = "jersey.config.server.mvc.encoding";

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        if (!config.isRegistered(ErrorTemplateExceptionMapper.class)) {
            context.register(ErrorTemplateExceptionMapper.class);
            context.register(new MvcBinder());

            return true;
        }

        return false;
    }
}
