package io.eigr.spawn.springboot.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "io.eigr.spawn")
public class SpawnProperties {

    public final String ACTOR_SYSTEM = "spawn-system";
    public final String PROXY_INTERFACE_DEFAULT = "127.0.0.1";

    public final int PROXY_PORT = 9001;
    public final String USER_FUNCTION_INTERFACE_DEFAULT = "127.0.0.1";
    public final int USER_FUNCTION_PORT = 8090;

    private String actorSystem = ACTOR_SYSTEM;

    private int userFunctionPort = USER_FUNCTION_PORT;
    private String userFunctionInterface = USER_FUNCTION_INTERFACE_DEFAULT;
    private int proxyPort = PROXY_PORT;
    private String proxyInterface = PROXY_INTERFACE_DEFAULT;

    private String userFunctionPackageName;

    public String getActorSystem() {
        return actorSystem;
    }

    public void setActorSystem(String actorSystem) {
        this.actorSystem = actorSystem;
    }

    public String getUserFunctionPackageName() {
        return userFunctionPackageName;
    }

    public void setUserFunctionPackageName(String userFunctionPackageName) {
        this.userFunctionPackageName = userFunctionPackageName;
    }

    public int getUserFunctionPort() {
        return userFunctionPort;
    }

    public void setUserFunctionPort(int userFunctionPort) {
        this.userFunctionPort = userFunctionPort;
    }

    public String getUserFunctionInterface() {
        return userFunctionInterface;
    }

    public void setUserFunctionInterface(String userFunctionInterface) {
        this.userFunctionInterface = userFunctionInterface;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyInterface() {
        return proxyInterface;
    }

    public void setProxyInterface(String proxyInterface) {
        this.proxyInterface = proxyInterface;
    }
}
