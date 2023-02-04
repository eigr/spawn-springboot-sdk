package io.eigr.spawn.springboot.internal;

import org.springframework.core.env.Environment;

public class GlobalEnvironment {
    private static ThreadLocal<Environment> env = new ThreadLocal<>();

    public static void setEnvironment(Environment env) {
        GlobalEnvironment.env.set(env);
    }

    public static Environment getEnvironment() {
        return GlobalEnvironment.env.get();
    }
}
