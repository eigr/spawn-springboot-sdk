package io.eigr.spawn.example.actors;

import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.ActorKind;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.annotations.Action;
import io.eigr.spawn.springboot.starter.annotations.Actor;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
@Actor(kind = ActorKind.ABSTRACT, stateType = AbstractState.class)
public class AbstractActor {

    @Action(name = "sum", inputType = Sum.class)
    public Value sum(Sum msg, ActorContext<AbstractState> context) {
        log.info("Received invocation. Message: {}. Context: {}", msg, context);
        int value = 1;
        if (context.getState().isPresent()) {
            log.info("State is present and value is {}", context.getState().get());
            Optional<AbstractState> oldState = context.getState();
            value = oldState.get().getValue() + msg.getValue();
        } else {
            log.info("State is NOT present. Msg getValue is {}", msg.getValue());
            value = msg.getValue();
        }

        log.info("New Value is {}", value);
        Sum resultValue = Sum.newBuilder()
                .setValue(value)
                .build();

        return Value.at()
                .response(resultValue)
                .state(updateState(value))
                .reply();
    }

    private AbstractState updateState(int value) {
        return AbstractState.newBuilder()
                .setValue(value)
                .build();
    }

}
