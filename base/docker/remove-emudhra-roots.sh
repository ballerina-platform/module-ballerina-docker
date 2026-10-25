#!/bin/sh
# Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
# Licensed under the Apache License, Version 2.0.
# https://www.apache.org/licenses/LICENSE-2.0

# Follow https://ops.java/emudhra. Match root subjects, not vendor-specific aliases.
# Removing trust anchors also rejects alternate encodings of the intermediates.
set -eu
truststore="${1:-${JAVA_HOME}/lib/security/cacerts}"
listing=$(mktemp)
aliases=$(mktemp)
trap 'rm -f "$listing" "$aliases"' EXIT HUP INT TERM

find_roots() {
    "${JAVA_HOME}/bin/keytool" -J-Duser.language=en -J-Duser.country=US \
        -list -v -keystore "$truststore" -storepass changeit > "$listing"
    awk '
        /^Alias name: / { alias = substr($0, 13) }
        /^Owner: / {
            subject = substr($0, 8)
            if (subject ~ /(^|, )CN=(emSign Root CA - G1|emSign Root CA - G2|emSign ECC Root CA - G3|AAA Certificate Services)(,|$)/) {
                print alias
            }
        }
    ' "$listing" > "$aliases"
}

find_roots
while IFS= read -r alias; do
    "${JAVA_HOME}/bin/keytool" -delete -keystore "$truststore" \
        -storepass changeit -noprompt -alias "$alias"
done < "$aliases"
find_roots
if [ -s "$aliases" ]; then
    echo 'Affected eMudhra trust anchors remain in the Java truststore' >&2
    exit 1
fi
