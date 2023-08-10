package io.eigr.spawn.springboot.starter.annotations;

import com.google.protobuf.GeneratedMessageV3;
import io.eigr.spawn.springboot.starter.ActorKind;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Actor
@Component
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public @interface NamedActor {

    ActorKind kind() default ActorKind.NAMED;
    boolean stateful() default true;
    Class<? extends GeneratedMessageV3> stateType();
    long deactivatedTimeout() default 60000;
    long snapshotTimeout() default 50000;
    String channel() default "";
    int minPoolSize() default 1;
    int maxPoolSize() default 0;
}
