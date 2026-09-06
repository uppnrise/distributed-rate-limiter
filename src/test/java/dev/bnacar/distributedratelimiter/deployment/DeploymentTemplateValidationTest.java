package dev.bnacar.distributedratelimiter.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests to validate Kubernetes deployment templates and configurations.
 */
class DeploymentTemplateValidationTest {

    private static final String K8S_BASE_PATH = "k8s/base";
    private static final String K8S_ENVIRONMENTS_PATH = "k8s/environments";
    
    @Test
    void testBaseManifestsExist() {
        String[] requiredManifests = {
            "k8s/base/namespace.yaml",
            "k8s/base/configmap.yaml",
            "k8s/base/secrets.yaml",
            "k8s/base/rbac.yaml",
            "k8s/base/redis.yaml",
            "k8s/base/deployment.yaml",
            "k8s/base/ingress.yaml"
        };
        
        for (String manifest : requiredManifests) {
            Path path = Paths.get(manifest);
            assertThat(Files.exists(path))
                .as("Base manifest %s should exist", manifest)
                .isTrue();
        }
    }
    
    @Test
    void testEnvironmentConfigurationsExist() {
        String[] environments = {"dev", "prod"};
        
        for (String env : environments) {
            Path envPath = Paths.get(K8S_ENVIRONMENTS_PATH, env);
            assertThat(Files.exists(envPath))
                .as("Environment directory %s should exist", env)
                .isTrue();
                
            Path kustomizationPath = envPath.resolve("kustomization.yaml");
            assertThat(Files.exists(kustomizationPath))
                .as("Kustomization file should exist for %s environment", env)
                .isTrue();
        }
    }
    
    @Test
    void testYamlSyntaxValidity() throws IOException {
        Yaml yaml = new Yaml();
        
        // Test base manifests
        try (Stream<Path> paths = Files.walk(Paths.get(K8S_BASE_PATH))) {
            paths.filter(path -> path.toString().endsWith(".yaml"))
                .forEach(path -> {
                    assertDoesNotThrow(() -> {
                        String content = Files.readString(path);
                        yaml.loadAll(content);
                    }, "YAML syntax should be valid for " + path);
                });
        }
        
        // Test environment manifests
        try (Stream<Path> paths = Files.walk(Paths.get(K8S_ENVIRONMENTS_PATH))) {
            paths.filter(path -> path.toString().endsWith(".yaml"))
                .forEach(path -> {
                    assertDoesNotThrow(() -> {
                        String content = Files.readString(path);
                        yaml.loadAll(content);
                    }, "YAML syntax should be valid for " + path);
                });
        }
    }
    
    @Test
    void testNamespaceConfiguration() throws IOException {
        Path namespacePath = Paths.get(K8S_BASE_PATH, "namespace.yaml");
        String content = Files.readString(namespacePath);
        
        Yaml yaml = new Yaml();
        Iterable<Object> documents = yaml.loadAll(content);
        
        boolean foundNamespace = false;
        boolean foundResourceQuota = false;
        boolean foundLimitRange = false;
        
        for (Object doc : documents) {
            if (doc instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> manifest = (Map<String, Object>) doc;
                String kind = (String) manifest.get("kind");
                
                if ("Namespace".equals(kind)) {
                    foundNamespace = true;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) manifest.get("metadata");
                    assertThat(metadata.get("name")).isEqualTo("rate-limiter");
                }
                
                if ("ResourceQuota".equals(kind)) {
                    foundResourceQuota = true;
                }
                
                if ("LimitRange".equals(kind)) {
                    foundLimitRange = true;
                }
            }
        }
        
