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

package org.glassfish.jersey.client;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@code JerseyWebTarget} implementation unit tests.
 *
 * @author Martin Matula
 */
public class JerseyWebTargetTest {
    private JerseyClient client;
    private JerseyWebTarget target;

    @BeforeEach
    public void setUp() {
        this.client = (JerseyClient) ClientBuilder.newClient();
        this.target = client.target("/");
    }

    @Test
    public void testClose() {
        client.close();
        try {
            target.getUriBuilder();
            fail("IllegalStateException was expected.");
        } catch (IllegalStateException e) {
            // ignore
        }
        try {
            target.getConfiguration();
            fail("IllegalStateException was expected.");
        } catch (IllegalStateException e) {
            // ignore
        }
    }

    public static class TestProvider implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            return context.proceed();
        }
    }

    // Reproducer JERSEY-1637
    @Test
    public void testRegisterNullOrEmptyContracts() {
        final TestProvider provider = new TestProvider();

        target.register(TestProvider.class, (Class<?>[]) null);
        assertFalse(target.getConfiguration().isRegistered(TestProvider.class));

        target.register(provider, (Class<?>[]) null);
        assertFalse(target.getConfiguration().isRegistered(TestProvider.class));
        assertFalse(target.getConfiguration().isRegistered(provider));

        target.register(TestProvider.class, new Class[0]);
        assertFalse(target.getConfiguration().isRegistered(TestProvider.class));

        target.register(provider, new Class[0]);
        assertFalse(target.getConfiguration().isRegistered(TestProvider.class));
        assertFalse(target.getConfiguration().isRegistered(provider));
    }

    @Test
    public void testResolveTemplate() {
        URI uri;
        UriBuilder uriBuilder;

        uri = target.resolveTemplate("a", "v").getUri();
        assertEquals("/", uri.toString());

        uri = target.path("{a}").resolveTemplate("a", "v").getUri();
        assertEquals("/v", uri.toString());

        uriBuilder = target.path("{a}").resolveTemplate("qqq", "qqq").getUriBuilder();
        assertEquals("/{a}", uriBuilder.toTemplate());

        uriBuilder = target.path("{a}").resolveTemplate("a", "v").resolveTemplate("a", "x").getUriBuilder();
        assertEquals("/v", uriBuilder.build().toString());

        try {
            target.resolveTemplate(null, null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    @Test
    public void testResolveTemplate2() {
        final JerseyWebTarget newTarget = target.path("path/{a}").queryParam("query", "{q}").resolveTemplate("a", "param-a");
        final JerseyUriBuilder uriBuilder = (JerseyUriBuilder) newTarget.getUriBuilder();
        uriBuilder.resolveTemplate("q", "param-q").resolveTemplate("a", "will-be-ignored");
        assertEquals(URI.create("/path/param-a?query=param-q"), uriBuilder.build());

        final UriBuilder uriBuilderNew = newTarget.resolveTemplate("a", "will-be-ignored").resolveTemplate("q",
                "new-q").getUriBuilder();
        assertEquals(URI.create("/path/param-a?query=new-q"), uriBuilderNew.build());
    }

    @Test
    public void testResolveTemplate3() {
        final JerseyWebTarget webTarget = target.path("path/{a}").path("{b}").queryParam("query", "{q}")
                .resolveTemplate("a", "param-a").resolveTemplate("q", "param-q");
        assertEquals("/path/param-a/{b}?query=param-q", webTarget.getUriBuilder().toTemplate());
        // resolve b in webTarget
        assertEquals(URI.create("/path/param-a/param-b?query=param-q"), webTarget.resolveTemplate("b",
                "param-b").getUri());

        // check that original webTarget has not been changed
        assertEquals("/path/param-a/{b}?query=param-q", webTarget.getUriBuilder().toTemplate());

        // resolve b in UriBuilder
        assertEquals(URI.create("/path/param-a/param-b?query=param-q"), ((JerseyUriBuilder) webTarget.getUriBuilder())
                .resolveTemplate("b", "param-b").build());

        // resolve in build method
        assertEquals(URI.create("/path/param-a/param-b?query=param-q"), ((JerseyUriBuilder) webTarget.getUriBuilder())
                .build("param-b"));
    }


    @Test
    public void testResolveTemplateFromEncoded() {
        final String a = "a%20%3F/*/";
        final String b = "/b/";
        assertEquals("/path/a%20%3F/*///b/", target.path("path/{a}/{b}").resolveTemplateFromEncoded("a",
                a).resolveTemplateFromEncoded("b", b).getUri().toString());
        assertEquals("/path/a%2520%253F%2F*%2F/%2Fb%2F", target.path("path/{a}/{b}").resolveTemplate("a",
                a).resolveTemplate("b", b).getUri().toString());
        assertEquals("/path/a%2520%253F/*///b/", target.path("path/{a}/{b}").resolveTemplate("a",
                a, false).resolveTemplate("b", b, false).getUri().toString());
    }


    @Test
    public void testResolveTemplatesFromEncoded() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("a", "a%20%3F/*/");
        map.put("b", "/b/");

        assertEquals("/path/a%20%3F/*///b/", target.path("path/{a}/{b}").resolveTemplatesFromEncoded(map).getUri()
                .toString());
        assertEquals("/path/a%2520%253F%2F*%2F/%2Fb%2F", target.path("path/{a}/{b}").resolveTemplates(map).getUri()
                .toString());
        assertEquals("/path/a%2520%253F/*///b/", target.path("path/{a}/{b}").resolveTemplates(map,
                false).getUri().toString());

        List<Map<String, Object>> corruptedTemplateValuesList = Arrays.asList(
                null,
                new HashMap<String, Object>() {{
                    put(null, "value");
                }},
                new HashMap<String, Object>() {{
                    put("name", null);
                }},
                new HashMap<String, Object>() {{
                    put("a", "foo");
                    put("name", null);
                }},
                new HashMap<String, Object>() {{
                    put("name", null);
                    put("a", "foo");
                }}
        );

        for (final Map<String, Object> corruptedTemplateValues : corruptedTemplateValuesList) {
            try {
                target.path("path/{a}/{b}").resolveTemplatesFromEncoded(corruptedTemplateValues);
                fail("NullPointerException expected. " + corruptedTemplateValues);
            } catch (NullPointerException ex) {
                // expected
            } catch (Exception e) {
                fail("NullPointerException expected for template values " + corruptedTemplateValues + ", caught: " + e);
            }
        }

        for (final Map<String, Object> corruptedTemplateValues : corruptedTemplateValuesList) {
            try {
                target.path("path/{a}/{b}").resolveTemplates(corruptedTemplateValues);
                fail("NullPointerException expected. " + corruptedTemplateValues);
            } catch (NullPointerException ex) {
                // expected
            } catch (Exception e) {
                fail("NullPointerException expected for template values " + corruptedTemplateValues + ", caught: " + e);
            }
        }

        for (final Map<String, Object> corruptedTemplateValues : corruptedTemplateValuesList) {
            for (final boolean encode : new boolean[]{true, false}) {
                try {
                    target.path("path/{a}/{b}").resolveTemplates(corruptedTemplateValues, encode);
                    fail("NullPointerException expected. " + corruptedTemplateValues);
                } catch (NullPointerException ex) {
                    // expected
                } catch (Exception e) {
                    fail("NullPointerException expected for template values " + corruptedTemplateValues + ", caught: " + e);
                }
            }
        }
    }


    @Test
    public void testGetUriBuilder() {
        final Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("a", "w1");
        UriBuilder uriBuilder = target.path("{a}").resolveTemplate("a", "v1").resolveTemplates(params).getUriBuilder();
        assertEquals("/v1", uriBuilder.build().toString());
    }

    @Test
    public void testQueryParams() {
        URI uri;

        uri = target.path("a").queryParam("q", "v1", "v2").queryParam("q").getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").queryParam("q", "v1", "v2").queryParam("q", (Object) null).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").queryParam("q", "v1", "v2").queryParam("q", (Object[]) null).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").queryParam("q", "v1", "v2").queryParam("q", new Object[]{}).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").queryParam("q", "v").getUri();
        assertEquals("/a?q=v", uri.toString());

        uri = target.path("a").queryParam("q1", "v1").queryParam("q2", "v2").queryParam("q1", (Object) null).getUri();
        assertEquals("/a?q2=v2", uri.toString());

        try {
            target.queryParam("q", "v1", null, "v2", null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }

        {
            uri = target.path("a").queryParam("q1", "v1").queryParam("q2", "v2").queryParam("q1", "w1", "w2")
                    .queryParam("q2", (Object) null).getUri();
            assertEquals("/a?q1=v1&q1=w1&q1=w2", uri.toString());
        }

        try {
            target.queryParam(null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }

        try {
            target.queryParam(null, "param");
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }

        try {
            target.path("a").queryParam("q1", "v1").queryParam("q2", "v2").queryParam("q1", "w1", null)
                    .queryParam("q2", (Object) null);

            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    @Test
    public void testMatrixParams() {
        URI uri;

        uri = target.path("a").matrixParam("q", "v1", "v2").matrixParam("q").getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").matrixParam("q", "v1", "v2").matrixParam("q", (Object) null).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").matrixParam("q", "v1", "v2").matrixParam("q", (Object[]) null).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").matrixParam("q", "v1", "v2").matrixParam("q", new Object[]{}).getUri();
        assertEquals("/a", uri.toString());

        uri = target.path("a").matrixParam("q", "v").getUri();
        assertEquals("/a;q=v", uri.toString());

        uri = target.path("a").matrixParam("q1", "v1").matrixParam("q2", "v2").matrixParam("q1", (Object) null).getUri();
        assertEquals("/a;q2=v2", uri.toString());

        try {
            target.matrixParam("q", "v1", null, "v2", null);
            fail("NullPointerException expected.");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    @Test
    public void testRemoveMatrixParams() {
        WebTarget wt = target;
        wt = wt.matrixParam("matrix1", "segment1");
        wt = wt.path("path1");
        wt = wt.matrixParam("matrix2", "segment1");
        wt = wt.matrixParam("matrix2", new Object[]{null});
        wt = wt.path("path2");
        wt = wt.matrixParam("matrix1", "segment1");
        wt = wt.matrixParam("matrix1", new Object[]{null});
        wt = wt.path("path3");
        URI uri = wt.getUri();
        assertEquals("/;matrix1=segment1/path1/path2/path3", uri.toString());
    }

    @Test
    public void testReplaceMatrixParam() {
        WebTarget wt = target;
        wt = wt.path("path1");
        wt = wt.matrixParam("matrix10", "segment10-delete");
        wt = wt.matrixParam("matrix11", "segment11");
        wt = wt.matrixParam("matrix10", new Object[]{null});
        wt = wt.path("path2");
        wt = wt.matrixParam("matrix20", "segment20-delete");
        wt = wt.matrixParam("matrix20", new Object[]{null});
        wt = wt.matrixParam("matrix20", "segment20-delete-again");
        wt = wt.matrixParam("matrix20", new Object[]{null});
        wt = wt.path("path3");
        wt = wt.matrixParam("matrix30", "segment30-delete");
        wt = wt.matrixParam("matrix30", new Object[]{null});
        wt = wt.matrixParam("matrix30", "segment30-delete-again");
        wt = wt.matrixParam("matrix30", new Object[]{null});
        wt = wt.matrixParam("matrix30", "segment30");
        wt = wt.path("path4");
        wt = wt.matrixParam("matrix40", "segment40-delete");
        wt = wt.matrixParam("matrix40", new Object[]{null});

        URI uri = wt.getUri();
        assertEquals("/path1;matrix11=segment11/path2/path3;matrix30=segment30/path4", uri.toString());
    }

    @Test
    public void testQueryParamNull() {
        assertThrows(NullPointerException.class, () -> {
            WebTarget wt = target;

            wt.queryParam(null);
        });
    }

    @Test
    public void testPathNull() {
        assertThrows(NullPointerException.class, () -> {
            WebTarget wt = target;

            wt.path(null);
        });
    }

    @Test
    public void testResolveTemplateNull1() {
        assertThrows(NullPointerException.class, () -> {
            WebTarget wt = target;

            wt.resolveTemplate(null, "", true);
        });
    }

    @Test
    public void testResolveTemplateNull2() {
        assertThrows(NullPointerException.class, () -> {
            WebTarget wt = target;

            wt.resolveTemplate("name", null, true);
        });
    }

    @Test
    public void testResolveTemplateFromEncodedNull1() {
        assertThrows(NullPointerException.class, () -> {
            WebTarget wt = target;

            wt.resolveTemplateFromEncoded(null, "");
        });
    }

    @Test
    public void testResolveTemplateFromEncodedNull2() {
        assertThrows(NullPointerException.class, () -> {
            WebTarget wt = target;

            wt.resolveTemplateFromEncoded("name", null);
        });
    }

    @Test
    public void testResolveTemplatesEncodedEmptyMap() {
        WebTarget wt = target;
        wt = wt.resolveTemplatesFromEncoded(Collections.<String, Object>emptyMap());

        assertEquals(target, wt);
    }

    @Test
    public void testResolveTemplatesEmptyMap() {
        WebTarget wt = target;
        wt = wt.resolveTemplates(Collections.<String, Object>emptyMap());

        assertEquals(target, wt);
    }

    @Test
    public void testResolveTemplatesEncodeSlashEmptyMap() {
        WebTarget wt = target;
        wt = wt.resolveTemplates(Collections.<String, Object>emptyMap(), false);

        assertEquals(target, wt);
    }

}


