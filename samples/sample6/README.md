## Sample:6: Multiple modules with docker annotation

- This sample runs ballerina project with multiple modules.

- Following artifacts will be generated from this sample.
    ``` 
    $> docker images
    pizza:latest
    docker:latest
    
    $> tree
    └── target
        ├── Ballerina.lock
        ├── burger
        │   └── Dockerfile
        ├── burger.balx
        ├── pizza
        │   └── Dockerfile
        └── pizza.balx
    ```
### How to run:
1. Initialize ballerina project.
```bash
sample6$> ballerina init
Ballerina project initialized
```

2. Compile the project directory. Command to run docker image will be printed on success:
```bash
$> ballerina build
@docker                  - complete 3/3 

Run following command to start docker container:
docker run -d -p 9096:9096 burger:latest

@docker                  - complete 3/3 

Run following command to start docker container:
docker run -d -p 9099:9099 pizza:latest
```

2. hello_config_file.balx, Dockerfile and docker image will be generated: 
```bash
$> tree
     └── target
         ├── Ballerina.lock
         ├── burger
         │   └── Dockerfile
         ├── burger.balx
         ├── pizza
         │   └── Dockerfile
         └── pizza.balx
```

3. Verify the docker image is created:
```bash
$> docker images
REPOSITORY                TAG                 IMAGE ID            CREATED              SIZE
pizza                     latest              7a2d55651ac7        47 seconds ago      120MB
burger                    latest              7a2d55651ac7        47 seconds ago      120MB

```

4. Run docker image as a container (Use the command printed on screen in step 1):
```bash
$> docker run -d -p 9096:9096 burger:latest
130ded2ae413d0c37021f2026f3a36ed92e993c39c260815e3aa5993d947dd00

$> docker run -d -p 9099:9099 pizza:latest
3bc5c7f7bcdd82a1c5b75698832bf7eb9db4449fe0d86bf2aba38b07c79a58e2
```

5. Verify docker container is running:
```bash
$> docker ps
CONTAINER ID        IMAGE                            COMMAND                  CREATED                  STATUS              PORTS                    NAMES
130ded2ae413        burger:latest                   "/bin/sh -c 'balleri…"   Less than a second ago   Up 3 seconds        0.0.0.0:9096->9096/tcp   thirsty_hopper
3bc5c7f7bcdd        pizza:latest                    "/bin/sh -c 'balleri…"   15 seconds ago           Up 14 seconds       0.0.0.0:9099->9099/tcp   hopeful_lalande

```

6. Access the hello world service with curl command:
```bash
$ curl http://localhost:9099/pizza/menu
Pizza menu 

$ curl https://localhost:9096/burger/menu -k
Burger menu 
```