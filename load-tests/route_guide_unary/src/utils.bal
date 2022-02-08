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
import ballerina/lang.'float;

type FeatureArray Feature[];

final readonly & Feature[] FEATURES = check populateFeatures();
configurable string featuresFilePath = "./resources/route_guide_db.json";
map<RouteNote[]> ROUTE_NOTES_MAP = {};

function keyFromPoint(Point p) returns string {
    return string `${p.latitude} ${p.longitude}`;
}

isolated function toRadians(float f) returns float {
    return f * 0.017453292519943295;
}

function calculateDistance(Point p1, Point p2) returns int {
    float cordFactor = 10000000; // 1x(10^7) OR 1e7
    float R = 6371000; // Earth radius in metres
    float lat1 = toRadians(<float>p1.latitude / cordFactor);
    float lat2 = toRadians(<float>p2.latitude / cordFactor);
    float lng1 = toRadians(<float>p1.longitude / cordFactor);
    float lng2 = toRadians(<float>p2.longitude / cordFactor);
    float dlat = lat2 - lat1;
    float dlng = lng2 - lng1;

    float a = 'float:sin(dlat / 2.0) * 'float:sin(dlat / 2.0) + 'float:cos(lat1) * 'float:cos(lat2) * 'float:sin(dlng / 2.0) * 'float:sin(dlng / 2.0);
    float c = 2.0 * 'float:atan2('float:sqrt(a), 'float:sqrt(1.0 - a));
    float distance = R * c;
    return <int>distance;
}

isolated function pointExistsInFeatures(Feature[] features, Point point) returns boolean {
    foreach Feature feature in features {
        if feature.name != "" && feature.location == point {
            return true;
        }
    }
    return false;
}

isolated function populateFeatures() returns readonly & Feature[]|error {
    json locationsJson = check io:fileReadJson(featuresFilePath);
    Feature[] features = check locationsJson.cloneWithType();
    return features.cloneReadOnly();
}
