package io.eigr.spawn.springboot.starter.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
public final class Entity {
    private String actorName;
    private Class<?> actorType;

    private Class stateType;
    private String actorBeanName;
    private boolean stateful;

    private long deactivateTimeout;

    private long snapshotTimeout;
    private Map<String, EntityMethod> actions = new HashMap<>();

    private Map<String, EntityMethod> timerActions = new HashMap<>();

    public Entity(
            String actorName,
            Class<?> actorType,
            Class stateType,
            String actorBeanName,
            boolean stateful,
            long deactivateTimeout,
            long snapshotTimeout,
            Map<String, EntityMethod> actions,
            Map<String, EntityMethod> timerActions) {
        this.actorName = actorName;
        this.actorType = actorType;
        this.stateType = stateType;
        this.actorBeanName = actorBeanName;
        this.stateful = stateful;
        this.deactivateTimeout = deactivateTimeout;
        this.snapshotTimeout = snapshotTimeout;
        this.actions = actions;
        this.timerActions = timerActions;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public Class<?> getActorType() {
        return actorType;
    }

    public Class getStateType() {
        return stateType;
    }

    public String getActorBeanName() {
        return actorBeanName;
    }

    public boolean isStateful() {
        return stateful;
    }

    public long getDeactivateTimeout() {
        return deactivateTimeout;
    }

    public long getSnapshotTimeout() {
        return snapshotTimeout;
    }

    public Map<String, EntityMethod> getActions() {
        return actions;
    }

    public static final class EntityMethod {
        private String name;

        private EntityMethodType type;

        private int fixedPeriod;

        private Method method;
        private Class<?> inputType;
        private Class<?> outputType;

        public EntityMethod(
                String name, EntityMethodType type, int fixedPeriod, Method method, Class<?> inputType, Class<?> outputType) {
            this.name = name;
            this.type = type;
            this.fixedPeriod = fixedPeriod;
            this.method = method;
            this.inputType = inputType;
            this.outputType = outputType;
        }

        public String getName() {
            return name;
        }

        public EntityMethodType getType() {
            return type;
        }

        public int getFixedPeriod() {
            return fixedPeriod;
        }

        public Method getMethod() {
            return method;
        }

        public Class getInputType() {
            return inputType;
        }

        public Class<?> getOutputType() {
            return outputType;
        }
    }

    public enum EntityMethodType {
        DIRECT, TIMER
    }

}
