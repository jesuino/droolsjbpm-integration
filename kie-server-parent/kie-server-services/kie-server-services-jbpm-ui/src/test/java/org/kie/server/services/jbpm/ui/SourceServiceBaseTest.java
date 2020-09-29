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

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.server.api.model.KieServerConfig;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.KieServerRegistryImpl;
import org.kie.server.services.jbpm.ui.source.SourceReference;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SourceServiceBaseTest {

    @Mock
    KieServerRegistry kieServerRegistry = new KieServerRegistryImpl();

    @Mock
    SourceReference sourceReference;

    @Mock
    KieServerConfig config;

    @Test
    public void testGetProcessSource() {
        String containerId = "container";
        String processId = "process";
        String processContent = "content";
        when(sourceReference.getProcessSource(processId)).thenReturn(processContent);
        SourceServiceBase sourceServiceBase = new SourceServiceBase(Collections.singletonMap(containerId, sourceReference));
        String returnedContent = sourceServiceBase.getProcessSource(containerId, processId);
        assertEquals(returnedContent, processContent);
        verify(sourceReference).getProcessSource(eq(processId));
    }
}
