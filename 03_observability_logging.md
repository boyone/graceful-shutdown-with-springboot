# Observability Logging

## Prerequisite

1. [k3d](https://k3d.io/v5.6.3/)
2. [docker](https://www.docker.com/)

## Getting Start

1. Open `terminal` and change directory to `observability_logging`

   ```sh
   cd observability_logging
   ```

2. Build image

   ```sh
   cd greeting-service
   ./gradlew bootBuildImage
   docker image ls
   ```

### Hello Kubernetes with `k3d`

1. Continue use cluster from `observability_metric`, import image to cluster

    ```sh
      k3d image import greeting-service:0.0.1-SNAPSHOT-LOGGING --cluster default
    ```

2. Create `k8s` directory to contains k8s-manifest

   - Change directory to root-working-directory(`observability_logging`)

     ```sh
     cd ../
     ```

   - Create directory call `k8s`

     ```sh
     mkdir k8s
     ```

     workspace's skeleton:

     ```txt
     observability_logging
       |-greeting-service
       |-k8s
     ```

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
     replicas: 1
     selector:
       matchLabels:
         app: greeting-service
     template:
       metadata:
         labels:
           app: greeting-service
       spec:
         containers:
           - image: greeting-service:0.0.1-SNAPSHOT-LOGGING
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

   workspace's skeleton:

   ```txt
   observability_logging
     |-greeting-service
     |-k8s
       |-deployment.yaml
       |-service.yaml
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

7. Open http://localhost:8080

---

## Deploy Loki for logging

1. Create and Change Working Directory

    - Create directory call `k8s/loki`
    - Change directory to `k8s/loki`

      ```sh
      cd loki
      ```

2. Prepare files

    - Create `Chart.yaml`

      ```yaml
      apiVersion: v2
      name: my-loki-helm
      description: A Helm chart for Loki Demo
      type: application
      version: 1.0.0

      dependencies:
        - name: "loki-stack"
          alias: loki
          condition: loki-stack.enabled
          repository: "https://grafana.github.io/helm-charts"
          version: "2.10.2"
      ```

   - Create `values.yaml`

      ```yaml
      loki:
        loki:
          image:
            repository: grafana/loki
            tag: 2.9.3
      ```

3. Running Loki with Helm

    - Update dependencies

      ```sh
      helm dependency update
      ```
    - Running prometheus

      ```sh
      helm upgrade -i loki . -n monitoring --create-namespace
      ```

---

## Create dashboard with Grafana

1. Go to Grafana: http://grafana.example.com:8888

2. Add new datasources with Loki

    - Loki server URL: `http://loki:3100`

3. Create dashboard

    -  Query log

        ![setup-logging-dashboard-01.png](/images/setup-logging-dashboard-01.png)

    -  Add new dashboard

        ![setup-logging-dashboard-02.png](/images/setup-logging-dashboard-02.png)

        ![setup-logging-dashboard-03.png](/images/setup-logging-dashboard-03.png)

    -  Custom dashboard then save dashboard

        ![setup-logging-dashboard-04.png](/images/setup-logging-dashboard-04.png)

        ![setup-logging-dashboard-05.png](/images/setup-logging-dashboard-05.png)
