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

package org.glassfish.jersey.tests.e2e.json;

import java.io.StringReader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.server.JSONP;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Michal Gajdos
 */
public class JsonProcessingTest extends JerseyTest {

    private static final String JSON_OBJECT_STR1 = "{\"foo\":\"bar\"}";
    private static final String JSON_OBJECT_STR2 = "{\"foo\": 12345}";
    private static final String JSON_ARRAY_STR1 = "[" + JSON_OBJECT_STR1 + "," + JSON_OBJECT_STR1 + "]";
    private static final String JSON_ARRAY_STR2 = "[" + JSON_OBJECT_STR2 + "," + JSON_OBJECT_STR2 + "]";
    private static final String JSON_ARRAY_VALUE_STR = "[null]";

    private static final JsonObject JSON_OBJECT = Json.createReader(new StringReader(JSON_OBJECT_STR1)).readObject();
    private static final JsonArray JSON_ARRAY = Json.createReader(new StringReader(JSON_ARRAY_STR1)).readArray();
    private static final JsonArray JSON_ARRAY_VALUE = Json.createReader(new StringReader(JSON_ARRAY_VALUE_STR))
                                                          .readArray();

    private static final JsonValue JSON_VALUE_BOOL = JsonValue.TRUE;
    private static final JsonString JSON_VALUE_STRING = Json.createReader(
            new StringReader(JSON_ARRAY_STR1)).readArray().getJsonObject(0).getJsonString("foo");
    private static final JsonNumber JSON_VALUE_NUMBER = Json.createReader(
            new StringReader(JSON_ARRAY_STR2)).readArray().getJsonObject(0).getJsonNumber("foo");

    @Path("/")
    public static class Resource {

        @POST
        @Path("jsonObject")
        public JsonObject postJsonObject(final JsonObject jsonObject) {
            return jsonObject;
        }

        @POST
        @Path("jsonStructure")
        public JsonStructure postJsonStructure(final JsonStructure jsonStructure) {
            return jsonStructure;
        }

        @POST
        @Path("jsonArray")
        public JsonArray postJsonArray(final JsonArray jsonArray) {
            return jsonArray;
        }

        @POST
        @Path("jsonValue")
        public JsonValue postJsonValue(final JsonValue jsonValue) {
            return jsonValue;
        }

        @POST
        @Path("jsonString")
        public JsonString postJsonString(final JsonString jsonString) {
            return jsonString;
        }

        @POST
        @Path("jsonNumber")
        public JsonValue postJsonNumber(final JsonNumber jsonNumber) {
            return jsonNumber;
        }

