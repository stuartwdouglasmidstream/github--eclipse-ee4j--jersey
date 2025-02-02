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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

import org.glassfish.jersey.internal.guava.Predicates;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.glassfish.jersey.message.MessageUtils;

/**
 * An interceptor that logs an entity if configured so and provides a common logic for {@link ClientLoggingFilter}
 * and {@link ServerLoggingFilter}.
 *
 * @author Ondrej Kosatka
 */
abstract class LoggingInterceptor implements WriterInterceptor {

    /**
     * Prefix will be printed before requests
     */
    static final String REQUEST_PREFIX = "> ";
    /**
     * Prefix will be printed before response
     */
    static final String RESPONSE_PREFIX = "< ";
    /**
     * The entity stream property
     */
    static final String ENTITY_LOGGER_PROPERTY = LoggingFeature.class.getName() + ".entityLogger";
    /**
     * Logging record id property
     */
    static final String LOGGING_ID_PROPERTY = LoggingFeature.class.getName() + ".id";
    private static final String NOTIFICATION_PREFIX = "* ";
    private static final MediaType TEXT_MEDIA_TYPE = new MediaType("text", "*");

    /**
     * application/vnd.api+json (documented here: http://jsonapi.org/)
     * is a modified form of JSON, which is not present in the JAX-RS
     * MediaType class as a static constant. Requested in Issue #3849
    */
    private static final MediaType APPLICATION_VND_API_JSON = new MediaType("application", "vnd.api+json");

    private static final Set<MediaType> READABLE_APP_MEDIA_TYPES = new HashSet<MediaType>() {{
        add(TEXT_MEDIA_TYPE);
        add(APPLICATION_VND_API_JSON);
        add(MediaType.APPLICATION_ATOM_XML_TYPE);
        add(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        add(MediaType.APPLICATION_JSON_TYPE);
        add(MediaType.APPLICATION_SVG_XML_TYPE);
        add(MediaType.APPLICATION_XHTML_XML_TYPE);
        add(MediaType.APPLICATION_XML_TYPE);
    }};

    private static final Comparator<Map.Entry<String, List<String>>> COMPARATOR =
            new Comparator<Map.Entry<String, List<String>>>() {

                @Override
                public int compare(final Map.Entry<String, List<String>> o1, final Map.Entry<String, List<String>> o2) {
                    return o1.getKey().compareToIgnoreCase(o2.getKey());
                }
            };

    @SuppressWarnings("NonConstantLogger")
    final Logger logger;
    final Level level;
    final AtomicLong _id = new AtomicLong(0);
    final Verbosity verbosity;
    final int maxEntitySize;
    final String separator;
    final Predicate<String> redactHeaderPredicate;

    /**
     * Creates a logging filter using builder instance with custom logger and entity logging turned on,
     * but potentially limiting the size of entity to be buffered and logged.
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

    LoggingInterceptor(LoggingFeature.LoggingFeatureBuilder builder) {
        this.logger = builder.filterLogger;
        this.level = builder.level;
        this.verbosity = builder.verbosity;
        this.maxEntitySize = Math.max(0, builder.maxEntitySize);
        this.separator = builder.separator;
        this.redactHeaderPredicate = builder.redactHeaders != null && !builder.redactHeaders.isEmpty()
                ? new RedactHeaderPredicate(builder.redactHeaders)
                : header -> false;
    }

    /**
     * Logs a {@link StringBuilder} parameter at required level.
     *
     * @param b message to log
     */
    void log(final StringBuilder b) {
        if (logger != null && logger.isLoggable(level)) {
            logger.log(level, b.toString());
        }
    }

    private StringBuilder prefixId(final StringBuilder b, final long id) {
        b.append(Long.toString(id)).append(" ");
        return b;
    }

    void printRequestLine(final StringBuilder b, final String note, final long id, final String method, final URI uri) {
        prefixId(b, id).append(NOTIFICATION_PREFIX)
                .append(note)
                .append(" on thread ").append(Thread.currentThread().getName())
                .append(separator);
        prefixId(b, id).append(REQUEST_PREFIX).append(method).append(" ")
                .append(uri.toASCIIString()).append(separator);
    }

    void printResponseLine(final StringBuilder b, final String note, final long id, final int status) {
        prefixId(b, id).append(NOTIFICATION_PREFIX)
                .append(note)
                .append(" on thread ").append(Thread.currentThread().getName()).append(separator);
        prefixId(b, id).append(RESPONSE_PREFIX)
                .append(Integer.toString(status))
                .append(separator);
    }

    void printPrefixedHeaders(final StringBuilder b,
                              final long id,
                              final String prefix,
                              final MultivaluedMap<String, String> headers) {
        for (final Map.Entry<String, List<String>> headerEntry : getSortedHeaders(headers.entrySet())) {
            final List<?> val = headerEntry.getValue();
            final String header = headerEntry.getKey();

            prefixId(b, id).append(prefix).append(header).append(": ");
            getValuesAppender(header, val).accept(b, val);
            b.append(separator);
        }
    }

    private BiConsumer<StringBuilder, List<?>> getValuesAppender(String header, List<?> values) {
        if (redactHeaderPredicate.test(header)) {
            return (b, v) -> b.append("[redacted]");
        } else if (values.size() == 1) {
            return (b, v) -> b.append(v.get(0));
        } else {
            return (b, v) -> {
                boolean add = false;
                for (final Object s : v) {
                    if (add) {
                        b.append(',');
                    }
                    add = true;
                    b.append(s);
                }
            };
        }
    }

    Set<Map.Entry<String, List<String>>> getSortedHeaders(final Set<Map.Entry<String, List<String>>> headers) {
        final TreeSet<Map.Entry<String, List<String>>> sortedHeaders = new TreeSet<Map.Entry<String, List<String>>>(COMPARATOR);
        sortedHeaders.addAll(headers);
        return sortedHeaders;
    }

    InputStream logInboundEntity(final StringBuilder b, InputStream stream, final Charset charset) throws IOException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        stream.mark(maxEntitySize + 1);
        final byte[] entity = new byte[maxEntitySize + 1];

        int entitySize = 0;
        while (entitySize < entity.length) {
            int readBytes = stream.read(entity, entitySize, entity.length - entitySize);
            if (readBytes < 0) {
                break;
            }
            entitySize += readBytes;
        }

        b.append(new String(entity, 0, Math.min(entitySize, maxEntitySize), charset));
        if (entitySize > maxEntitySize) {
            b.append("...more...");
        }
        b.append('\n');
        stream.reset();
        return stream;
    }

