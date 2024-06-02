# Observability Logging

### Hello Kubernetes with `k3d`

1. Create Cluster

    ```sh
      k3d cluster create default -p "8080:8080@loadbalancer" -p "8888:80@loadbalancer" --servers 1 --agents 3
      kubectl get nodes
    ```

    - Delete Cluster `k3d cluster delete default`

---

### Deploy Loki for logging

1. Create and Change Working Directory

    - Create directory call `k8s/monitoring/loki`
    - Change directory to `k8s/monitoring/loki`

      ```sh
        cd monitoring/loki
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

### Create dashboard with Grafana

1. Set datasources from Loki in Grafana

    - Loki server URL: `http://loki:3100`

2. Create dashboard

    -  Query log

        ![setup-logging-dashboard-01.png](/images/setup-logging-dashboard-01.png)

    -  Add new dashboard

        ![setup-logging-dashboard-02.png](/images/setup-logging-dashboard-02.png)

        ![setup-logging-dashboard-03.png](/images/setup-logging-dashboard-03.png)

    -  Custom dashboard then save dashboard

        ![setup-logging-dashboard-04.png](/images/setup-logging-dashboard-04.png)

        ![setup-logging-dashboard-05.png](/images/setup-logging-dashboard-05.png)
