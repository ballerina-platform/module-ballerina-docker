## Sample:5: Add files to Docker image

- This sample runs simple ballerina hello world service in a docker container with two files.

- Following artifacts will be generated from this sample.
    ``` 
    $> docker images
    hello_config_file:latest
    
    $> tree
    .
    ├── docker
    │   ├── Dockerfile
    │   ├── data.txt
    │   └── Config.toml
    ├── hello_config_file.bal
    └── hello_config_file.jar
    ```
### How to run:

1. Compile the  hello_config_file.bal file. Command to run docker image will be printed on success:
```bash
$> bal build hello_config_file.bal
Compiling source
        hello_config_file.bal

Generating executables
        hello_config_file.jar

Generating docker artifacts...
        @docker                  - complete 2/2 

        Run the following command to start a Docker container:
        docker run -d -p 9090:9090 hello_config_file:latest
```

2. hello_config_file.jar, Dockerfile and docker image will be generated: 
```bash
$> tree
.
├── README.md
├── conf
│   ├── data.txt
│   └── Config.toml
├── docker
│   ├── Dockerfile
│   ├── data.txt
│   └── Config.toml
├── hello_config_file.bal
└── hello_config_file.jar
```

3. Verify the docker image is created:
```bash
$> docker images
REPOSITORY                 TAG                 IMAGE ID            CREATED              SIZE
hello_config_file          latest              7a2d55651ac7        47 seconds ago      120MB

```

4. Run docker image as a container (Use the command printed on screen in step 1):
```bash
$> docker run -d -p 9090:9090 hello_config_file:latest
130ded2ae413d0c37021f2026f3a36ed92e993c39c260815e3aa5993d947dd00
```

5. Verify docker container is running:
```bash
$> docker ps
CONTAINER ID        IMAGE                            COMMAND                  CREATED                  STATUS              PORTS                    NAMES
130ded2ae413        hello_config_file:latest      "/bin/sh -c 'balleri…"   Less than a second ago   Up 3 seconds        0.0.0.0:9090->9090/tcp   thirsty_hopper
```

6. Access the hello world service with curl command:
```bash
$> curl http://localhost:9090/helloWorld/config
   Configuration: john@ballerina.com,jane@ballerian.com apim,esb
$> curl http://localhost:9090/helloWorld/data
   Data: Lorem ipsum dolor sit amet.
```

7. Remove docker instance and image.
```bash
$> docker rm -f 130ded2ae413
$> docker rmi hello_config_file:latest
```