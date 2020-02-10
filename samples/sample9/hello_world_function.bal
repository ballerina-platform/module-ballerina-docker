import ballerina/docker;
import ballerina/io;

@docker:Config {}
public function main() {
    io:println("Hello, World!");
}
