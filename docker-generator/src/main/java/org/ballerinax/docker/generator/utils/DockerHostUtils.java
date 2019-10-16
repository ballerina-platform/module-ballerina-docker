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

package org.ballerinax.docker.generator.utils;

import java.util.Locale;

/**
 *
 */
public class DockerHostUtils {
    interface SystemDelegate {
        
        String getProperty(String key);
        
        String getenv(String name);
    }
    private static final SystemDelegate defaultSystemDelegate = new SystemDelegate() {
        @Override
        public String getProperty(final String key) {
            return System.getProperty(key);
        }
        
        @Override
        public String getenv(final String name) {
            return System.getenv(name);
        }
    };
    private static SystemDelegate systemDelegate = defaultSystemDelegate;
    
    private static final String DEFAULT_UNIX_ENDPOINT = "unix:///var/run/docker.sock";
    private static final String DEFAULT_WINDOWS_ENDPOINT = "npipe:////./pipe/docker_engine";
    private static final String DEFAULT_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 2375;
    
    public static String defaultDockerEndpoint() {
        final String osName = System.getProperty("os.name");
        final String os = osName.toLowerCase(Locale.ENGLISH);
        if (os.equalsIgnoreCase("linux") || os.contains("mac")) {
            return DEFAULT_UNIX_ENDPOINT;
        } else if (System.getProperty("os.name").equalsIgnoreCase("Windows 10")) {
            //from Docker doc: Windows 10 64bit: Pro, Enterprise or Education
            return DEFAULT_WINDOWS_ENDPOINT;
        } else {
            return DEFAULT_ADDRESS + ":" + defaultPort();
        }
    }
}
