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
package org.kie.server.services.jbpm.ui.source;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.drools.compiler.kie.builder.impl.KieContainerImpl;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.definition.process.Process;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceReference {

    private static final Logger logger = LoggerFactory.getLogger(SourceReference.class);

    private static final String DEFAULT_KBASE_NAME = "defaultKieBase";
    private KieContainer kieContainer;
    private String kieBaseName;

    public SourceReference(KieContainer kieContainer, String kieBaseName) {
        this.kieContainer = kieContainer;
        if (kieBaseName == null || kieBaseName.isEmpty()) {
            KieBaseModel defaultKBaseModel = ((KieContainerImpl) kieContainer).getKieProject().getDefaultKieBaseModel();
            if (defaultKBaseModel != null) {
                kieBaseName = defaultKBaseModel.getName();
            } else {
                kieBaseName = DEFAULT_KBASE_NAME;
            }
        }
        this.kieBaseName = kieBaseName;
    }

    public String getProcessSource(String processId) {
        Process process = kieContainer.getKieBase(kieBaseName).getProcess(processId);
        if (process != null) {
            Resource resource = process.getResource();
            try {
                return IOUtils.toString(resource.getReader());
            } catch (IOException e) {
                logger.debug("Error retrieving source for process {}.", processId, e);
            }
        }
        return null;
    }

}