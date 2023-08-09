package io.eigr.spawn.springboot.starter.annotations;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public @interface Actor {
    long deactivatedTimeout() default 60000;
    long snapshotTimeout() default 50000;
    String channel() default "";
    int minPoolSize() default 1;
    int maxPoolSize() default 0;
}
