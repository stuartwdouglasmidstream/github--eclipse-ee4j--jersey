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

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.VariantSelector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;

/**
 * Helper class to provide some common functionality related to MVC.
 *
 * @author Michal Gajdos
 */
public final class TemplateHelper {

    private static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

    /**
     * Return an absolute path to the given class where segments are separated using {@code delim} character and {@code path}
     * is appended to this path.
     *
     * @param resourceClass class for which an absolute path should be obtained.
     * @param path segment to be appended to the resulting path.
     * @param delim character used for separating path segments.
     * @return an absolute path to the resource class.
     */
    public static String getAbsolutePath(Class<?> resourceClass, String path, char delim) {
        return '/' + resourceClass.getName().replace('.', '/').replace('$', delim) + delim + path;
    }

    /**
     * Get media types for which the {@link org.glassfish.jersey.server.mvc.spi.ResolvedViewable resolved viewable} could be
     * produced.
     *
     * @param containerRequest request to obtain acceptable media types.
     * @param extendedUriInfo uri info to obtain resource method from and its producible media types.
     * @param varyHeaderValue Vary header reference.
     * @return list of producible media types.
     */
    public static List<MediaType> getProducibleMediaTypes(final ContainerRequest containerRequest,
                                                          final ExtendedUriInfo extendedUriInfo,
                                                          final Ref<String> varyHeaderValue) {
        final List<MediaType> producedTypes = getResourceMethodProducibleTypes(extendedUriInfo);
        final MediaType[] mediaTypes = producedTypes.toArray(new MediaType[producedTypes.size()]);

        final List<Variant> variants = VariantSelector.selectVariants(
                containerRequest, Variant.mediaTypes(mediaTypes).build(),
                varyHeaderValue == null ? Refs.<String>emptyRef() : varyHeaderValue);

        return variants.stream()
                       .map(variant -> MediaTypes.stripQualityParams(variant.getMediaType()))
                       .collect(Collectors.toList());
    }

    /**
     * Get template name from given {@link org.glassfish.jersey.server.mvc.Viewable viewable} or return {@code index} if the given
     * viewable doesn't contain a valid template name.
     *
     * @param viewable viewable to obtain template name from.
     * @return {@code non-null}, {@code non-empty} template name.
     */
    public static String getTemplateName(final Viewable viewable) {
        return viewable.getTemplateName() == null || viewable.getTemplateName().isEmpty() ? "index" : viewable.getTemplateName();
    }

    /**
     * Return a list of producible media types of the last matched resource method.
     *
     * @param extendedUriInfo uri info to obtain resource method from.
     * @return list of producible media types of the last matched resource method.
     */
    private static List<MediaType> getResourceMethodProducibleTypes(final ExtendedUriInfo extendedUriInfo) {
        if (extendedUriInfo.getMatchedResourceMethod() != null
                && !extendedUriInfo.getMatchedResourceMethod().getProducedTypes().isEmpty()) {
            return extendedUriInfo.getMatchedResourceMethod().getProducedTypes();
        }
        return Arrays.asList(MediaType.WILDCARD_TYPE);
    }

    /**
     * Extract {@link org.glassfish.jersey.server.mvc.Template template} annotation from given list.
     *
     * @param annotations list of annotations.
     * @return {@link org.glassfish.jersey.server.mvc.Template template} annotation or {@code null} if this annotation is not present.
     */
    public static Template getTemplateAnnotation(final Annotation[] annotations) {
        if (annotations != null && annotations.length > 0) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof Template) {
                    return (Template) annotation;
                }
            }
        }

        return null;
    }

    /**
     * Get output encoding from configuration.
     * @param configuration Configuration.
     * @param suffix Template processor suffix of the
     *               to configuration property {@link org.glassfish.jersey.server.mvc.MvcFeature#ENCODING}.
     *
     * @return Encoding read from configuration properties or a default encoding if no encoding is configured.
     */
    public static Charset getTemplateOutputEncoding(Configuration configuration, String suffix) {
        final String enc = PropertiesHelper.getValue(configuration.getProperties(), MvcFeature.ENCODING + suffix,
                String.class, null);
        if (enc == null) {
            return DEFAULT_ENCODING;
        } else {
            return Charset.forName(enc);
        }
    }

    /**
     * Prevents instantiation.
     */
    private TemplateHelper() {
    }
}
