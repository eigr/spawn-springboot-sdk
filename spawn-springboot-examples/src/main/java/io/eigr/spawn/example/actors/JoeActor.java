package io.eigr.spawn.example.actors;

import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.annotations.Action;
import io.eigr.spawn.springboot.starter.annotations.Actor;
import io.eigr.spawn.springboot.starter.annotations.NamedActor;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
@Actor(name = "joe", snapshotTimeout = 5000, deactivatedTimeout = 10000)
@NamedActor(stateType = JoeState.class)
public class JoeActor {

    @Action
    public Value get(ActorContext<JoeState> context) {
        log.info("Received invocation. Context: {}", context);
        if (context.getState().isPresent()) {
            JoeState state = context.getState().get();
            return Value.<JoeState, Sum>at()
                    .state(state)
                    .response(Sum.newBuilder().setValue(state.getValue()).build())
                    .reply();
        }
        return Value.at().empty();
    }

    @Action(name = "sum", inputType = Sum.class)
    public Value sum(Sum msg, ActorContext<JoeState> context) {
        log.info("Received invocation. Message: {}. Context: {}", msg, context);
        int value = 1;
        if (context.getState().isPresent()) {
            log.info("State is present and value is {}", context.getState().get());
            Optional<JoeState> oldState = context.getState();
            value = oldState.get().getValue() + msg.getValue();
        } else {
            //log.info("State is NOT present. Msg getValue is {}", msg.getValue());
            value = msg.getValue();
        }

        log.info("New Value is {}", value);
        Sum resultValue = Sum.newBuilder().setValue(value).build();

        return Value.at().response(resultValue).state(updateState(value)).reply();
    }

    private JoeState updateState(int value) {
        return JoeState.newBuilder().setValue(value).build();
    }

}
