# Observability Metric

## Prerequisite

1. [k3d](https://k3d.io/v5.6.3/)
2. [docker](https://www.docker.com/)
3. [helm](https://helm.sh/docs/intro/install/)

## Getting Start

1. Open `terminal` and change directory to `observability_metric`

   ```sh
   cd observability_metric
   ```

2. Build image

   ```sh
   cd greeting-service
   ./gradlew bootBuildImage
   docker image ls
   ```

### Hello Kubernetes with `k3d`

1. Create Cluster

   ```sh
   k3d cluster create default -p "8080:8080@loadbalancer" -p "8888:80@loadbalancer" --servers 1 --agents 3
   k3d image import greeting-service:0.0.1-SNAPSHOT-METRIC --cluster default
   kubectl get nodes
   ```

   - Delete Cluster `k3d cluster delete default`

2. Create `k8s` directory to contains k8s-manifest

   - Change directory to root-working-directory(`observability_metric`)

     ```sh
     cd ../
     ```

   - Create directory call `k8s`

     ```sh
     mkdir k8s
     ```

     workspace's skeleton:

     ```txt
     observability_metric
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
           - image: greeting-service:0.0.1-SNAPSHOT-METRIC
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
   observability_metric
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

7. Open http://localhost:8080/actuator

---

## Config application for support metrics

1. Add packages, update `build.gradle` file

   ```gradle

   version = '0.0.2-SNAPSHOT-METRIC'      <============ change here

   ...

   dependencies {
     implementation 'org.springframework.boot:spring-boot-starter-web'
     implementation 'org.springframework.boot:spring-boot-starter-actuator'
     implementation 'io.micrometer:micrometer-registry-prometheus'             <============ add here
     testImplementation 'org.springframework.boot:spring-boot-starter-test'
     testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
   }

   ```

2. Update `/src/main/resources/application.yaml`

   ```yaml

   ...

   management:
     endpoints:
       web:
         exposure:
           include: "health, metrics, prometheus"   <================ Update here

   ...

   ```

3. Build docker image again

   ```sh
   cd ../greeting-service
   ./gradlew bootBuildImage
   docker image ls
   ```

4. Load image to cluster

   ```sh
   k3d image import greeting-service:0.0.2-SNAPSHOT-METRIC --cluster default
   ```

5. Change `spec.containers.image` to `greeting-service:0.0.2-SNAPSHOT-METRIC` at `deployment.yaml`

   ```yaml

   ---
   spec:
     containers:
       - image: greeting-service:0.0.2-SNAPSHOT-METRIC     <============== change here
         imagePullPolicy: IfNotPresent
         name: greeting-service
   ```

6. Change to `k8s` directory and apply deployment

   ```sh
   kubectl apply -f deployment.yaml
   ```

7. Open http://localhost:8080/actuator, should see `prometheus` path

---

## Deploy Prometheus for metric

1. Create and Change Working Directory

   - Create directory call `k8s/prometheus`
   - Change directory to `k8s/prometheus`

     ```sh
     cd prometheus
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
       - name: 'prometheus'
         alias: prometheus
         condition: prometheus.enabled
         repository: 'https://prometheus-community.github.io/helm-charts'
         version: '25.19.1'
     ```

   - Create `values.yaml`

     ```yaml
     prometheus:
       server:
         ingress:
           enabled: true
           hosts:
             - prometheus.example.com
       extraScrapeConfigs: |
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

## Deploy Grafana for dashboard

1. Create and Change Working Directory

   - Create directory call `k8s/grafana`
   - Change directory to `k8s/grafana`

     ```sh
     cd grafana
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
       - name: 'grafana'
         alias: grafana
         condition: grafana.enabled
         repository: 'https://grafana.github.io/helm-charts'
         version: '8.0.0'
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
   - Import dashboard id: `11378`
