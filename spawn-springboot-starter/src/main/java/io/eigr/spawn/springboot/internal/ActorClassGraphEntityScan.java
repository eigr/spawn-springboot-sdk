package io.eigr.spawn.springboot.internal;

import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.springboot.starter.ActorKind;
import io.eigr.spawn.springboot.starter.annotations.*;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ActorClassGraphEntityScan implements EntityScan {
    private static final Logger log = LoggerFactory.getLogger(ActorClassGraphEntityScan.class);
    private final SpawnProperties properties;
    private final ClassGraph classGraph;

    private List<Entity> entities;

    public ActorClassGraphEntityScan(SpawnProperties properties) {
        this.properties = properties;
        this.classGraph = new ClassGraph()
                .enableAnnotationInfo()
                .blacklistPackages("org.springframework", "com.typesafe", "com.google", "com.fasterxml", "org.slf4j", "org.eclipse", "com.twitter", "io.spray", "org.reactivestreams", "org.scala", "io.grpc", "io.opencensus", "org.yaml");
    }

    @Override
    public List<Entity> findEntities() {
        Instant now = Instant.now();
        if (Objects.isNull(entities)) {
            entities = getActors();
        }
        log.debug("Found {} Entity(ies) in {}", (entities.size()), Duration.between(now, Instant.now()));
        return entities;
    }

    public Optional<Entity> findEntity(Class type) {
        List<Entity> actors = getActors();
        return actors.stream().filter(entity -> entity.getActorType() == type).findFirst();
    }

    private List<Entity> getActors() {
        if (Objects.isNull(entities)) {
            entities = getEntities();
        }
        return entities;
    }

    private List<Entity> getEntities() {
        final List<Class<?>> namedActorEntities = getClassAnnotationWith(NamedActor.class);
        final List<Class<?>> unnamedActorEntities = getClassAnnotationWith(UnNamedActor.class);
        final List<Class<?>> pooledActorEntities = getClassAnnotationWith(PooledActor.class);

        return Stream.of(namedActorEntities, unnamedActorEntities, pooledActorEntities)
                .flatMap(Collection::stream)
                .map(entity -> {
                    Actor actor = entity.getAnnotation(Actor.class);
                    // ex. if actor.getClass().isAssignableFrom(namedActorEntities.getClass());

                    String actorBeanName = entity.getSimpleName();
                    String actorName = getActorName(actor, actorBeanName);
                    ActorKind kind = actor.kind();
                    long deactivateTimeout = actor.deactivatedTimeout();
                    long snapshotTimeout = actor.snapshotTimeout();
                    boolean isStateful = actor.stateful();
                    Class stateType = actor.stateType();
                    int minPoolSize = actor.minPoolSize();
                    int maxPoolSize = actor.maxPoolSize();

                    final Map<String, Entity.EntityMethod> actions = buildActions(entity, Action.class);
                    final Map<String, Entity.EntityMethod> timerActions = buildActions(entity, TimerAction.class);

                    Entity entityType = new Entity(
                            actorName,
                            entity,
                            getKind(kind),
                            stateType,
                            actorBeanName,
                            isStateful,
                            deactivateTimeout,
                            snapshotTimeout,
                            actions,
                            timerActions,
                            minPoolSize,
                            maxPoolSize);

                    log.info("Registering Actor: {}", actorName);
                    log.debug("Registering Entity -> {}", entityType);
                    return entityType;
                }).collect(Collectors.toList());
    }

    private Map<String, Entity.EntityMethod> buildActions(Class<?> entity, Class<? extends Annotation> annotationType) {
        final Map<String, Entity.EntityMethod> actions = new HashMap<>();

        List<Method> methods = Arrays.stream(entity.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType))
                .collect(Collectors.toList());

        for (Method method : methods) {
            try {
                method.setAccessible(true);
                String commandName = getCommandName(method, annotationType);
                Class<?> inputType = getInputType(method, annotationType);
                Class<?> outputType = getOutputType(method, annotationType);

                Entity.EntityMethod action = new Entity.EntityMethod(
                        commandName,
                        getEntityMethodType(method, annotationType),
                        getPeriod(method, annotationType),
                        method,
                        inputType,
                        outputType);

                actions.put(commandName, action);
            } catch (SecurityException e) {
                log.error("Failure on load Actor Action", e);
            }
        }
        return actions;
    }

    private int getPeriod(Method method, Class<? extends Annotation> type) {
        int period = 0;

        if (type.isAssignableFrom(TimerAction.class)) {
            TimerAction act = method.getAnnotation(TimerAction.class);
            period = act.period();
        }

        return period;
    }

    private Entity.EntityMethodType getEntityMethodType(Method method, Class<? extends Annotation> type) {
        Entity.EntityMethodType entityMethodType = null;

        if (type.isAssignableFrom(Action.class)) {
            entityMethodType = Entity.EntityMethodType.DIRECT;
        }

        if (type.isAssignableFrom(TimerAction.class)) {
            entityMethodType = Entity.EntityMethodType.TIMER;
        }

        return entityMethodType;
    }

    private String getCommandName(Method method, Class<? extends Annotation> type) {
        String commandName = "";

        if (type.isAssignableFrom(Action.class)) {
            Action act = method.getAnnotation(Action.class);
            commandName = ((!act.name().equalsIgnoreCase("")) ? act.name() : method.getName());
        }

        if (type.isAssignableFrom(TimerAction.class)) {
            TimerAction act = method.getAnnotation(TimerAction.class);
            commandName = ((!act.name().equalsIgnoreCase("")) ? act.name() : method.getName());
        }

        return commandName;
    }

    private Class<?> getInputType(Method method, Class<? extends Annotation> type) {
        Class<?> inputType = null;

        if (type.isAssignableFrom(Action.class)) {
            Action act = method.getAnnotation(Action.class);
            inputType = (!act.inputType().isAssignableFrom(Action.Default.class) ? act.inputType() : method.getParameterTypes()[0]);
        }

        if (type.isAssignableFrom(TimerAction.class)) {
            TimerAction act = method.getAnnotation(TimerAction.class);
            inputType = (!act.inputType().isAssignableFrom(TimerAction.Default.class) ? act.inputType() : method.getParameterTypes()[0]);
        }

        return inputType;
    }

    private Class<?> getOutputType(Method method, Class<? extends Annotation> type) {
        Class<?> outputType = null;

        if (type.isAssignableFrom(Action.class)) {
            Action act = method.getAnnotation(Action.class);
            outputType = (!act.outputType().isAssignableFrom(Action.Default.class) ? act.outputType() : method.getReturnType());
        }

        if (type.isAssignableFrom(TimerAction.class)) {
            TimerAction act = method.getAnnotation(TimerAction.class);
            outputType = (!act.outputType().isAssignableFrom(TimerAction.Default.class) ? act.outputType() : method.getReturnType());
        }

        return outputType;
    }

    private String getActorName(Actor actor, String beanName) {
        if (isNullOrEmpty(actor)) {
            return beanName;
        }

        return actor.name();
    }

    private boolean isNullOrEmpty(Actor actor) {
        return (Objects.isNull(actor.name()) || actor.name().isEmpty());
    }

    private ActorOuterClass.Kind getKind(ActorKind kind) {
        switch (kind) {
            case UNAMED:
                return ActorOuterClass.Kind.UNAMED;
            case POOLED:
                return ActorOuterClass.Kind.POOLED;
            case PROXY:
                return ActorOuterClass.Kind.PROXY;
            default:
                return ActorOuterClass.Kind.NAMED;
        }
    }

    private List<Class<?>> getClassAnnotationWith(Class<? extends Annotation> annotationType) {
        if (Objects.nonNull(properties.getUserFunctionPackageName())) {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
            Set<BeanDefinition> definitions = scanner.findCandidateComponents(properties.getUserFunctionPackageName());

            return definitions.stream().map(getBeanDefinitionClass()).filter(this::isEntity).collect(Collectors.toList());
        } else {
            try (ScanResult result = classGraph.scan()) {
                return result.getClassesWithAnnotation(annotationType.getName()).loadClasses();
            }
        }

    }

    private boolean isEntity(Class<?> t) {
        return !EmptyCLass.class.getSimpleName().equals(t.getClass().getSimpleName());
    }

    private Function<BeanDefinition, Class<?>> getBeanDefinitionClass() {
        return beanDefinition -> {
            String className = beanDefinition.getBeanClassName();
            String packageName = className.substring(0, className.lastIndexOf('.'));
            log.debug("PackageName: {} ClassName: {}", packageName, className);
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.error("Error during entity function discovery phase", e);
            }
            return EmptyCLass.class;
        };
    }

    final class EmptyCLass {
    }
}
