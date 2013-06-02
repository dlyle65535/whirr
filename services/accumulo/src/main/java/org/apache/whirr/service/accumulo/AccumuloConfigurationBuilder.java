/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.whirr.service.accumulo;

import java.io.IOException;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterSpec;
import static org.apache.whirr.RolePredicates.role;
import org.apache.whirr.service.hadoop.HadoopConfigurationConverter;
import org.apache.whirr.service.zookeeper.ZooKeeperCluster;
import org.jclouds.scriptbuilder.domain.Statement;


public class AccumuloConfigurationBuilder {
 public static Statement buildAccumuloSite(String path, ClusterSpec clusterSpec, Cluster cluster)
      throws ConfigurationException, IOException {
    Configuration config = buildAccumuloSiteConfiguration(clusterSpec, cluster,
        new PropertiesConfiguration(AccumuloConfigurationBuilder.class.getResource("/" + AccumuloConstants.FILE_ACCUMULO_DEFAULT_PROPERTIES)));
    return HadoopConfigurationConverter.asCreateXmlConfigurationFileStatement(path, config);
  }

  static Configuration buildAccumuloSiteConfiguration(ClusterSpec clusterSpec, Cluster cluster, Configuration defaults)
      throws ConfigurationException, IOException {
    Configuration config = build(clusterSpec, cluster, defaults, "accumulo-site");

    Cluster.Instance master = cluster.getInstanceMatching(
      role(AccumuloMasterClusterActionHandler.ROLE));
    String masterHostName = master.getPublicHostName();

    //config.setProperty("hbase.rootdir", String.format("hdfs://%s:8020/hbase", masterHostName));
    config.setProperty("accumulo.zookeeper.quorum", ZooKeeperCluster.getHosts(cluster));

    return config;
  }

  public static Statement buildAccumuloEnv(String path, ClusterSpec clusterSpec, Cluster cluster)
      throws ConfigurationException, IOException {
    Configuration config = buildAccumuloEnvConfiguration(clusterSpec, cluster,
            new PropertiesConfiguration(AccumuloConfigurationBuilder.class.getResource("/" + AccumuloConstants.FILE_ACCUMULO_DEFAULT_PROPERTIES)));
    return HadoopConfigurationConverter.asCreateEnvironmentVariablesFileStatement(path, config);
  }

  static Configuration buildAccumuloEnvConfiguration(ClusterSpec clusterSpec, Cluster cluster, Configuration defaults)
      throws ConfigurationException, IOException {
    Configuration config = build(clusterSpec, cluster, defaults, "accumulo-env");

    return config;
  }

  private static Configuration build(ClusterSpec clusterSpec, Cluster cluster, Configuration defaults, String prefix)
      throws ConfigurationException {
    CompositeConfiguration config = new CompositeConfiguration();
    Configuration sub = clusterSpec.getConfigurationForKeysWithPrefix(prefix);
    config.addConfiguration(sub.subset(prefix)); // remove prefix
    config.addConfiguration(defaults.subset(prefix));
    return config;
  }    
}
