/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.docker.models;

import org.wso2.ballerinalang.compiler.util.CompilerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Docker context holder class.
 */
public class DockerContext {
    private static DockerContext instance;
    private final Map<String, DockerDataHolder> dockerContext;
    private String currentPackage;
    private CompilerContext compilerContext;

    private DockerContext() {
        dockerContext = new HashMap<>();
    }

    public static DockerContext getInstance() {
        synchronized (DockerDataHolder.class) {
            if (instance == null) {
                instance = new DockerContext();
            }
        }
        return instance;
    }

    public void addDataHolder(String packageID) {
        this.setCurrentPackage(packageID);
        this.dockerContext.put(packageID, new DockerDataHolder());
    }

    public void setCurrentPackage(String packageID) {
        this.currentPackage = packageID;
    }

    public DockerDataHolder getDataHolder() {
        return this.dockerContext.get(this.currentPackage);
    }

    public DockerDataHolder getDataHolder(String packageID) {
        return this.dockerContext.get(packageID);
    }

    public CompilerContext getCompilerContext() {
        return compilerContext;
    }

    public void setCompilerContext(CompilerContext compilerContext) {
        this.compilerContext = compilerContext;
    }

    public String getCurrentPackage() {
        return currentPackage;
    }
}
