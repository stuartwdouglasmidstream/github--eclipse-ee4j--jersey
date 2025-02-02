/*
 * Copyright (c) 2013, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.uri;

import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link UriComponent} class.
 * <p/>
 * Migrated from Jersey 1.x E2E tests:
 * <ul>
 * <li>{@code com.sun.jersey.impl.uri.riComponentDecodeTest}</li>
 * <li>{@code com.sun.jersey.impl.uri.riComponentEncodeTest}</li>
 * <li>{@code com.sun.jersey.impl.uri.riComponentValidateTest}</li>
 * </ul>
 *
 * @author Marek Potociar
 */
public class UriComponentTest {

    @Test
    public void testNull() {
        decodeCatch(null);
    }

    @Test
    public void testEmpty() {
        assertEquals("", UriComponent.decode("", null));
    }

    @Test
    public void testNoPercentEscapedOctets() {
        assertEquals("xyz", UriComponent.decode("xyz", null));
    }

    @Test
    public void testZeroValuePercentEscapedOctet() {
        assertEquals("\u0000", UriComponent.decode("%00", null));
    }

    @Test
    public void testASCIIPercentEscapedOctets() {
        assertEquals(" ", UriComponent.decode("%20", null));
        assertEquals("   ", UriComponent.decode("%20%20%20", null));
        assertEquals("a b c ", UriComponent.decode("a%20b%20c%20", null));
        assertEquals("a  b  c  ", UriComponent.decode("a%20%20b%20%20c%20%20", null));

        assertEquals("0123456789", UriComponent.decode("%30%31%32%33%34%35%36%37%38%39", null));
        assertEquals("00112233445566778899", UriComponent.decode("%300%311%322%333%344%355%366%377%388%399", null));
    }

    @Test
    public void testPercentUnicodeEscapedOctets() {
        assertEquals("copyright\u00a9", UriComponent.decode("copyright%c2%a9", null));
        assertEquals("copyright\u00a9", UriComponent.decode("copyright%C2%A9", null));

        assertEquals("thumbsup\ud83d\udc4d", UriComponent.decode("thumbsup%f0%9f%91%8d", null));
        assertEquals("thumbsup\ud83d\udc4d", UriComponent.decode("thumbsup%F0%9F%91%8D", null));
    }

    @Test
    public void testHost() {
        assertEquals("[fec0::abcd%251]", UriComponent.decode("[fec0::abcd%251]", UriComponent.Type.HOST));
    }

    @Test
    public void testInvalidPercentEscapedOctets() {
        assertTrue(decodeCatch("%"));
        assertTrue(decodeCatch("%1"));
        assertTrue(decodeCatch(" %1"));
        assertTrue(decodeCatch("%z1"));
        assertTrue(decodeCatch("%1z"));
    }

    private boolean decodeCatch(final String s) {
        try {
            UriComponent.decode(s, null);
            return false;
        } catch (final IllegalArgumentException e) {
            return true;
        }
    }

    @Test
    public void testDecodePathEmptySlash() {
        _testDecodePath("", "");
        _testDecodePath("/", "", "");
        _testDecodePath("//", "", "", "");
        _testDecodePath("///", "", "", "", "");
    }

    @Test
    public void testDecodePath() {
        _testDecodePath("a", "a");
        _testDecodePath("/a", "", "a");
        _testDecodePath("/a/", "", "a", "");
        _testDecodePath("/a//", "", "a", "", "");
        _testDecodePath("/a///", "", "a", "", "", "");

        _testDecodePath("a/b/c", "a", "b", "c");
        _testDecodePath("a//b//c//", "a", "", "b", "", "c", "", "");
        _testDecodePath("//a//b//c//", "", "", "a", "", "b", "", "c", "", "");
        _testDecodePath("///a///b///c///", "", "", "", "a", "", "", "b", "", "", "c", "", "", "");
    }

    @Test
    public void testDecodeUnicodePath() {
        _testDecodePath("/%F0%9F%91%8D/thumbsup", "", "\ud83d\udc4d", "thumbsup");
    }