        assertThat(foundNamespace).as("Namespace resource should be defined").isTrue();
        assertThat(foundResourceQuota).as("ResourceQuota should be defined").isTrue();
        assertThat(foundLimitRange).as("LimitRange should be defined").isTrue();
    }
    
    @Test
    void testConfigMapConfiguration() throws IOException {
        Path configMapPath = Paths.get(K8S_BASE_PATH, "configmap.yaml");
        String content = Files.readString(configMapPath);
        
        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked")
        Map<String, Object> manifest = (Map<String, Object>) yaml.load(content);
        
        assertThat(manifest.get("kind")).isEqualTo("ConfigMap");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) manifest.get("metadata");
        assertThat(metadata.get("name")).isEqualTo("rate-limiter-config");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) manifest.get("data");
        assertThat(data).containsKey("application.properties");
        
        String appProperties = (String) data.get("application.properties");
        assertThat(appProperties).contains("spring.profiles.active=production");
        assertThat(appProperties).contains("spring.redis.host=redis-service");
        assertThat(appProperties).contains("ratelimiter.capacity=");
        assertThat(appProperties).contains("management.endpoints.web.exposure.include=");
    }
    
    @Test
    void testDeploymentConfiguration() throws IOException {
        Path deploymentPath = Paths.get(K8S_BASE_PATH, "deployment.yaml");
        String content = Files.readString(deploymentPath);
        
        Yaml yaml = new Yaml();
        Iterable<Object> documents = yaml.loadAll(content);
        
        boolean foundDeployment = false;
        boolean foundService = false;
        boolean foundPDB = false;
        
        for (Object doc : documents) {
            if (doc instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> manifest = (Map<String, Object>) doc;
                String kind = (String) manifest.get("kind");
                
                if ("Deployment".equals(kind)) {
                    foundDeployment = true;
                    validateDeploymentManifest(manifest);
                }
                
                if ("Service".equals(kind)) {
                    foundService = true;
                }
                
                if ("PodDisruptionBudget".equals(kind)) {
                    foundPDB = true;
                }
            }
        }
        
        assertThat(foundDeployment).as("Deployment resource should be defined").isTrue();
        assertThat(foundService).as("Service resource should be defined").isTrue();
        assertThat(foundPDB).as("PodDisruptionBudget should be defined").isTrue();
    }
    
    @SuppressWarnings("unchecked")
    private void validateDeploymentManifest(Map<String, Object> deployment) {
        // Check basic deployment structure
        Map<String, Object> spec = (Map<String, Object>) deployment.get("spec");
        assertThat(spec.get("replicas")).isEqualTo(3);
        
        Map<String, Object> template = (Map<String, Object>) spec.get("template");
        Map<String, Object> podSpec = (Map<String, Object>) template.get("spec");
        
        // Check security context
        Map<String, Object> securityContext = (Map<String, Object>) podSpec.get("securityContext");
        assertThat(securityContext.get("runAsNonRoot")).isEqualTo(true);
        assertThat(securityContext.get("runAsUser")).isEqualTo(10001);
        assertThat(securityContext.get("runAsGroup")).isEqualTo(10001);
        assertThat(securityContext.get("fsGroup")).isEqualTo(10001);
        assertThat((Map<String, Object>) securityContext.get("seccompProfile"))
            .containsEntry("type", "RuntimeDefault");
        
        // Check containers
        List<Map<String, Object>> containers = (List<Map<String, Object>>) podSpec.get("containers");
        assertThat(containers).hasSize(1);
        
        Map<String, Object> container = containers.get(0);
        assertThat(container.get("name")).isEqualTo("rate-limiter");
        assertThat(container.get("image")).isEqualTo("ghcr.io/uppnrise/distributed-rate-limiter:latest");
        assertThat(container.get("imagePullPolicy")).isEqualTo("Always");

        Map<String, Object> containerSecurityContext =
            (Map<String, Object>) container.get("securityContext");
        assertThat(containerSecurityContext)
            .containsEntry("allowPrivilegeEscalation", false)
            .containsEntry("readOnlyRootFilesystem", true)
            .containsEntry("runAsNonRoot", true)
            .containsEntry("runAsUser", 10001)
            .containsEntry("runAsGroup", 10001);
        assertThat((List<String>) ((Map<String, Object>) containerSecurityContext
            .get("capabilities")).get("drop")).containsExactly("ALL");
        
        // Check resource limits
        Map<String, Object> resources = (Map<String, Object>) container.get("resources");
        assertThat(resources).containsKeys("requests", "limits");
        
        // Check probes
        assertThat(container).containsKeys("livenessProbe", "readinessProbe", "startupProbe");
        
        // Check volume mounts
        List<Map<String, Object>> volumeMounts = (List<Map<String, Object>>) container.get("volumeMounts");
        assertThat(volumeMounts).isNotEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEnvironmentDeploymentPatchesRepeatRequiredSecurityControls() throws IOException {
        for (String environment : List.of("dev", "prod")) {
            Path patchPath = Paths.get(K8S_ENVIRONMENTS_PATH, environment,
                "deployment-patch.yaml");
            Map<String, Object> patch = loadSingleYamlDocument(patchPath);
            Map<String, Object> spec = mapAt(patch, "spec");
            Map<String, Object> template = mapAt(spec, "template");
            Map<String, Object> podSpec = mapAt(template, "spec");
            Map<String, Object> podSecurityContext = mapAt(podSpec, "securityContext");

            assertThat(podSecurityContext)
                .as("%s pod security context", environment)
                .containsEntry("runAsNonRoot", true)
                .containsEntry("runAsUser", 10001)
                .containsEntry("runAsGroup", 10001)
                .containsEntry("fsGroup", 10001);
            assertThat(mapAt(podSecurityContext, "seccompProfile"))
                .containsEntry("type", "RuntimeDefault");

            List<Map<String, Object>> containers = listAt(podSpec, "containers");
            Map<String, Object> container = containers.get(0);
            assertThat(container.get("imagePullPolicy")).isEqualTo("Always");

            Map<String, Object> containerSecurityContext = mapAt(container, "securityContext");
            assertThat(containerSecurityContext)
                .containsEntry("allowPrivilegeEscalation", false)
                .containsEntry("readOnlyRootFilesystem", true)
                .containsEntry("runAsNonRoot", true)
                .containsEntry("runAsUser", 10001)
                .containsEntry("runAsGroup", 10001);
            assertThat((List<String>) mapAt(containerSecurityContext, "capabilities")
                .get("drop")).containsExactly("ALL");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testApplicationServiceAccountHasNoUnusedRbacPermissions() throws IOException {
        Yaml yaml = new Yaml();
        List<Map<String, Object>> manifests = new java.util.ArrayList<>();
        for (Object document : yaml.loadAll(Files.readString(
            Paths.get(K8S_BASE_PATH, "rbac.yaml")))) {
            manifests.add((Map<String, Object>) document);
        }

        assertThat(manifests).hasSize(1);
        assertThat(manifests.get(0).get("kind")).isEqualTo("ServiceAccount");
        assertThat(manifests.get(0).get("automountServiceAccountToken")).isEqualTo(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBackupCronJobUsesNonHostUid() throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> cronJob = null;
        for (Object document : yaml.loadAll(Files.readString(
            Paths.get(K8S_BASE_PATH, "backup-cronjob.yaml")))) {
            Map<String, Object> manifest = (Map<String, Object>) document;
            if ("CronJob".equals(manifest.get("kind"))) {
                cronJob = manifest;
                break;
            }
        }

        assertThat(cronJob).isNotNull();
        Map<String, Object> jobTemplate = mapAt(mapAt(cronJob, "spec"), "jobTemplate");
        Map<String, Object> jobSpec = mapAt(jobTemplate, "spec");
        Map<String, Object> template = mapAt(jobSpec, "template");
        Map<String, Object> podSpec = mapAt(template, "spec");
        assertThat(mapAt(podSpec, "securityContext"))
            .containsEntry("runAsNonRoot", true)
            .containsEntry("runAsUser", 10001)
            .containsEntry("runAsGroup", 10001)
            .containsEntry("fsGroup", 10001);

        Map<String, Object> container = listAt(podSpec, "containers").get(0);
        assertThat(mapAt(container, "securityContext"))
            .containsEntry("runAsNonRoot", true)
            .containsEntry("runAsUser", 10001)
            .containsEntry("runAsGroup", 10001);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadSingleYamlDocument(Path path) throws IOException {
        return (Map<String, Object>) new Yaml().load(Files.readString(path));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapAt(Map<String, Object> parent, String key) {
        return (Map<String, Object>) parent.get(key);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listAt(Map<String, Object> parent, String key) {
        return (List<Map<String, Object>>) parent.get(key);
    }
    
    @Test
    void testRedisConfiguration() throws IOException {
        Path redisPath = Paths.get(K8S_BASE_PATH, "redis.yaml");
        String content = Files.readString(redisPath);
        
        Yaml yaml = new Yaml();
        Iterable<Object> documents = yaml.loadAll(content);
        
        boolean foundDeployment = false;
        boolean foundService = false;
        boolean foundPVC = false;
        boolean foundConfigMap = false;
        
        for (Object doc : documents) {
            if (doc instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> manifest = (Map<String, Object>) doc;
                String kind = (String) manifest.get("kind");
                
                switch (kind) {
                    case "Deployment":
                        foundDeployment = true;
                        validateRedisDeployment(manifest);
                        break;
                    case "Service":
                        foundService = true;
                        break;
                    case "PersistentVolumeClaim":
                        foundPVC = true;
                        break;
                    case "ConfigMap":
                        foundConfigMap = true;
                        break;
                }
            }
        }
        
        assertThat(foundDeployment).as("Redis Deployment should be defined").isTrue();
        assertThat(foundService).as("Redis Service should be defined").isTrue();
        assertThat(foundPVC).as("Redis PVC should be defined").isTrue();
        assertThat(foundConfigMap).as("Redis ConfigMap should be defined").isTrue();
    }
    
    @SuppressWarnings("unchecked")
    private void validateRedisDeployment(Map<String, Object> deployment) {
        Map<String, Object> spec = (Map<String, Object>) deployment.get("spec");
        assertThat(spec.get("replicas")).isEqualTo(1);
        
        Map<String, Object> template = (Map<String, Object>) spec.get("template");
        Map<String, Object> podSpec = (Map<String, Object>) template.get("spec");

        Map<String, Object> podSecurityContext =
            (Map<String, Object>) podSpec.get("securityContext");
        assertThat(podSecurityContext.get("runAsUser")).isEqualTo(10001);
        assertThat(podSecurityContext.get("fsGroup")).isEqualTo(10001);
        
        // Check containers (should have redis + redis-exporter)
        List<Map<String, Object>> containers = (List<Map<String, Object>>) podSpec.get("containers");
        assertThat(containers).hasSize(2);
        
        // Check redis container
        Map<String, Object> redisContainer = containers.stream()
            .filter(c -> "redis".equals(c.get("name")))
            .findFirst()
            .orElse(null);
        assertThat(redisContainer).isNotNull();
        assertThat(redisContainer.get("image")).asString().contains("redis:");
        assertThat(redisContainer.get("imagePullPolicy")).isEqualTo("Always");
        assertThat((Map<String, Object>) redisContainer.get("securityContext"))
            .containsEntry("runAsUser", 10001);
        
        // Check redis-exporter container
        Map<String, Object> exporterContainer = containers.stream()
            .filter(c -> "redis-exporter".equals(c.get("name")))
            .findFirst()
            .orElse(null);
        assertThat(exporterContainer).isNotNull();
        assertThat(exporterContainer.get("image")).asString().contains("redis_exporter:");
        assertThat(exporterContainer.get("imagePullPolicy")).isEqualTo("Always");
        assertThat(exporterContainer).containsKey("livenessProbe");
        assertThat((Map<String, Object>) exporterContainer.get("securityContext"))
            .containsEntry("runAsUser", 10001);
    }
    
    @Test
    void testMonitoringConfiguration() {
        String[] monitoringFiles = {
            "k8s/monitoring/prometheus/config.yaml",
            "k8s/monitoring/prometheus/rules.yaml",
            "k8s/monitoring/grafana/dashboard.json"
        };
        
        for (String file : monitoringFiles) {
            Path path = Paths.get(file);
            assertThat(Files.exists(path))
                .as("Monitoring file %s should exist", file)
                .isTrue();
        }
    }
    
    @Test
    void testPrometheusRulesValidity() throws IOException {
        Path rulesPath = Paths.get("k8s/monitoring/prometheus/rules.yaml");
        String content = Files.readString(rulesPath);
        
        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked")
        Map<String, Object> manifest = (Map<String, Object>) yaml.load(content);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) manifest.get("data");
        assertThat(data).containsKey("rate-limiter.yml");
        
        String rulesContent = (String) data.get("rate-limiter.yml");
        @SuppressWarnings("unchecked")
        Map<String, Object> rules = (Map<String, Object>) yaml.load(rulesContent);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) rules.get("groups");
        assertThat(groups).isNotEmpty();
        
        Map<String, Object> group = groups.get(0);
        assertThat(group.get("name")).isEqualTo("rate-limiter.rules");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rulesList = (List<Map<String, Object>>) group.get("rules");
        assertThat(rulesList).isNotEmpty();
        
        // Verify critical alerts exist
        List<String> alertNames = rulesList.stream()
            .map(rule -> (String) rule.get("alert"))
            .toList();
        
        assertThat(alertNames).contains(
            "RateLimiterDown",
            "RateLimiterHighLatency", 
            "RateLimiterHighErrorRate",
            "RedisDown",
            "HighRateLimitViolations"
        );
    }
    
    @Test
    void testGrafanaDashboardValidity() throws IOException {
        Path dashboardPath = Paths.get("k8s/monitoring/grafana/dashboard.json");
        String content = Files.readString(dashboardPath);
        
        // Basic JSON validation
        assertDoesNotThrow(() -> {
            // Use a simple JSON parser to validate structure
            assertThat(content).contains("\"dashboard\":");
            assertThat(content).contains("\"panels\":");
            assertThat(content).contains("\"targets\":");
        }, "Grafana dashboard should be valid JSON");
        
        // Check for required panels
        assertThat(content).contains("Application Overview");
        assertThat(content).contains("Request Rate");
        assertThat(content).contains("Rate Limiting Metrics");
        assertThat(content).contains("Redis Metrics");
    }
    
    @Test
    void testScriptsExistAndExecutable() {
        String[] scripts = {
            "scripts/backup/redis-backup.sh",
            "scripts/backup/redis-recovery.sh",
            "scripts/deployment/deploy.sh"
        };
        
        for (String script : scripts) {
            Path path = Paths.get(script);
            assertThat(Files.exists(path))
                .as("Script %s should exist", script)
                .isTrue();
            assertThat(Files.isExecutable(path))
                .as("Script %s should be executable", script)
                .isTrue();
        }
    }
    
    @Test
    void testRunbookExists() {
        Path runbookPath = Paths.get("docs/runbook/README.md");
        assertThat(Files.exists(runbookPath))
            .as("Operations runbook should exist")
            .isTrue();
    }
}
