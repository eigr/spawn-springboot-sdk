package io.eigr.spawn.springboot.starter.internal;

import io.eigr.spawn.springboot.starter.ActorIdentity;
import io.eigr.spawn.springboot.starter.annotations.Action;
import io.eigr.spawn.springboot.starter.annotations.ActorEntity;
import io.eigr.spawn.springboot.starter.annotations.TimerAction;
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

public final class ActorClassGraphEntityScan implements EntityScan {
    private static final Logger log = LoggerFactory.getLogger(ActorClassGraphEntityScan.class);
    private final SpawnProperties properties;
    private final ClassGraph classGraph;

    public ActorClassGraphEntityScan(SpawnProperties properties) {
        this.properties = properties;
        this.classGraph = new ClassGraph()
                .enableAnnotationInfo()
                .blacklistPackages(
                        "org.springframework",
                        "com.typesafe",
                        "com.google",
                        "com.fasterxml",
                        "org.slf4j",
                        "org.eclipse",
                        "com.twitter",
                        "io.spray",
                        "org.reactivestreams",
                        "org.scala",
                        "io.grpc",
                        "io.opencensus",
                        "org.yaml");
    }

    @Override
    public List<Entity> findEntities() {
        Instant now = Instant.now();
        List<Entity> actors = getActors();
        log.debug("Found {} Entity(ies) in {}", (actors.size()), Duration.between(now, Instant.now()));
        return actors;
    }

    public Optional<Entity> findEntity(Class type) {
        List<Entity> actors = getActors();
        return actors.stream()
                .filter(entity -> entity.getActorType() == type)
                .findFirst();
    }

    private List<Entity> getActors() {
        return getEntities();
    }

    private List<Entity> getEntities() {
        final List<Class<?>> actorEntities = getClassAnnotationWith(ActorEntity.class);

        return actorEntities.stream().map(entity -> {
            ActorEntity actor = entity.getAnnotation(ActorEntity.class);
            long deactivateTimeout = actor.deactivatedTimeout();
            long snapshotTimeout = actor.snapshotTimeout();
            String actorBeanName = entity.getSimpleName();
            boolean isStateful = actor.stateful();
            Class stateType = actor.stateType();

            String actorName;
            if (!Objects.isNull(actor.name()) || !actor.name().isEmpty()) {
                actorName = actor.name();
            } else {
                actorName = actorBeanName;
            }

            final Map<String, Entity.EntityMethod> actions = new HashMap<>();
            final Map<String, Entity.EntityMethod> timerActions = new HashMap<>();
            for (Method method : entity.getDeclaredMethods()) {

                if (method.isAnnotationPresent(Action.class)) {
                    Action act = method.getAnnotation(Action.class);
                    try {
                        method.setAccessible(true);
                        String methodName = method.getName();
                        String commandName = (
                                (!act.name().equalsIgnoreCase("")) ? act.name() : methodName
                        );
                        Class<?> inputType = (
                                !act.inputType().isAssignableFrom(Action.Default.class) ? act.inputType() :
                                        method.getParameterTypes()[0]
                        );
                        Class<?> outputType = (
                                !act.outputType().isAssignableFrom(Action.Default.class) ? act.outputType() :
                                        method.getReturnType()
                        );

                        Entity.EntityMethod action = new Entity.EntityMethod(
                                commandName,
                                Entity.EntityMethodType.DIRECT,
                                0,
                                method,
                                inputType,
                                outputType
                        );

                        actions.put(commandName, action);
                    } catch (SecurityException e) {
                        log.error("Failure on load Actor Action", e);
                    }
                }

                if (method.isAnnotationPresent(TimerAction.class)) {
                    TimerAction act = method.getAnnotation(TimerAction.class);
                    try {
                        method.setAccessible(true);
                        String methodName = method.getName();
                        String actionName = (
                                (!act.name().equalsIgnoreCase("")) ? act.name() : methodName
                        );
                        Class<?> inputType = (
                                !act.inputType().isAssignableFrom(TimerAction.Default.class) ? act.inputType() :
                                        method.getParameterTypes()[0]
                        );
                        Class<?> outputType = (
                                !act.outputType().isAssignableFrom(TimerAction.Default.class) ? act.outputType() :
                                        method.getReturnType()
                        );

                        Entity.EntityMethod timerAction = new Entity.EntityMethod(
                                actionName,
                                Entity.EntityMethodType.TIMER,
                                act.period(),
                                method,
                                inputType,
                                outputType
                        );

                        timerActions.put(actionName, timerAction);
                    } catch (SecurityException e) {
                        log.error("Failure on load Actor Action", e);
                    }
                }
            }

            Entity entityType = new Entity(
                    actorName,
                    entity,
                    stateType,
                    actorBeanName,
                    isStateful,
                    deactivateTimeout,
                    snapshotTimeout,
                    actions,
                    timerActions);

            log.info("Registering Actor: {}", actorName);
            log.debug("Registering Entity -> {}", entityType);
            return entityType;
        })
                .collect(Collectors.toList());
    }

    private List<Class<?>> getClassAnnotationWith(Class<? extends Annotation> annotationType) {
        if (Objects.nonNull(properties.getUserFunctionPackageName())) {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
            Set<BeanDefinition> definitions = scanner.findCandidateComponents(properties.getUserFunctionPackageName());

            return definitions.stream()
                    .map(getBeanDefinitionClass())
                    .filter(this::isEntity)
                    .collect(Collectors.toList());
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
