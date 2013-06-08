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

public class AccumuloConstants {

    public static final String KEY_TARBALL_URL = "whirr.accumulo.tarball.url";
    public static final String FUNCTION_INSTALL = "install_accumulo";
    public static final String FUNCTION_CONFIGURE = "configure_accumulo";
    public static final String PROP_ACCUMULO_ZOOKEEPER_QUORUM = "accumulo.zookeeper.quorum";
    public static final String PROP_ACCUMULO_ZOOKEEPER_CLIENTPORT = "accumulo.zookeeper.property.clientPort";
    public static final String FILE_ACCUMULO_SITE_XML = "accumulo-site.xml";
    public static final String FILE_ACCUMULO_DEFAULT_PROPERTIES = "whirr-accumulo-default.properties";
    //Install and configuration script params
    public static final String PARAM_TARBALL_URL = "-u";
    public static final String PARAM_QUORUM = "-q";
    public static final String PARAM_MASTER = "-m";

    private AccumuloConstants() {
    }
}