    private void _testDecodePath(final String path, final String... segments) {
        final List<PathSegment> ps = UriComponent.decodePath(path, true);
        assertEquals(segments.length, ps.size());

        for (int i = 0; i < segments.length; i++) {
            assertEquals(segments[i], ps.get(i).getPath());
        }
    }

    @Test
    public void testDecodeQuery() {
        _testDecodeQuery("");
        _testDecodeQuery("&");
        _testDecodeQuery("=");
        _testDecodeQuery("&=junk");
        _testDecodeQuery("&&");
        _testDecodeQuery("a", "a", "");
        _testDecodeQuery("a&", "a", "");
        _testDecodeQuery("&a&", "a", "");
        _testDecodeQuery("a&&", "a", "");
        _testDecodeQuery("a=", "a", "");
        _testDecodeQuery("a=&", "a", "");
        _testDecodeQuery("a=&&", "a", "");
        _testDecodeQuery("a=x", "a", "x");
        _testDecodeQuery("a==x", "a", "=x");
        _testDecodeQuery("a=x&", "a", "x");
        _testDecodeQuery("a=x&&", "a", "x");
        _testDecodeQuery("a=x&b=y", "a", "x", "b", "y");
        _testDecodeQuery("a=x&&b=y", "a", "x", "b", "y");

        _testDecodeQuery("+a+", true, " a ", "");
        _testDecodeQuery("+a+=", true, " a ", "");
        _testDecodeQuery("+a+=+x+", true, " a ", " x ");
        _testDecodeQuery("%20a%20=%20x%20", true, " a ", " x ");

        _testDecodeQuery("+a+", false, " a ", "");
        _testDecodeQuery("+a+=", false, " a ", "");
        _testDecodeQuery("+a+=+x+", false, " a ", "+x+");
        _testDecodeQuery("%20a%20=%20x%20", false, " a ", "%20x%20");

        _testDecodeQuery("+a+", false, true, "+a+", "");
        _testDecodeQuery("+a+=", false, true, "+a+", "");
        _testDecodeQuery("+a+=+x+", false, true, "+a+", " x ");
        _testDecodeQuery("%20a%20=%20x%20", false, true, "%20a%20", " x ");

        _testDecodeQuery("+a+", false, false, "+a+", "");
        _testDecodeQuery("+a+=", false, false, "+a+", "");
        _testDecodeQuery("+a+=+x+", false, false, "+a+", "+x+");
        _testDecodeQuery("%20a%20=%20x%20", false, false, "%20a%20", "%20x%20");
    }

    @Test
    public void testDecodeUnicodeQuery() {
        _testDecodeQuery("+thumbsup%F0%9F%91%8D+=+chicken%F0%9F%90%94+", true, " thumbsup\ud83d\udc4d ", " chicken\ud83d\udc14 ");
        _testDecodeQuery("%20thumbsup%F0%9F%91%8D%20=%20chicken%F0%9F%90%94%20", true, " thumbsup\ud83d\udc4d ",
                " chicken\ud83d\udc14 ");
        _testDecodeQuery("+thumbsup%F0%9F%91%8D+=+chicken%F0%9F%90%94+", false, " thumbsup\ud83d\udc4d ",
                "+chicken%F0%9F%90%94+");
        _testDecodeQuery("%20thumbsup%F0%9F%91%8D%20=%20chicken%F0%9F%90%94%20", false, " thumbsup\ud83d\udc4d ",
                "%20chicken%F0%9F%90%94%20");
    }

    @Test
    public void testDecodeQueryParam() {
        assertEquals(" ", UriComponent.decode("+", UriComponent.Type.QUERY_PARAM));
        assertEquals("a b c ", UriComponent.decode("a+b+c+", UriComponent.Type.QUERY_PARAM));
        assertEquals(" ", UriComponent.decode("%20", UriComponent.Type.QUERY_PARAM));
        assertEquals("a b c ", UriComponent.decode("a%20b%20c%20", UriComponent.Type.QUERY_PARAM));
    }

