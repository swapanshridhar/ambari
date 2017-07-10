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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.SecretReference;

/**
 * This class encapsulates a configuration update request.
 * The configuration properties are grouped at service level. It is assumed that
 * different components of a service don't overload same property name.
 */
public class ConfigurationResponse {

  private final Long clusterId;

  private final StackId stackId;

  private final String type;

  private String versionTag;

  private Long version;

  private List<Long> serviceConfigVersions;

  private Map<String, String> configs;

  private Map<String, Map<String, String>> configAttributes;

  private Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes;

  public ConfigurationResponse(Long clusterId, StackId stackId,
      String type, String versionTag, Long version,
      Map<String, String> configs,
      Map<String, Map<String, String>> configAttributes) {
    this.clusterId = clusterId;
    this.stackId = stackId;
    this.configs = configs;
    this.type = type;
    this.versionTag = versionTag;
    this.version = version;
    this.configs = configs;
    this.configAttributes = configAttributes;
    SecretReference.replacePasswordsWithReferencesForCustomProperties(configAttributes, configs, type, version);
  }

  public ConfigurationResponse(Long clusterId, StackId stackId,
                               String type, String versionTag, Long version,
                               Map<String, String> configs,
                               Map<String, Map<String, String>> configAttributes,
                               Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes) {
    this.clusterId = clusterId;
    this.stackId = stackId;
    this.configs = configs;
    this.type = type;
    this.versionTag = versionTag;
    this.version = version;
    this.configs = configs;
    this.configAttributes = configAttributes;
    this.propertiesTypes = propertiesTypes;
    SecretReference.replacePasswordsWithReferences(propertiesTypes, configs, type, version);
    SecretReference.replacePasswordsWithReferencesForCustomProperties(configAttributes, configs, type, version);
  }

  /**
   * Constructor.
   *
   * @param clusterId
   * @param config
   */
  public ConfigurationResponse(Long clusterId, Config config) {
    this(clusterId, config.getStackId(), config.getType(), config.getTag(),
        config.getVersion(), config.getProperties(),
        config.getPropertiesAttributes(), config.getPropertiesTypes());
  }

  /**
   * @return the versionTag
   */
  public String getVersionTag() {
    return versionTag;
  }

  /**
   * @param versionTag the versionTag to set
   */
  public void setVersionTag(String versionTag) {
    this.versionTag = versionTag;
  }

  /**
   * @return the configs
   */
  public Map<String, String> getConfigs() {
    return configs;
  }

  /**
   * @param configs the configs to set
   */
  public void setConfigs(Map<String, String> configs) {
    this.configs = configs;
  }

  public Map<String, Map<String, String>> getConfigAttributes() {
    return configAttributes;
  }

  public void setConfigAttributes(Map<String, Map<String, String>> configAttributes) {
    this.configAttributes = configAttributes;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @return the clusterId
   */
  public Long getClusterId() {
    return clusterId;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  /**
   * Gets the Stack ID that this configuration is scoped for.
   *
   * @return
   */
  public StackId getStackId() {
    return stackId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConfigurationResponse that = (ConfigurationResponse) o;

    if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) {
      return false;
    }

    if (stackId != null ? !stackId.equals(that.stackId) : that.stackId != null) {
      return false;
    }

    if (type != null ? !type.equals(that.type) : that.type != null) {
      return false;
    }

    if (version != null ? !version.equals(that.version) : that.version != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = clusterId != null ? clusterId.hashCode() : 0;
    result = 31 * result + (stackId != null ? stackId.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  public List<Long> getServiceConfigVersions() {
    return serviceConfigVersions;
  }

  public void setServiceConfigVersions(List<Long> serviceConfigVersions) {
    this.serviceConfigVersions = serviceConfigVersions;
  }

  public Map<PropertyInfo.PropertyType, Set<String>> getPropertiesTypes() {
    return propertiesTypes;
  }

  public void setPropertiesTypes(Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes) {
    this.propertiesTypes = propertiesTypes;
  }
}
