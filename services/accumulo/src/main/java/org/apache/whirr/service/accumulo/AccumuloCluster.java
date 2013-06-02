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
import java.net.InetAddress;
import org.apache.whirr.Cluster;
import org.apache.whirr.RolePredicates;

public class AccumuloCluster {
   public static InetAddress getMasterPublicAddress(Cluster cluster) throws IOException {
    return cluster.getInstanceMatching(
      RolePredicates.role(AccumuloMasterClusterActionHandler.ROLE))
        .getPublicAddress();
  }  
}
