# --------------------------------------------------------------------
# Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------

FROM openjdk:8u201-jdk-nanoserver-sac2016
SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"]

LABEL maintainer="dev@ballerina.io"

ARG BALLERINA_DIST

RUN New-Item -path C:\ -name "ballerina" -type directory | Out-Null;
RUN New-Item -path C:\ -name "tmp" -type directory | Out-Null;

# copy distribution to created `tmp` folder
COPY $BALLERINA_DIST C:\\tmp

# unzip archive to `ballerina` folder
RUN Expand-Archive C:\tmp\$env:BALLERINA_DIST C:\ballerina;

# remove archive
RUN Remove-Item C:\tmp -Force -Recurse;

# rename folder to `runtime`
RUN Get-ChildItem -Path C:\ballerina\ -Recurse -Directory | \
    ForEach { \
        Rename-Item -Path $_.FullName -NewName "runtime"; \
        break; \
    };

# set BALLERINA_HOME environment variable
RUN SETX /M BALLERINA_HOME C:\ballerina\runtime;

# add ballerina batch file to path
RUN $ballerinaPath = '{0}\bin' -f $Env:BALLERINA_HOME; \
	$newPath = ('{0};{1}' -f $ballerinaPath, $Env:PATH); \
	SETX /M PATH $newPath;

# set working directory
RUN New-Item -path C:\ballerina\ -name "home" -type directory | Out-Null;
WORKDIR C:\\ballerina\\home
# VOLUME C:\\ballerina\\home
