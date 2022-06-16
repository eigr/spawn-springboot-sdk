package io.eigr.spawn.springboot.starter;

import java.util.Optional;

public final class ActorContext<S extends Object> {

    private Optional<S> state;

    public ActorContext(){
        this.state = Optional.empty();
    }

    public ActorContext(S state) {
        this.state = Optional.of(state);
    }

    public Optional<S> getState()  {
        return state;
    }

}