    @Test
    public void testDecodeUnicodeQueryParam() {
        assertEquals("thumbsup \ud83d\udc4d chicken \ud83d\udc14",
                UriComponent.decode("thumbsup+%F0%9F%91%8D+chicken+%F0%9F%90%94", UriComponent.Type.QUERY_PARAM));
    }

    @Test
    public void testDecodeQueryParamSpaceEncoded() {
        assertEquals("+", UriComponent.decode("+", UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));
        assertEquals("a+b+c+", UriComponent.decode("a+b+c+", UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));
        assertEquals(" ", UriComponent.decode("%20", UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));
        assertEquals("a b c ", UriComponent.decode("a%20b%20c%20", UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));
    }

    private void _testDecodeQuery(final String q, final String... query) {
        _testDecodeQuery(q, true, query);
    }

    private void _testDecodeQuery(final String q, final boolean decode, final String... query) {
        _testDecodeQuery(q, true, decode, query);
    }

    private void _testDecodeQuery(final String q, final boolean decodeNames, final boolean decodeValues, final String... query) {
        final MultivaluedMap<String, String> queryParameters = UriComponent.decodeQuery(q, decodeNames, decodeValues);

        assertEquals(query.length / 2, queryParameters.size());

        for (int i = 0; i < query.length; i += 2) {
            assertEquals(query[i + 1], queryParameters.getFirst(query[i]));
        }
    }

    @Test
    public void testDecodeMatrix() {
        _testDecodeMatrix("path", "path");
        _testDecodeMatrix("path;", "path");
        _testDecodeMatrix("path;=", "path");
        _testDecodeMatrix("path;=junk", "path");
        _testDecodeMatrix("path;;", "path");
        _testDecodeMatrix("path;a", "path", "a", "");
        _testDecodeMatrix("path;;a", "path", "a", "");
        _testDecodeMatrix("path;a;", "path", "a", "");
        _testDecodeMatrix("path;a;;", "path", "a", "");
        _testDecodeMatrix("path;a=", "path", "a", "");
        _testDecodeMatrix("path;a=;", "path", "a", "");
        _testDecodeMatrix("path;a=;;", "path", "a", "");
        _testDecodeMatrix("path;a=x", "path", "a", "x");
        _testDecodeMatrix("path;a=x;", "path", "a", "x");
        _testDecodeMatrix("path;a=x;;", "path", "a", "x");
        _testDecodeMatrix("path;a=x;b=y", "path", "a", "x", "b", "y");
        _testDecodeMatrix("path;a=x;;b=y", "path", "a", "x", "b", "y");
        _testDecodeMatrix("path;a==x;", "path", "a", "=x");

        _testDecodeMatrix("", "");
        _testDecodeMatrix(";", "");
        _testDecodeMatrix(";=", "");
        _testDecodeMatrix(";=junk", "");
        _testDecodeMatrix(";;", "");
        _testDecodeMatrix(";a", "", "a", "");
        _testDecodeMatrix(";;a", "", "a", "");
        _testDecodeMatrix(";a;", "", "a", "");
        _testDecodeMatrix(";a;;", "", "a", "");
        _testDecodeMatrix(";a=", "", "a", "");
        _testDecodeMatrix(";a=;", "", "a", "");
        _testDecodeMatrix(";a=;;", "", "a", "");
        _testDecodeMatrix(";a=x", "", "a", "x");
        _testDecodeMatrix(";a==x", "", "a", "=x");
        _testDecodeMatrix(";a=x;", "", "a", "x");
        _testDecodeMatrix(";a=x;;", "", "a", "x");
        _testDecodeMatrix(";a=x;b=y", "", "a", "x", "b", "y");
        _testDecodeMatrix(";a=x;;b=y", "", "a", "x", "b", "y");

        _testDecodeMatrix(";%20a%20=%20x%20", "", true, " a ", " x ");
        _testDecodeMatrix(";%20a%20=%20x%20", "", false, " a ", "%20x%20");
    }

