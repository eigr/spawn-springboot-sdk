package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.annotations.ActorEntity;
import io.eigr.spawn.springboot.starter.annotations.Command;

@ActorEntity(name = "joe", persistent = true)
public class ExampleActor {

    @Command(name = "get")
    public Value get(ActorContext<Example.MyBusinessMessage> context) {
        Example.MyBusinessMessage state = context.getState();
        return Value.ActorValue.at()
                .state(state)
                .noReply();
    }

    @Command(name = "sum", inputType = Example.MyBusinessMessage.class)
    public Value sum(Example.MyBusinessMessage msg, ActorContext<Example.MyBusinessMessage> context) {
        int value = context.getState().getValue() + msg.getValue();

        Example.MyBusinessMessage resultValue = Example.MyBusinessMessage.newBuilder()
                .setValue(value)
                .build();

        return Value.ActorValue.at()
                .value(resultValue)
                .state(resultValue)
                .reply();
    }

}
