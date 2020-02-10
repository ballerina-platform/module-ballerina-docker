## Docker Annotation examples

### Prerequisites: 

- [Docker](https://docs.docker.com/install/) is installed and configured.
- [Ballerina Docker Extension](README.md) is configured.
- [Base docker image](../base/README.md) is built or pulled. 
- Mini-kube users should configure following annotations in every sample with valid values: 
```ballerina
@docker:Config {
    dockerHost: "tcp://192.168.99.100:2376", 
    dockerCertPath: "/Users/anuruddha/.minikube/certs"
}  
```
- If DOCKER_HOST and DOCKER_CERT_PATH are exported as environment variables, priority will be given to environment variables.
- Docker for windows users should enable remote access to the API.
 ![alt tag](./images/docker_for_windows.png)
### Try docker annotation examples:

1. [Example1: Docker Hello World](sample1/)
1. [Example2: Change docker image's tag and registry with annotations](sample2/)
1. [Example3: Push docker image to docker registry](sample3/)
1. [Example4: Remote debug ballerina service with docker](sample4/)
1. [Example5: Copy files to Docker image](sample5/)
1. [Example6: Multiple modules with docker annotation](sample6/)
1. [Example7: Run ballerina service in http and https with docker](sample7/)
1. [Example8: Override CMD of the generated Dockerfile](sample8/)
1. [Example9: Docker Hello World Function](sample9/)