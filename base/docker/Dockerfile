# --------------------------------------------------------------------
# Copyright (c) 2018, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

FROM alpine:3.22.6

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

LABEL maintainer="dev@ballerina.io"

# Ballerina runtime distribution filename.
ARG BALLERINA_DIST

# Add Ballerina runtime.
COPY ${BALLERINA_DIST} /root/

# Create folders, unzip distribution, create users, & set permissions.
RUN mkdir -p /ballerina/files \
    && addgroup troupe \
    && adduser -S -s /bin/bash -g 'ballerina' -G troupe -D ballerina \
    && apk add --no-cache --upgrade apk-tools \
    && apk upgrade --no-cache \
    && apk add --no-cache bash docker-cli gcompat libgcc \
        "libcrypto3>=3.5.9-r0" "libssl3>=3.5.9-r0"

RUN set -eux; \
    ARCH="$(apk --print-arch)"; \
    case "${ARCH}" in \
       amd64|x86_64) \
         ESUM='dbd75c6332ad995d20b6e25624b84652ef332d6babee4e9ddac3f36ee38d57be'; \
         BINARY_URL='https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.5.1%2B2/OpenJDK25U-jdk_x64_alpine-linux_hotspot_25.0.5.1_2.tar.gz'; \
         ;; \
       aarch64) \
         ESUM='0501ef55ea9ec19b14987def7a73e044cd1748f7f3b97237b49007d387a84a1a'; \
         BINARY_URL='https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.5.1%2B2/OpenJDK25U-jdk_aarch64_alpine-linux_hotspot_25.0.5.1_2.tar.gz'; \
        ;;\
      *) \
         echo "Unsupported arch: ${ARCH}"; \
         exit 1; \
      ;; \
    esac; \
	wget -O /tmp/openjdk.tar.gz ${BINARY_URL}; \
	echo "${ESUM} */tmp/openjdk.tar.gz" | sha256sum -c -; \
	mkdir -p /opt/java/openjdk; \
	tar --extract \
	    --file /tmp/openjdk.tar.gz \
	    --directory /opt/java/openjdk \
	    --strip-components 1 \
	    --no-same-owner \
	  ; \
    rm -rf /tmp/openjdk.tar.gz;

ENV LD_PRELOAD=/lib/libgcompat.so.0

ENV JAVA_HOME=/opt/java/openjdk \
    PATH="/opt/java/openjdk/bin:$PATH"

RUN unzip /root/${BALLERINA_DIST} -d /ballerina/ > /dev/null 2>&1 \
    && mv /ballerina/ballerina* /ballerina/runtime \
    && mkdir -p /ballerina/runtime/logs \
    && chown -R ballerina:troupe /ballerina \
    && rm -rf /root/${BALLERINA_DIST} > /dev/null 2>&1 \
    && rm -rf /var/cache/apk/*

ENV BALLERINA_HOME /ballerina/runtime
ENV PATH $BALLERINA_HOME/bin:$PATH

WORKDIR /home/ballerina
VOLUME /home/ballerina

USER ballerina
