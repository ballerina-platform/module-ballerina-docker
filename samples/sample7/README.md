## Sample7: Run ballerina service in http and https with docker

- This sample runs simple ballerina hello world service in a docker container in http and https.
- http port will be 9090 and https port will be 9696.
- Following artifacts will be generated from this sample.
    ``` 
    $> docker images
    hello_world_docker:latest
    
    $> tree
    ├── hello-world-docker.jar
    └── docker
        └── Dockerfile
    ```
### How to run:

1. Compile the hello-world-docker.bal file. Command to run docker image will be printed on success:
```bash
$> bal build hello_world_docker.bal 
Compiling source
        hello_world_docker.bal

Generating executables
        hello_world_docker.jar

Generating docker artifacts...
        @docker                  - complete 2/2 

        Run the following command to start a Docker container:
        docker run -d -p 9696:9696 -p 9090:9090 hello_world_docker:latest

```

2. hello_world_docker.jar, Dockerfile and docker image will be generated: 
```bash
$> tree
.
├── README.md
├── docker
│   └── Dockerfile
├── hello_world_docker.bal
└── hello_world_docker.jar
```

3. Verify the docker image is created:
```bash
$> docker images
REPOSITORY                                                       TAG                               IMAGE ID            CREATED             SIZE
hello_world_docker                                               latest                            ee71c24c4645        38 seconds ago      125MB
```

4. Run docker image as a container (Use the command printed on screen in step 1):
```bash
$> docker run -d -p 9696:9696 -p 9090:9090 hello_world_docker:latest
4c6fbbfe2dac9908561970120f3db061b462875ad14c62a215ad352aeee0de0d
```

5. Verify docker container is running:
```bash
$> docker ps
CONTAINER ID        IMAGE                        COMMAND                  CREATED              STATUS              PORTS                                            NAMES
b4a44791dbc9        hello_world_docker:latest    "/bin/sh -c 'balleri…"   About a minute ago   Up About a minute   0.0.0.0:9090->9090/tcp, 0.0.0.0:9696->9696/tcp   blissful_roentgen
```

6. Access the hello world service with curl command:
```bash
$> curl http://localhost:9090/helloWorld/sayHello
Hello, World from service helloWorld !
$> curl -k https://localhost:9696/helloWorld/sayHello
Hello, World from service helloWorld !
```

7. Remove docker instance and image.
```bash
$> docker rm -f 4c6fbbfe2dac
$> docker rmi  hello_world_docker:latest
```
