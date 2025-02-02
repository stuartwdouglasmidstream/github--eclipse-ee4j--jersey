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

package org.glassfish.jersey.media.multipart.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collections;
import java.util.Set;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.MultiPart;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link org.glassfish.jersey.media.multipart.internal.MultiPartReaderClientSide} (in the client) and
 * {@link org.glassfish.jersey.media.multipart.internal.MultiPartWriter} (in the server)
 .*/
public class MultiPartReaderWriterTest extends MultiPartJerseyTest {

    private static Path TMP_DIRECTORY;
    private static String ORIGINAL_TMP_DIRECTORY;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        ORIGINAL_TMP_DIRECTORY = System.getProperty("java.io.tmpdir");

        TMP_DIRECTORY = Files.createTempDirectory(MultiPartReaderWriterTest.class.getName());
        System.setProperty("java.io.tmpdir", TMP_DIRECTORY.toString());
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();

        try {
            Files.delete(TMP_DIRECTORY);
        } finally {
            System.setProperty("java.io.tmpdir", ORIGINAL_TMP_DIRECTORY);
        }
    }

    @Override
    protected Set<Class<?>> getResourceClasses() {
        return Collections.<Class<?>>singleton(MultiPartResource.class);
    }

    @Test
    public void testZero() {
        final WebTarget target = target().path("multipart").path("zero");
        final String result = target.request("text/plain").get(String.class);
        assertEquals("Hello, world\r\n", result);
    }

    @Test
    public void testOne() {
        final WebTarget target = target().path("multipart/one");

        try {
            final MultiPart result = target.request("multipart/mixed").get(MultiPart.class);
            checkMediaType(new MediaType("multipart", "mixed"), result.getMediaType());
            assertEquals(1, result.getBodyParts().size());
            final BodyPart part = result.getBodyParts().get(0);
            checkMediaType(new MediaType("text", "plain"), part.getMediaType());
            checkEntity("This is the only segment", (BodyPartEntity) part.getEntity());

            result.getParameterizedHeaders();
            result.cleanup();
        } catch (final IOException | ParseException e) {
            e.printStackTrace(System.out);
            fail("Caught exception: " + e);
        }
    }

    @Test
    public void testETag() {
        final WebTarget target = target().path("multipart/etag");

        try {
            final MultiPart result = target.request("multipart/mixed").get(MultiPart.class);
            checkMediaType(new MediaType("multipart", "mixed"), result.getMediaType());
            assertEquals(1, result.getBodyParts().size());
            final BodyPart part = result.getBodyParts().get(0);
            checkMediaType(new MediaType("text", "plain"), part.getMediaType());
            checkEntity("This is the only segment", (BodyPartEntity) part.getEntity());
            assertEquals("\"value\"", part.getHeaders().getFirst("ETag"));

            result.getParameterizedHeaders();
            result.cleanup();
        } catch (final IOException | ParseException e) {
            e.printStackTrace(System.out);
            fail("Caught exception: " + e);
        }
    }

    @Test
    public void testTwo() {
        final WebTarget target = target().path("multipart/two");
        try {
            final MultiPart result = target.request("multipart/mixed").get(MultiPart.class);
            checkMediaType(new MediaType("multipart", "mixed"), result.getMediaType());
            assertEquals(2, result.getBodyParts().size());
            final BodyPart part1 = result.getBodyParts().get(0);
            checkMediaType(new MediaType("text", "plain"), part1.getMediaType());
            checkEntity("This is the first segment", (BodyPartEntity) part1.getEntity());
            final BodyPart part2 = result.getBodyParts().get(1);
            checkMediaType(new MediaType("text", "xml"), part2.getMediaType());
            checkEntity("<outer><inner>value</inner></outer>", (BodyPartEntity) part2.getEntity());

            result.getParameterizedHeaders();
            result.cleanup();
        } catch (final IOException | ParseException e) {
            e.printStackTrace(System.out);
            fail("Caught exception: " + e);
        }
    }

    @Test
    public void testThree() {
        final WebTarget target = target().path("multipart/three");
        try {
            final MultiPart result = target.request("multipart/mixed").get(MultiPart.class);
            checkMediaType(new MediaType("multipart", "mixed"), result.getMediaType());
            assertEquals(2, result.getBodyParts().size());
            final BodyPart part1 = result.getBodyParts().get(0);
            checkMediaType(new MediaType("text", "plain"), part1.getMediaType());
            checkEntity("This is the first segment", (BodyPartEntity) part1.getEntity());
            final BodyPart part2 = result.getBodyParts().get(1);
            checkMediaType(new MediaType("x-application", "x-format"), part2.getMediaType());
            final MultiPartBean entity = part2.getEntityAs(MultiPartBean.class);
            assertEquals("myname", entity.getName());
            assertEquals("myvalue", entity.getValue());

            result.getParameterizedHeaders();
            result.cleanup();
        } catch (final IOException | ParseException e) {
            e.printStackTrace(System.out);
            fail("Caught exception: " + e);
        }
    }

    @Test
    public void testFour() {
        final WebTarget target = target().path("multipart/four");

        final MultiPartBean bean = new MultiPartBean("myname", "myvalue");
        final MultiPart entity = new MultiPart()
                .bodyPart("This is the first segment", new MediaType("text", "plain"))
                .bodyPart(bean, new MediaType("x-application", "x-format"));
        final String response = target.request("text/plain").put(Entity.entity(entity, "multipart/mixed"), String.class);
        if (!response.startsWith("SUCCESS:")) {
            fail("Response is '" + response + "'");
        }
    }

    @Test
    public void testFourBiz() {
        final WebTarget target = target().path("multipart/four");

        final MultiPartBean bean = new MultiPartBean("myname", "myvalue");
        final MultiPart entity = new MultiPart()
                .bodyPart("This is the first segment", new MediaType("text", "plain"))
                .bodyPart(bean, new MediaType("x-application", "x-format"));
        final String response = target.request("text/plain").header("Content-Type", "multipart/mixed")
                .put(Entity.entity(entity, "multipart/mixed"), String.class);
        if (!response.startsWith("SUCCESS:")) {
            fail("Response is '" + response + "'");
        }
    }

    /**
     * Test sending a completely empty MultiPart.
     */
    @Test
    public void testSix() {
        assertThrows(ProcessingException.class, () -> {
            target()
                    .path("multipart/six")
                    .request("text/plain")
                    .post(Entity.entity(new MultiPart(), "multipart/mixed"), String.class);
            fail("Should have thrown an exception about zero body parts");
        });
    }

    /**
     * Zero length body part.
     */
    @Test
    public void testTen() {
        final WebTarget target = target().path("multipart/ten");

        final MultiPartBean bean = new MultiPartBean("myname", "myvalue");
        final MultiPart entity = new MultiPart()
                .bodyPart(bean, new MediaType("x-application", "x-format"))
                .bodyPart("", MediaType.APPLICATION_OCTET_STREAM_TYPE);
        final String response = target.request("text/plain").put(Entity.entity(entity, "multipart/mixed"), String.class);

        if (!response.startsWith("SUCCESS:")) {
            fail("Response is '" + response + "'");
        }
    }

    /**
     * Echo back various sized body part entities, and check size/content
     */
    @Test
    public void testEleven() throws Exception {
        final String seed = "0123456789ABCDEF";
        checkEleven(seed, 0);
        checkEleven(seed, 1);
        checkEleven(seed, 10);
        checkEleven(seed, 100);
        checkEleven(seed, 1000);
        checkEleven(seed, 10000);
        checkEleven(seed, 100000);
    }

    /**
     * Echo back the multipart that was sent.
     */
    @Test
    public void testTwelve() throws Exception {
        final WebTarget target = target().path("multipart/twelve");

        final MultiPart entity = new MultiPart().bodyPart("CONTENT", MediaType.TEXT_PLAIN_TYPE);
        final MultiPart response = target.request("multipart/mixed")
                .put(Entity.entity(entity, "multipart/mixed"), MultiPart.class);
        final String actual = response.getBodyParts().get(0).getEntityAs(String.class);
        assertEquals("CONTENT", actual);
        response.cleanup();
    }

    /**
     * Call clean up explicitly.
     */
    @Test
    public void testThirteen() {
        final WebTarget target = target().path("multipart/thirteen");

        final MultiPart entity = new MultiPart()
                .bodyPart("CONTENT", MediaType.TEXT_PLAIN_TYPE);
        final String response = target.request("multipart/mixed").put(Entity.entity(entity, "multipart/mixed"), String.class);
        assertEquals("cleanup", response);
    }

    /**
     * Test for JERSEY-2515 - Jersey should not leave any temporary files after verifying that it is possible
     * to create files in java.io.tmpdir.
     */
    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldNotBeAnyTemporaryFiles() {
        // Make sure the MBR is initialized on the client-side as well.
        target().request().get();
        assertEquals(0, TMP_DIRECTORY.toFile().listFiles().length);
    }

    private void checkEntity(final String expected, final BodyPartEntity entity) throws IOException {
        // Convert the raw bytes into a String
        final InputStreamReader sr = new InputStreamReader(entity.getInputStream());
        final StringWriter sw = new StringWriter();
        while (true) {
            final int ch = sr.read();
            if (ch < 0) {
                break;
            }
            sw.append((char) ch);
        }
        // Perform the comparison
        assertEquals(expected, sw.toString());
    }

    private void checkEleven(final String seed, final int multiplier) throws Exception {
        final StringBuilder sb = new StringBuilder(seed.length() * multiplier);
        for (int i = 0; i < multiplier; i++) {
            sb.append(seed);
        }
        final String expected = sb.toString();

        final WebTarget target = target().path("multipart/eleven");

        final MultiPart entity = new MultiPart().bodyPart(expected, MediaType.TEXT_PLAIN_TYPE);
        final MultiPart response = target.request("multipart/mixed")
                .put(Entity.entity(entity, "multipart/mixed"), MultiPart.class);
        final String actual = response.getBodyParts().get(0).getEntityAs(String.class);
        assertEquals(expected.length(), actual.length(), "Length for multiplier " + multiplier);
        assertEquals(expected, actual, "Content for multiplier " + multiplier);
        response.cleanup();
    }

    private void checkMediaType(final MediaType expected, final MediaType actual) {
        assertEquals(expected.getType(), actual.getType(), "Expected MediaType=" + expected);
        assertEquals(expected.getSubtype(), actual.getSubtype(), "Expected MediaType=" + expected);
    }

}
