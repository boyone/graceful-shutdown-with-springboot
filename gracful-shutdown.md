# Graceful Shutdown

## Prerequisite

1. [k3d](https://k3d.io/v5.6.3/)
2. [hey](https://github.com/rakyll/hey)
3. [docker](https://www.docker.com/)

## Getting Start

1. Build image

   ```sh
   cd greeting-service
   ./gradlew bootBuildImage
   docker image ls
   ```

### Hello Kubernetes with `k3d`

1. Create Cluster

   ```sh
   k3d cluster create default -p "8080:8080@loadbalancer" --servers 1 --agents 3
   k3d image import greeting-service:0.0.1-SNAPSHOT --cluster default
   kubectl get nodes
   ```

   - Delete Cluster `k3d cluster delete default`

2. Create and Change Working Directory

   - Create directory call `k8s`
   - Change directory to `k8s`

     ```sh
     cd k8s
     ```

3. Create Deployment call deployment.yaml

   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     labels:
       app: greeting-service
     name: greeting-service
   spec:
     replicas: 2
     selector:
       matchLabels:
         app: greeting-service
     template:
       metadata:
         labels:
           app: greeting-service
       spec:
         containers:
           - image: greeting-service:0.0.1-SNAPSHOT
             imagePullPolicy: IfNotPresent
             name: greeting-service
   ```

4. Create Service service.yaml

   ```yaml
   apiVersion: v1
   kind: Service
   metadata:
     labels:
       app: greeting-service
     name: greeting-service
   spec:
     ports:
       - port: 8080
       protocol: TCP
       targetPort: 8080
     selector:
       app: greeting-service
     type: LoadBalancer
   ```

5. Create Deployment with `apply`

   ```sh
   kubectl apply -f deployment.yaml
   kubectl get deployments
   kubectl get pods
   ```

6. Create Service with `apply`

   ```sh
   kubectl apply -f service.yaml
   kubectl get service
   ```

7. Test with curl

   ```sh
   curl http://localhost:8080/
   ```

8. Test with `hey`

   ```sh
   hey -c 20 -z 10s http://localhost:8080
   ```

---

## Test Scaling

1. Change `replecas` from 2 to 3 at `deployment.yaml`

   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     labels:
       app: greeting-service
     name: greeting-service
   spec:
     replicas: 3 # change from 2 to 3
     selector:
       matchLabels:
         app: greeting-service
     template:
       metadata:
         labels:
           app: greeting-service
       spec:
         containers:
           - image: greeting-service:0.0.1-SNAPSHOT
             imagePullPolicy: IfNotPresent
             name: greeting-service
   ```

2. Proof Scaling

   ![run test](./images/run-test-with-hey.png)

   1. Open new terminal and type following command[`test terminal`]

      ```sh
      hey -c 20 -z 10s http://localhost:8080
      ```

   2. Switch to previous terminal then type following command[`apply terminal`]

      ```sh
      kubectl apply -f deployment.yaml
      ```

   3. Press `enter` on `test terminal` and switch to `apply terminal` immediately then press `enter`

3. Waiting for result

   ```sh
   Error distribution:
   [XXXXX]	Get "http://localhost:8080": EOF
   ```

4. What happens during scaling?

---

## Add Graceful Shutdown

1. Change greeting message

   ```java
   @GetMapping("/")
   public String getGreeting() {
       return "Hello, Graceful Shutdown!";
   }
   ```

2. Change artifact version at `build.gradle` from `'0.0.1-SNAPSHOT'` to version `'0.0.2-SNAPSHOT'`

   ```gradle
   version = '0.0.2-SNAPSHOT'
   ```

3. add <project>/src/main/resources/application.yaml

   ```yaml
   server:
     port: 8080
   shutdown: graceful
   tomcat:
     connection-timeout: 2s
     keep-alive-timeout: 15s
     threads:
       max: 50
       min-spare: 5

   spring:
     application:
       name: catalog-service
     lifecycle:
       timeout-per-shutdown-phase: 15s
   ```

4. Build docker image

   - Change directory to `greeting-service`

   ```sh
   ./gradlew bootBuildImage
   docker image ls
   ```

5. Load image to cluster

   ```sh
   k3d image import greeting-service:0.0.2-SNAPSHOT --cluster default
   ```

6. Change image from `greeting-service:0.0.1-SNAPSHOT` to `greeting-service:0.0.2-SNAPSHOT` at `deployment.yaml` file

   ```yaml
   spec:
     containers:
       - image: greeting-service:0.0.2-SNAPSHOT
         imagePullPolicy: IfNotPresent
         name: greeting-service
   ```

7. Proof Graceful Shutdown

   ![run test](./images/run-test-with-hey.png)

   1. Open new terminal and type following command[`test terminal`]

      ```sh
      hey -c 20 -z 10s http://localhost:8080
      ```

   2. Switch to previous terminal then type following command[`apply terminal`]

      - `k8s` directory

      ```sh
      kubectl apply -f deployment.yaml
      ```

   3. Press `enter` on `test terminal` and switch to `apply terminal` immediately then press `enter`

8. What happens?

---

## Add Health Check

1. Change greeting message

   ```java
   @GetMapping("/")
   public String getGreeting() {
       return "Hello, Graceful Shutdown and Health Check!";
   }
   ```

2. Change artifact version at `build.gradle` from `'0.0.1-SNAPSHOT'` to version `'0.0.2-SNAPSHOT'`

   ```gradle
   version = '0.0.3-SNAPSHOT'
   ```

3. Add `implementation 'org.springframework.boot:spring-boot-starter-actuator'` to `build.gradle`

   ```gradle
   dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
   }
   ```

4. Set exposing the health Actuator endpoint at application.yaml

   ```yaml
   server:
     port: 8080
     shutdown: graceful
     tomcat:
       connection-timeout: 2s
       keep-alive-timeout: 15s
       threads:
         max: 50
         min-spare: 5

   spring:
     application:
       name: catalog-service
     lifecycle:
       timeout-per-shutdown-phase: 15s

   management:
     endpoints:
       web:
       exposure:
         include: health
     endpoint:
       health:
         show-details: always
         show-components: always
         probes:
           enabled: true
   ```

5. Build docker image

   - Change directory to `greeting-service`

   ```sh
   ./gradlew bootBuildImage
   docker image ls
   ```

6. Load image to cluster

   ```sh
   k3d image import greeting-service:0.0.3-SNAPSHOT --cluster default
   ```

7. Change image from `greeting-service:0.0.2-SNAPSHOT` to `greeting-service:0.0.3-SNAPSHOT` at `deployment.yaml` file

   ```yaml
   spec:
     containers:
       - image: greeting-service:0.0.3-SNAPSHOT
         imagePullPolicy: IfNotPresent
         name: greeting-service
   ```

8. Proof Graceful Shutdown

   ![run test](./images/run-test-with-hey.png)

   1. Open new terminal and type following command[`test terminal`]

      ```sh
      hey -c 20 -z 10s http://localhost:8080
      ```

   2. Switch to previous terminal then type following command[`apply terminal`]

      - `k8s` directory

      ```sh
      kubectl apply -f deployment.yaml
      ```

   3. Press `enter` on `test terminal` and switch to `apply terminal` immediately then press `enter`

9. What happens?

10. Check Application's health

    ```sh
    curl http://localhost:8080/actuator/health
    curl http://localhost:8080/actuator/health/liveness
    curl http://localhost:8080/actuator/health/rediness
    ```

---

## Trip set `perStop` at container's lifecycle

```yaml
spec:
  containers:
    - image: greeting-service:0.0.2-SNAPSHOT
      imagePullPolicy: IfNotPresent
      name: greeting-service
      lifecycle:
        preStop:
          exec:
            command: ['sh', '-c', 'sleep 5']
```
