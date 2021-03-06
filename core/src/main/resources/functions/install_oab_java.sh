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
function install_oab_java_deb() {
  # Enable multiverse
  # TODO: check that it is not already enabled
  sed -i -e 's/universe$/universe multiverse/' /etc/apt/sources.list
  
  apt-get update
  apt-get -y install lsb-release pciutils
  DISTRO=`lsb_release -s -c`
    
  wget https://raw.github.com/flexiondotorg/oab-java6/master/oab-java.sh -O oab-java.sh
  chmod +x oab-java.sh
  ./oab-java.sh
  
  apt-get update
  
  apt-get -y --allow-unauthenticated install sun-java6-jdk
  
  echo "export JAVA_HOME=/usr/lib/jvm/java-6-sun" >> /etc/profile
  echo "export JAVA_HOME=/usr/lib/jvm/java-6-sun" >> ~root/.bashrc
  export JAVA_HOME=/usr/lib/jvm/java-6-sun
  java -version
  
}

function install_oab_java_rpm() {
  JDK_INSTALL_PATH=/usr/java
  mkdir -p $JDK_INSTALL_PATH
  cd $JDK_INSTALL_PATH
  if [ -z "${JDK_INSTALL_URL+xxx}" ]; then
	  MACHINE_TYPE=`uname -m`
	  if [ ${MACHINE_TYPE} == 'x86_64' ]; then
	    JDK_PACKAGE=jdk-6u21-linux-x64-rpm.bin
	  else
	    JDK_PACKAGE=jdk-6u21-linux-i586-rpm.bin
	  fi
	  wget http://whirr-third-party.s3.amazonaws.com/$JDK_PACKAGE
  else
    JDK_PACKAGE=$(basename $JDK_INSTALL_URL)
    wget $JDK_INSTALL_URL
  fi
  chmod +x $JDK_PACKAGE  
  mv /bin/more /bin/more.no
  yes | ./$JDK_PACKAGE -noregister
  mv /bin/more.no /bin/more
  rm -f *.rpm $JDK_PACKAGE
  
  export JAVA_HOME=$(ls -d $JDK_INSTALL_PATH/jdk*)
  echo "export JAVA_HOME=$JAVA_HOME" >> /etc/profile
  alternatives --install /usr/bin/java java $JAVA_HOME/bin/java 17000
  alternatives --set java $JAVA_HOME/bin/java
  java -version
}

function install_oab_java() {
  if which dpkg &> /dev/null; then
    install_oab_java_deb
  elif which rpm &> /dev/null; then
    install_oab_java_rpm
  fi
}
