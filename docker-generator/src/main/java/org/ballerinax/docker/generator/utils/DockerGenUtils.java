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

package org.ballerinax.docker.generator.utils;


import org.apache.commons.io.FileUtils;
import org.ballerinax.docker.generator.DockerGenConstants;
import org.ballerinax.docker.generator.exceptions.DockerGenException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    
    /**
     * Write content to a File. Create the required directories if they don't not exists.
     *
     * @param context        context of the file
     * @param targetFilePath target file path
     * @throws IOException If an error occurs when writing to a file
     */
    public static void writeToFile(String context, String targetFilePath) throws IOException {
        File newFile = new File(targetFilePath);
        if (newFile.exists() && newFile.delete()) {
            Files.write(Paths.get(targetFilePath), context.getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (newFile.getParentFile().mkdirs()) {
            Files.write(Paths.get(targetFilePath), context.getBytes(StandardCharsets.UTF_8));
            return;
        }
        Files.write(Paths.get(targetFilePath), context.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Extract the ballerina file name from a given file path.
     *
     * @param balxFilePath balx file path.
     * @return output file name of balx
     */
    public static String extractBalxName(String balxFilePath) {
        if (balxFilePath.contains(".balx")) {
            return balxFilePath.substring(balxFilePath.lastIndexOf(File.separator) + 1, balxFilePath.lastIndexOf(
                    ".balx"));
        }
        return null;
    }
    
    
    /**
     * Copy file or directory.
     *
     * @param source      source file/directory path
     * @param destination destination file/directory path
     */
    public static void copyFileOrDirectory(String source, String destination) throws DockerGenException {
        File src = new File(source);
        
        if (!src.exists()) {
            throw new DockerGenException("Error while copying file/folder '" + source + "' as it does not exist");
        }
        
        File dst = new File(destination);
        try {
            
            // if source is file
            if (Files.isRegularFile(Paths.get(source))) {
                if (Files.isDirectory(dst.toPath())) {
                    // if destination is directory
                    FileUtils.copyFileToDirectory(src, dst);
                } else {
                    // if destination is file
                    FileUtils.copyFile(src, dst);
                }
            } else if (Files.isDirectory(Paths.get(source))) {
                FileUtils.copyDirectory(src, dst);
            }
        } catch (IOException e) {
            throw new DockerGenException("Error while copying file/folder '" + source + "' to '" + destination + "'",
                    e);
        }
    }
    
    /**
     * Cleans error message getting rid of java class names.
     *
     * @param errorMessage The error message to be updated.
     * @return Cleaned error message.
     */
    public static String cleanErrorMessage(String errorMessage) {
        if (errorMessage.contains("unable to find valid certification path")) {
            errorMessage = "unable to find docker cert path.";
        } else if (errorMessage.contains("Connection refused")) {
            errorMessage = "connection refused to docker host";
        } else if (errorMessage.contains("Unable to connect to server")) {
            errorMessage = errorMessage.replace("Unable to connect to server: Timeout: GET",
                    "unable to connect to docker host: ");
        } else if (errorMessage.contains("tcp protocol is not supported")) {
            errorMessage = "unexpected error occurred internally";
        }
    
        return errorMessage;
    }
}
