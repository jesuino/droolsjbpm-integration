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

package org.kie.server.services.jbpm.ui;

import java.util.Map;

import org.kie.server.services.jbpm.ui.source.SourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(SourceServiceBase.class);

    private Map<String, SourceReference> sourceReferenceMap;

    public SourceServiceBase(Map<String, SourceReference> sourceReferenceMap) {
        this.sourceReferenceMap = sourceReferenceMap;
    }

    public String getProcessSource(String containerId, String processId) {
        String processSource = sourceReferenceMap.get(containerId).getProcessSource(processId);
        if (processSource == null) {
            logger.warn("Could not find source for process '" + processId + "' within container " + containerId);
        }
        return processSource;
    }
}