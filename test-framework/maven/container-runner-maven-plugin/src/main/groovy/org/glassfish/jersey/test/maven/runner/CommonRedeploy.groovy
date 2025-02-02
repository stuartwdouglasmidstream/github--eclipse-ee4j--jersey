/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.test.maven.runner

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Parameter
import org.glassfish.jersey.client.ClientProperties

import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget

/**
 * Common functionality of Redeploy Mojos.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
trait CommonRedeploy extends CommonStop {

    /**
     * The exit code that denotes a detected memory leak.
     */
    final static int MEMORY_LEAK_DETECTED_ERROR_EXIT_CODE = 68

    /**
     * The path and query to execute after each application redeploy
     */
    @Parameter(required = true, name = "requestPathQuery")
    String requestPathQuery

    /**
     * The number of redeploys to execute.
     */
    @Parameter(required = true, name = "redeployCount")
    int redeployCount

    /**
     * The http method to use while executing the http request to {@link #requestPathQuery}.
     */
    @Parameter(defaultValue = "GET", name = "method")
    String method

    /**
     * A http status to expect while queering the application between redeploys.
     */
    @Parameter(defaultValue = "200", name = "expectedStatus")
    int expectedStatus

    /**
     * The archive location of an application to be redeployed.
     */
    @Parameter(required = true, name = "warPath")
    File warPath

    @Parameter(defaultValue = "false", name = "skipRedeploy", property = "jersey.runner.skipRedeploy")
    boolean skipRedeploy

    def redeployAndSendRequest(String shell, String stopShell) {
        redeployAndSendRequest(shell, stopShell, null)
    }

    def redeployAndSendRequest(String shell, String stopShell, Map env) {
        final Client client = ClientBuilder.newClient()
                .property(ClientProperties.CONNECT_TIMEOUT, 30000)
                .property(ClientProperties.READ_TIMEOUT, 30000)
        final WebTarget target = client.target("http://localhost:${port}/${normalizedRequestPathQuery()}")

        getLog().info("WEB Target URI: " + target.getUri())

        try {
            int i = 1
            for (; i <= redeployCount; ++i) {
                getLog().info("Executing request $method to " + target.getUri() + " (iteration $i/$redeployCount)")
                def response = target.request().method(method)
                getLog().info("Received http status " + response.getStatus())

                if (expectedStatus != response?.getStatus()) {
                    throw new MojoExecutionException("After $i iterations, the http request ended with unexpected code! Expected: <$expectedStatus>, actual: <${response.getStatus()}>")
                }

                executeShell(shell, env)
            }
            log.info("The test ended after ${i - 1} iterations.")
        } catch (Exception e) {
            log.error("Exception encountered during the redeploy cycle! ", e)
            throw e
        } finally {
            try {
                executeShell(stopShell, env)
            } catch (MojoExecutionException e) {
                getLog().warn("Stop command threw an exception: " + e.getMessage())
                // not re-throwing so that the possible original exception is not masked and success exit is preserved
            } finally {
                try {
                    executeShell("/runner/verify.sh", ["ERROR_EXIT_CODE": MEMORY_LEAK_DETECTED_ERROR_EXIT_CODE as String])
                } catch (ShellMojoExecutionException e) {
                    if (e.errorCode == MEMORY_LEAK_DETECTED_ERROR_EXIT_CODE) {
                        // re-throw the exception and mask all the other exceptions because we verified that an ERROR (e.g. OutOfMemoryError) occurred
                        throw e
                    }
                }
            }
        }
    }

    Map commonEnvironment() {
        return [
                "WAR_PATH"          : warPath.absolutePath,
                "REQUEST_PATH_QUERY": normalizedRequestPathQuery(),
                "SKIP_REDEPLOY"     : skipRedeploy as String
        ] << super.commonEnvironment()
    }

    private String normalizedRequestPathQuery() {
        while (requestPathQuery.startsWith("/")) {
            requestPathQuery = requestPathQuery.substring(1, requestPathQuery.length())
        }
        return requestPathQuery
    }

    void setRequestPathQuery(final String requestPathQuery) {
        this.requestPathQuery = requestPathQuery
    }

    void setRedeployCount(final int redeployCount) {
        this.redeployCount = redeployCount
    }

    void setMethod(final String method) {
        this.method = method
    }

    void setExpectedStatus(final int expectedStatus) {
        this.expectedStatus = expectedStatus
    }

    void setWarPath(final File warPath) {
        this.warPath = warPath
    }

    void setSkipRedeploy(final boolean skipRedeploy) {
        this.skipRedeploy = skipRedeploy
    }
}
