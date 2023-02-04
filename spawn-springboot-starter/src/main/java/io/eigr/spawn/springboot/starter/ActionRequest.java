package io.eigr.spawn.springboot.starter;

import com.google.protobuf.GeneratedMessageV3;
import lombok.*;

import java.util.Objects;
import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode
public class ActionRequest<Req extends GeneratedMessageV3, Resp extends GeneratedMessageV3> {

    private String actorName;

    private Class actorType;

    private String action;

    private Optional<Req> value;

    private Class<Resp> responseType;

    private Optional<InvocationOpts> opts;

    private ActionRequest(){}

    private ActionRequest(String actorName, String action){
        this.actorName = actorName;
        this.action = action;
        this.value = Optional.empty();
        this.opts = Optional.empty();
    }

    public static ActionRequest of() {
        return new ActionRequest();
    }

    public static ActionRequest of(String actorName, String action) {
        return new ActionRequest(actorName, action);
    }

    public ActionRequest actorName(String actorName) {
        if (Objects.nonNull(this.actorType)) {
            throw new IllegalArgumentException("You must define only one of actorName or actorType. Give preference to actorName.");
        }

        this.actorName = actorName;
        return this;
    }

    public ActionRequest actorType(Class actorType) {
        if (Objects.nonNull(this.actorName)) {
            throw new IllegalArgumentException("You must define only one of actorName or actorType. Give preference to actorName.");
        }

        this.actorType = actorType;
        return this;
    }

    public ActionRequest action(String action) {
        this.action = action;
        return this;
    }

    public ActionRequest value(Req value) {
        this.value = Optional.of(value);
        return this;
    }

    public ActionRequest responseType(Class<Resp> responseType) {
        this.responseType = responseType;
        return this;
    }

    public ActionRequest options(InvocationOpts opts) {
        this.opts =  Optional.of(opts);
        return this;
    }

    public ActionRequest build() {
        Objects.requireNonNull(this.responseType, "The responseType attribute is mandatory");
        if (Objects.isNull(this.actorName) && Objects.isNull(this.actorType)) {
            throw new IllegalArgumentException("You must set the actorName or actorType attribute");
        }
        return this;
    }
}
