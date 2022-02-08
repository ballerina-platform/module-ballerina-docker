#!/bin/bash -e
# Copyright 2021 WSO2 Inc. (http://wso2.org)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ----------------------------------------------------------------------------
# Post run script for ballerina performance tests
# ----------------------------------------------------------------------------
set -e

export scriptsDir="/home/bal-admin/module-ballerina-grpc/load-tests/route_guide_unary/scripts"
export resultsDir="/home/bal-admin/module-ballerina-grpc/load-tests/route_guide_unary/results"

# Using the ballerina zip version for testing. Once finalized, can use a docker image with process_csv_output util
echo "----------Downloading Ballerina----------"
wget https://dist.ballerina.io/downloads/swan-lake-beta3/ballerina-linux-installer-x64-swan-lake-beta3.deb

echo "----------Setting Up Ballerina----------"
sudo dpkg -i ballerina-linux-installer-x64-swan-lake-beta3.deb

echo "----------Finalizing results----------"
bal run $scriptsDir/process_csv_output/ -- "gRPC Route Guide Unary" 10 "$scriptsDir/ghz_output.csv" "$resultsDir/summary.csv" "1800"
