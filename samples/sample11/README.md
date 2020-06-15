## Sample1: Docker Hello World Thin Jar

- This sample runs simple ballerina hello world service in a docker container with generated thin jar.
- The service is annotated with @docker:Config{} and disabled the `uberJar` option. 
- Following files will be generated from this sample.
    ``` 
    $> docker image
    hello_world_docker:latest
    
    $> tree
    ├── hello_world_docker.jar
    └── docker/
        ├── Dockerfile
        ├── ballerina-auth-1.0.0-java.jar
        ├── ballerina-auth-1.0.0.jar
        ├── ballerina-cache-2.0.0.jar
        ├── ballerina-config-1.0.0-java.jar
        ├── ballerina-config-1.0.0.jar
        ├── ballerina-crypto-1.0.0-java.jar
        ├── ballerina-crypto-1.0.0.jar
        ├── ballerina-docker-1.0.0.jar
        ├── ballerina-http-1.0.0-java.jar
        ├── ballerina-http-1.0.0.jar
        ├── ballerina-io-0.5.0-java.jar
        ├── ballerina-io-0.5.0.jar
        ├── ballerina-java-0.9.0-java.jar
        ├── ballerina-java-0.9.0.jar
        ├── ballerina-lang.__internal-0.1.0.jar
        ├── ballerina-lang.array-1.1.0.jar
        ├── ballerina-lang.float-1.0.0.jar
        ├── ballerina-lang.int-1.1.0.jar
        ├── ballerina-lang.object-1.0.0.jar
        ├── ballerina-lang.string-1.1.0.jar
        ├── ballerina-log-1.0.0-java.jar
        ├── ballerina-log-1.0.0.jar
        ├── ballerina-math-1.0.0-java.jar
        ├── ballerina-math-1.0.0.jar
        ├── ballerina-mime-1.0.0-java.jar
        ├── ballerina-mime-1.0.0.jar
        ├── ballerina-reflect-0.5.0-java.jar
        ├── ballerina-reflect-0.5.0.jar
        ├── ballerina-runtime-0.5.0-java.jar
        ├── ballerina-runtime-0.5.0.jar
        ├── ballerina-stringutils-0.5.0.jar
        ├── ballerina-system-0.6.0-java.jar
        ├── ballerina-system-0.6.0.jar
        ├── ballerina-task-1.1.0-java.jar
        ├── ballerina-task-1.1.0.jar
        ├── ballerina-time-1.0.0-java.jar
        ├── ballerina-time-1.0.0.jar
        ├── bcpkix-jdk15on-1.61.jar
        ├── bcprov-jdk15on-1.61.jar
        ├── commons-pool-1.5.6.wso2v1.jar
        ├── hello_world_docker.jar
        ├── mimepull-1.9.7.jar
        ├── netty-buffer-4.1.39.Final.jar
        ├── netty-codec-4.1.39.Final.jar
        ├── netty-codec-http-4.1.39.Final.jar
        ├── netty-codec-http2-4.1.39.Final.jar
        ├── netty-common-4.1.39.Final.jar
        ├── netty-handler-4.1.39.Final.jar
        ├── netty-handler-proxy-4.1.39.Final.jar
        ├── netty-resolver-4.1.39.Final.jar
        ├── netty-tcnative-boringssl-static-2.0.25.Final.jar
        ├── netty-transport-4.1.39.Final.jar
        ├── org.wso2.carbon.messaging-2.3.7.jar
        ├── org.wso2.transport.http.netty-6.3.8.jar
        ├── quartz-2.3.0.jar
        └── snakeyaml-1.16.0.wso2v1.jar
    ```
### How to run:

1. Compile the  hello_world_docker.bal file. Command to run docker image will be printed on success:
```bash
$> ballerina build hello_world_docker.bal
Compiling source
        hello_world_docker.bal

Generating executables
        hello_world_docker.jar

Generating docker artifacts...
        @docker                  - complete 2/2 

        Run the following command to start a Docker container:
        docker run -d -p 9090:9090 hello_world_docker:latest
```

2. hello_world_docker.jar, Dockerfile and docker image will be generated: 
```bash
$> tree
.
├── README.md
├── hello_world_docker.bal
├── hello_world_docker.jar
└── docker
    └── Dockerfile
```

3. Verify the docker image is created:
```bash
$> docker images
REPOSITORY             TAG                 IMAGE ID            CREATED             SIZE
hello_world_docker    latest              df83ae43f69b        2 minutes ago        102MB

```

4. Run docker image as a container (Use the command printed on screen in step 1):
```bash
$> docker run -d -p 9090:9090 hello_world_docker:latest
68eb4160ac769f131ebd3ed59f8ee0f6fe6a2e1924e290b04a4cd7513e9b71d1
```

5. Verify docker container is running:
```bash
$> docker ps
CONTAINER ID        IMAGE                       COMMAND                  CREATED              STATUS              PORTS                    NAMES
68eb4160ac76        hello_world_docker:latest   "java -cp …"        About a minute ago   Up About a minute   0.0.0.0:9090->9090/tcp   vigilant_swartz
```

6. Access the hello world service with curl command:
```bash
$> curl http://localhost:9090/helloWorld/sayHello
Hello, World from service helloWorld !
```

7. Remove docker instance and image.
```bash
$> docker rm -f 68eb4160ac76
$> docker rmi hello_world_docker:latest
```