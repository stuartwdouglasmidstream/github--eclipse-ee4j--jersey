/*
 * Copyright (c) 2015, 2022 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.jdk.connector.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

import javax.net.ServerSocketFactory;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdk.connector.JdkConnectorProperties;
import org.glassfish.jersey.jdk.connector.JdkConnectorProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Petr Janouch
 */
public class ConnectionPoolTest extends JerseyTest {

    @Test
    public void testBasic() throws InterruptedException {
        String msg1 = "message 1";
        String msg2 = "message 2";
        CountDownLatch latch = new CountDownLatch(2);
        sendMessageToJersey(msg1, latch);
        sendMessageToJersey(msg2, latch);

        /* the idle timeout is 10s and only 1 connection is allowed, so the test should fail unless the pool reuses
        the connection for both requests */
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private void sendMessageToJersey(String message, final CountDownLatch latch) {
        target("echo").request().async().post(Entity.entity(message, MediaType.TEXT_PLAIN), new InvocationCallback<String>() {
            @Override
            public void completed(String response) {
                System.out.println("#Received: " + response);
                latch.countDown();
            }

            @Override
            public void failed(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    @Test
    public void testPersistentConnection() throws IOException, InterruptedException {
        TestServer testServer = new TestServer(true);

        try {
            testServer.start();
            CountDownLatch latch = new CountDownLatch(2);
            AtomicInteger result1 = new AtomicInteger(-1);
            sendGetToTestServer(result1, latch);
            AtomicInteger result2 = new AtomicInteger(-1);
            sendGetToTestServer(result2, latch);

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            assertEquals(1, result1.get());
            assertEquals(1, result2.get());
        } finally {
            testServer.stop();
        }
    }

    @Test
    public void testNonPersistentConnection() throws IOException, InterruptedException {
        TestServer testServer = new TestServer(false);

        try {
            testServer.start();
            CountDownLatch latch1 = new CountDownLatch(1);
            AtomicInteger result1 = new AtomicInteger(-1);
            sendGetToTestServer(result1, latch1);
            assertTrue(latch1.await(5, TimeUnit.SECONDS));
            CountDownLatch latch2 = new CountDownLatch(1);

            AtomicInteger result2 = new AtomicInteger(-1);
            sendGetToTestServer(result2, latch2);

            assertTrue(latch2.await(5, TimeUnit.SECONDS));

            assertEquals(1, result1.get());
            assertEquals(2, result2.get());
        } finally {
            testServer.stop();
        }
    }

    private void sendGetToTestServer(final AtomicInteger result, final CountDownLatch latch) {
        getClient().target("http://localhost:" + TestServer.PORT).request().async().get(new InvocationCallback<Integer>() {
            @Override
            public void completed(Integer response) {
                System.out.println("#Received: " + response);
                result.set(response);
                latch.countDown();
            }

            @Override
            public void failed(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(EchoResource.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new JdkConnectorProvider());
        config.property(JdkConnectorProperties.MAX_CONNECTIONS_PER_DESTINATION, 1);
        config.property(JdkConnectorProperties.CONNECTION_IDLE_TIMEOUT, 10_000);
    }

    @Path("/echo")
    public static class EchoResource {

        @POST
        public String post(String entity) {
            return entity;
        }
    }

    private static class TestServer {

        static final int PORT = 8321;

        private final boolean persistentConnection;
        private final ServerSocket serverSocket;
        private final ExecutorService executorService = Executors.newCachedThreadPool();
        private final AtomicInteger connectionsCount = new AtomicInteger(0);

        private volatile boolean stopped = false;

        TestServer(boolean persistentConnection) throws IOException {
            this.persistentConnection = persistentConnection;
            ServerSocketFactory socketFactory = ServerSocketFactory.getDefault();
            serverSocket = socketFactory.createServerSocket(PORT);
        }

        void start() {
            executorService.execute(() -> {
                try {
                    while (!stopped) {
                        final Socket socket = serverSocket.accept();
                        connectionsCount.incrementAndGet();
                        executorService.submit(() -> handleConnection(socket));

                    }
                } catch (IOException e) {
                    //do nothing
                }
            });
        }

        private void handleConnection(Socket socket) {

            try {
                InputStream inputStream = socket.getInputStream();
                ByteArrayOutputStream receivedMessage = new ByteArrayOutputStream();

                while (!stopped && !socket.isClosed()) {
                    int result = inputStream.read();
                    if (result == -1) {
                        return;
                    }

                    receivedMessage.write((byte) result);
                    String msg = new String(receivedMessage.toByteArray(), "ASCII");
                    if (msg.contains("\r\n\r\n")) {
                        receivedMessage = new ByteArrayOutputStream();
                        OutputStream outputStream = socket.getOutputStream();
                        String response = "HTTP/1.1 200 OK\r\nContent-Length: 1\r\nContent-Type: text/plain\r\n";
                        if (!persistentConnection) {
                            response += "Connection: Close\r\n";
                        }
                        response += "\r\n" + connectionsCount.get();
                        outputStream.write(response.getBytes("ASCII"));
                        outputStream.flush();
                    }
                }
            } catch (IOException e) {
                if (!e.getClass().equals(SocketException.class)) {
                    e.printStackTrace();
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void stop() throws IOException {
            executorService.shutdown();
            serverSocket.close();
        }
    }
}
