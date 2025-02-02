/*
 * Copyright (c) 2016, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.FeatureContext;

import jakarta.annotation.Priority;

import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.glassfish.jersey.message.MessageUtils;

/**
 * Server filter logs requests and responses to specified logger, at required level, with entity or not.
 * <p>
 * The filter is registered in {@link LoggingFeature#configure(FeatureContext)} and can be used on server side only. The Priority
 * is set to the maximum value, which means that filter is called as the first filter when request arrives and similarly as the
 * last filter when the response is dispatched, so request and response is logged as arrives or as dispatched.
 *
 * @author Pavel Bucek
 * @author Martin Matula
 * @author Ondrej Kosatka
 */
@ConstrainedTo(RuntimeType.SERVER)
@PreMatching
@Priority(Integer.MIN_VALUE)
@SuppressWarnings("ClassWithMultipleLoggers")
final class ServerLoggingFilter extends LoggingInterceptor implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * Create a logging filter using builder instance with custom logger and custom settings of entity
     * logging.
     *
     * @param builder       loggingFeatureBuilder which contains values for:
     *  logger         the logger to log messages to.
     *  level          level at which the messages will be logged.
     *  verbosity      verbosity of the logged messages. See {@link Verbosity}.
     *  maxEntitySize  maximum number of entity bytes to be logged (and buffered) - if the entity is larger,
     *                      logging filter will print (and buffer in memory) only the specified number of bytes
     *                      and print "...more..." string at the end. Negative values are interpreted as zero.
     *  separator      delimiter for particular log lines. Default is Linux new line delimiter
     *  redactHeaders  a collection of HTTP headers to be redacted when logging.
     */
    public ServerLoggingFilter(final LoggingFeature.LoggingFeatureBuilder builder) {
        super(builder);
    }

    @Override
    public void filter(final ContainerRequestContext context) throws IOException {
        if (!logger.isLoggable(level)) {
            return;
        }
        final long id = _id.incrementAndGet();
        context.setProperty(LOGGING_ID_PROPERTY, id);

        final StringBuilder b = new StringBuilder();

        printRequestLine(b, "Server has received a request", id, context.getMethod(), context.getUriInfo().getRequestUri());
        printPrefixedHeaders(b, id, REQUEST_PREFIX, context.getHeaders());

        if (printEntity(verbosity, context.getMediaType()) && context.hasEntity()) {
            context.setEntityStream(
                    logInboundEntity(b, context.getEntityStream(), MessageUtils.getCharset(context.getMediaType())));
        }

        log(b);
    }

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
            throws IOException {
        if (!logger.isLoggable(level)) {
            return;
        }
        final Object requestId = requestContext.getProperty(LOGGING_ID_PROPERTY);
        final long id = requestId != null ? (Long) requestId : _id.incrementAndGet();

        final StringBuilder b = new StringBuilder();

        printResponseLine(b, "Server responded with a response", id, responseContext.getStatus());
        printPrefixedHeaders(b, id, RESPONSE_PREFIX, responseContext.getStringHeaders());

        if (printEntity(verbosity, responseContext.getMediaType()) && responseContext.hasEntity()) {
            final OutputStream stream = new LoggingStream(b, responseContext.getEntityStream());
            responseContext.setEntityStream(stream);
            requestContext.setProperty(ENTITY_LOGGER_PROPERTY, stream);
            // not calling log(b) here - it will be called by the interceptor
        } else {
            log(b);
        }
    }
}
