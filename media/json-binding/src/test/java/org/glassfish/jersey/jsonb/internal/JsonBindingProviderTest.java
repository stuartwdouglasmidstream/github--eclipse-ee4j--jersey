/*
 * Copyright (c) 2020, 2022 Markus KARG
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

package org.glassfish.jersey.jsonb.internal;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit Test for {@link JsonBindingProvider}.
 *
 * @author Markus KARG (markus@headcrashing.eu)
 */
public final class JsonBindingProviderTest {

    @Test
    public final void shouldThrowNoContentException() throws IOException {
        assertThrows(NoContentException.class, () -> {
            // given
            final Providers providers = new EmptyProviders();
            final MessageBodyReader<Foo> mbr = (MessageBodyReader) new JsonBindingProvider(providers);

            // when
            mbr.readFrom(Foo.class, Foo.class, new Annotation[0], APPLICATION_JSON_TYPE,
                    new MultivaluedHashMap<>(), new ByteArrayInputStream(new byte[0]));

            // then
            // should throw NoContentException
        });
    }

    private static final class Foo {
        // no members
    }

    private static final class EmptyProviders implements Providers {

        @Override
        public final <T> MessageBodyReader<T> getMessageBodyReader(final Class<T> type, final Type genericType,
                final Annotation[] annotations, final MediaType mediaType) {
            return null;
        }

        @Override
        public final <T> MessageBodyWriter<T> getMessageBodyWriter(final Class<T> type, final Type genericType,
                final Annotation[] annotations, final MediaType mediaType) {
            return null;
        }

        @Override
        public final <T extends Throwable> ExceptionMapper<T> getExceptionMapper(final Class<T> type) {
            return null;
        }

        @Override
        public final <T> ContextResolver<T> getContextResolver(final Class<T> contextType, final MediaType mediaType) {
            return null;
        }

    }

}
