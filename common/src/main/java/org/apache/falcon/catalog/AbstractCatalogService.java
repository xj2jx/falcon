/**
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

package org.apache.falcon.catalog;

import org.apache.falcon.FalconException;

/**
 * Interface definition for a catalog registry service
 * such as Hive or HCatalog.
 * Catalog should minimally support the following operations.
 */
public abstract class AbstractCatalogService {

    /**
     * This method checks if the catalog service is alive.
     *
     * @param catalogBaseUrl url for the catalog service
     * @return if the service was reachable
     * @throws FalconException exception
     */
    public abstract boolean isAlive(String catalogBaseUrl) throws FalconException;

    public abstract boolean tableExists(String catalogUrl, String database, String tableName)
        throws FalconException;
}