    @Test
    public void testDecodeUnicodeMatrix() {
        _testDecodeMatrix(";thumbsup%F0%9F%91%8D=chicken%F0%9F%90%94", "", true, "thumbsup\ud83d\udc4d", "chicken\ud83d\udc14");
        _testDecodeMatrix(";thumbsup%F0%9F%91%8D=chicken%F0%9F%90%94", "", false, "thumbsup\ud83d\udc4d", "chicken%F0%9F%90%94");
    }

    private void _testDecodeMatrix(final String path, final String pathSegment, final String... matrix) {
        _testDecodeMatrix(path, pathSegment, true, matrix);
    }

    private void _testDecodeMatrix(final String path, final String pathSegment, final boolean decode, final String... matrix) {
        final List<PathSegment> ps = UriComponent.decodePath(path, decode);
        final MultivaluedMap<String, String> matrixParameters = ps.get(0).getMatrixParameters();

        assertEquals(pathSegment, ps.get(0).getPath());
        assertEquals(matrix.length / 2, matrixParameters.size());

        for (int i = 0; i < matrix.length; i += 2) {
            assertEquals(matrix[i + 1], matrixParameters.getFirst(matrix[i]));
        }
    }

    @Test
    public void testEncodePathSegment() {
        assertEquals("%2Fa%2Fb%2Fc", UriComponent.encode("/a/b/c", UriComponent.Type.PATH_SEGMENT));
        assertEquals("%2Fa%20%2Fb%20%2Fc%20", UriComponent.encode("/a /b /c ", UriComponent.Type.PATH_SEGMENT));
        assertEquals("%2Fcopyright%C2%A9", UriComponent.encode("/copyright\u00a9", UriComponent.Type.PATH_SEGMENT));
        assertEquals("%2Fa%3Bx%2Fb%3Bx%2Fc%3Bx", UriComponent.encode("/a;x/b;x/c;x", UriComponent.Type.PATH_SEGMENT));
        assertEquals("%2Fthumbsup%F0%9F%91%8D", UriComponent.encode("/thumbsup\ud83d\udc4d", UriComponent.Type.PATH_SEGMENT));
    }

    @Test
    public void testEncodePath() {
        assertEquals("/a/b/c", UriComponent.encode("/a/b/c", UriComponent.Type.PATH));
        assertEquals("/a%20/b%20/c%20", UriComponent.encode("/a /b /c ", UriComponent.Type.PATH));
        assertEquals("/copyright%C2%A9", UriComponent.encode("/copyright\u00a9", UriComponent.Type.PATH));
        assertEquals("/a;x/b;x/c;x", UriComponent.encode("/a;x/b;x/c;x", UriComponent.Type.PATH));
        assertEquals("/thumbsup%F0%9F%91%8D", UriComponent.encode("/thumbsup\ud83d\udc4d", UriComponent.Type.PATH));
    }

    @Test
    public void testContextualEncodePath() {
        assertEquals("/a/b/c", UriComponent.contextualEncode("/a/b/c", UriComponent.Type.PATH));
        assertEquals("/a%20/b%20/c%20", UriComponent.contextualEncode("/a /b /c ", UriComponent.Type.PATH));
        assertEquals("/copyright%C2%A9", UriComponent.contextualEncode("/copyright\u00a9", UriComponent.Type.PATH));
        assertEquals("/thumbsup%F0%9F%91%8D", UriComponent.contextualEncode("/thumbsup\ud83d\udc4d", UriComponent.Type.PATH));

        assertEquals("/a%20/b%20/c%20", UriComponent.contextualEncode("/a%20/b%20/c%20", UriComponent.Type.PATH));
        assertEquals("/copyright%C2%A9", UriComponent.contextualEncode("/copyright%C2%A9", UriComponent.Type.PATH));
        assertEquals("/thumbsup%F0%9F%91%8D", UriComponent.contextualEncode("/thumbsup%F0%9F%91%8D", UriComponent.Type.PATH));
    }

