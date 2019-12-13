## Sample4: Remote debug ballerina service with docker

- This sample runs simple ballerina hello world service in a docker container in debug mode.
- Debug port is default set as 5005. This can be configured with "debugPort: 5005" attribute.
- Following artifacts will be generated from this sample.
    ``` 
    $> docker images
    helloworld-debug:latest
    
    $> tree
    ├── hello-world-docker.jar
    └── docker
        └── Dockerfile
    ```
### How to run:

1. Compile the docker_debug.bal  file. Command to run docker image will be printed on success:
```bash
$> ballerina build docker_debug.bal 
Compiling source
        docker_debug.bal

Generating executables
        docker_debug.jar

Generating docker artifacts...
        @docker                  - complete 2/2 

        Run the following command to start a Docker container:
        docker run -d -p 9090:9090 -p 5005:5005 helloworld-debug:latest

```
**_Note that the debug port(5005) is also exposed_**

2. docker_debug.jar, Dockerfile and docker image will be generated: 
```bash
$> tree
.
├── README.md
├── docker_debug.bal
├── docker_debug.jar
└── docker
    └── Dockerfile
```

3. Verify the docker image is created:
```bash
$> docker images
REPOSITORY            TAG                 IMAGE ID            CREATED             SIZE
helloworld-debug     latest              fb9a455e4686        2 minutes ago       102MB
```

4. Run docker image as a container (Use the command printed on screen in step 1):
```bash
$> docker run -d -p 9090:9090 -p 5005:5005 helloworld-debug:latest
4c6fbbfe2dac9908561970120f3db061b462875ad14c62a215ad352aeee0de0d
```

5. Verify docker container is running:
```bash
$> docker ps
CONTAINER ID        IMAGE                     COMMAND                  CREATED             STATUS              PORTS                                            NAMES
4c6fbbfe2dac        helloworld-debug:latest   "/bin/sh -c 'balleri…"   1 second ago        Up 8 seconds        0.0.0.0:5005->5005/tcp, 0.0.0.0:9090->9090/tcp   condescending_lumiere
```

6. Check the docker logs to verify docker image is in debug mode.
```bash
$> docker logs -f 4c6fbbfe2dac
ballerina: deploying service(s) in 'docker_debug.jar'
Ballerina remote debugger is activated on port : 5005
```

7. Connect to the docker instance from IntelliJ IDEA with ballerina remote debugger.
```bash
ip: localhost
port: 5005
```

8. Remove docker instance and image.
```bash
$> docker rm -f 4c6fbbfe2dac
$> docker rmi helloworld-debug:latest
```
