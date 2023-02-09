# Actor options

This is an example of what kind of Actors you can create with Spawn

## Singleton Actor

In this example we are creating an actor in a Singleton way, that is, it is a known actor at compile time.

```java

```

It can be invoked with:

```java

```

## Abstract Actor

We can also create Unnamed Dynamic/Lazy actors, that is, despite having its abstract behavior defined at compile time, a Lazy actor will only have a concrete instance when it is associated with an identifier/name at runtime. Below follows the same previous actor being defined as abstract.

```java

```

It can be invoked with:

```java

```

Notice that the only thing that has changed is the the kind of actor, in this case the kind is set to `Kind.ABSTRACT`
And we need to reference the original name in the invocation or instantiate it before using `spawn.spawnActor`

## Pooled Actor

Sometimes we want a particular actor to be able to serve requests concurrently, 
however actors will always serve one request at a time using buffering mechanisms to receive requests in their mailbox 
and serve each request one by one. So to get around this behaviour you can configure your Actor as a Pooled Actor, 
this way the system will generate a pool of actors to meet certain requests. See an example below:

```java

```

It can be invoked with:

```java

```

## Default Actions

Actors also have some standard actions that are not implemented by the user and that can be used 
as a way to get the state of an actor without the invocation requiring an extra trip to the host functions. 
You can think of them as a cache of their state, every time you invoke a default action on an actor 
it will return the value directly from the Sidecar process without this target process needing 
to invoke its equivalent host function.

Let's take an example. Suppose Actor Joe wants to know the current state of Actor Robert. 
What Joe can do is invoke Actor Robert's default action called get_state. 
This will make Actor Joe's sidecar find Actor Robert's sidecar somewhere in the cluster and Actor Robert's sidecar will 
return its own state directly to Joe without having to resort to your host function, this in turn will save you a called 
over the network and therefore this type of invocation is faster than invocations of user-defined actions usually are.

Any invocations to actions with the following names will follow this rule: "get", "Get", "get_state", "getState", "GetState"

> **_NOTE_**: You can override this behavior by defining your actor as an action with the same name as the default actions. 
> In this case it will be the Action defined by you that will be called, implying perhaps another network roundtrip
