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
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.whirr.Cluster;
import org.apache.whirr.ClusterSpec;
import static org.apache.whirr.RolePredicates.role;
import org.apache.whirr.service.ClusterActionEvent;
import static org.apache.whirr.service.ClusterActionHandlerSupport.addStatement;
import static org.apache.whirr.service.ClusterActionHandlerSupport.handleFirewallRules;
import static org.apache.whirr.service.ClusterActionHandlerSupport.prepareRemoteFileUrl;
import org.apache.whirr.service.zookeeper.ZooKeeperCluster;
import org.apache.whirr.template.TemplateUtils;
import static org.jclouds.scriptbuilder.domain.Statements.call;

public class AccumuloTabletServerClusterActionHandler extends AccumuloClusterActionHandler {

    public static final String ROLE = "accumulo-tserver";

    @Override
    public String getRole() {
        return ROLE;
    }

    @Override
    protected void beforeBootstrap(ClusterActionEvent event) throws IOException {
        ClusterSpec clusterSpec = event.getClusterSpec();
        Configuration conf = getConfiguration(clusterSpec);

        addStatement(event, call("retry_helpers"));
        addStatement(event, call("configure_hostnames"));
        addStatement(event, call("install_tarball"));

        addStatement(event, call(getInstallFunction(conf, "java", "install_openjdk")));

        String tarurl = prepareRemoteFileUrl(event,
                getConfiguration(clusterSpec).getString(AccumuloConstants.KEY_TARBALL_URL));

        addStatement(event, call(
                getInstallFunction(getConfiguration(clusterSpec)),
                AccumuloConstants.PARAM_TARBALL_URL, tarurl));
    }

    @Override
    protected void beforeConfigure(ClusterActionEvent event)
            throws IOException, InterruptedException {
        ClusterSpec clusterSpec = event.getClusterSpec();
        Cluster cluster = event.getCluster();
        Configuration conf = getConfiguration(clusterSpec);

        Cluster.Instance instance = cluster.getInstanceMatching(
                role(AccumuloMasterClusterActionHandler.ROLE));
        InetAddress masterPublicAddress = instance.getPublicAddress();

        //Velocity is assuming flat classloaders or TCCL to load templates.
        //This doesn't work in OSGi unless we set the TCCL to the bundle classloader before invocation
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();

        handleFirewallRules(event);

        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            event.getStatementBuilder().addStatements(
                    AccumuloConfigurationBuilder.buildAccumuloSite("/tmp/hbase-site.xml", clusterSpec, cluster),
                    AccumuloConfigurationBuilder.buildAccumuloEnv("/tmp/hbase-env.sh", clusterSpec, cluster));
                    //TemplateUtils.createFileFromTemplate("/tmp/hbase-hadoop-metrics.properties", event.getTemplateEngine(), getMetricsTemplate(event, clusterSpec, cluster), clusterSpec, cluster));
        } catch (ConfigurationException e) {
            throw new IOException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldTccl);
        }

        String master = masterPublicAddress.getHostName();
        String quorum = ZooKeeperCluster.getHosts(cluster);

        String tarurl = prepareRemoteFileUrl(event,
                conf.getString(AccumuloConstants.KEY_TARBALL_URL));

        addStatement(event, call("retry_helpers"));
        addStatement(event, call(
                getConfigureFunction(conf),
                ROLE,
                AccumuloConstants.PARAM_MASTER, master,
                AccumuloConstants.PARAM_QUORUM, quorum,
                AccumuloConstants.PARAM_TARBALL_URL, tarurl));
    }
}
