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

package org.glassfish.jersey.server.mvc.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.ResolvedViewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;
import org.glassfish.jersey.server.mvc.spi.ViewableContext;
import org.glassfish.jersey.server.mvc.spi.ViewableContextException;

/**
 * {@link jakarta.ws.rs.ext.MessageBodyWriter Message body writer} for {@link org.glassfish.jersey.server.mvc.Viewable viewable}
 * entities.
 *
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
@Provider
@ConstrainedTo(RuntimeType.SERVER)
final class ViewableMessageBodyWriter implements MessageBodyWriter<Viewable> {

    @Inject
    private InjectionManager injectionManager;

    @Context
    private jakarta.inject.Provider<ExtendedUriInfo> extendedUriInfoProvider;
    @Context
    private jakarta.inject.Provider<ContainerRequest> requestProvider;
    @Context
    private jakarta.inject.Provider<ResourceInfo> resourceInfoProvider;

    private static final Logger LOGGER = Logger.getLogger(ViewableMessageBodyWriter.class.getName());


    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                               final MediaType mediaType) {
        return Viewable.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(final Viewable viewable, final Class<?> type, final Type genericType,
                        final Annotation[] annotations, final MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final Viewable viewable,
                        final Class<?> type,
                        final Type genericType,
                        final Annotation[] annotations,
                        final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders,
                        final OutputStream entityStream) throws IOException, WebApplicationException {

        try {
            final ResolvedViewable resolvedViewable = resolve(viewable);
            if (resolvedViewable == null) {
                final String message = LocalizationMessages.TEMPLATE_NAME_COULD_NOT_BE_RESOLVED(viewable.getTemplateName());
                throw new WebApplicationException(new ProcessingException(message), Response.Status.NOT_FOUND);
            }

            if (!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, resolvedViewable.getMediaType());
            }
            resolvedViewable.writeTo(entityStream, httpHeaders);
        } catch (ViewableContextException vce) {
            throw new NotFoundException(vce);
        }
    }

    /**
     * Resolve the given {@link org.glassfish.jersey.server.mvc.Viewable viewable} using
     * {@link org.glassfish.jersey.server.mvc.spi.ViewableContext}.
     *
     * @param viewable viewable to be resolved.
     * @return resolved viewable or {@code null}, if the viewable cannot be resolved.
     */
    private ResolvedViewable resolve(final Viewable viewable) {
        if (viewable instanceof ResolvedViewable) {
            return (ResolvedViewable) viewable;
        } else {
            final ViewableContext viewableContext = getViewableContext();
            final Set<TemplateProcessor> templateProcessors = getTemplateProcessors();

            final List<MediaType> producibleMediaTypes = TemplateHelper
                    .getProducibleMediaTypes(requestProvider.get(), extendedUriInfoProvider.get(), null);

            final Class<?> resourceClass = resourceInfoProvider.get().getResourceClass();
            if (viewable instanceof ImplicitViewable) {
                // Template Names.
                final ImplicitViewable implicitViewable = (ImplicitViewable) viewable;

                for (final String templateName : implicitViewable.getTemplateNames()) {
                    final Viewable simpleViewable = new Viewable(templateName, viewable.getModel());

                    final ResolvedViewable resolvedViewable = resolve(simpleViewable, producibleMediaTypes,
                            implicitViewable.getResolvingClass(), viewableContext, templateProcessors);

                    if (resolvedViewable != null) {
                        return resolvedViewable;
                    }
                }
            } else {
                return resolve(viewable, producibleMediaTypes, resourceClass, viewableContext, templateProcessors);
            }

            return null;
        }
    }

    /**
     * Resolve given {@link org.glassfish.jersey.server.mvc.Viewable viewable} for a list of {@link jakarta.ws.rs.core.MediaType mediaTypes} and a {@link Class resolvingClass}
     * using given {@link org.glassfish.jersey.server.mvc.spi.ViewableContext viewableContext} and a set of {@link org.glassfish.jersey.server.mvc.spi.TemplateProcessor templateProcessors}
     *
     * @param viewable viewable to be resolved.
     * @param mediaTypes producible media types.
     * @param resolvingClass non-null resolving class.
     * @param viewableContext viewable context.
     * @param templateProcessors collection of available template processors.
     * @return resolved viewable or {@code null}, if the viewable cannot be resolved.
     */
    private ResolvedViewable resolve(final Viewable viewable, final List<MediaType> mediaTypes, final Class<?> resolvingClass,
                                     final ViewableContext viewableContext, final Set<TemplateProcessor> templateProcessors) {
        for (TemplateProcessor templateProcessor : templateProcessors) {
            for (final MediaType mediaType : mediaTypes) {
                final ResolvedViewable resolvedViewable = viewableContext
                        .resolveViewable(viewable, mediaType, resolvingClass, templateProcessor);

                if (resolvedViewable != null) {
                    return resolvedViewable;
                }
            }
        }

        return null;
    }

    /**
     * Get a {@link java.util.LinkedHashSet collection} of available template processors.
     *
     * @return set of template processors.
     */
    private Set<TemplateProcessor> getTemplateProcessors() {
        final Set<TemplateProcessor> templateProcessors = new LinkedHashSet<>();

        templateProcessors.addAll(Providers.getCustomProviders(injectionManager, TemplateProcessor.class));
        templateProcessors.addAll(Providers.getProviders(injectionManager, TemplateProcessor.class));

        return templateProcessors;
    }

    /**
     * Get {@link org.glassfish.jersey.server.mvc.spi.ViewableContext viewable context}. User defined (custom) contexts have higher priority than the default ones
     * (i.e. {@link ResolvingViewableContext}).
     *
     * @return {@code non-null} viewable context.
     */
    private ViewableContext getViewableContext() {
        final Set<ViewableContext> customProviders = Providers.getCustomProviders(injectionManager, ViewableContext.class);
        if (!customProviders.isEmpty()) {
            return customProviders.iterator().next();
        }
        return Providers.getProviders(injectionManager, ViewableContext.class).iterator().next();
    }
}
