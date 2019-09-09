#!/bin/bash
# ---------------------------------------------------------------------------
#  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

docker_sample_dir=$(pwd)
#export DOCKER_USERNAME=<user_name>
#export DOCKER_PASSWORD=<password>

for number in {1..5}
do
	echo "======================== Testing sample-$number ========================"
	pushd "$docker_sample_dir"/sample"$number"
	if [[ number -eq 1 ]]; then
		ballerina build hello_world_docker.bal
		dockerId=$(docker run -d -p 9090:9090 hello_world_docker:latest)
		sleep 2
		curl http://localhost:9090/helloWorld/sayHello
		docker kill $dockerId
	fi

	if [[ number -eq 2 ]]; then
		ballerina build hello_world_docker.bal
		dockerId=$(docker run -d -p 9090:9090 docker.abc.com/helloworld:v1.0)
		sleep 2
		curl https://localhost:9090/helloWorld/sayHello -k
		docker kill $dockerId
	fi

	if [[ number -eq 3 ]]; then
	  export DOCKER_USERNAME=ballerina
    export DOCKER_PASSWORD=ballerina
		ballerina build docker_push_sample.bal
		dockerId=$(docker run -d -p 9090:9090 index.docker.io/$DOCKER_USERNAME/helloworld-push:v2.0.0)
		sleep 2
		curl http://localhost:9090/HelloWorld/sayHello
		docker kill $dockerId
	fi

	if [[ number -eq 4 ]]; then
		ballerina build docker_debug.bal
		dockerId=$(docker run -d -p 9090:9090 -p 5005:5005 helloworld-debug:latest)
		sleep 2
		docker logs $dockerId
		docker kill $dockerId
	fi

	if [[ number -eq 5 ]]; then
		ballerina build hello_config_file.bal
		dockerId=$(docker run -d -p 9090:9090 hello_config_file:latest)
		sleep 2
		curl http://localhost:9090/helloWorld/config/john
		curl http://localhost:9090/helloWorld/config/jane
		curl http://localhost:9090/helloWorld/data
		docker kill $dockerId
	fi
	echo "======================== End of sample-$number ========================"
	popd
done