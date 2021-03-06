/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.server.services.casemgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import javax.persistence.EntityManagerFactory;

import org.jbpm.casemgmt.api.AdvanceCaseRuntimeDataService;
import org.jbpm.casemgmt.api.CaseRuntimeDataService;
import org.jbpm.casemgmt.api.admin.CaseInstanceMigrationService;
import org.jbpm.casemgmt.api.generator.CaseIdGenerator;
import org.jbpm.casemgmt.impl.AdvanceCaseRuntimeDataServiceImpl;
import org.jbpm.casemgmt.impl.AuthorizationManagerImpl;
import org.jbpm.casemgmt.impl.CaseRuntimeDataServiceImpl;
import org.jbpm.casemgmt.impl.CaseServiceImpl;
import org.jbpm.casemgmt.impl.admin.CaseInstanceMigrationServiceImpl;
import org.jbpm.casemgmt.impl.event.CaseConfigurationDeploymentListener;
import org.jbpm.casemgmt.impl.generator.TableCaseIdGenerator;
import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.admin.ProcessInstanceMigrationService;
import org.jbpm.shared.services.impl.TransactionalCommandService;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.Message;
import org.kie.server.api.model.Severity;
import org.kie.server.services.api.KieContainerCommandService;
import org.kie.server.services.api.KieContainerInstance;
import org.kie.server.services.api.KieServerApplicationComponentsService;
import org.kie.server.services.api.KieServerExtension;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.api.SupportedTransports;
import org.kie.server.services.impl.KieServerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseKieServerExtension implements KieServerExtension {

    public static final String EXTENSION_NAME = "Case-Mgmt";

    private static final Logger logger = LoggerFactory.getLogger(CaseKieServerExtension.class);

    private static final Boolean disabled = Boolean.parseBoolean(System.getProperty(KieServerConstants.KIE_CASE_SERVER_EXT_DISABLED, "false"));
    private static final Boolean jbpmDisabled = Boolean.parseBoolean(System.getProperty(KieServerConstants.KIE_JBPM_SERVER_EXT_DISABLED, "false"));

    protected String persistenceUnitName = KieServerConstants.KIE_SERVER_PERSISTENCE_UNIT_NAME;

    protected List<Object> services = new ArrayList<Object>();
    protected boolean initialized = false;

    protected KieServerRegistry registry;

    protected CaseManagementServiceBase caseManagementServiceBase;
    protected CaseManagementRuntimeDataServiceBase caseManagementRuntimeDataService;
    protected CaseAdminServiceBase caseAdminServiceBase;
    protected AdvanceCaseRuntimeDataService advanceCaseRuntimeDataService;

    protected CaseRuntimeDataService caseRuntimeDataService;
    protected KieContainerCommandService kieContainerCommandService;

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isActive() {
        return disabled == false && jbpmDisabled == false;
    }

    @Override
    public void init(KieServerImpl kieServer, KieServerRegistry registry) {
        this.registry = registry;
        KieServerExtension jbpmExtension = registry.getServerExtension("jBPM");
        if (jbpmExtension == null) {
            initialized = false;
            logger.warn("jBPM extension not found, Case Management cannot work without jBPM extension, disabling itself");
            return;
        }

        configureServices(kieServer, registry);

        this.services.add(this.caseManagementServiceBase);
        this.services.add(this.caseManagementRuntimeDataService);
        this.services.add(this.caseRuntimeDataService);
        this.services.add(this.advanceCaseRuntimeDataService);

        this.kieContainerCommandService = new CaseKieContainerCommandServiceImpl(registry, caseManagementServiceBase, caseManagementRuntimeDataService, caseAdminServiceBase);

        initialized = true;
    }

    protected void configureServices(KieServerImpl kieServer, KieServerRegistry registry) {

        KieServerExtension jbpmExtension = registry.getServerExtension("jBPM");
        List<Object> jbpmServices = jbpmExtension.getServices();
        RuntimeDataService runtimeDataService = null;
        ProcessService processService = null;
        DeploymentService deploymentService = null;
        ProcessInstanceMigrationService processInstanceMigrationService = null;

        for (Object object : jbpmServices) {
            // in case given service is null (meaning was not configured) continue with next one
            if (object == null) {
                continue;
            }
            if (RuntimeDataService.class.isAssignableFrom(object.getClass())) {
                runtimeDataService = (RuntimeDataService) object;
                continue;
            } else if (ProcessService.class.isAssignableFrom(object.getClass())) {
                processService = (ProcessService) object;
                continue;
            } else if (DeploymentService.class.isAssignableFrom(object.getClass())) {
                deploymentService = (DeploymentService) object;
                continue;
            } else if (ProcessInstanceMigrationService.class.isAssignableFrom(object.getClass())) {
                processInstanceMigrationService = (ProcessInstanceMigrationService) object;
                continue;
            }
        }
        CaseIdGenerator caseIdGenerator = getCaseIdGenerator();
        EntityManagerFactory emf = EntityManagerFactoryManager.get().getOrCreate(persistenceUnitName);

        // build case runtime data service
        caseRuntimeDataService = new CaseRuntimeDataServiceImpl();
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setCaseIdGenerator(caseIdGenerator);
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setRuntimeDataService(runtimeDataService);
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setCommandService(new TransactionalCommandService(EntityManagerFactoryManager.get().getOrCreate(persistenceUnitName)));
        ((CaseRuntimeDataServiceImpl) caseRuntimeDataService).setIdentityProvider(registry.getIdentityProvider());

        // build case service
        CaseServiceImpl caseService = new CaseServiceImpl();
        ((CaseServiceImpl) caseService).setCaseIdGenerator(caseIdGenerator);
        ((CaseServiceImpl) caseService).setCaseRuntimeDataService(caseRuntimeDataService);
        ((CaseServiceImpl) caseService).setProcessService(processService);
        ((CaseServiceImpl) caseService).setDeploymentService(deploymentService);
        ((CaseServiceImpl) caseService).setRuntimeDataService(runtimeDataService);
        ((CaseServiceImpl) caseService).setCommandService(new TransactionalCommandService(emf));
        ((CaseServiceImpl) caseService).setAuthorizationManager(new AuthorizationManagerImpl(registry.getIdentityProvider(),
                                                                                             new TransactionalCommandService(EntityManagerFactoryManager.get().getOrCreate(persistenceUnitName))));
        ((CaseServiceImpl) caseService).setIdentityProvider(registry.getIdentityProvider());

        // build case configuration on deployment listener
        CaseConfigurationDeploymentListener configurationListener = new CaseConfigurationDeploymentListener(registry.getIdentityProvider());

        // configure case mgmt services as listeners
        ((KModuleDeploymentService) deploymentService).addListener((CaseRuntimeDataServiceImpl) caseRuntimeDataService);
        ((KModuleDeploymentService) deploymentService).addListener(configurationListener);

        CaseInstanceMigrationService caseInstanceMigrationService = new CaseInstanceMigrationServiceImpl();
        ((CaseInstanceMigrationServiceImpl) caseInstanceMigrationService).setCaseRuntimeDataService(caseRuntimeDataService);
        ((CaseInstanceMigrationServiceImpl) caseInstanceMigrationService).setCommandService(new TransactionalCommandService(EntityManagerFactoryManager.get().getOrCreate(persistenceUnitName)));
        ((CaseInstanceMigrationServiceImpl) caseInstanceMigrationService).setProcessInstanceMigrationService(processInstanceMigrationService);
        ((CaseInstanceMigrationServiceImpl) caseInstanceMigrationService).setProcessService(processService);

        advanceCaseRuntimeDataService = new AdvanceCaseRuntimeDataServiceImpl();
        ((AdvanceCaseRuntimeDataServiceImpl) advanceCaseRuntimeDataService).setEmf(emf);
        ((AdvanceCaseRuntimeDataServiceImpl) advanceCaseRuntimeDataService).setCommandService(new TransactionalCommandService(emf));

        this.caseManagementServiceBase = new CaseManagementServiceBase(caseService, caseRuntimeDataService, registry);
        this.caseManagementRuntimeDataService = new CaseManagementRuntimeDataServiceBase(caseRuntimeDataService, advanceCaseRuntimeDataService, registry);
        this.caseAdminServiceBase = new CaseAdminServiceBase(caseInstanceMigrationService, registry);
    }

    protected CaseIdGenerator getCaseIdGenerator() {
        String selectedGenerator = System.getProperty(KieServerConstants.CFG_CASE_ID_GENERATOR);

        if (selectedGenerator == null) {
            return new TableCaseIdGenerator(new TransactionalCommandService(EntityManagerFactoryManager.get().getOrCreate(persistenceUnitName)));
        }

        ServiceLoader<CaseIdGenerator> generators = ServiceLoader.load(CaseIdGenerator.class);

        for (CaseIdGenerator generator : generators) {
            if (generator.getIdentifier().equals(selectedGenerator)) {
                return generator;
            }
        }

        throw new IllegalStateException("Unable to find case id generator identified by " + selectedGenerator);
    }

    @Override
    public void destroy(KieServerImpl kieServer, KieServerRegistry registry) {
        if (!initialized) {
            return;
        }
    }

    @Override
    public void createContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        if (!initialized) {
            return;
        }
    }

    @Override
    public void updateContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        if (!initialized) {
            return;
        }
        // recreate configuration for updated container
        disposeContainer(id, kieContainerInstance, parameters);
        createContainer(id, kieContainerInstance, parameters);
    }

    @Override
    public boolean isUpdateContainerAllowed(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        return true;
    }

    @Override
    public void disposeContainer(String id, KieContainerInstance kieContainerInstance, Map<String, Object> parameters) {
        if (!initialized) {
            return;
        }
    }

    @Override
    public List<Object> getAppComponents(SupportedTransports type) {
        List<Object> appComponentsList = new ArrayList<Object>();
        if (!initialized) {
            return appComponentsList;
        }
        ServiceLoader<KieServerApplicationComponentsService> appComponentsServices = ServiceLoader.load(KieServerApplicationComponentsService.class);

        Object[] services = {
                             caseManagementServiceBase,
                             caseManagementRuntimeDataService,
                             caseAdminServiceBase,
                             registry,
                             advanceCaseRuntimeDataService
        };
        for (KieServerApplicationComponentsService appComponentsService : appComponentsServices) {
            appComponentsList.addAll(appComponentsService.getAppComponents(EXTENSION_NAME, type, services));
        }
        return appComponentsList;
    }

    @Override
    public <T> T getAppComponents(Class<T> serviceType) {
        if (!initialized) {
            return null;
        }
        if (serviceType.isAssignableFrom(kieContainerCommandService.getClass())) {
            return (T) kieContainerCommandService;
        }

        final Optional<Object> service = services.stream().filter(s -> serviceType.isAssignableFrom(s.getClass())).findFirst();
        return (T) service.orElse(null);
    }

    @Override
    public String getImplementedCapability() {
        return KieServerConstants.CAPABILITY_CASE;
    }

    @Override
    public List<Object> getServices() {
        return services;
    }

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    @Override
    public Integer getStartOrder() {
        return 8;
    }

    @Override
    public String toString() {
        return EXTENSION_NAME + " KIE Server extension";
    }

    @Override
    public List<Message> healthCheck(boolean report) {
        List<Message> messages = KieServerExtension.super.healthCheck(report);

        if (report) {
            messages.add(new Message(Severity.INFO, getExtensionName() + " is alive"));
        }
        return messages;
    }
}
