package io.eigr.spawn.springboot.internal;

import io.eigr.functions.protocol.actors.ActorOuterClass;

import java.lang.reflect.Method;
import java.util.Map;

public final class Entity {
    private String actorName;
    private Class<?> actorType;

    private ActorOuterClass.Kind kind;

    private Class stateType;
    private String actorBeanName;
    private boolean stateful;

    private long deactivateTimeout;

    private long snapshotTimeout;
    private Map<String, EntityMethod> actions;

    private Map<String, EntityMethod> timerActions;

    private int minPoolSize;
    private int maxPoolSize;

    public Entity(
            String actorName,
            Class<?> actorType,
            ActorOuterClass.Kind kind,
            Class stateType,
            String actorBeanName,
            boolean stateful,
            long deactivateTimeout,
            long snapshotTimeout,
            Map<String, EntityMethod> actions,
            Map<String, EntityMethod> timerActions,
            int minPoolSize,
            int maxPoolSize) {
        this.actorName = actorName;
        this.actorType = actorType;
        this.kind = kind;
        this.stateType = stateType;
        this.actorBeanName = actorBeanName;
        this.stateful = stateful;
        this.deactivateTimeout = deactivateTimeout;
        this.snapshotTimeout = snapshotTimeout;
        this.actions = actions;
        this.timerActions = timerActions;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
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

    public ActorOuterClass.Kind getKind() {
        return kind;
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

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public enum EntityMethodType {
        DIRECT, TIMER
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

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("EntityMethod{");
            sb.append("name='").append(name).append('\'');
            sb.append(", type=").append(type);
            sb.append(", fixedPeriod=").append(fixedPeriod);
            sb.append(", method=").append(method);
            sb.append(", inputType=").append(inputType);
            sb.append(", outputType=").append(outputType);
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Entity{");
        sb.append("actorName='").append(actorName).append('\'');
        sb.append(", actorType=").append(actorType);
        sb.append(", kind=").append(kind);
        sb.append(", stateType=").append(stateType);
        sb.append(", actorBeanName='").append(actorBeanName).append('\'');
        sb.append(", stateful=").append(stateful);
        sb.append(", deactivateTimeout=").append(deactivateTimeout);
        sb.append(", snapshotTimeout=").append(snapshotTimeout);
        sb.append(", actions=").append(actions);
        sb.append(", timerActions=").append(timerActions);
        sb.append(", minPoolSize=").append(minPoolSize);
        sb.append(", maxPoolSize=").append(maxPoolSize);
        sb.append('}');
        return sb.toString();
    }
}
