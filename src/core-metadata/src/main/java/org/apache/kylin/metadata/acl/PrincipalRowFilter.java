/*
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

package org.apache.kylin.metadata.acl;

import java.util.List;

import org.apache.kylin.guava30.shaded.common.collect.Lists;

import lombok.Getter;

public class PrincipalRowFilter {

    @Getter
    private List<RowSet> rowSets = Lists.newArrayList();

    @Getter
    private List<RowSet> likeRowSets = Lists.newArrayList();

    @Getter
    private List<AclTCR.FilterGroup> rowFilter = Lists.newArrayList();
}
