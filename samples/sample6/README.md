## Sample7: Multiple modules with docker annotation

- This sample contains multiple modules with docker annotation. Each module has a simple API which runs an HTTPS service in a docker container.
- Following artifacts will be generated from this sample.
    ``` 
    $> docker images
    pizza 
    burger
    
    $> tree
    ├── src
    |   └── burger
    |   └── pizza
    └── target
        └── bin
        |   └── burger.jar
        |   └── pizza.jar
        └── docker
            └── burger
            |   └── Dockerfile
            └── pizza
                └── Dockerfile
    ```
### How to run:
1. Compile all modules. Command to run docker image will be printed on success:
```bash
$> ballerina build -a
Compiling source
        foody/burger:0.0.1
        foody/pizza:0.0.1

Creating balos
        target/balo/burger-2019r3-any-0.0.1.balo
        target/balo/pizza-2019r3-any-0.0.1.balo

Running tests
        foody/burger:0.0.1
        No tests found

        foody/pizza:0.0.1
        No tests found


Generating executables
        target/bin/burger.jar
        target/bin/pizza.jar

Generating docker artifacts...
        @docker                  - complete 2/2 

        Run the following command to start a Docker container:
        docker run -d -p 9096:9096 burger:latest


Generating docker artifacts...
        @docker                  - complete 2/2 

        Run the following command to start a Docker container:
        docker run -d -p 9099:9099 pizza:latest
```

2. jar file, Dockerfile and docker image will be generated for each module: 
```bash
$> tree
├── README.md
├── Ballerina.lock
├── Ballerina.toml
├── src
|   └── burger
|   └── pizza
└──target
    └── bin
    |    └── burger.jar
    |    └── pizza.jar
    └── docker
        └── burger
            └── Dockerfile
        └── pizza
            └── Dockerfile

```
3. Verify the docker image is created:
```bash
$> docker images
REPOSITORY                                   TAG                 IMAGE ID            CREATED              SIZE
pizza                                        latest              445bd0e52a19        About a minute ago   112MB
burger                                       latest              7cf8892ad733        2 minutes ago        112MB
```

4. Run docker image as a container (Use the command printed on screen in step 1):
```bash
$> docker run -d -p 9096:9096 burger:latest
67d1a12f550bab74ff1eb02725e083db970e2d295ffb372a97ec3a43f0ba812a

$> docker run -d -p 9099:9099 pizza:latest
87de140566332bea24add6628afde54e9122cf76fcc3209040af0b44c051c795
```

5. Verify docker container is running:
```bash
$> docker ps
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                    NAMES
7dd116abbeec        pizza:latest        "/bin/sh -c 'java -j…"   5 seconds ago       Up 3 seconds        0.0.0.0:9099->9099/tcp   angry_cartwright
51b1d31feb7e        burger:latest       "/bin/sh -c 'java -j…"   58 seconds ago      Up 57 seconds       0.0.0.0:9096->9096/tcp   interesting_easley
```

6. Access the hello world service with curl command:
```bash
$>  curl -k https://localhost:9096/burger/menu
Burger menu
$>  curl -k http://localhost:9099/pizza/menu
Pizza menu
```

7. Remove docker instance and image.
```bash
$> docker rm -f 7dd116abbeec
$> docker rmi  pizza:latest

$> docker rm -f 51b1d31feb7e
$> docker rmi  burger:latest

```
