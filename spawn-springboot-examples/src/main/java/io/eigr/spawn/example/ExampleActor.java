package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.annotations.ActorEntity;
import io.eigr.spawn.springboot.starter.annotations.Command;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ActorEntity(name = "joe", stateType = Example.MyBusinessMessage.class)
public class ExampleActor {

    @Command(name = "get")
    public Value get(ActorContext<Example.MyBusinessMessage> context) {
        log.info("Received invocation. Context: {}", context);
        if(context.getState().isPresent()) {
            Example.MyBusinessMessage state = context.getState().get();

            return Value.ActorValue.<Example.MyBusinessMessage, Example.MyBusinessMessage>at()
                    .state(state)
                    .noReply();
        }

        return Value.ActorValue.at()
                .noReply();

    }

    @Command(name = "sum", inputType = Example.MyBusinessMessage.class)
    public Value sum(Example.MyBusinessMessage msg, ActorContext<Example.MyBusinessMessage> context) {
        log.info("Received invocation. Message: {}. Context: {}", msg, context);
        Example.MyBusinessMessage resultValue = null;

        if(context.getState().isPresent()) {
            int value = context.getState().get().getValue() + msg.getValue();
            resultValue = Example.MyBusinessMessage.newBuilder()
                    .setValue(value)
                    .build();
        } else {
            resultValue = Example.MyBusinessMessage.newBuilder()
                    .setValue(1)
                    .build();
        }

        return Value.ActorValue.at()
                .value(resultValue)
                .state(resultValue)
                .reply();
    }

}
