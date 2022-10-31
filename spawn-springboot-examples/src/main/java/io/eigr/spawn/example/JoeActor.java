package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.annotations.ActorEntity;
import io.eigr.spawn.springboot.starter.annotations.Command;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
@ActorEntity(
        name = "joe",
        stateType = MyState.class,
        snapshotTimeout = 5000,
        deactivatedTimeout = 10000
)
public class JoeActor {
    @Command
    public Value get(ActorContext<MyState> context) {
        log.info("Received invocation. Context: {}", context);
        if (context.getState().isPresent()) {
            MyState state = context.getState().get();
            return Value.ActorValue.<MyState, MyBusinessMessage>at()
                    .state(state)
                    .value(MyBusinessMessage.newBuilder()
                            .setValue(state.getValue())
                            .build())
                    .reply();
        }
        return Value.ActorValue.at()
                .empty();
    }

    @Command(name = "sum", inputType = MyBusinessMessage.class)
    public Value sum(MyBusinessMessage msg, ActorContext<MyState> context) {
        log.info("Received invocation. Message: {}. Context: {}", msg, context);
        int value = 1;
        if (context.getState().isPresent()) {
            log.info("State is present and value is {}", context.getState().get());
            Optional<MyState> oldState = context.getState();
            value = oldState.get().getValue() + msg.getValue();
        } else {
            log.info("State is NOT present. Msg getValue is {}", msg.getValue());
            value = msg.getValue();
        }

        log.info("New Value is {}", value);
        MyBusinessMessage resultValue = MyBusinessMessage.newBuilder()
                .setValue(value)
                .build();

        return Value.ActorValue.at()
                .value(resultValue)
                .state(updateState(value))
                .reply();
    }

    private MyState updateState(int value) {
        return MyState.newBuilder()
                .setValue(value)
                .build();
    }

}
