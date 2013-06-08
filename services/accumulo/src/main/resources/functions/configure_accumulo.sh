#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
function configure_accumulo() {
  local OPTIND
  local OPTARG
  
  ROLES=$1
  shift
  
  # get parameters
  MASTER_HOST=
  ZOOKEEKER_QUORUM=
  PORT=
  ACCUMULO_TAR_URL=
  while getopts "m:q:p:u:" OPTION; do
    case $OPTION in
    m)
      MASTER_HOST="$OPTARG"
      ;;
    q)
      ZOOKEEPER_QUORUM="$OPTARG"
      ;;
    p)
      PORT="$OPTARG"
      ;;
    u)
      ACCUMULO_TAR_URL="$OPTARG"
      ;;
    esac
  done
  
 # assign default URL if no other given (optional)
  ACCUMULO_TAR_URL=${ACCUMULO_TAR_URL:-http://archive.apache.org/dist/accumulo/1.4.2/accumulo-1.4.2-dist.tar.gz}
  # derive details from the URL
  ACCUMULO_TAR_FILE=${ACCUMULO_TAR_URL##*/}
  ACCUMULO_TAR_MD5_FILE=$ACCUMULO_TAR_FILE.md5
  # extract "version" or the name of the directory contained in the tarball,
  # but since accumulo has used different namings use the directory instead.
  ACCUMULO_VERSION=${ACCUMULO_TAR_URL%/*.tar.gz}
  ACCUMULO_VERSION=${ACCUMULO_VERSION##*/}
  # simple check that we have a proper URL or default to use filename
  if [[ "${ACCUMULO_VERSION:0:8}" != "accumulo" ]]; then
    ACCUMULO_VERSION=${ACCUMULO_TAR_FILE%.tar.gz}
  fi
  ACCUMULO_HOME=/usr/local/$ACCUMULO_VERSION
  ACCUMULO_CONF_DIR=$ACCUMULO_HOME/conf

  case $CLOUD_PROVIDER in
  ec2 | aws-ec2 )
    MOUNT=/mnt
    ;;
  *)
    MOUNT=/data
    ;;
  esac

  mkdir -p $MOUNT/accumulo
  chown hadoop:hadoop $MOUNT/accumulo
  if [ ! -e $MOUNT/tmp ]; then
    mkdir $MOUNT/tmp
    chmod a+rwxt $MOUNT/tmp
  fi
  mkdir /etc/accumulo
  ln -s $ACCUMULO_CONF_DIR /etc/accumulo/conf

  # Copy generated configuration files in place
  cp /tmp/accumulo-site.xml $ACCUMULO_CONF_DIR
  cp /tmp/accumulo-env.sh $ACCUMULO_CONF_DIR
  cp /tmp/accumulo-hadoop-metrics.properties $ACCUMULO_CONF_DIR/hadoop-metrics.properties

  # ACCUMULO_PID_DIR should exist and be owned by hadoop:hadoop
  mkdir -p /var/run/accumulo
  chown -R hadoop:hadoop /var/run/accumulo
  
  # Create the actual log dir
  mkdir -p $MOUNT/accumulo/logs
  chown -R hadoop:hadoop $MOUNT/accumulo/logs

  # Create a symlink at $ACCUMULO_LOG_DIR
  ACCUMULO_LOG_DIR=$(. $ACCUMULO_CONF_DIR/accumulo-env.sh; echo $ACCUMULO_LOG_DIR)
  ACCUMULO_LOG_DIR=${ACCUMULO_LOG_DIR:-/var/log/accumulo/logs}
  rm -rf $ACCUMULO_LOG_DIR
  mkdir -p $(dirname $ACCUMULO_LOG_DIR)
  ln -s $MOUNT/accumulo/logs $ACCUMULO_LOG_DIR
  chown -R hadoop:hadoop $ACCUMULO_LOG_DIR

  start_accumulo
}

function start_accumulo() {
  if which dpkg &> /dev/null; then
    AS_HADOOP="su -s /bin/bash - hadoop -c"
  elif which rpm &> /dev/null; then
    AS_HADOOP="/sbin/runuser -s /bin/bash - hadoop -c"
  fi
  $AS_HADOOP "$ACCUMULO_HOME/bin/start-here.sh"
}

