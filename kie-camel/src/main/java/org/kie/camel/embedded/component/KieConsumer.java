/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.camel.embedded.component;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.drools.core.command.runtime.RegisterChannelCommand;
import org.drools.core.command.runtime.UnregisterChannelCommand;
import org.kie.api.runtime.Channel;
import org.kie.api.runtime.CommandExecutor;

/** A consumer that consumes objects sent into channels of a drools
 * session */
public class KieConsumer extends DefaultConsumer {

    private KieEmbeddedEndpoint ke;
    private CommandExecutor krt;
    private String channelId;

    public KieConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
        ke = (KieEmbeddedEndpoint)endpoint;
        krt = ke.getExecutor();
        channelId = ke.getChannel();
    }

    @Override
    protected void doStop() throws Exception {
        krt.execute(new UnregisterChannelCommand(channelId));
        super.doStop();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        KSessionChannel channel = new KSessionChannel();
        krt.execute(new RegisterChannelCommand(channelId, channel));
    }

    class KSessionChannel implements Channel {
        public void send(Object pojo) {
            Exchange exchange = ke.createExchange(pojo);
            try {
                getProcessor().process(exchange);
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

}