    @Override
    public void aroundWriteTo(final WriterInterceptorContext writerInterceptorContext)
            throws IOException, WebApplicationException {
        final LoggingStream stream = (LoggingStream) writerInterceptorContext.getProperty(ENTITY_LOGGER_PROPERTY);
        writerInterceptorContext.proceed();
        if (logger.isLoggable(level) && printEntity(verbosity, writerInterceptorContext.getMediaType())) {
            if (stream != null) {
                log(stream.getStringBuilder(MessageUtils.getCharset(writerInterceptorContext.getMediaType())));
            }
        }
    }

    /**
     * Returns {@code true} if specified {@link MediaType} is considered textual.
     * <p>
     * See {@link #READABLE_APP_MEDIA_TYPES}.
     *
     * @param mediaType the media type of the entity
     * @return {@code true} if specified {@link MediaType} is considered textual.
     */
    static boolean isReadable(MediaType mediaType) {
        if (mediaType != null) {
            for (MediaType readableMediaType : READABLE_APP_MEDIA_TYPES) {
                if (readableMediaType.isCompatible(mediaType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if entity has to be printed.
     *
     * @param verbosity the configured verbosity .
     * @param mediaType the media type of the payload.
     * @return {@code true} if entity has to be printed.
     */
    static boolean printEntity(Verbosity verbosity, MediaType mediaType) {
        return verbosity == Verbosity.PAYLOAD_ANY || (verbosity == Verbosity.PAYLOAD_TEXT && isReadable(mediaType));
    }

    /**
     * Helper class used to log an entity to the output stream up to the specified maximum number of bytes.
     */
    class LoggingStream extends FilterOutputStream {

        private final StringBuilder b;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        /**
         * Creates {@code LoggingStream} with the entity and the underlying output stream as parameters.
         *
         * @param b     contains the entity to log.
         * @param inner the underlying output stream.
         */
        LoggingStream(final StringBuilder b, final OutputStream inner) {
            super(inner);

            this.b = b;
        }

        StringBuilder getStringBuilder(final Charset charset) {
            // write entity to the builder
            final byte[] entity = baos.toByteArray();

            b.append(new String(entity, 0, Math.min(entity.length, maxEntitySize), charset));
            if (entity.length > maxEntitySize) {
                b.append("...more...");
            }
            b.append('\n');

            return b;
        }

        @Override
        public void write(final int i) throws IOException {
            if (baos.size() <= maxEntitySize) {
                baos.write(i);
            }
            out.write(i);
        }

        @Override
        public void write(byte[] ba, int off, int len) throws IOException {
            if ((off | len | ba.length - (len + off) | off + len) < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (baos.size() <= maxEntitySize) {
                baos.write(ba, off, len);
            }
            out.write(ba, off, len);
        }
    }

    private static final class RedactHeaderPredicate implements Predicate<String> {
        private final Set<String> headersToRedact;

        RedactHeaderPredicate(Collection<String> headersToRedact) {
            this.headersToRedact = headersToRedact.stream()
                    .filter(Objects::nonNull)
                    .filter(Predicates.not(String::isEmpty))
                    .map(RedactHeaderPredicate::normalize)
                    .collect(Collectors.toSet());
        }

        @Override
        public boolean test(String header) {
            return headersToRedact.contains(normalize(header));
        }

        private static String normalize(String input) {
            return input.trim().toLowerCase(Locale.ROOT);
        }
    }
}
