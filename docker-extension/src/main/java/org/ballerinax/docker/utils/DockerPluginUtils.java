/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinax.docker.utils;

import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.generator.DockerGenConstants;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

/**
 * Util methods used for artifact generation.
 */
public class DockerPluginUtils {

    private static final boolean debugEnabled = "true".equals(System.getenv(DockerGenConstants.ENABLE_DEBUG_LOGS));
    private static final PrintStream error = System.err;
    private static final PrintStream out = System.out;

    /**
     * Extract the ballerina file name from a given file path.
     *
     * @param uberJarFilePath balx file path.
     * @return output file name of balx
     */
    public static String extractUberJarName(String uberJarFilePath) {
        if (uberJarFilePath.contains("-executable")) {
            uberJarFilePath = uberJarFilePath.substring(uberJarFilePath.lastIndexOf(File.separator) + 1,
                    uberJarFilePath.lastIndexOf("" + "-executable"));
        }
    
        if (uberJarFilePath.contains(".jar")) {
            return uberJarFilePath.substring(uberJarFilePath.lastIndexOf(File.separator) + 1,
                    uberJarFilePath.lastIndexOf("" + ".jar"));
        }
        
        return null;
    }

    /**
     * Prints an Error message.
     *
     * @param msg message to be printed
     */
    public static void printError(String msg) {
        error.println("error [docker plugin]: " + msg);
    }

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

    /**
     * Recursively deletes a given directory or a file.
     *
     * @param pathToBeDeleted path to directory or file
     * @throws DockerPluginException if an error occurs while deleting
     */
    public static void deleteDirectory(Path pathToBeDeleted) throws DockerPluginException {
        if (!Files.exists(pathToBeDeleted)) {
            return;
        }
        try {
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new DockerPluginException("Unable to delete directory: " + pathToBeDeleted.toString(), e);
        }
    }

    /**
     * Resolve variable value from environment variable if $env{} is used. Else return the value.
     *
     * @param variable variable value
     * @return Resolved variable
     */
    public static String resolveValue(String variable) throws DockerPluginException {
        if (variable.contains("$env{")) {
            //remove white spaces
            variable = variable.replace(" ", "");
            //extract variable name
            final String envVariable = variable.substring(variable.lastIndexOf("$env{") + 5,
                    variable.lastIndexOf("}"));
            //resolve value
            String value = Optional.ofNullable(System.getenv(envVariable)).orElseThrow(
                    () -> new DockerPluginException("error resolving value: " + envVariable + " is not set in " +
                            "the environment."));
            // substitute value
            return variable.replace("$env{" + envVariable + "}", value);
        }
        return variable;
    }
}
