/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.whirr.service.puppet.functions;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.apache.whirr.service.puppet.PuppetConstants.PUPPET_ROLE_PREFIX;
import static org.apache.whirr.service.puppet.predicates.PuppetPredicates.isPuppetRole;

import com.google.common.base.Function;

public enum RolesManagedByPuppet implements Function<Iterable<String>, Iterable<String>> {
  INSTANCE;
  public Iterable<String> apply(Iterable<String> roles) {
    return transform(filter(roles, isPuppetRole()), new Function<String, String>() {

      @Override
      public String apply(String arg0) {
        return arg0.replaceFirst(PUPPET_ROLE_PREFIX, "");
      }

    });
  }
}
