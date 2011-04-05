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

package org.apache.whirr.service.hbase;

import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.whirr.service.ClusterActionHandlerSupport;
import org.apache.whirr.service.ClusterSpec;

/**
 * Base class for HBase service handlers.
 */
public abstract class HBaseClusterActionHandler
       extends ClusterActionHandlerSupport {

  /**
   * Returns a composite configuration that is made up from the global
   * configuration coming from the Whirr core with a hbase defaults
   * properties.
   *
   * @param clusterSpec  The cluster specification instance.
   * @return The composite configuration.
   */
  protected synchronized Configuration getConfiguration(
      ClusterSpec clusterSpec) throws IOException {
    try {
      Configuration defaults = new PropertiesConfiguration(
        HBaseConstants.FILE_HBASE_DEFAULT_PROPERTIES);
      return super.getConfiguration(clusterSpec, defaults);
    } catch (ConfigurationException e) {
      throw new IOException("Error loading HBase default properties.", e);
    }
  }
}
