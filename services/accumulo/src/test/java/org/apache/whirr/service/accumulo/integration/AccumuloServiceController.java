/**
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

package org.apache.whirr.service.accumulo.integration;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.accumulo.core.conf.AccumuloConfiguration;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterController;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.RolePredicates;
import org.apache.whirr.service.accumulo.AccumuloMasterClusterActionHandler;
import org.apache.whirr.service.hadoop.HadoopProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccumuloServiceController {

  private final String configResource;

  private static final Logger LOG =
    LoggerFactory.getLogger(AccumuloServiceController.class);

  private static final Map<String, AccumuloServiceController> INSTANCES = new HashMap<String, AccumuloServiceController>();

  public static AccumuloServiceController getInstance(String configResource) {
    AccumuloServiceController controller = INSTANCES.get(configResource);
    if (controller == null) {
      controller = new AccumuloServiceController(configResource);
      INSTANCES.put(configResource, controller);
    }
    return controller;
  }

  private boolean running;
  private ClusterSpec clusterSpec;
  private ClusterController controller;
  private HadoopProxy proxy;
  private Cluster cluster;
  

  private AccumuloServiceController(String configResource) {
    this.configResource = configResource;
  }

  public synchronized boolean ensureClusterRunning() throws Exception {
    if (running) {
      LOG.info("Cluster already running.");
      return false;
    } else {
      startup();
      return true;
    }
  }

  public synchronized void startup() throws Exception {
    LOG.info("Starting up cluster...");
    CompositeConfiguration config = new CompositeConfiguration();
    if (System.getProperty("config") != null) {
      config.addConfiguration(new PropertiesConfiguration(System.getProperty("config")));
    }
    config.addConfiguration(new PropertiesConfiguration(this.configResource));
    clusterSpec = ClusterSpec.withTemporaryKeys(config);
    controller = new ClusterController();

    cluster = controller.launchCluster(clusterSpec);
    proxy = new HadoopProxy(clusterSpec, cluster);
    proxy.start();

    waitForMaster();
    running = true;
  }

  

  private void waitForMaster() throws IOException {
    LOG.info("Waiting for master...");
    InetAddress masterAddress = cluster.getInstanceMatching(RolePredicates.role(
        AccumuloMasterClusterActionHandler.ROLE)).getPublicAddress();

    while (true) {
      try {
        checkMonitor(masterAddress);
        break;
      } catch (Exception e) {
        try {
          System.out.print(".");
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          break;
        }
      }
    }
    System.out.println();
    LOG.info("Master reported in. Continuing.");
  }
  
  private void checkMonitor(InetAddress thriftAddress) throws Exception {
    TTransport transport = new TSocket(thriftAddress.getHostName(),
        AccumuloMasterClusterActionHandler.PORT);
    transport.open();
    LOG.info("Connected to master server monitor.");
  }

  public synchronized void shutdown() throws IOException, InterruptedException {
    LOG.info("Shutting down cluster...");
    if (proxy != null) {
      proxy.stop();
    }
    if (controller != null) {
      controller.destroyCluster(clusterSpec);
    }
    running = false;
  }

  
}
