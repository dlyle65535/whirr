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
package org.apache.whirr.service.accumulo.osgi;

import java.util.Properties;
import org.apache.whirr.service.ClusterActionHandler;
import org.apache.whirr.service.accumulo.AccumuloMasterClusterActionHandler;
import org.apache.whirr.service.accumulo.AccumuloTabletServerClusterActionHandler;
import org.jclouds.scriptbuilder.functionloader.osgi.BundleFunctionLoader;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private BundleFunctionLoader functionLoader;
    private final ClusterActionHandler tabletServerClusterActionHandler = new AccumuloTabletServerClusterActionHandler();
    private ServiceRegistration tabletServerRegistration;
    private final ClusterActionHandler accumuloMasterClusterActionHandler = new AccumuloMasterClusterActionHandler();
    private ServiceRegistration accumuloMasterRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        //Initialize OSGi based FunctionLoader
        functionLoader = new BundleFunctionLoader(context);
        functionLoader.start();

        Properties tabletServerProps = new Properties();
        tabletServerProps.put("name", "hadoop-datanode");
        tabletServerRegistration = context.registerService(ClusterActionHandler.class.getName(), tabletServerClusterActionHandler, tabletServerProps);

        Properties accumuloMasterProps = new Properties();
        accumuloMasterProps.put("name", "hadoop-namenode");
        accumuloMasterRegistration = context.registerService(ClusterActionHandler.class.getName(), accumuloMasterClusterActionHandler, accumuloMasterProps);

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (tabletServerRegistration != null) {
            tabletServerRegistration.unregister();
        }
        if (accumuloMasterRegistration != null) {
            accumuloMasterRegistration.unregister();
        }


        if (functionLoader != null) {
            functionLoader.stop();
        }
    }
}

