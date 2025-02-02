/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.routing;

import java.util.List;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.internal.routing.CombinedMediaType;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.ResourceMethod;

/**
 * A single router responsible for selecting a single method from all the methods
 * bound to the same routed request path.
 *
 * The method selection algorithm selects the handling method based on the HTTP request
 * method name, requested media type as well as defined resource method media type
 * capabilities.
 */
final class OctetStreamMethodSelectingRouter extends AbstractMethodSelectingRouter implements Router {

    /**
     * Create a new {@code MethodSelectingRouter} for all the methods on the same path.
     *
     * The router selects the method that best matches the request based on
     * produce/consume information from the resource method models.
     *
     * @param workers        message body workers.
     * @param methodRoutings [method model, method methodAcceptorPair] pairs.
     */
    OctetStreamMethodSelectingRouter(MessageBodyWorkers workers, List<MethodRouting> methodRoutings) {
        super(workers, methodRoutings);
    }

    @Override
    protected AbstractMethodSelectingRouter.ConsumesProducesAcceptor createConsumesProducesAcceptor(
            CombinedMediaType.EffectiveMediaType consumes,
            CombinedMediaType.EffectiveMediaType produces,
            MethodRouting methodRouting) {
        return new ConsumesProducesAcceptor(consumes, produces, methodRouting);
    }

    private static class ConsumesProducesAcceptor extends AbstractMethodSelectingRouter.ConsumesProducesAcceptor {
        private ConsumesProducesAcceptor(
                CombinedMediaType.EffectiveMediaType consumes,
                CombinedMediaType.EffectiveMediaType produces,
                MethodRouting methodRouting) {
            super(consumes, produces, methodRouting);
        }

        /**
         * Determines whether this {@code ConsumesProducesAcceptor} router can process the {@code request}.
         *
         * @param requestContext The request to be tested.
         * @return True if the {@code request} can be processed by this router, false otherwise.
         */
        boolean isConsumable(ContainerRequest requestContext) {
            MediaType contentType = requestContext.getMediaType();
            if (contentType == null && methodRouting.method.getType() != ResourceMethod.JaxrsType.SUB_RESOURCE_LOCATOR
                    && methodRouting.method.getInvocable().requiresEntity()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            }
            return contentType == null || consumes.getMediaType().isCompatible(contentType);
        }
    }
}
