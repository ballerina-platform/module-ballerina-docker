// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/time;
import ballerina/regex;

public function main(string label, int users, string ghz_csv_path, string output_csv_path, string test_duration) returns error? {
    string[][] csv_data = check io:fileReadCsv(ghz_csv_path);

    string sample_data = regex:split(csv_data[0][15], " ")[1];
    int num_samples = check int:fromString(regex:split(sample_data, "=")[1]);
    float average = (check float:fromString(regex:split(csv_data[0][17], "=")[1]))/1000000;
    float median = (check float:fromString(regex:split(csv_data[0][21], "=")[1]))/1000000;
    float ninety_line = 0f;
    float ninety_five_line = (check float:fromString(regex:split(csv_data[0][22], "=")[1]))/1000000;
    float ninety_nine_line = 0f;
    float min = (check float:fromString(regex:split(csv_data[0][18], "=")[1]))/1000000;
    float max = (check float:fromString(regex:split(csv_data[0][19], "=")[1]))/1000000;
    float error_count = check float:fromString(regex:split(csv_data[0][14], "=")[1]);
    float error_percent = (error_count * 100.0) / <float> num_samples;
    io:println(error_count);
    io:println(error_percent);
    float throughput = check float:fromString(regex:split(csv_data[0][20], "=")[1]);

    int date = time:utcNow()[0];

    var results = [label, num_samples, average, median, ninety_line, ninety_five_line, ninety_nine_line, min,
                        max, error_percent, throughput, 0, 0, date, 0, users];

    check writeResultsToCsv(results, output_csv_path);
}

function writeResultsToCsv(any[] results, string output_path) returns error? {
    string[][] summary_data = check io:fileReadCsv(output_path);
    string[] final_results = [];
    foreach var result in results {
        final_results.push(result.toString());
    }
    summary_data.push(final_results);
    check io:fileWriteCsv(output_path, summary_data);
}
