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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;
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
import static org.jclouds.scriptbuilder.domain.Statements.call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccumuloMasterClusterActionHandler extends AccumuloClusterActionHandler {

    public static final String ROLE = "accumulo-master";
    //Default Monitor Port
    public static final int PORT = 50065;
    private static final Logger LOG =
            LoggerFactory.getLogger(AccumuloMasterClusterActionHandler.class);

    @Override
    public String getRole() {
        return ROLE;
    }

    @Override
    protected void beforeBootstrap(ClusterActionEvent event) throws IOException {
        ClusterSpec clusterSpec = event.getClusterSpec();
        Configuration conf = getConfiguration(clusterSpec);

        addStatement(event, call("retry_helpers"));
        addStatement(event, call("install_tarball"));
        addStatement(event, call("configure_hostnames"));

        addStatement(event, call(getInstallFunction(conf, "java", "install_openjdk")));

        String tarurl = prepareRemoteFileUrl(event,
                getConfiguration(clusterSpec).getString(AccumuloConstants.KEY_TARBALL_URL));

        addStatement(event, call(
                getInstallFunction(getConfiguration(clusterSpec)),
                AccumuloConstants.PARAM_TARBALL_URL, tarurl));
    }

    @Override
    protected void beforeConfigure(ClusterActionEvent event) throws IOException, InterruptedException {
        ClusterSpec clusterSpec = event.getClusterSpec();
        Cluster cluster = event.getCluster();
        Configuration conf = getConfiguration(clusterSpec);

        LOG.info("Authorizing firewall");
        Cluster.Instance instance = cluster.getInstanceMatching(role(ROLE));
        InetAddress masterPublicAddress = instance.getPublicAddress();

        //Velocity is assuming flat classloaders or TCCL to load templates.
        //This doesn't work in OSGi unless we set the TCCL to the bundle classloader before invocation
        ClassLoader oldTccl = Thread.currentThread().getContextClassLoader();

        handleFirewallRules(event);

        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            event.getStatementBuilder().addStatements(
                    AccumuloConfigurationBuilder.buildAccumuloSite("/tmp/accumulo-site.xml", clusterSpec, cluster),
                    AccumuloConfigurationBuilder.buildAccumuloEnv("/tmp/accumulo-env.sh", clusterSpec, cluster));                                      
//accumulo-metrics.xml
//accumulo.policy.example
//gc
//generic_logger.xml
//log4j.properties
//masters
//monitor
//monitor_logger.xml
//slaves
//tracers

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

    @Override
    protected void afterConfigure(ClusterActionEvent event) throws IOException {
        ClusterSpec clusterSpec = event.getClusterSpec();
        Cluster cluster = event.getCluster();


        LOG.info("Completed configuration of {}", clusterSpec.getClusterName());
        Cluster.Instance instance = cluster.getInstanceMatching(role(ROLE));
        InetAddress masterPublicAddress = instance.getPublicAddress();

        LOG.info("Web UI available at http://{}", masterPublicAddress.getHostName());
        String quorum = ZooKeeperCluster.getHosts(cluster);
        Properties config = createClientSideProperties(masterPublicAddress, quorum);
        createClientSideAccumuloSiteFile(clusterSpec, config);
        event.setCluster(new Cluster(cluster.getInstances(), config));
    }

    private Properties createClientSideProperties(InetAddress master, String quorum) throws IOException {
        Properties config = new Properties();
        config.setProperty(AccumuloConstants.PROP_ACCUMULO_ZOOKEEPER_QUORUM, quorum);
        config.setProperty(AccumuloConstants.PROP_ACCUMULO_ZOOKEEPER_CLIENTPORT, "2181");
        config.setProperty("hadoop.socks.server", "localhost:6666");
        config.setProperty("hadoop.rpc.socket.factory.class.default", "org.apache.hadoop.net.SocksSocketFactory");
        return config;
    }

    private void createClientSideAccumuloSiteFile(ClusterSpec clusterSpec, Properties config) {
        File configDir = getConfigDir(clusterSpec);
        File accumuloSiteFile = new File(configDir, AccumuloConstants.FILE_ACCUMULO_SITE_XML);
        try {
            Files.write(generateAccumuloConfigurationFile(config), accumuloSiteFile, Charsets.UTF_8);
            LOG.info("Wrote Accumulo site file {}", accumuloSiteFile);
        } catch (IOException e) {
            LOG.error("Problem writing Accumulo site file {}", accumuloSiteFile, e);
        }
    }

    private File getConfigDir(ClusterSpec clusterSpec) {
        File configDir = new File(new File(System.getProperty("user.home")), ".whirr");
        configDir = new File(configDir, clusterSpec.getClusterName());
        configDir.mkdirs();
        return configDir;
    }

    private CharSequence generateAccumuloConfigurationFile(Properties config) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>\n");
        sb.append("<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>\n");
        sb.append("<configuration>\n");
        for (Map.Entry<Object, Object> entry : config.entrySet()) {
            sb.append("  <property>\n");
            sb.append("    <name>").append(entry.getKey()).append("</name>\n");
            sb.append("    <value>").append(entry.getValue()).append("</value>\n");
            sb.append("  </property>\n");
        }
        sb.append("</configuration>\n");
        return sb;
    }
}
