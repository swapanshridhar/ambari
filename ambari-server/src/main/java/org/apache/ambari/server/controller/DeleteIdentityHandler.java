/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.ambari.server.controller.KerberosHelperImpl.BASE_LOG_DIR;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.serveraction.kerberos.AbstractPrepareKerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.Component;
import org.apache.ambari.server.serveraction.kerberos.DestroyPrincipalsServerAction;
import org.apache.ambari.server.serveraction.kerberos.KDCType;
import org.apache.ambari.server.serveraction.kerberos.KerberosOperationHandler;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.utils.StageUtils;

/**
 * I delete kerberos identities (principals and keytabs) of a given component.
 */
class DeleteIdentityHandler {
  private final AmbariCustomCommandExecutionHelper customCommandExecutionHelper;
  private final Integer taskTimeout;
  private final StageFactory stageFactory;
  private final AmbariManagementController ambariManagementController;

  public DeleteIdentityHandler(AmbariCustomCommandExecutionHelper customCommandExecutionHelper, Integer taskTimeout, StageFactory stageFactory, AmbariManagementController ambariManagementController) {
    this.customCommandExecutionHelper = customCommandExecutionHelper;
    this.taskTimeout = taskTimeout;
    this.stageFactory = stageFactory;
    this.ambariManagementController = ambariManagementController;
  }

  /**
   * Creates and adds stages to the given stage container for deleting kerberos identities.
   * The service component that belongs to the identity doesn't need to be installed.
   */
  public void addDeleteIdentityStages(Cluster cluster, OrderedRequestStageContainer stageContainer, CommandParams commandParameters, boolean manageIdentities)
    throws AmbariException
  {
    ServiceComponentHostServerActionEvent event = new ServiceComponentHostServerActionEvent("AMBARI_SERVER", StageUtils.getHostName(), System.currentTimeMillis());
    String hostParamsJson = StageUtils.getGson().toJson(customCommandExecutionHelper.createDefaultHostParams(cluster, cluster.getDesiredStackVersion()));
    stageContainer.setClusterHostInfo(StageUtils.getGson().toJson(StageUtils.getClusterHostInfo(cluster)));
    if (manageIdentities) {
      addPrepareDeleteIdentity(cluster, hostParamsJson, event, commandParameters, stageContainer);
      addDestroyPrincipals(cluster, hostParamsJson, event, commandParameters, stageContainer);
      addDeleteKeytab(cluster, newHashSet(commandParameters.component.getHostName()), hostParamsJson, commandParameters, stageContainer);
    }
    addFinalize(cluster, hostParamsJson, event, stageContainer, commandParameters);
  }

  private void addPrepareDeleteIdentity(Cluster cluster,
                                        String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                        CommandParams commandParameters,
                                        OrderedRequestStageContainer stageContainer)
    throws AmbariException
  {
    Stage stage = createServerActionStage(stageContainer.getLastStageId(),
      cluster,
      stageContainer.getId(),
      "Prepare delete identities",
      "{}",
      hostParamsJson,
      PrepareDeleteIdentityServerAction.class,
      event,
      commandParameters.asMap(),
      "Prepare delete identities",
      taskTimeout);
    stageContainer.addStage(stage);
  }

  private void addDestroyPrincipals(Cluster cluster,
                                    String hostParamsJson, ServiceComponentHostServerActionEvent event,
                                    CommandParams commandParameters,
                                    OrderedRequestStageContainer stageContainer)
    throws AmbariException
  {
    Stage stage = createServerActionStage(stageContainer.getLastStageId(),
      cluster,
      stageContainer.getId(),
      "Destroy Principals",
      "{}",
      hostParamsJson,
      DestroyPrincipalsServerAction.class,
      event,
      commandParameters.asMap(),
      "Destroy Principals",
      Math.max(ServerAction.DEFAULT_LONG_RUNNING_TASK_TIMEOUT_SECONDS, taskTimeout));
    stageContainer.addStage(stage);
  }

  private void addDeleteKeytab(Cluster cluster,
                               Set<String> hostFilter,
                               String hostParamsJson,
                               CommandParams commandParameters,
                               OrderedRequestStageContainer stageContainer)
    throws AmbariException
  {
    Stage stage = createNewStage(stageContainer.getLastStageId(),
      cluster,
      stageContainer.getId(),
      "Delete Keytabs",
      commandParameters.asJson(),
      hostParamsJson);

    Map<String, String> requestParams = new HashMap<>();
    List<RequestResourceFilter> requestResourceFilters = new ArrayList<>();
    RequestResourceFilter reqResFilter = new RequestResourceFilter("KERBEROS", "KERBEROS_CLIENT", new ArrayList<>(hostFilter));
    requestResourceFilters.add(reqResFilter);

    ActionExecutionContext actionExecContext = new ActionExecutionContext(
      cluster.getClusterName(),
      "REMOVE_KEYTAB",
      requestResourceFilters,
      requestParams);
    customCommandExecutionHelper.addExecutionCommandsToStage(actionExecContext, stage, requestParams, null);
    stageContainer.addStage(stage);
  }

