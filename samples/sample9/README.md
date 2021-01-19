## Sample9: Docker Hello World Function

- This sample runs simple ballerina main function in a docker container. 
- The service is annotated with @docker:Config{} without passing any parameters. 
- Default values for docker annotation attributes will be used to create docker artifacts.
- Following files will be generated from this sample.
    ``` 
    $> docker image
    hello_world_function:latest
    
    $> tree
    ├── hello_world_function.jar
    └── docker
        └── Dockerfile
    ```
### How to run:

1. Compile the  hello_world_function.bal file. Command to run docker image will be printed on success:
```bash
$> bal build hello_world_function.bal
Compiling source
        hello_world_function.bal

Generating executables
        hello_world_function.jar

Generating docker artifacts...
        @docker                  - complete 2/2 

        Run the following command to start a Docker container:
        docker run -d hello_world_function:latest
```

2. hello_world_function.jar, Dockerfile and docker image will be generated: 
```bash
$> tree
.
├── README.md
├── hello_world_function.bal
├── hello_world_function.jar
└── docker
    └── Dockerfile
```

3. Verify the docker image is created:
```bash
$> docker images
REPOSITORY             TAG                 IMAGE ID            CREATED             SIZE
hello_world_function    latest             df83ae43f69b        2 minutes ago        102MB

```

4. Run docker image as a container (Use the command printed on screen in step 1):
```bash
$> docker run -d hello_world_function:latest
68eb4160ac769f131ebd3ed59f8ee0f6fe6a2e1924e290b04a4cd7513e9b71d1
```

5. Verify docker container is running:
```bash
$> docker logs 68eb4160ac769f131ebd3ed59f8ee0f6fe6a2e1924e290b04a4cd7513e9b71d1
Hello, World!
```


7. Remove docker instance and image.
```bash
$> docker rm -f 68eb4160ac76
$> docker rmi hello_world_function:latest
```