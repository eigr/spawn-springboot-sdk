package io.eigr.spawn.springboot.starter.util;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor
public final class SpawnContainer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnContainer.class);
    private static final Slf4jLogConsumer LOG_CONSUMER = new Slf4jLogConsumer(LOGGER);

    private String proxyImage;
    private String proxyHost;
    private String proxyPort;
    private String userFunctionHost;
    private String userFunctionPort;

    private Network network;

    private String stateStoreHost;
    private String stateStoreKey;

    private GenericContainer spawnContainer;

    @Builder
    private static SpawnContainer of(
            String proxyImage,
            String proxyHost,
            String proxyPort,
            String userFunctionHost,
            String userFunctionPort,
            Network network,
            String stateStoreHost,
            String stateStoreKey){
        SpawnContainer container = new SpawnContainer();
        container.proxyImage = Objects.isNull(proxyImage) ? "eigr/spawn-proxy:0.5.0-rc.13" : proxyImage;
        container.proxyHost = Objects.isNull(proxyHost) ? "0.0.0.0" : proxyHost;
        container.proxyPort = Objects.isNull(proxyPort) ? "9001" : proxyPort;
        container.userFunctionHost = Objects.isNull(userFunctionHost) ? "0.0.0.0" : userFunctionHost;
        container.userFunctionPort = Objects.isNull(userFunctionPort) ? "8091": userFunctionPort;
        container.stateStoreHost = Objects.isNull(stateStoreHost) ? "localhost": stateStoreHost;
        container.stateStoreKey = Objects.isNull(stateStoreKey) ? "3Jnb0hZiHIzHTOih7t2cTEPEpY98Tu1wvQkPfq/XwqE=" : stateStoreKey;

        if (Objects.nonNull(network)) {
            container.network = network;
        }
        buildSpawnContainer(container);
        return container;
    }

    private static void buildSpawnContainer(SpawnContainer container) {
        GenericContainer spawnContainer = new GenericContainer<>(container.proxyImage)
                .withEnv("PROXY_HTTP_PORT", container.proxyPort)
                .withEnv("PROXY_CLUSTER_STRATEGY", "gossip")
                .withEnv("PROXY_DATABASE_TYPE", "mysql")
                .withEnv("PROXY_DATABASE_HOST", container.stateStoreHost)
                .withEnv("PROXY_DATABASE_POOL_SIZE", "30")
                .withEnv("SPAWN_STATESTORE_KEY", container.stateStoreKey)
                .withEnv("USER_FUNCTION_HOST", container.userFunctionHost)
                .withEnv("USER_FUNCTION_PORT", container.userFunctionPort)
                .withExposedPorts(Integer.valueOf(container.proxyPort))
                .withAccessToHost(true)
                .withReuse(true);

        container.spawnContainer = spawnContainer;
    }

    public List<Integer> getExposedPorts() {
        return this.spawnContainer.getExposedPorts();
    }

    public Integer getExposedPort() {
        return (Integer) this.spawnContainer.getExposedPorts().get(0);
    }

    public void start(){
        this.spawnContainer.start();
        this.spawnContainer.followOutput(LOG_CONSUMER);
    }

    @Override
    @SneakyThrows
    public void close() {
        if (Objects.nonNull(this.spawnContainer)) {
            this.spawnContainer.stop();
        }
    }
}
