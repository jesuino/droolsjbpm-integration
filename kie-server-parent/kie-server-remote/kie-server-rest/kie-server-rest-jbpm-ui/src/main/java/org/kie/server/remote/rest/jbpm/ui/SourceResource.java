/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.server.remote.rest.jbpm.ui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.kie.server.remote.rest.common.Header;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.jbpm.ui.SourceServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.server.api.rest.RestURI.CONTAINER_ID;
import static org.kie.server.api.rest.RestURI.PROCESS_ID;
import static org.kie.server.api.rest.RestURI.PROCESS_SOURCE_GET_URI;
import static org.kie.server.api.rest.RestURI.SOURCE_URI;
import static org.kie.server.remote.rest.common.util.RestUtils.buildConversationIdHeader;
import static org.kie.server.remote.rest.common.util.RestUtils.createResponse;
import static org.kie.server.remote.rest.common.util.RestUtils.errorMessage;
import static org.kie.server.remote.rest.common.util.RestUtils.getVariant;
import static org.kie.server.remote.rest.common.util.RestUtils.internalServerError;
import static org.kie.server.remote.rest.common.util.RestUtils.notFound;

@Api(value = "Sources")
@Path("server/" + SOURCE_URI)
public class SourceResource {

    private static final Logger logger = LoggerFactory.getLogger(SourceResource.class);
    private SourceServiceBase sourceServiceBase;
    private KieServerRegistry context;

    public SourceResource() {

    }

    public SourceResource(SourceServiceBase sourceServiceBase, KieServerRegistry context) {
        this.sourceServiceBase = sourceServiceBase;
        this.context = context;
    }

    @ApiOperation(value = "Returns the process source file of a specified process definition id.",
            response = String.class, code = 200)
    @ApiResponses(value = {@ApiResponse(code = 500, message = "Unexpected error"),
            @ApiResponse(code = 404, message = "Process definition, source or Container Id not found")})
    @GET
    @Path(PROCESS_SOURCE_GET_URI)
    @Produces({MediaType.APPLICATION_XML})
    public Response getProcessSource(@javax.ws.rs.core.Context HttpHeaders headers,
            @ApiParam(value = "container id that process definition belongs to", required = true, example = "evaluation_1.0.0-SNAPSHOT") @PathParam(CONTAINER_ID) String containerId,
            @ApiParam(value = "identifier of the process definition that source should be loaded for", required = true, example = "evaluation") @PathParam(PROCESS_ID) String processId) {
        Variant v = getVariant(headers);
        Header conversationIdHeader = buildConversationIdHeader(containerId, context, headers);
        try {
            String processSource = sourceServiceBase.getProcessSource(containerId, processId);

            logger.debug("Returning OK response with content '{}'", processSource);
            return createResponse(processSource, v, Response.Status.OK, conversationIdHeader);
        } catch (IllegalArgumentException e) {
            return notFound("Source for process id " + processId + " not found", v, conversationIdHeader);
        } catch (Exception e) {
            logger.error("Unexpected error during processing {}", e.getMessage(), e);
            return internalServerError(errorMessage(e), v, conversationIdHeader);
        }
    }
}