    @Test
    public void testEncodeQuery() {
        assertEquals("a%20b%20c.-%2A_=+&%25xx%2520",
                UriComponent.encode("a b c.-*_=+&%xx%20", UriComponent.Type.QUERY));
        assertEquals("a+b+c.-%2A_%3D%2B%26%25xx%2520",
                UriComponent.encode("a b c.-*_=+&%xx%20", UriComponent.Type.QUERY_PARAM));
        assertEquals("a%20b%20c.-%2A_%3D%2B%26%25xx%2520",
                UriComponent.encode("a b c.-*_=+&%xx%20", UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));

        assertEquals("thumbsup%F0%9F%91%8D", UriComponent.encode("thumbsup\ud83d\udc4d", UriComponent.Type.QUERY));
        assertEquals("thumbsup%F0%9F%91%8D", UriComponent.encode("thumbsup\ud83d\udc4d", UriComponent.Type.QUERY_PARAM));
    }

    @Test
    public void testContextualEncodeQuery() {
        assertEquals("a%20b%20c.-%2A_=+&%25xx%20",
                UriComponent.contextualEncode("a b c.-*_=+&%xx%20", UriComponent.Type.QUERY));
        assertEquals("a+b+c.-%2A_%3D%2B%26%25xx%20",
                UriComponent.contextualEncode("a b c.-*_=+&%xx%20", UriComponent.Type.QUERY_PARAM));
        assertEquals("a%20b%20c.-%2A_%3D%2B%26%25xx%20",
                UriComponent.contextualEncode("a b c.-*_=+&%xx%20", UriComponent.Type.QUERY_PARAM_SPACE_ENCODED));

        assertEquals("thumbsup%F0%9F%91%8Dthumbsup%F0%9F%91%8D",
                UriComponent.contextualEncode("thumbsup%F0%9F%91%8Dthumbsup\ud83d\udc4d", UriComponent.Type.QUERY));
        assertEquals("thumbsup%F0%9F%91%8Dthumbsup%F0%9F%91%8D",
                UriComponent.contextualEncode("thumbsup%F0%9F%91%8Dthumbsup\ud83d\udc4d", UriComponent.Type.QUERY_PARAM));
    }

    @Test
    public void testContextualEncodeMatrixParam() {
        assertEquals("a%3Db%20c%3Bx", UriComponent.contextualEncode("a=b c;x", UriComponent.Type.MATRIX_PARAM));

        assertEquals("a%3Db%20c%3Bx%3Dthumbsup%F0%9F%91%8D", UriComponent.contextualEncode("a=b c;x=thumbsup\ud83d\udc4d",
                UriComponent.Type.MATRIX_PARAM));
    }

    @Test
    public void testContextualEncodePercent() {
        assertEquals("%25", UriComponent.contextualEncode("%", UriComponent.Type.PATH));
        assertEquals("a%25", UriComponent.contextualEncode("a%", UriComponent.Type.PATH));
        assertEquals("a%25x", UriComponent.contextualEncode("a%x", UriComponent.Type.PATH));
        assertEquals("a%25%20%20", UriComponent.contextualEncode("a%  ", UriComponent.Type.PATH));

        assertEquals("a%20a%20%20", UriComponent.contextualEncode("a a%20 ", UriComponent.Type.PATH));

    }

    @Test
    public void testEncodeTemplateNames() {
        assertEquals("%7Bfoo%7D", UriComponent.encodeTemplateNames("{foo}"));
    }

    @Test
    public void testValidatePath() {
        assertEquals(false, UriComponent.valid("/x y", UriComponent.Type.PATH));
        assertEquals(true, UriComponent.valid("/x20y", UriComponent.Type.PATH));
        assertEquals(true, UriComponent.valid("/x%20y", UriComponent.Type.PATH));
    }

}
