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
import org.apache.commons.configuration.Configuration;
import org.apache.whirr.ClusterSpec;
import org.apache.whirr.service.ClusterActionHandlerSupport;

public abstract class AccumuloClusterActionHandler extends ClusterActionHandlerSupport{
 protected synchronized Configuration getConfiguration(
      ClusterSpec clusterSpec) throws IOException {
    return getConfiguration(clusterSpec,
      AccumuloConstants.FILE_ACCUMULO_DEFAULT_PROPERTIES);
  }

  protected String getInstallFunction(Configuration config) {
    return getInstallFunction(config, "accumulo", AccumuloConstants.FUNCTION_INSTALL);
  }

  protected String getConfigureFunction(Configuration config) {
    return getConfigureFunction(config, "accumulo", AccumuloConstants.FUNCTION_CONFIGURE);
  }
    
}
