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

package org.ballerinax.docker.generator;


import java.io.PrintStream;

/**
 * Util methods used for artifact generation.
 */
public class DockerGenUtils {

    private static final boolean debugEnabled = "true".equals(System.getProperty(DockerGenConstants.ENABLE_DEBUG_LOGS));
    private static final PrintStream out = System.out;

    /**
     * Prints a debug message.
     *
     * @param msg message to be printed
     */
    public static void printDebug(String msg) {
        if (debugEnabled) {
            out.println("debug [docker plugin]: " + msg);
        }
    }

    /**
     * Checks if a String is empty ("") or null.
     *
     * @param str the String to check, may be null
     * @return true if the String is empty or null
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str != null && (strLen = str.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }
}
