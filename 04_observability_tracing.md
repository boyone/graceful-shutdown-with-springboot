# Observability Logging

## Prerequisite

1. [k3d](https://k3d.io/v5.6.3/)
2. [docker](https://www.docker.com/)

## Getting Start

1. Open `terminal` and change directory to `observability_tracing`

   ```sh
   cd observability_tracing
   ```

2. Build image

   ```sh
   cd greeting-service
   docker build -t greeting-service:0.0.1-SNAPSHOT-TRACING .
   docker image ls
   ```

3. Start postgres for assume external database

   ```sh
   cd ..
   docker compose up -d
   ```

### Hello Kubernetes with `k3d`

1. Continue use cluster from `observability_metric`, import image to cluster

    ```sh
    k3d image import greeting-service:0.0.1-SNAPSHOT-TRACING --cluster default
    ```

2. Create `k8s` directory to contains k8s-manifest

   - Change directory to root-working-directory(`observability_tracing`)

     ```sh
     cd ../
     ```

   - Create directory call `k8s`

     ```sh
     mkdir k8s
     ```

     workspace's skeleton:

     ```txt
     observability_tracing
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
            - image: greeting-service:0.0.1-SNAPSHOT-TRACING
              imagePullPolicy: IfNotPresent
              name: greeting-service
              env:
                - name: SPRING_DATASOURCE_URL
                  value: jdbc:postgresql://host.k3d.internal:5434/user
                - name: SPRING_DATASOURCE_USERNAME
                  value: postgres
                - name: SPRING_DATASOURCE_PASSWORD
                  value: postgres
                - name: OTEL_EXPORTER_OTLP_ENDPOINT
                  value: http://tempo.monitoring.svc.cluster.local:4317
                - name: OTEL_EXPORTER_OTLP_PROTOCOL
                  value: grpc
                - name: OTEL_RESOURCE_ATTRIBUTES
                  value: service.name=greeting
                - name: OTEL_LOGS_EXPORTER
                  value: none
                - name: OTEL_METRICS_EXPORTER
                  value: none
                - name: OTEL_TRACES_EXPORTER
                  value: otlp
   ```

   Reference: <https://opentelemetry.io/docs/languages/sdk-configuration/otlp-exporter/#otel_exporter_otlp_protocol>

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
   observability_tracing
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

7. Open <http://localhost:8080>

---

## Deploy Tempo for tracing

1. Create and Change Working Directory

    - Create directory call `k8s/tempo`
    - Change directory to `k8s/tempo`

      ```sh
      cd tempo
      ```

2. Prepare files

    - Create `Chart.yaml`

      ```yaml
      apiVersion: v2
      name: my-tempo-helm
      description: A Helm chart for Tempo Demo
      type: application
      version: 1.0.0

      dependencies:
        - name: "tempo"
          alias: tempo
          condition: tempo.enabled
          repository: "https://grafana.github.io/helm-charts"
          version: "1.9.0"
      ```

3. Running Loki with Helm

    - Update dependencies

      ```sh
      helm dependency update
      ```

    - Running prometheus

      ```sh
      helm upgrade -i tempo . -n monitoring --create-namespace
      ```

---

## Create dashboard with Grafana

1. Go to Grafana: <http://grafana.example.com:8888>

2. Add new datasources with Loki

    - Loki server URL: `http://tempo:3100`

3. Create dashboard

    - Query trace

        ![setup-tracing-dashboard-01.png](/images/setup-tracing-dashboard-01.png)

    - Add new dashboard

        ![setup-tracing-dashboard-02.png](/images/setup-tracing-dashboard-02.png)

        ![setup-tracing-dashboard-03.png](/images/setup-tracing-dashboard-03.png)

    - Custom dashboard then save dashboard

        ![setup-tracing-dashboard-04.png](/images/setup-tracing-dashboard-04.png)

        ![setup-tracing-dashboard-05.png](/images/setup-tracing-dashboard-05.png)
