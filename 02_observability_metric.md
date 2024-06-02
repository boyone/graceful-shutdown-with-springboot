# Observability Metric

### Hello Kubernetes with `k3d`

1. Create Cluster

    ```sh
      k3d cluster create default -p "8080:8080@loadbalancer" -p "8888:80@loadbalancer" --servers 1 --agents 3
      kubectl get nodes
    ```

    - Delete Cluster `k3d cluster delete default`

---

### Config application for support metrics

1. Add packages and update artifact version

    - Update `build.gradle` file

      ```gradle
      version = '0.0.4-SNAPSHOT'   <============ update here

      ...

      dependencies {

        ...
        
        implementation 'io.micrometer:micrometer-registry-prometheus'             <============ add here
      }
      
      ```

2. Update `/src/main/resources/application.yaml`

    ```yaml

    ...

    management:
      endpoints:
        web:
        exposure:
          include: health, metrics, prometheus   <================ Update here

    ...
      
    ```

3. Build docker image

   - Change directory to `greeting-service`

   ```sh
   ./gradlew bootBuildImage
   docker image ls
   ```

4. Load image to cluster

   ```sh
   k3d image import greeting-service:0.0.4-SNAPSHOT --cluster default
   ```

5. Change image to `greeting-service:0.0.4-SNAPSHOT` at `deployment.yaml` file

    ```yaml
    
    ...

    spec:
      containers:
        - image: greeting-service:0.0.4-SNAPSHOT      <============== change here
          imagePullPolicy: IfNotPresent
          name: greeting-service

    ...

    ```

6. Change to `k8s` directory and apply deployment

    ```sh
    kubectl apply -f deployment.yaml
    ```

7. Open http://localhost:8080/actuator


---

### Deploy Prometheus for metrics

1. Create and Change Working Directory

    - Create directory call `k8s/monitoring/prometheus`
    - Change directory to `k8s/monitoring/prometheus`

      ```sh
        cd monitoring/prometheus
      ```

2. Prepare files

    - Create `Chart.yaml`

      ```yaml
        apiVersion: v2
        name: my-prometheus-helm
        description: A Helm chart for Prometheus Demo
        type: application
        version: 1.0.0

        dependencies:
          - name: "prometheus"
            alias: prometheus
            condition: prometheus.enabled
            repository: "https://prometheus-community.github.io/helm-charts"
            version: "25.19.1"
      ```

   - Create `values.yaml`

      ```yaml
      prometheus:
        server:
          ingress:
            enabled: true
            hosts:
              - prometheus.example.com
          serverFiles:
        prometheus.yml:
          scrape_configs:
            - job_name: 'spring-boot-application'
              metrics_path: '/actuator/prometheus'
              scrape_interval: 3s # This can be adjusted based on our needs
              static_configs:
                - targets: ['greeting-service.default.svc.cluster.local:8080']
                  labels:
                    application: 'Greeting service'
      ```

3. Running Prometheus with Helm

    - Add repository
    
      ```sh
        helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
      ```

    - Update dependencies

      ```sh
        helm dependency update
      ```
    - Running prometheus

      ```sh
        helm upgrade -i prometheus . -n monitoring --create-namespace
      ```

    - Open: http://prometheus.example.com:8888

---

### Deploy Grafana for dashboard

1. Create and Change Working Directory

    - Create directory call `k8s/monitoring/grafana`
    - Change directory to `k8s/monitoring/grafana`

      ```sh
        cd monitoring/grafana
      ```

2. Prepare files

    - Create `Chart.yaml`

      ```yaml
        apiVersion: v2
        name: my-grafana-helm
        description: A Helm chart for Grafana Demo
        type: application
        version: 1.0.0

        dependencies:
          - name: "grafana"
            alias: grafana
            condition: grafana.enabled
            repository: "https://grafana.github.io/helm-charts"
            version: "7.3.8"
      ```

   - Create `values.yaml`

      ```yaml
        grafana:
          ingress:
            enabled: true
            hosts:
              - grafana.example.com
      ```

3. Running Grafana with Helm

    - Add repository

      ```sh
        helm repo add grafana https://grafana.github.io/helm-charts
      ```

    - Update dependencies

      ```sh
        helm dependency update
      ```
    - Running prometheus

      ```sh
        helm upgrade -i grafana . -n monitoring --create-namespace
      ```

    - Open: http://grafana.example.com:8888

4. Login

    - Username: **admin**
    - Password:

      ```sh
        kubectl get secret grafana -n monitoring -o jsonpath="{.data.admin-password}" | base64 -d ; echo
      ```

5. Set datasources from Prometheus

    - Prometheus server URL: `http://prometheus-server`
