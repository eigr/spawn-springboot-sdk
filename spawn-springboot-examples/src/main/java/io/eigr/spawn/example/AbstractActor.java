package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.annotations.Action;
import io.eigr.spawn.springboot.starter.annotations.ActorEntity;
import io.eigr.spawn.springboot.starter.ActorKind;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
@ActorEntity(kind = ActorKind.ABSTRACT, stateType = MyState.class)
public class AbstractActor {

    @Action(name = "sum", inputType = MyBusinessMessage.class)
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

        return Value.at()
                .response(resultValue)
                .state(updateState(value))
                .reply();
    }

    private MyState updateState(int value) {
        return MyState.newBuilder()
                .setValue(value)
                .build();
    }

}
