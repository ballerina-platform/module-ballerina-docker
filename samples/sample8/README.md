## Sample8: Override CMD of the generated Dockerfile

- The CMD of the generated Dockerfile can be overridden using the `cmd` field of the @docker:Config{} annotation. 
- In this sample we are enabling http trace logs by overriding the `CMD` value of the generated Dockerfile. 
- The `cmd` field will be as `CMD java -jar ${APP} --b7a.http.accesslog.console=true` in the @docker:Config{} annotation.
- Following files will be generated from this sample.
    ``` 
    $> docker image
    hello_world_cmd_docker:latest
    
    $> tree
    ├── hello_world_cmd_docker.jar
    └── docker
        └── Dockerfile
    ```
### How to run:

1. Compile the  hello_world_cmd_docker.bal file. Command to run docker image will be printed on success:
```bash
$> ballerina build hello_world_cmd_docker.bal
Compiling source
	hello_world_cmd_docker.bal

Generating executables
	hello_world_cmd_docker.jar

Generating docker artifacts...
	@docker 		 - complete 2/2

	Run the following command to start a Docker container:
	docker run -d -p 9090:9090 hello_world_cmd_docker:latest
```

2. hello_world_cmd_docker.jar, Dockerfile and docker image will be generated: 
```bash
$> tree
.
├── README.md
├── docker
│   └── Dockerfile
├── hello_world_cmd_docker.bal
└── hello_world_cmd_docker.jar
```

3. Verify the docker image is created:
```bash
$> docker images
REPOSITORY                  TAG                 IMAGE ID            CREATED             SIZE
hello_world_cmd_docker    latest              df83ae43f69b        2 minutes ago        102MB

```

4. Run docker image as a container (Use the command printed on screen in step 1):
```bash
$> docker run -d -p 9090:9090 hello_world_cmd_docker:latest
68eb4160ac769f131ebd3ed59f8ee0f6fe6a2e1924e290b04a4cd7513e9b71d1
```

5. Verify docker container is running:
```bash
$> docker ps
CONTAINER ID        IMAGE                           COMMAND                  CREATED              STATUS              PORTS                    NAMES
68eb4160ac76        hello_world_cmd_docker:latest   "/bin/sh -c 'balleri…"   About a minute ago   Up About a minute   0.0.0.0:9090->9090/tcp   vigilant_swartz

```

6. Check logs to see if http trace logs are enabled:
```bash
$> docker logs 68eb4160ac769f131ebd3ed59f8ee0f6fe6a2e1924e290b04a4cd7513e9b71d1
ballerina: HTTP access log enabled
[ballerina/http] started HTTP/WS listener 0.0.0.0:9090
```

7. Access the hello world service with curl command:
```bash
$> curl http://localhost:9090/helloWorld/sayHello
Hello, World from service helloWorld !
```

8. Remove docker instance and image.
```bash
$> docker rm -f 68eb4160ac76
$> docker rmi hello_world_cmd_docker:latest
```