/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinax.docker.test.utils;

/**
 * Represents an java process output.
 */
public class ProcessOutput {
    private int exitCode;
    private String stdOutput;
    private String errOutput;
    
    public int getExitCode() {
        return exitCode;
    }
    
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
    
    public String getStdOutput() {
        return stdOutput;
    }
    
    public void setStdOutput(String stdOutput) {
        this.stdOutput = stdOutput;
    }
    
    public String getErrOutput() {
        return errOutput;
    }
    
    public void setErrOutput(String errOutput) {
        this.errOutput = errOutput;
    }
    
    @Override
    public String toString() {
        return "ProcessOutput{" +
               "exitCode=" + exitCode +
               ", stdOutput='" + stdOutput + '\'' +
               ", errOutput='" + errOutput + '\'' +
               '}';
    }
}
