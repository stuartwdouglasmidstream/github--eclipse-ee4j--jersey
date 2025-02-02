/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;

import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Pavel Bucek
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JsonMoxyTest extends AbstractTypeTester {

    @Path("JAXBElementListResource")
    @Produces({"application/json"})
    @Consumes({"application/json"})
    public static class JAXBElementListResource extends AResource<List<JAXBElement<String>>> {
    }

    private List<JAXBElement<String>> getJAXBElementList() {
        return Arrays.asList(getJAXBElementArray());
    }

    public static final class MoxyJsonConfigurationContextResolver implements ContextResolver<MoxyJsonConfig> {

        @Override
        public MoxyJsonConfig getContext(final Class<?> type) {
            final MoxyJsonConfig configuration = new MoxyJsonConfig();
            configuration.setIncludeRoot(true);
            return configuration;
        }
    }

    @SuppressWarnings("unchecked")
    public void _testListOrArray(final boolean isList, final MediaType mt) {
        final Object in = isList ? getJAXBElementList() : getJAXBElementArray();
        final GenericType gt = isList ? new GenericType<List<JAXBElement<String>>>() {
        } : new GenericType<JAXBElement<String>[]>() {
        };

        final WebTarget target = target(isList ? "JAXBElementListResource" : "JAXBElementArrayResource");
        final Object out = target.request(mt).post(Entity.entity(new GenericEntity(in, gt.getType()), mt), gt);

        final List<JAXBElement<String>> inList =
                isList ? ((List<JAXBElement<String>>) in) : Arrays.asList((JAXBElement<String>[]) in);
        final List<JAXBElement<String>> outList = isList ? ((List<JAXBElement<String>>) out) : Arrays
                .asList((JAXBElement<String>[]) out);
        assertEquals(inList.size(), outList.size(), "Lengths differ");
        for (int i = 0; i < inList.size(); i++) {
            assertEquals(inList.get(i).getName(), outList.get(i).getName(), "Names of elements at index " + i + " differ");
            assertEquals(inList.get(i).getValue(), outList.get(i).getValue(), "Values of elements at index " + i + " differ");
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementListJSONRepresentation() {
        _testListOrArray(true, MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("JAXBElementArrayResource")
    @Produces({"application/json"})
    @Consumes({"application/json"})
    public static class JAXBElementArrayResource extends AResource<JAXBElement<String>[]> {
    }

    @SuppressWarnings("unchecked")
    private JAXBElement<String>[] getJAXBElementArray() {
        return new JAXBElement[] {
                new JAXBElement(QName.valueOf("element1"), String.class, "ahoj"),
                new JAXBElement(QName.valueOf("element2"), String.class, "nazdar")
        };
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementArrayJSONRepresentation() {
        _testListOrArray(false, MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return ((ResourceConfig) super.configure())
                .register(MoxyJsonFeature.class)
                .register(MoxyJsonConfigurationContextResolver.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        super.configureClient(config);
        config.register(MoxyJsonFeature.class);
        config.register(MoxyJsonConfigurationContextResolver.class);
    }

    @Path("JAXBElementBeanJSONResource")
    @Consumes("application/json")
    @Produces("application/json")
    public static class JAXBElementBeanJSONResource extends AResource<JAXBElement<String>> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementBeanJSONRepresentation() {
        final WebTarget target = target("JAXBElementBeanJSONResource");

        final GenericType<JAXBElement<String>> genericType = new GenericType<JAXBElement<String>>() {
        };
        final GenericEntity<JAXBElement<String>> jaxbElementGenericEntity = new GenericEntity<>(
                new JAXBElement<>(new QName("test"), String.class, "CONTENT"), genericType.getType());

        final Response rib = target.request().post(
                Entity.entity(jaxbElementGenericEntity, "application/json"));

        // TODO: the following would not be needed if i knew how to workaround JAXBElement<String>.class literal
        final byte[] inBytes = getRequestEntity();
        final byte[] outBytes = getEntityAsByteArray(rib);

        assertEquals(inBytes.length, outBytes.length, new String(outBytes));
        for (int i = 0; i < inBytes.length; i++) {
            if (inBytes[i] != outBytes[i]) {
                assertEquals(inBytes[i], outBytes[i], "Index: " + i);
            }
        }
    }

    @Path("JaxbBeanResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JaxbBeanResourceJSON extends AResource<JaxbBean> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJaxbBeanRepresentationJSON() {
        final WebTarget target = target("JaxbBeanResourceJSON");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/json"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JaxbBeanResourceJSONMediaType")
    @Produces("application/foo+json")
    @Consumes("application/foo+json")
    public static class JaxbBeanResourceJSONMediaType extends AResource<JaxbBean> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJaxbBeanRepresentationJSONMediaType() {
        final WebTarget target = target("JaxbBeanResourceJSONMediaType");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/foo+json"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBElementBeanResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JAXBElementBeanResourceJSON extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementBeanRepresentationJSON() {
        final WebTarget target = target("JAXBElementBeanResourceJSON");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/json"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBElementBeanResourceJSONMediaType")
    @Produces("application/foo+json")
    @Consumes("application/foo+json")
    public static class JAXBElementBeanResourceJSONMediaType extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementBeanRepresentationJSONMediaType() {
        final WebTarget target = target("JAXBElementBeanResourceJSONMediaType");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/foo+json"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JAXBTypeResourceJSON {

        @POST
        public JaxbBean post(final JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBTypeRepresentationJSON() {
        final WebTarget target = target("JAXBTypeResourceJSON");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBeanType out = target.request().post(Entity.entity(in, "application/json"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceJSONMediaType")
    @Produces("application/foo+json")
    @Consumes("application/foo+json")
    public static class JAXBTypeResourceJSONMediaType {

        @POST
        public JaxbBean post(final JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBTypeRepresentationJSONMediaType() {
        final WebTarget target = target("JAXBTypeResourceJSONMediaType");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBeanType out = target.request().post(Entity.entity(in, "application/foo+json"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBListResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBListResource {

        @POST
        public List<JaxbBean> post(final List<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("set")
        public Set<JaxbBean> postSet(final Set<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("queue")
        public Queue<JaxbBean> postQueue(final Queue<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("stack")
        public Stack<JaxbBean> postStack(final Stack<JaxbBean> l) {
            return l;
        }

        @POST
        @Path("custom")
        public MyArrayList<JaxbBean> postCustom(final MyArrayList<JaxbBean> l) {
            return l;
        }

        @GET
        public Collection<JaxbBean> get() {
            final ArrayList<JaxbBean> l = new ArrayList<>();
            l.add(new JaxbBean("one"));
            l.add(new JaxbBean("two"));
            l.add(new JaxbBean("three"));
            return l;
        }

        @POST
        @Path("type")
        public List<JaxbBean> postType(final Collection<JaxbBeanType> l) {
            final List<JaxbBean> beans = new ArrayList<>();
            for (final JaxbBeanType t : l) {
                beans.add(new JaxbBean(t.value));
            }
            return beans;
        }
    }

    @Path("JAXBListResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBListResourceMediaType extends JAXBListResource {
    }

    @Path("JAXBListResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JAXBListResourceJSON extends JAXBListResource {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBListRepresentationJSONCollection() throws Exception {
        final WebTarget target = target("JAXBListResourceJSON");

        final Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b = target.request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/json"), new GenericType<Collection<JaxbBean>>() {
        });

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/json"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBListRepresentationJSONLinkedList() throws Exception {
        final WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        final Collection<JaxbBean> b;

        a = new LinkedList<>(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue<JaxbBean>) a) {
        }, "application/json"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBListRepresentationJSONSet() throws Exception {
        final WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        final Collection<JaxbBean> b;

        a = new HashSet<>(a);
        b = target.path("set").request().post(Entity.entity(new GenericEntity<Set<JaxbBean>>((Set<JaxbBean>) a) {
        }, "application/json"), new GenericType<Set<JaxbBean>>() {
        });
        final Comparator<JaxbBean> c = new Comparator<JaxbBean>() {
            @Override
            public int compare(final JaxbBean t, final JaxbBean t1) {
                return t.value.compareTo(t1.value);
            }
        };
        final TreeSet<JaxbBean> t1 = new TreeSet<>(c);
        final TreeSet<JaxbBean> t2 = new TreeSet<>(c);
        t1.addAll(a);
        t2.addAll(b);
        assertEquals(t1, t2);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBListRepresentationJSONStack() throws Exception {
        final WebTarget target = target("JAXBListResourceJSON");

        final Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        final Collection<JaxbBean> b;

        final Stack<JaxbBean> s = new Stack<>();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/json"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    @Disabled("Until JERSEY-2825 is fixed.")
    public void testJAXBListRepresentationJSONArrayList() throws Exception {
        final WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(new GenericType<Collection<JaxbBean>>() {});
        final Collection<JaxbBean> b;

        a = new MyArrayList<>(a);
        b = target.path("custom").request()
                .post(Entity.entity(new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList<JaxbBean>) a) {}, "application/json"),
                        new GenericType<MyArrayList<JaxbBean>>() {});
        assertEquals(a, b);
    }

    @Path("JAXBListResourceJSONMediaType")
    @Produces("application/foo+json")
    @Consumes("application/foo+json")
    public static class JAXBListResourceJSONMediaType extends JAXBListResource {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBListRepresentationJSONMediaType() throws Exception {
        final WebTarget target = target("JAXBListResourceJSONMediaType");

        final Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b = target.request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/foo+json"), new GenericType<Collection<JaxbBean>>() {
        });

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/foo+json"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);
    }
}
