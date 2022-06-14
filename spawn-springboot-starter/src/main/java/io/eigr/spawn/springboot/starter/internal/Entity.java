package io.eigr.spawn.springboot.starter.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
public final class Entity {
    private String actorName;
    private Class<?> actorType;
    private String actorBeanName;
    private boolean persistent;
    private Map<String, EntityMethod> commands = new HashMap<>();

    public Entity(String actorName, Class<?> actorType, String actorBeanName, boolean persistent, Map<String, EntityMethod> commands) {
        this.actorName = actorName;
        this.actorType = actorType;
        this.actorBeanName = actorBeanName;
        this.persistent = persistent;
        this.commands = commands;
    }

    public String getActorName() {
        return actorName;
    }

    public Class<?> getActorType() {
        return actorType;
    }

    public String getActorBeanName() {
        return actorBeanName;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public Map<String, EntityMethod> getCommands() {
        return commands;
    }

    public static final class EntityMethod {
        private String name;
        private Method method;
        private Class<?> inputType;
        private Class<?> outputType;

        public EntityMethod(String name, Method method, Class<?> inputType, Class<?> outputType) {
            this.name = name;
            this.method = method;
            this.inputType = inputType;
            this.outputType = outputType;
        }

        public String getName() {
            return name;
        }

        public Method getMethod() {
            return method;
        }

        public Class<?> getInputType() {
            return inputType;
        }

        public Class<?> getOutputType() {
            return outputType;
        }
    }

}