  private void addFinalize(Cluster cluster,
                           String hostParamsJson, ServiceComponentHostServerActionEvent event,
                           OrderedRequestStageContainer requestStageContainer,
                           CommandParams commandParameters)
    throws AmbariException
  {
    Stage stage = createServerActionStage(requestStageContainer.getLastStageId(),
      cluster,
      requestStageContainer.getId(),
      "Finalize Operations",
      "{}",
      hostParamsJson,
      DeleteDataDirAction.class,
      event,
      commandParameters.asMap(),
      "Finalize Operations", 300);
    requestStageContainer.addStage(stage);
  }


  public static class CommandParams {
    private final Component component;
    private final List<String> identities;
    private final String authName;
    private final File dataDirectory;
    private final String defaultRealm;
    private final KDCType kdcType;

    public CommandParams(Component component, List<String> identities, String authName, File dataDirectory, String defaultRealm, KDCType kdcType) {
      this.component = component;
      this.identities = identities;
      this.authName = authName;
      this.dataDirectory = dataDirectory;
      this.defaultRealm = defaultRealm;
      this.kdcType = kdcType;
    }

    public Map<String, String> asMap() {
      Map<String, String> commandParameters = new HashMap<>();
      commandParameters.put(KerberosServerAction.AUTHENTICATED_USER_NAME, authName);
      commandParameters.put(KerberosServerAction.DEFAULT_REALM, defaultRealm);
      commandParameters.put(KerberosServerAction.KDC_TYPE, kdcType.name());
      commandParameters.put(KerberosServerAction.IDENTITY_FILTER, StageUtils.getGson().toJson(identities));
      commandParameters.put(KerberosServerAction.COMPONENT_FILTER, StageUtils.getGson().toJson(component));
      commandParameters.put(KerberosServerAction.DATA_DIRECTORY, dataDirectory.getAbsolutePath());
      return commandParameters;
    }

    public String asJson() {
      return StageUtils.getGson().toJson(asMap());
    }
  }

  private static class PrepareDeleteIdentityServerAction extends AbstractPrepareKerberosServerAction {
    @Override
    public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
      KerberosDescriptor kerberosDescriptor = getKerberosDescriptor();
      processServiceComponents(
        getCluster(),
        kerberosDescriptor,
        Collections.singletonList(getComponentFilter()),
        getIdentityFilter(),
        dataDirectory(),
        calculateConfig(kerberosDescriptor),
        new HashMap<String, Map<String, String>>(),
        false,
        new HashMap<String, Set<String>>());
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
    }

    protected Component getComponentFilter() {
      return StageUtils.getGson().fromJson(getCommandParameterValue(KerberosServerAction.COMPONENT_FILTER), Component.class);
    }

    private Map<String, Map<String, String>> calculateConfig(KerberosDescriptor kerberosDescriptor) throws AmbariException {
      return getKerberosHelper().calculateConfigurations(getCluster(), null, kerberosDescriptor.getProperties());
    }

    private String dataDirectory() {
      return getCommandParameterValue(getCommandParameters(), DATA_DIRECTORY);
    }

    private KerberosDescriptor getKerberosDescriptor() throws AmbariException {
      return getKerberosHelper().getKerberosDescriptor(getCluster());
    }
  }

  private Stage createNewStage(long id, Cluster cluster, long requestId, String requestContext, String commandParams, String hostParams) {
    Stage stage = stageFactory.createNew(requestId,
      BASE_LOG_DIR + File.pathSeparator + requestId,
      cluster.getClusterName(),
      cluster.getClusterId(),
      requestContext,
      commandParams,
      hostParams);
    stage.setStageId(id);
    return stage;
  }

  private Stage createServerActionStage(long id, Cluster cluster, long requestId,
                                       String requestContext,
                                       String commandParams, String hostParams,
                                       Class<? extends ServerAction> actionClass,
                                       ServiceComponentHostServerActionEvent event,
                                       Map<String, String> commandParameters, String commandDetail,
                                       Integer timeout) throws AmbariException {

    Stage stage = createNewStage(id, cluster, requestId, requestContext,  commandParams, hostParams);
    stage.addServerActionCommand(actionClass.getName(), null, Role.AMBARI_SERVER_ACTION,
      RoleCommand.EXECUTE, cluster.getClusterName(), event, commandParameters, commandDetail,
      ambariManagementController.findConfigurationTagsWithOverrides(cluster, null), timeout,
      false, false);

    return stage;
  }

  private static class DeleteDataDirAction extends KerberosServerAction {

    @Override
    public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
      deleteDataDirectory(getCommandParameterValue(DATA_DIRECTORY));
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", actionLog.getStdOut(), actionLog.getStdErr());
    }

    @Override
    protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal, KerberosOperationHandler operationHandler, Map<String, String> kerberosConfiguration, Map<String, Object> requestSharedDataContext) throws AmbariException {
      return null;
    }
  }
}
