/*
 * Copyright (c) 2010, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jersey.examples.extendedwadl;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.glassfish.jersey.examples.extendedwadl.resources.MyApplication;
import org.glassfish.jersey.internal.util.SimpleNamespaceResolver;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.wadl.internal.WadlUtils;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import org.w3c.dom.Document;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * WADL extension example tests.
 *
 * @author Naresh
 * @author Miroslav Fuksa
 */
public class ExtendedWadlWebappTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(ExtendedWadlWebappTest.class.getName());

    @Override
    protected DeploymentContext configureDeployment() {
        final ResourceConfig resourceConfig = new ResourceConfig(new MyApplication().getClasses());
        resourceConfig.property(ServerProperties.WADL_GENERATOR_CONFIG, "org.glassfish.jersey.examples.extendedwadl"
                + ".SampleWadlGeneratorConfig");

        final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.name("resource-programmatic").path("programmatic").addMethod("GET")

                .handledBy(new ProgrammaticResource());
        resourceConfig.registerResources(resourceBuilder.build());
        return DeploymentContext.builder(resourceConfig).contextPath("extended-wadl-webapp").build();
    }

    /**
     * Test checks that the WADL generated using the WadlGenerator api doesn't
     * contain the expected text.
     *
     * @throws java.lang.Exception in case of test error.
     */
    @Test
    public void testExtendedWadl() throws Exception {
        String wadl = target().path("application.wadl")
                .queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true").request(MediaTypes.WADL_TYPE).get(String.class);

        LOGGER.fine(wadl);
        assertTrue("Generated wadl is of null length", !wadl.isEmpty());
        assertTrue("Generated wadl doesn't contain the expected text",
                wadl.contains("This is a paragraph"));

        assertFalse(wadl.contains("application.wadl/xsd0.xsd"));
    }

    @Test
    public void testWadlOptionsMethod() throws Exception {
        String wadl = target().path("items")
                .queryParam(WadlUtils.DETAILED_WADL_QUERY_PARAM, "true").request(MediaTypes.WADL_TYPE).options(String.class);

        LOGGER.fine(wadl);
        assertTrue("Generated wadl is of null length", !wadl.isEmpty());
        assertTrue("Generated wadl doesn't contain the expected text",
                wadl.contains("This is a paragraph"));
        checkWadl(wadl, getBaseUri());
    }

    /**
     * Programmatic resource class javadoc.
     */
    private static class ProgrammaticResource implements Inflector<ContainerRequestContext, Response> {
        @Override
        public Response apply(ContainerRequestContext data) {
            return Response.ok("programmatic").build();
        }
    }

    private void checkWadl(String wadl, URI baseUri) throws Exception {
        DocumentBuilderFactory bf = DocumentBuilderFactory.newInstance();
        bf.setNamespaceAware(true);
        bf.setValidating(false);
//        if (!SaxHelper.isXdkDocumentBuilderFactory(bf)) {
//            bf.setXIncludeAware(false);
//        }
        DocumentBuilder b = bf.newDocumentBuilder();
        Document document = b.parse(new ByteArrayInputStream(wadl.getBytes(Charset.forName("UTF-8"))));
        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new SimpleNamespaceResolver("wadl", "http://wadl.dev.java.net/2009/02"));
        String val = (String) xp.evaluate("/wadl:application/wadl:resources/@base", document, XPathConstants.STRING);
        assertEquals(baseUri.toString(), val.endsWith("/") ? val.substring(0, val.length() - 1) : val);
        val = (String) xp.evaluate("count(//wadl:resource)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements.", "4", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='items'])", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements with 'items' path.", "1", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='{id}'])", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements with '{id}' path.", "1", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='try-hard'])", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements with 'try-hard' path.", "1", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='value/{value}'])", document, XPathConstants.STRING);
        assertEquals("Unexpected number of resource elements with 'value/{value}' path.", "1", val);

        val = (String) xp.evaluate("count(//wadl:resource[@path='{id}']/wadl:method)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of methods in resource element with '{id}' path.", "2", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='{id}']/wadl:method[@id='getItem']"
                        + "/wadl:doc[contains(., 'Typically returns the item if it exists.')])",
                document, XPathConstants.STRING);
        assertEquals("Unexpected documentation of getItem resource method at '{id}' path", "1", val);

        val = (String) xp.evaluate("count(//wadl:resource[@path='try-hard']/wadl:method)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of methods in resource element with 'try-hard' path.", "1", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='try-hard']/wadl:method[@id='getItem']"
                        + "/wadl:doc[contains(., 'Tries hard to return the item if it exists.')])",
                document, XPathConstants.STRING);
        assertEquals("Unexpected documentation of getItem resource method at 'try-hard' path", "1", val);

        val = (String) xp.evaluate("count(//wadl:resource[@path='items']/wadl:method)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of methods in resource element with 'items' path.", "4", val);
        val = (String) xp.evaluate("count(//wadl:resource[@path='value/{value}']/wadl:method)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of methods in resource element with 'value/{value}' path.", "1", val);

        val = (String) xp.evaluate("count(//wadl:application/wadl:doc)", document, XPathConstants.STRING);
        assertEquals("Unexpected number of doc elements in application element.", "3", val);
    }
}
