/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jnh.connector;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.MultivaluedMap;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.innate.ClientProxy;
import org.glassfish.jersey.client.innate.Expect100ContinueUsage;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.Version;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Provides a Jersey client {@link Connector}, which internally uses Java's {@link HttpClient}.
 * The following properties are provided to Java's {@link HttpClient.Builder} during creation of the {@link HttpClient}:
 * <ul>
 *     <li>{@link ClientProperties#CONNECT_TIMEOUT}</li>
 *     <li>{@link ClientProperties#FOLLOW_REDIRECTS}</li>
 *     <li>{@link JavaNetHttpClientProperties#COOKIE_HANDLER}</li>
 *     <li>{@link JavaNetHttpClientProperties#SSL_PARAMETERS}</li>
 * </ul>
 *
 * @author Steffen Nießing
 */
public class JavaNetHttpConnector implements Connector {
    private static final Logger LOGGER = Logger.getLogger(JavaNetHttpConnector.class.getName());

    private final HttpClient httpClient;

    /**
     * Constructs a new {@link Connector} for a Jersey client instance using Java's {@link HttpClient}.
     *
     * @param client a Jersey client instance to get additional configuration properties from (e.g. {@link SSLContext})
     * @param configuration the configuration properties for this connector
     */
    public JavaNetHttpConnector(final Client client, final Configuration configuration) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
        httpClientBuilder.version(HttpClient.Version.HTTP_1_1);
        SSLContext sslContext = client.getSslContext();
        if (sslContext != null) {
            httpClientBuilder.sslContext(sslContext);
        }
        final CookieHandler cookieHandler =
                getPropertyOrNull(configuration, JavaNetHttpClientProperties.COOKIE_HANDLER, CookieHandler.class);
        if (cookieHandler != null) {
            httpClientBuilder.cookieHandler(cookieHandler);
        }
        final Boolean disableCookies =
                getPropertyOrNull(configuration, JavaNetHttpClientProperties.DISABLE_COOKIES, Boolean.class);
        if (Boolean.TRUE.equals(disableCookies)) {
            httpClientBuilder.cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_NONE));
        }
        Boolean redirect = getPropertyOrNull(configuration, ClientProperties.FOLLOW_REDIRECTS, Boolean.class);
        if (redirect != null) {
            httpClientBuilder.followRedirects(redirect ? HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER);
        } else {
            httpClientBuilder.followRedirects(HttpClient.Redirect.NORMAL);
        }
        SSLParameters sslParameters =
                getPropertyOrNull(configuration, JavaNetHttpClientProperties.SSL_PARAMETERS, SSLParameters.class);
        if (sslParameters != null) {
            httpClientBuilder.sslParameters(sslParameters);
        }
        final Authenticator preemptiveAuthenticator =
                getPropertyOrNull(configuration,
                        JavaNetHttpClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION, Authenticator.class);
        if (preemptiveAuthenticator != null) {
            httpClientBuilder.authenticator(preemptiveAuthenticator);
        }
        configureProxy(httpClientBuilder, configuration);
        this.httpClient = httpClientBuilder.build();
    }

    private static void configureProxy(HttpClient.Builder builder, final Configuration config) {

        final Optional<ClientProxy> proxy = ClientProxy.proxyFromConfiguration(config);
        proxy.ifPresent(clientProxy -> {
            final URI u = clientProxy.uri();
            final InetSocketAddress proxyAddress = new InetSocketAddress(u.getHost(),
                    u.getPort());
            if (clientProxy.userName() != null) {
                final Authenticator authenticator = new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(clientProxy.userName(), clientProxy.password() == null
                                ? null : clientProxy.password().toCharArray());
                    }
                    @Override
                    protected RequestorType getRequestorType() {
                        return RequestorType.PROXY;
                    }
                };
                builder.authenticator(authenticator);
            }
            builder.proxy(ProxySelector.of(proxyAddress));
        });
    }

    /**
     * Implements a {@link org.glassfish.jersey.message.internal.OutboundMessageContext.StreamProvider}
     * for a {@link ByteArrayOutputStream}.
     */
    private static class ByteArrayOutputStreamProvider implements OutboundMessageContext.StreamProvider {
        private ByteArrayOutputStream byteArrayOutputStream;

        public ByteArrayOutputStream getByteArrayOutputStream() {
            return byteArrayOutputStream;
        }

        @Override
        public OutputStream getOutputStream(int contentLength) throws IOException {
            this.byteArrayOutputStream = contentLength > 0 ? new ByteArrayOutputStream(contentLength)
                    : new ByteArrayOutputStream();
            return this.byteArrayOutputStream;
        }
    }

    /**
     * Builds a request for the {@link HttpClient} from Jersey's {@link ClientRequest}.
     *
     * @param request the Jersey request to get request data from
     * @return the {@link HttpRequest} instance for the {@link HttpClient} request
     */
    private HttpRequest getHttpRequest(ClientRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(request.getUri());
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
        if (request.hasEntity()) {
            try {
                request.enableBuffering();
                ByteArrayOutputStreamProvider byteBufferStreamProvider = new ByteArrayOutputStreamProvider();
                request.setStreamProvider(byteBufferStreamProvider);
                request.writeEntity();
                bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(
                        byteBufferStreamProvider.getByteArrayOutputStream().toByteArray()
                );
            } catch (IOException e) {
                throw new ProcessingException(LocalizationMessages.ERROR_INVALID_ENTITY(), e);
            }
        }
        builder.method(request.getMethod(), bodyPublisher);
        for (Map.Entry<String, List<String>> entry : request.getRequestHeaders().entrySet()) {
            String headerName = entry.getKey();
            for (String headerValue : entry.getValue()) {
                builder.header(headerName, headerValue);
            }
        }
        final Integer connectTimeout = request.resolveProperty(ClientProperties.READ_TIMEOUT, Integer.class);
        if (connectTimeout != null) {
            builder.timeout(Duration.ofMillis(connectTimeout));
        }
        processExtensions(builder, request);
        return builder.build();
    }

     private static void processExtensions(HttpRequest.Builder builder, ClientRequest request) {
        builder.expectContinue(Expect100ContinueUsage.isAllowed(request, request.getMethod()));
    }

    /**
     * Retrieves a property from the configuration, if it was provided.
     *
     * @param configuration the {@link Configuration} to get the property information from
     * @param propertyKey the name of the property to retrieve
     * @param resultClass the type to which the property value should be case
     * @param <T> the generic type parameter of the result type
     * @return the requested property or {@code null}, if it was not provided or has the wrong type
     */
    @SuppressWarnings("unchecked")
    private <T> T getPropertyOrNull(final Configuration configuration, final String propertyKey, final Class<T> resultClass) {
        Object propertyObject = configuration.getProperty(propertyKey);
        if (propertyObject == null) {
            return null;
        }
        if (!resultClass.isInstance(propertyObject)) {
            LOGGER.warning(LocalizationMessages.ERROR_INVALID_CLASS(propertyKey, resultClass.getName()));
            return null;
        }
        return (T) propertyObject;
    }

    /**
     * Translates a {@link HttpResponse} from the {@link HttpClient} to a Jersey {@link ClientResponse}.
     *
     * @param request the {@link ClientRequest} to get additional information (e.g. header values) from
     * @param response the {@link HttpClient} response object
     * @return the translated Jersey {@link ClientResponse} object
     */
    private ClientResponse buildClientResponse(ClientRequest request, HttpResponse<InputStream> response) {
        ClientResponse clientResponse = new ClientResponse(Statuses.from(response.statusCode()), request);
        MultivaluedMap<String, String> headers = clientResponse.getHeaders();
        for (Map.Entry<String, List<String>> entry : response.headers().map().entrySet()) {
            String headerName = entry.getKey();
            if (headers.get(headerName) != null) {
                headers.get(headerName).addAll(entry.getValue());
            } else {
                headers.put(headerName, entry.getValue());
            }
        }
        clientResponse.setEntityStream(response.body());
        return clientResponse;
    }

    /**
     * Returns the underlying {@link HttpClient} instance used by this connector.
     *
     * @return the Java {@link HttpClient} instance
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public ClientResponse apply(ClientRequest request) {
        HttpRequest httpRequest = getHttpRequest(request);
        try {
            HttpResponse<InputStream> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            return buildClientResponse(request, response);
        } catch (IOException | InterruptedException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
        HttpRequest httpRequest = getHttpRequest(request);
        CompletableFuture<ClientResponse> response = this.httpClient
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(httpResponse -> buildClientResponse(request, httpResponse));
        response.thenAccept(callback::response);
        return response;
    }

    @Override
    public String getName() {
        return "Java HttpClient Connector " + Version.getVersion();
    }

    @Override
    public void close() {

    }

    public CookieHandler getCookieHandler() {
        final Optional<CookieHandler> cookieHandler = httpClient.cookieHandler();
        if (cookieHandler.isPresent()) {
            return cookieHandler.get();
        }
        return null;
    }
}
