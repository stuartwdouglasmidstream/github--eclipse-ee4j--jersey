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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import jakarta.activation.DataSource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.message.internal.FileProvider;
import org.glassfish.jersey.server.ResourceConfig;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Paul Sandoz
 * @author Martin Matula
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EntityTypesTest extends AbstractTypeTester {

    @Path("InputStreamResource")
    public static class InputStreamResource {

        @POST
        public InputStream post(final InputStream in) throws IOException {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            int read;
            final byte[] data = new byte[2048];
            while ((read = in.read(data)) != -1) {
                out.write(data, 0, read);
            }

            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testInputStream() {
        final ByteArrayInputStream in = new ByteArrayInputStream("CONTENT".getBytes());
        _test(in, InputStreamResource.class);
    }

    @Path("StringResource")
    public static class StringResource extends AResource<String> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testString() {
        _test("CONTENT", StringResource.class);
    }

    @Path("DataSourceResource")
    public static class DataSourceResource extends AResource<DataSource> {
    }

    @Path("ByteArrayResource")
    public static class ByteArrayResource extends AResource<byte[]> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testByteArrayRepresentation() {
        _test("CONTENT".getBytes(), ByteArrayResource.class);
    }

    @Path("JaxbBeanResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JaxbBeanResource extends AResource<JaxbBean> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJaxbBeanRepresentation() {
        _test(new JaxbBean("CONTENT"), JaxbBeanResource.class, MediaType.APPLICATION_XML_TYPE);
    }

    @Path("JaxbBeanResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JaxbBeanResourceMediaType extends AResource<JaxbBean> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJaxbBeanRepresentationMediaType() {
        _test(new JaxbBean("CONTENT"), JaxbBeanResourceMediaType.class, MediaType.valueOf("application/foo+xml"));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJaxbBeanRepresentationError() {
        final WebTarget target = target("JaxbBeanResource");

        final String xml = "<root>foo</root>";
        final Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("JaxbBeanTextResource")
    @Produces("text/xml")
    @Consumes("text/xml")
    public static class JaxbBeanTextResource extends AResource<JaxbBean> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJaxbBeanTextRepresentation() {
        _test(new JaxbBean("CONTENT"), JaxbBeanTextResource.class, MediaType.TEXT_XML_TYPE);
    }

    @Path("JAXBElementBeanResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBElementBeanResource extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementBeanRepresentation() {
        _test(new JaxbBean("CONTENT"), JAXBElementBeanResource.class, MediaType.APPLICATION_XML_TYPE);
    }

    @Path("JAXBElementListResource")
    @Produces({"application/xml", "application/json"})
    @Consumes({"application/xml", "application/json"})
    public static class JAXBElementListResource extends AResource<List<JAXBElement<String>>> {
    }

    private List<JAXBElement<String>> getJAXBElementList() {
        return Arrays.asList(getJAXBElementArray());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementListXMLRepresentation() {
        _testListOrArray(true, MediaType.APPLICATION_XML_TYPE);
    }

    public void _testListOrArray(final boolean isList, final MediaType mt) {
        final Object in = isList ? getJAXBElementList() : getJAXBElementArray();
        final GenericType gt = isList ? new GenericType<List<JAXBElement<String>>>() {
        } : new GenericType<JAXBElement<String>[]>() {
        };

        final WebTarget target = target(isList ? "JAXBElementListResource" : "JAXBElementArrayResource");
        final Object out = target.request(mt).post(Entity.entity(new GenericEntity(in, gt.getType()), mt), gt);

        final List<JAXBElement<String>> inList = isList ? ((List<JAXBElement<String>>) in) : Arrays
                .asList((JAXBElement<String>[]) in);
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
    @Produces({"application/xml", "application/json"})
    @Consumes({"application/xml", "application/json"})
    public static class JAXBElementArrayResource extends AResource<JAXBElement<String>[]> {
    }

    private JAXBElement<String>[] getJAXBElementArray() {
        return new JAXBElement[] {
                new JAXBElement(QName.valueOf("element1"), String.class, "ahoj"),
                new JAXBElement(QName.valueOf("element2"), String.class, "nazdar")
        };
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementArrayXMLRepresentation() {
        _testListOrArray(false, MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementArrayJSONRepresentation() {
        _testListOrArray(false, MediaType.APPLICATION_JSON_TYPE);
    }

    @Path("JAXBElementBeanResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBElementBeanResourceMediaType extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementBeanRepresentationMediaType() {
        _test(new JaxbBean("CONTENT"), JAXBElementBeanResourceMediaType.class, MediaType.valueOf("application/foo+xml"));
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementBeanRepresentationError() {
        final WebTarget target = target("JAXBElementBeanResource");

        final String xml = "<root><value>foo";
        final Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("JAXBElementBeanTextResource")
    @Produces("text/xml")
    @Consumes("text/xml")
    public static class JAXBElementBeanTextResource extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBElementBeanTextRepresentation() {
        _test(new JaxbBean("CONTENT"), JAXBElementBeanTextResource.class, MediaType.TEXT_XML_TYPE);
    }

    @Path("JaxbBeanResourceAtom")
    @Produces("application/atom+xml")
    @Consumes("application/atom+xml")
    public static class JaxbBeanResourceAtom extends AResource<JAXBElement<JaxbBean>> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJaxbBeanRepresentationMediaTypeAtom() {
        _test(new JaxbBean("CONTENT"), JaxbBeanResourceAtom.class, MediaType.valueOf("application/atom+xml"));
    }

    @Path("JAXBTypeResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBTypeResource {

        @POST
        public JaxbBean post(final JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Override
    protected Application configure() {
        return ((ResourceConfig) super.configure()).register(new JettisonFeature());
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        super.configureClient(config);
        config.register(new JettisonFeature());
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBTypeRepresentation() {
        final WebTarget target = target("JAXBTypeResource");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBeanType out = target.request().post(Entity.entity(in, "application/xml"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBTypeResourceMediaType extends JAXBTypeResource {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBTypeRepresentationMediaType() {
        final WebTarget target = target("JAXBTypeResourceMediaType");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBeanType out = target.request().post(Entity.entity(in, "application/foo+xml"), JaxbBeanType.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBObjectResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBObjectResource {

        @POST
        public Object post(final Object o) {
            return o;
        }
    }

    @Provider
    public static class JAXBObjectResolver implements ContextResolver<JAXBContext> {

        public JAXBContext getContext(final Class<?> c) {
            if (Object.class == c) {
                try {
                    return JAXBContext.newInstance(JaxbBean.class);
                } catch (final JAXBException ex) {
                }
            }
            return null;
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBObjectRepresentation() {
        final WebTarget target = target("JAXBObjectResource");
        final Object in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/xml"), JaxbBean.class);
        assertEquals(in, out);
    }

    @Path("JAXBObjectResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBObjectResourceMediaType extends JAXBObjectResource {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBObjectRepresentationMediaType() {
        final WebTarget target = target("JAXBObjectResourceMediaType");
        final Object in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/foo+xml"), JaxbBean.class);
        assertEquals(in, out);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBObjectRepresentationError() {
        final WebTarget target = target("JAXBObjectResource");

        final String xml = "<root>foo</root>";
        final Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("FileResource")
    public static class FileResource extends AResource<File> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testFileRepresentation() throws IOException {
        final FileProvider fp = new FileProvider();
        final File in = fp.readFrom(File.class, File.class, null, null, null,
                new ByteArrayInputStream("CONTENT".getBytes()));

        _test(in, FileResource.class);
    }

    @Produces("application/x-www-form-urlencoded")
    @Consumes("application/x-www-form-urlencoded")
    @Path("FormResource")
    public static class FormResource extends AResource<Form> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testFormRepresentation() {
        final Form fp = new Form();
        fp.param("Email", "johndoe@gmail.com");
        fp.param("Passwd", "north 23AZ");
        fp.param("service", "cl");
        fp.param("source", "Gulp-CalGul-1.05");

        final WebTarget target = target("FormResource");
        final Form response = target.request().post(Entity.entity(fp, MediaType.APPLICATION_FORM_URLENCODED_TYPE), Form.class);

        assertEquals(fp.asMap().size(), response.asMap().size());
        for (final Map.Entry<String, List<String>> entry : fp.asMap().entrySet()) {
            final List<String> s = response.asMap().get(entry.getKey());
            assertEquals(entry.getValue().size(), s.size());
            for (Iterator<String> it1 = entry.getValue().listIterator(), it2 = s.listIterator(); it1.hasNext(); ) {
                assertEquals(it1.next(), it2.next());
            }
        }
    }

    @Produces("application/json")
    @Consumes("application/json")
    @Path("JSONObjectResource")
    public static class JSONObjectResource extends AResource<JSONObject> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJSONObjectRepresentation() throws Exception {
        final JSONObject object = new JSONObject();
        object.put("userid", 1234)
                .put("username", "1234")
                .put("email", "a@b")
                .put("password", "****");

        _test(object, JSONObjectResource.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Produces("application/xxx+json")
    @Consumes("application/xxx+json")
    @Path("JSONObjectResourceGeneralMediaType")
    public static class JSONObjectResourceGeneralMediaType extends AResource<JSONObject> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJSONObjectRepresentationGeneralMediaTyp() throws Exception {
        final JSONObject object = new JSONObject();
        object.put("userid", 1234)
                .put("username", "1234")
                .put("email", "a@b")
                .put("password", "****");

        _test(object, JSONObjectResourceGeneralMediaType.class, MediaType.valueOf("application/xxx+json"));
    }

    @Produces("application/json")
    @Consumes("application/json")
    @Path("JSONOArrayResource")
    public static class JSONOArrayResource extends AResource<JSONArray> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJSONArrayRepresentation() throws Exception {
        final JSONArray array = new JSONArray();
        array.put("One").put("Two").put("Three").put(1).put(2.0);

        _test(array, JSONOArrayResource.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Produces("application/xxx+json")
    @Consumes("application/xxx+json")
    @Path("JSONOArrayResourceGeneralMediaType")
    public static class JSONOArrayResourceGeneralMediaType extends AResource<JSONArray> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJSONArrayRepresentationGeneralMediaType() throws Exception {
        final JSONArray array = new JSONArray();
        array.put("One").put("Two").put("Three").put(1).put(2.0);

        _test(array, JSONOArrayResourceGeneralMediaType.class, MediaType.valueOf("application/xxx+json"));
    }

    //    @Path("FeedResource")
    //    public static class FeedResource extends AResource<Feed> {
    //    }
    //
    //    @Test
    //    public void testFeedRepresentation() throws Exception {
    //        InputStream in = this.getClass().getResourceAsStream("feed.xml");
    //        AtomFeedProvider afp = new AtomFeedProvider();
    //        Feed f = afp.readFrom(Feed.class, Feed.class, null, null, null, in);
    //
    //        _test(f, FeedResource.class);
    //    }
    //
    //    @Path("EntryResource")
    //    public static class EntryResource extends AResource<Entry> {
    //    }
    //
    //    @Test
    //    public void testEntryRepresentation() throws Exception {
    //        InputStream in = this.getClass().getResourceAsStream("entry.xml");
    //        AtomEntryProvider afp = new AtomEntryProvider();
    //        Entry e = afp.readFrom(Entry.class, Entry.class, null, null, null, in);
    //
    //        _test(e, EntryResource.class);
    //    }

    @Path("ReaderResource")
    public static class ReaderResource extends AResource<Reader> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testReaderRepresentation() throws Exception {
        _test(new StringReader("CONTENT"), ReaderResource.class);
    }

    private static final String XML_DOCUMENT = "<n:x xmlns:n=\"urn:n\"><n:e>CONTNET</n:e></n:x>";

    @Path("StreamSourceResource")
    public static class StreamSourceResource extends AResource<StreamSource> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testStreamSourceRepresentation() throws Exception {
        final StreamSource ss = new StreamSource(
                new ByteArrayInputStream(XML_DOCUMENT.getBytes()));
        _test(ss, StreamSourceResource.class);
    }

    @Path("SAXSourceResource")
    public static class SAXSourceResource extends AResource<SAXSource> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testSAXSourceRepresentation() throws Exception {
        final StreamSource ss = new StreamSource(
                new ByteArrayInputStream(XML_DOCUMENT.getBytes()));
        _test(ss, SAXSourceResource.class);
    }

    @Path("DOMSourceResource")
    public static class DOMSourceResource extends AResource<DOMSource> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testDOMSourceRepresentation() throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final Document d = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(XML_DOCUMENT)));
        final DOMSource ds = new DOMSource(d);
        _test(ds, DOMSourceResource.class);
    }

    @Path("DocumentResource")
    public static class DocumentResource extends AResource<Document> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testDocumentRepresentation() throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final Document d = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(XML_DOCUMENT)));
        _test(d, DocumentResource.class);
    }

    @Path("FormMultivaluedMapResource")
    @Produces("application/x-www-form-urlencoded")
    @Consumes("application/x-www-form-urlencoded")
    public static class FormMultivaluedMapResource {

        @POST
        public MultivaluedMap<String, String> post(final MultivaluedMap<String, String> t) {
            return t;
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testFormMultivaluedMapRepresentation() {
        final MultivaluedMap<String, String> fp = new MultivaluedStringMap();
        fp.add("Email", "johndoe@gmail.com");
        fp.add("Passwd", "north 23AZ");
        fp.add("service", "cl");
        fp.add("source", "Gulp-CalGul-1.05");
        fp.add("source", "foo.java");
        fp.add("source", "bar.java");

        final WebTarget target = target("FormMultivaluedMapResource");
        final MultivaluedMap _fp = target.request()
                .post(Entity.entity(fp, "application/x-www-form-urlencoded"), MultivaluedMap.class);
        assertEquals(fp, _fp);
    }

    @Path("StreamingOutputResource")
    public static class StreamingOutputResource {

        @GET
        public StreamingOutput get() {
            return new StreamingOutput() {
                public void write(final OutputStream entity) throws IOException {
                    entity.write("CONTENT".getBytes());
                }
            };
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testStreamingOutputRepresentation() throws Exception {
        final WebTarget target = target("StreamingOutputResource");
        assertEquals("CONTENT", target.request().get(String.class));
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

        final Response rib = target.request().post(
                Entity.entity(new JAXBElement<>(new QName("test"), String.class, "CONTENT"), "application/json"));

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

    @Path("JaxbBeanResourceFastInfoset")
    @Produces("application/fastinfoset")
    @Consumes("application/fastinfoset")
    public static class JaxbBeanResourceFastInfoset extends AResource<JaxbBean> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    @Disabled("TODO: unignore once fi support implemented (JERSEY-1190)")
    // TODO: unignore once fi support implemented (JERSEY-1190)
    public void testJaxbBeanRepresentationFastInfoset() {
        final WebTarget target = target("JaxbBeanResourceFastInfoset");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/fastinfoset"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBElementBeanResourceFastInfoset")
    @Produces("application/fastinfoset")
    @Consumes("application/fastinfoset")
    public static class JAXBElementBeanResourceFastInfoset extends AResource<JAXBElement<JaxbBeanType>> {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    @Disabled("TODO: unignore once fi support implemented (JERSEY-1190)")
    // TODO: unignore once fi support implemented (JERSEY-1190)
    public void testJAXBElementBeanRepresentationFastInfoset() {
        final WebTarget target = target("JAXBElementBeanResourceFastInfoset");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBean out = target.request().post(Entity.entity(in, "application/fastinfoset"), JaxbBean.class);
        assertEquals(in.value, out.value);
    }

    @Path("JAXBTypeResourceFastInfoset")
    @Produces("application/fastinfoset")
    @Consumes("application/fastinfoset")
    public static class JAXBTypeResourceFastInfoset {

        @POST
        public JaxbBean post(final JaxbBeanType t) {
            return new JaxbBean(t.value);
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    @Disabled("TODO: unignore once fi support implemented (JERSEY-1190)")
    // TODO: unignore once fi support implemented (JERSEY-1190)
    public void testJAXBTypeRepresentationFastInfoset() {
        final WebTarget target = target("JAXBTypeResourceFastInfoset");
        final JaxbBean in = new JaxbBean("CONTENT");
        final JaxbBeanType out = target.request().post(Entity.entity(in, "application/fastinfoset"), JaxbBeanType.class);
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

    @Path("JAXBArrayResource")
    @Produces("application/xml")
    @Consumes("application/xml")
    public static class JAXBArrayResource {

        @POST
        public JaxbBean[] post(final JaxbBean[] l) {
            return l;
        }

        @GET
        public JaxbBean[] get() {
            final ArrayList<JaxbBean> l = new ArrayList<>();
            l.add(new JaxbBean("one"));
            l.add(new JaxbBean("two"));
            l.add(new JaxbBean("three"));
            return l.toArray(new JaxbBean[l.size()]);
        }

        @POST
        @Path("type")
        public JaxbBean[] postType(final JaxbBeanType[] l) {
            final List<JaxbBean> beans = new ArrayList<>();
            for (final JaxbBeanType t : l) {
                beans.add(new JaxbBean(t.value));
            }
            return beans.toArray(new JaxbBean[beans.size()]);
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBArrayRepresentation() {
        final WebTarget target = target("JAXBArrayResource");

        final JaxbBean[] a = target.request().get(JaxbBean[].class);
        JaxbBean[] b = target.request().post(Entity.entity(a, "application/xml"), JaxbBean[].class);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }

        b = target.path("type").request().post(Entity.entity(a, "application/xml"), JaxbBean[].class);
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i]);
        }
    }

    @Path("JAXBListResourceMediaType")
    @Produces("application/foo+xml")
    @Consumes("application/foo+xml")
    public static class JAXBListResourceMediaType extends JAXBListResource {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBListRepresentationMediaType() {
        final WebTarget target = target("JAXBListResourceMediaType");

        Collection<JaxbBean> a = target.request().get(
                new GenericType<Collection<JaxbBean>>() {
                });
        Collection<JaxbBean> b = target.request()
                .post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
                        }, "application/foo+xml"),
                        new GenericType<Collection<JaxbBean>>() {
                        });

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/foo+xml"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new LinkedList<>(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue<JaxbBean>) a) {
        }, "application/foo+xml"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new HashSet<>(a);
        b = target.path("set").request().post(Entity.entity(new GenericEntity<Set<JaxbBean>>((Set<JaxbBean>) a) {
        }, "application/foo+xml"), new GenericType<Set<JaxbBean>>() {
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

        final Stack<JaxbBean> s = new Stack<>();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/foo+xml"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);

        a = new MyArrayList<>(a);
        b = target.path("custom").request()
                .post(Entity.entity(new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList<JaxbBean>) a) {
                }, "application/foo+xml"), new GenericType<MyArrayList<JaxbBean>>() {
                });
        assertEquals(a, b);
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBListRepresentationError() {
        final WebTarget target = target("JAXBListResource");

        final String xml = "<root><value>foo";
        final Response cr = target.request().post(Entity.entity(xml, "application/xml"));
        assertEquals(400, cr.getStatus());
    }

    @Path("JAXBListResourceFastInfoset")
    @Produces("application/fastinfoset")
    @Consumes("application/fastinfoset")
    public static class JAXBListResourceFastInfoset extends JAXBListResource {
    }

    /**
     * TODO, the unmarshalling fails.
     */
    @Test
    @Execution(ExecutionMode.CONCURRENT)
    @Disabled("TODO: unignore once fi support implemented (JERSEY-1190)")
    // TODO: unignore once fi support implemented (JERSEY-1190)
    public void testJAXBListRepresentationFastInfoset() {
        final WebTarget target = target("JAXBListResourceFastInfoset");

        Collection<JaxbBean> a = target.request().get(new GenericType<Collection<JaxbBean>>() {
        });

        Collection<JaxbBean> b = target.request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
                }, "application/fastinfoset"), new GenericType<Collection<JaxbBean>>() {
                }
        );

        assertEquals(a, b);

        b = target.path("type").request().post(Entity.entity(new GenericEntity<Collection<JaxbBean>>(a) {
        }, "application/fastinfoset"), new GenericType<Collection<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new LinkedList<>(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue<JaxbBean>) a) {
        }, "application/fastinfoset"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);

        a = new HashSet<>(a);
        b = target.path("set").request().post(Entity.entity(new GenericEntity<Set<JaxbBean>>((Set<JaxbBean>) a) {
        }, "application/fastinfoset"), new GenericType<Set<JaxbBean>>() {
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

        final Stack<JaxbBean> s = new Stack<>();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/fastinfoset"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);

        a = new MyArrayList<>(a);
        b = target.path("custom").request()
                .post(Entity.entity(new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList<JaxbBean>) a) {
                }, "application/fastinfoset"), new GenericType<MyArrayList<JaxbBean>>() {
                });
        assertEquals(a, b);
    }

    @Path("JAXBListResourceJSON")
    @Produces("application/json")
    @Consumes("application/json")
    public static class JAXBListResourceJSON extends JAXBListResource {
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testJAXBListRepresentationJSON() throws Exception {
        final WebTarget target = target("JAXBListResourceJSON");

        Collection<JaxbBean> a = target.request().get(
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

        a = new LinkedList<>(a);
        b = target.path("queue").request().post(Entity.entity(new GenericEntity<Queue<JaxbBean>>((Queue<JaxbBean>) a) {
        }, "application/json"), new GenericType<Queue<JaxbBean>>() {
        });
        assertEquals(a, b);

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

        final Stack<JaxbBean> s = new Stack<>();
        s.addAll(a);
        b = target.path("stack").request().post(Entity.entity(new GenericEntity<Stack<JaxbBean>>(s) {
        }, "application/json"), new GenericType<Stack<JaxbBean>>() {
        });
        assertEquals(s, b);

        a = new MyArrayList<>(a);
        b = target.path("custom").request()
                .post(Entity.entity(new GenericEntity<MyArrayList<JaxbBean>>((MyArrayList<JaxbBean>) a) {
                }, "application/json"), new GenericType<MyArrayList<JaxbBean>>() {
                });
        assertEquals(a, b);

        // TODO: would be nice to produce/consume a real JSON array like following
        // instead of what we have now:
        //        JSONArray a = r.get(JSONArray.class);
        //        JSONArray b = new JSONArray().
        //                put(new JSONObject().put("value", "one")).
        //                put(new JSONObject().put("value", "two")).
        //                put(new JSONObject().put("value", "three"));
        //        assertEquals(a.toString(), b.toString());
        //        JSONArray c = r.post(JSONArray.class, b);
        //        assertEquals(a.toString(), c.toString());
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

        // TODO: would be nice to produce/consume a real JSON array like following
        // instead of what we have now:
        //        JSONArray a = r.get(JSONArray.class);
        //        JSONArray b = new JSONArray().
        //                put(new JSONObject().put("value", "one")).
        //                put(new JSONObject().put("value", "two")).
        //                put(new JSONObject().put("value", "three"));
        //        assertEquals(a.toString(), b.toString());
        //        JSONArray c = r.post(JSONArray.class, b);
        //        assertEquals(a.toString(), c.toString());
    }

    @Path("/NoContentTypeJAXBResource")
    public static class NoContentTypeJAXBResource {

        @POST
        public JaxbBean post(@Context final HttpHeaders headers, final JaxbBean bean) {
            assertThat(headers.getMediaType(), is(MediaType.APPLICATION_XML_TYPE));
            return bean;
        }
    }

    @Test
    @Execution(ExecutionMode.CONCURRENT)
    public void testNoContentTypeJaxbEntity() throws IOException {
        assertThat(target("NoContentTypeJAXBResource").request("application/xml").post(Entity.xml(new JaxbBean("foo")))
                        .getMediaType(),
                is(MediaType.APPLICATION_XML_TYPE));
    }
}
