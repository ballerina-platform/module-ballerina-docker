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

import org.ballerinax.docker.DockerGenConstants;

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
    private static final PrintStream error = System.err;
    private static final PrintStream out = System.out;

    /**
     * Extract the ballerina file name from a given file path
     *
     * @param balxFilePath balx file path.
     * @return output file name of balx
     */
    public static String extractBalxName(String balxFilePath) {
        return balxFilePath.substring(balxFilePath.lastIndexOf(File.separator) + 1, balxFilePath.lastIndexOf("" +
                ".balx"));
    }

    /**
     * Prints an Error message.
     *
     * @param msg message to be printed
     */
    public static void printError(String msg) {
        String ansiReset = "\u001B[0m";
        String ansiRed = "\u001B[31m";
        error.println(ansiRed + "error [docker plugin]: " + msg + ansiReset);
    }

    /**
     * Prints a debug message.
     *
     * @param msg message to be printed
     */
    public static void printDebug(String msg) {
        String ansiReset = "\u001B[0m";
        String ansiBlue = "\u001B[34m";
        if (debugEnabled) {
            out.println(ansiBlue + "debug: " + msg + ansiReset);
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

}
