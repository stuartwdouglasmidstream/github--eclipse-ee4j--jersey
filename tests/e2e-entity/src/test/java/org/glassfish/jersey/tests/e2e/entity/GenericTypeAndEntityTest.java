/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Paul Sandoz
 * @author Martin Matula
 */
public class GenericTypeAndEntityTest extends AbstractTypeTester {

    @Provider
    @SuppressWarnings("UnusedDeclaration")
    public static class ListIntegerWriter implements MessageBodyReader<List<Integer>>, MessageBodyWriter<List<Integer>> {

        private final Type t;

        public ListIntegerWriter() {
            final List<Integer> l = new ArrayList<>();
            final GenericEntity<List<Integer>> ge = new GenericEntity<List<Integer>>(l) {};
            this.t = ge.getType();
        }

        public boolean isWriteable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return this.t.equals(t);
        }

        public long getSize(final List<Integer> l, final Class<?> type, final Type genericType, final Annotation[] annotations,
                            final MediaType mediaType) {
            return -1;
        }

        public void writeTo(final List<Integer> l, final Class<?> c, final Type t, final Annotation[] as,
                            final MediaType mt, final MultivaluedMap<String, Object> hs,
                            final OutputStream out) throws IOException, WebApplicationException {
            final StringBuilder sb = new StringBuilder();
            for (final Integer i : l) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(i);
            }
            out.write(sb.toString().getBytes());
        }

        @Override
        public boolean isReadable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                                  final MediaType mediaType) {
            return this.t.equals(genericType);
        }

        @Override
        public List<Integer> readFrom(final Class<List<Integer>> type, final Type genericType, final Annotation[] annotations,
                                      final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders,
                                      final InputStream entityStream) throws IOException, WebApplicationException {
            return Arrays.stream(ReaderWriter.readFromAsString(entityStream, mediaType).split(","))
                         .map(input -> Integer.valueOf(input.trim()))
                         .collect(Collectors.toList());
        }
    }

    public static class GenericListResource<T> {

        @POST
        @Path("genericType")
        public List<T> post(final List<T> post) {
            return post;
        }
    }

    @Path("ListResource")
    public static class ListResource extends GenericListResource<Integer> {

        @GET
        @Path("type")
        public List<Integer> type() {
            return Arrays.asList(1, 2, 3, 4);
        }

        @GET
        @Path("genericEntity")
        public GenericEntity<List<Integer>> genericEntity() {
            return new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {};
        }

        @GET
        @Path("object")
        public Object object() {
            return new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {};
        }

        @GET
        @Path("response")
        public Response response() {
            return Response.ok(new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {}).build();
        }

        @GET
        @Path("wrongGenericEntity")
        public GenericEntity<List<Integer>> wrongGenericEntity() {
            // wrongly constructed generic entity: generic type of the generic entity
            // is not generic but just a List interface type. In this case
            // the return generic type will be used
            return new GenericEntity<>(Arrays.asList(1, 2, 3, 4), List.class);
        }
    }

    public static class GenericListMediaTypeResource<T> {

        @POST
        @Path("genericType")
        @Produces("text/plain")
        public List<T> post(final List<T> post) {
            return post;
        }
    }

    @Path("ListResourceWithMediaType")
    public static class ListResourceWithMediaType extends GenericListMediaTypeResource<Integer> {

        @GET
        @Path("type")
        @Produces("text/plain")
        public List<Integer> type() {
            return Arrays.asList(1, 2, 3, 4);
        }

        @GET
        @Path("genericEntity")
        @Produces("text/plain")
        public GenericEntity<List<Integer>> genericEntity() {
            return new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {};
        }

        @GET
        @Path("object")
        @Produces("text/plain")
        public Object object() {
            return new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {};
        }

        @GET
        @Path("response")
        @Produces("text/plain")
        public Response response() {
            return Response.ok(new GenericEntity<List<Integer>>(Arrays.asList(1, 2, 3, 4)) {}).build();
        }

        @GET
        @Path("wrongGenericEntity")
        @Produces("text/plain")
        public GenericEntity<List<Integer>> wrongGenericEntity() {
            // wrongly constructed generic entity: generic type of the generic entity
            // is not generic but just a List interface type. In this case
            // the return generic type will be used
            return new GenericEntity<>(Arrays.asList(1, 2, 3, 4), List.class);
        }
    }

    @Test
    public void testGenericType() {
        _genericTest(ListResource.class);
    }

    @Test
    public void testGenericTypeWithMediaType() {
        _genericTest(ListResourceWithMediaType.class);
    }

    private void _genericTest(final Class resourceClass) {
        final WebTarget target = target(resourceClass.getSimpleName());

        _testPath(target, "type");
        _testPath(target, "genericEntity");
        _testPath(target, "object");
        _testPath(target, "response");
        _testPath(target, "wrongGenericEntity");

        _testPathPost(target, "genericType");
    }

    private void _testPath(final WebTarget target, final String path) {
        assertEquals("1, 2, 3, 4", target.path(path).request().get(String.class));
    }

    private void _testPathPost(final WebTarget target, final String path) {
        assertEquals("1, 2, 3, 4", target.path(path).request().post(Entity.text("1, 2, 3, 4"), String.class));
    }

    @Provider
    public static class MapStringReader implements MessageBodyReader<Map<String, String>>,
                                                   MessageBodyWriter<Map<String, String>> {

        private final Type mapStringType;

        public MapStringReader() {
            final ParameterizedType iface = (ParameterizedType) this.getClass().getGenericInterfaces()[0];
            mapStringType = iface.getActualTypeArguments()[0];
        }

        public boolean isReadable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return Map.class == c && mapStringType.equals(t);
        }

        public Map<String, String> readFrom(final Class<Map<String, String>> c, final Type t, final Annotation[] as,
                                            final MediaType mt, final MultivaluedMap<String, String> headers,
                                            final InputStream in) throws IOException {
            final String[] v = ReaderWriter.readFromAsString(in, mt).split(",");
            final Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i < v.length; i = i + 2) {
                m.put(v[i], v[i + 1]);
            }
            return m;
        }

        @Override
        public boolean isWriteable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return Map.class.isAssignableFrom(c) && mapStringType.equals(t);
        }

        @Override
        public long getSize(final Map<String, String> t, final Class<?> type, final Type genericType, final Annotation[] as,
                            final MediaType mt) {
            return -1;
        }

        @Override
        public void writeTo(final Map<String, String> t, final Class<?> c, final Type genericType, final Annotation[] as,
                            final MediaType mt, final MultivaluedMap<String, Object> hs,
                            final OutputStream out) throws IOException, WebApplicationException {
            final StringBuilder sb = new StringBuilder();
            for (final Map.Entry<String, String> e : t.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(e.getKey()).append(',').append(e.getValue());
            }
            out.write(sb.toString().getBytes());
        }
    }

    public static class GenericMapResource<K, V> {

        @POST
        public Map<K, V> post(final Map<K, V> m) {
            return m;
        }
    }

    @Path("/MapResource")
    public static class MapResource extends GenericMapResource<String, String> {

    }

    @Test
    public void testGenericMap() throws Exception {
        assertThat(target("/MapResource").request().post(Entity.text("a,b,c,d"), String.class), is("a,b,c,d"));
    }

    @Provider
    public static class MapListStringReader implements MessageBodyReader<Map<String, List<String>>>,
                                                       MessageBodyWriter<Map<String, List<String>>> {

        private final Type mapListStringType;

        public MapListStringReader() {
            final ParameterizedType iface = (ParameterizedType) this.getClass().getGenericInterfaces()[0];
            mapListStringType = iface.getActualTypeArguments()[0];
        }

        public boolean isReadable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return Map.class == c && mapListStringType.equals(t);
        }

        public Map<String, List<String>> readFrom(final Class<Map<String, List<String>>> c, final Type t,
                                                  final Annotation[] as, final MediaType mt, final MultivaluedMap<String,
                String> headers, final InputStream in) throws IOException {
            try {
                final JSONObject o = new JSONObject(ReaderWriter.readFromAsString(in, mt));

                final Map<String, List<String>> m = new LinkedHashMap<>();
                final Iterator keys = o.keys();
                while (keys.hasNext()) {
                    final String key = (String) keys.next();
                    final List<String> l = new ArrayList<>();
                    m.put(key, l);
                    final JSONArray a = o.getJSONArray(key);
                    for (int i = 0; i < a.length(); i++) {
                        l.add(a.getString(i));
                    }
                }
                return m;
            } catch (final JSONException ex) {
                throw new IOException(ex);
            }
        }

        @Override
        public boolean isWriteable(final Class<?> c, final Type t, final Annotation[] as, final MediaType mt) {
            return Map.class.isAssignableFrom(c) && mapListStringType.equals(t);
        }

        @Override
        public long getSize(final Map<String, List<String>> t, final Class<?> type, final Type genericType,
                            final Annotation[] as, final MediaType mt) {
            return -1;
        }

        @Override
        public void writeTo(final Map<String, List<String>> t, final Class<?> c, final Type genericType, final Annotation[] as,
                            final MediaType mt, final MultivaluedMap<String, Object> hs,
                            final OutputStream out) throws IOException, WebApplicationException {
            try {
                final JSONObject o = new JSONObject();
                for (final Map.Entry<String, List<String>> e : t.entrySet()) {
                    o.put(e.getKey(), e.getValue());
                }
                out.write(o.toString().getBytes());
            } catch (final JSONException ex) {
                throw new IOException(ex);
            }
        }
    }

    public static class GenericMapListResource<K, V> {

        @POST
        public Map<K, List<V>> post(final Map<K, List<V>> m) {
            return m;
        }
    }

    @Path("/MapListResource")
    public static class MapListResource extends GenericMapListResource<String, String> {

    }

    @Test
    public void testGenericMapList() throws Exception {
        final String json = "{\"a\":[\"1\",\"2\"],\"b\":[\"1\",\"2\"]}";
        assertThat(target("/MapListResource").request().post(Entity.text(json), String.class), is(json));
    }
}