        @GET
        @JSONP
        @Path("jsonObjectWithPadding")
        @Produces("application/javascript")
        public JsonObject getJsonObjectWithPadding() {
            return JSON_OBJECT;
        }
    }

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(Resource.class)
                // Make sure to disable auto-discovery (MOXy, BeanValidation, ...) and register ValidationFeature.
                .property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
                .register(JsonProcessingFeature.class);
    }

    @Test
    public void testJsonObject() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT));

        assertEquals(JSON_OBJECT, response.readEntity(JsonObject.class));
    }

    @Test
    public void testJsonObjectAsString() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON)
                                                      .post(Entity.json(JSON_OBJECT_STR1));

        assertEquals(JSON_OBJECT, response.readEntity(JsonObject.class));
    }

    @Test
    public void testJsonObjectPlus() throws Exception {
        final Response response = target("jsonObject").request("application/foo+json").post(Entity.json(JSON_OBJECT));

        assertEquals(JSON_OBJECT, response.readEntity(JsonObject.class));
    }

    @Test
    public void testJsonObjectAsStringPlus() throws Exception {
        final Response response = target("jsonObject").request("application/foo+json")
                                                      .post(Entity.json(JSON_OBJECT_STR1));

        assertEquals(JSON_OBJECT, response.readEntity(JsonObject.class));
    }

    @Test
    public void testJsonObjectWrongTarget() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectAsStringWrongTarget() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON)
                                                     .post(Entity.json(JSON_OBJECT_STR1));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectWrongEntity() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectAsStringWrongEntity() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON)
                                                      .post(Entity.json(JSON_ARRAY_STR1));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectWrongMediaType() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_OCTET_STREAM).post(Entity.json(JSON_OBJECT));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonObjectAsStringWrongMediaType() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_OCTET_STREAM)
                                                      .post(Entity.json(JSON_OBJECT_STR1));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArray() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY));

        assertEquals(JSON_ARRAY, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonArrayAsString() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON)
                                                     .post(Entity.json(JSON_ARRAY_STR1));

        assertEquals(JSON_ARRAY, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonArrayPlus() throws Exception {
        final Response response = target("jsonArray").request("application/foo+json").post(Entity.json(JSON_ARRAY));

        assertEquals(JSON_ARRAY, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonArrayAsStringPlus() throws Exception {
        final Response response = target("jsonArray").request("application/foo+json")
                                                     .post(Entity.json(JSON_ARRAY_STR1));

        assertEquals(JSON_ARRAY, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonArrayWrongTarget() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayAsStringWrongTarget() throws Exception {
        final Response response = target("jsonObject").request(MediaType.APPLICATION_JSON)
                                                      .post(Entity.json(JSON_ARRAY_STR1));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayWrongEntity() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayAsStringWrongEntity() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON)
                                                     .post(Entity.json(JSON_OBJECT_STR1));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayWrongMediaType() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_OCTET_STREAM).post(Entity.json(JSON_ARRAY));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArraytAsStringWrongMediaType() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_OCTET_STREAM)
                                                     .post(Entity.json(JSON_ARRAY_STR1));

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testJsonArrayValueEntity() throws Exception {
        final Response response = target("jsonArray").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY_VALUE));

        assertEquals(JSON_ARRAY_VALUE, response.readEntity(JsonArray.class));
    }

    @Test
    public void testJsonStructureArray() throws Exception {
        final Response response = target("jsonStructure").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_ARRAY));

        assertEquals(JSON_ARRAY, response.readEntity(JsonStructure.class));
    }

    @Test
    public void testJsonStructureObject() throws Exception {
        final Response response = target("jsonStructure").request(MediaType.APPLICATION_JSON).post(Entity.json(JSON_OBJECT));

        assertEquals(JSON_OBJECT, response.readEntity(JsonStructure.class));
    }

    @Test
    public void testJsonValueBool() throws Exception {
        final Response response = target("jsonValue").request(MediaType.APPLICATION_JSON)
                                                     .post(Entity.json(JSON_VALUE_BOOL));

        assertEquals(JSON_VALUE_BOOL, response.readEntity(JsonValue.class));
    }

    @Test
    public void testJsonValueString() throws Exception {
        final Response response = target("jsonString").request(MediaType.APPLICATION_JSON)
                                                      .post(Entity.json(JSON_VALUE_STRING));

        assertEquals(JSON_VALUE_STRING, response.readEntity(JsonString.class));
    }

    @Test
    public void testJsonValueStringAsValue() throws Exception {
        final Response response = target("jsonValue").request(MediaType.APPLICATION_JSON)
                                                     .post(Entity.json(JSON_VALUE_STRING));

        assertEquals(JSON_VALUE_STRING, response.readEntity(JsonString.class));
    }

    @Test
    public void testJsonValueStringAsString() throws Exception {
        final Response response = target("jsonValue").request(MediaType.APPLICATION_JSON)
                                                     .post(Entity.json("\"Red 5\""));

        assertEquals("Red 5", response.readEntity(JsonString.class).getString());
    }

    @Test
    public void testJsonValueNumber() throws Exception {
        final Response response = target("jsonNumber").request(MediaType.APPLICATION_JSON)
                                                      .post(Entity.json(JSON_VALUE_NUMBER));

        assertEquals(JSON_VALUE_NUMBER, response.readEntity(JsonNumber.class));
    }

    @Test
    public void testJsonValueNumberAsValue() throws Exception {
        final Response response = target("jsonValue").request(MediaType.APPLICATION_JSON)
                                                     .post(Entity.json(JSON_VALUE_NUMBER));

        assertEquals(JSON_VALUE_NUMBER, response.readEntity(JsonNumber.class));
    }

    @Test
    public void testJsonObjectWithPadding() throws Exception {
        final Response response = target("jsonObjectWithPadding").request("application/javascript").get();

        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is(JSONP.DEFAULT_CALLBACK + "(" + JSON_OBJECT_STR1 + ")"));
    }
}
