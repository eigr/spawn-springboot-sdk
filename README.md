# Spawn Springboot SDK

Spawn Springboot SDK is the support library for the Spawn Actors system.

Spawn is based on the sidecar proxy pattern to provide the multi-language Actor Model framework.
Spawn's technology stack on top of BEAM VM (Erlang's virtual machine) provides support for different languages from its 
native Actor model.

For a broader understanding of Spawn please consult its official [repository](https://github.com/eigr-labs/spawn).

## Installation

To proceed, create a container and send it to a container registry that will be accessible via Kubernetes in the future. This can be done by executing the following command in the application directory via terminal:

```
mvn install
```

This command will compile the maven application, and thanks to the jib Maven plugin, it will also publish the container image in your docker hub registry :)


## How to use

First add the following settings to your `pom.xml` file:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
        <repository>
            <id>spring-releases</id>
            <name>Spring Releases</name>
            <url>https://repo.spring.io/release</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
    
    <pluginRepositories>
        <pluginRepository>
            <id>spring-releases</id>
            <name>Spring Releases</name>
            <url>https://repo.spring.io/release</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>

    <dependencies>
        <dependency>
            <groupId>com.github.eigr-labs.spawn-springboot-sdk</groupId>
            <artifactId>spawn-springboot-starter</artifactId>
            <version>v0.1.10</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>1.6.2</version>
            </extension>
        </extensions>

        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>0.6.1</version>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:3.19.2:exe:${os.detected.classifier}</protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.47.0:exe:${os.detected.classifier}</pluginArtifact>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>compile-custom</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.7</version>
            </plugin>
            <plugin>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>2.5.1</version>
                <executions>
                    <execution>
                        <id>getClasspathFilenames</id>
                        <goals>
                            <!-- provides the jars of the classpath as properties inside of maven
                                 so that we can refer to one of the jars in the exec plugin config below -->
                            <goal>properties</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Now that your setup is configured the second step is to add your protobuf files to the project.
For that create a folder called ***proto*** inside ***src/main***.

Now just add your protobuf files that reflect your business domain inside **src/main/proto** folder. For example:

`example.proto`
```protobuf
syntax = "proto3";

package io.eigr.spawn.example;

option java_multiple_files = true;
option java_package = "io.eigr.spawn.example";
option java_outer_classname = "ExampleProtos";

message MyState {
  int32 value = 1;
}

message MyBusinessMessage {
  int32 value = 1;
}
```

It is important to try to separate the type of message that must be stored as the actors' state from the type of messages 
that will be exchanged between their actors' operations calls. In other words, the Actor's internal state is also represented 
as a protobuf type, and it is a good practice to separate these types of messages from the others in its business domain.

In the above case `MyState` is the type protobuf that represents the state of the Actor that we will create later 
while `MyBusiness` is the type of message that we will send and receive from this Actor.

Now that the protobuf types have been created we can proceed with the code. Example definition of an Actor:

`Actor.java`:

```java
package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.annotations.ActorEntity;
import io.eigr.spawn.springboot.starter.annotations.Command;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
@ActorEntity(name = "joe", stateType = MyState.class) // 1
public class Actor {

    @Command(name = "get") // 2
    public Value get(ActorContext<MyState> context) { // 3
        log.info("Received invocation. Context: {}", context);
        if (context.getState().isPresent()) {
            MyState state = context.getState().get();

            return Value.ActorValue.<MyState, MyBusinessMessage>at()
                    .state(state)
                    .value(MyBusinessMessage.newBuilder()
                            .setValue(state.getValue())
                            .build())
                    .reply(); //4
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
```

**Explaining the code:**

1. Every Actor must contain the `@ActorEntity` annotation so that it can be registered as both a Spring Bean and a Spawn Actor.
2. Use the `@Command` annotation to tell Spawn which methods to expose as Actor actions. Every command must have a name by which it can be invoked and may contain other input and output type annotations.
3. An ActorContext object will always be passed to the action method via Spawn's sidecar proxy. It is via ActorContext that it will be possible to access the current state of the Actor. This will always be the second argument of the method unless your method is not taking any business type arguments, in which case ActorContext will be passed as the first and only argument. This is used when your method just needs to return some value.
4. To return values and the updated state it will always be necessary to use the return type `Value`. This in turn must be built through the `ActorValue` builder class. Return values can be one of three types:
   * **reply**: When the intention is to send some type of data to the caller.
   * **noReply**: When you don't want to send any return to the caller and only the Actor's state can be updated.
   * **empty**: Similar to noReply but no action will be taken regarding the actor state.

`App.java`:

```java
package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.SpawnSystem;
import io.eigr.spawn.springboot.starter.autoconfigure.EnableSpawn;
// others imports omitted

@Log4j2
@EnableSpawn // 1
@SpringBootApplication
@EntityScan("io.eigr.spawn.example") // 2
public class App {
    public static void main(String[] args) {SpringApplication.run(App.class, args);}

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            SpawnSystem actorSystem = ctx.getBean(SpawnSystem.class); // 3
            log.info("Let's invoke some Actor");
            for (int i = 0; i < 10; i++) {
                MyBusinessMessage arg = MyBusinessMessage.newBuilder()
                        .setValue(i)
                        .build(); // 4

                MyBusinessMessage sumResult = (MyBusinessMessage) actorSystem.invoke("joe", "sum", arg, MyBusinessMessage.class); // 5
                log.info("Actor invoke Sum Actor Action value result: {}", sumResult.getValue());

                MyBusinessMessage getResult = (MyBusinessMessage) actorSystem.invoke("joe", "get", arg, MyBusinessMessage.class);
                log.info("Actor invoke Get Actor Action value result: {}", getResult.getValue());
            }
        };
    }
}
```

**Explaining the code:**

1. Enables the Spawn Actor system
2. Indicates in which package your actors will be searched. This is a Spring annotation and is for finding any beans your application declares.
3. All interaction with actors takes place through the `SpawnSystem` class. SpawnSystem in turn is a normal Spring Bean and can be injected into any Spring class normally, in the above case we prefer to retrieve it through Spring's own context with ***ctx.getBean(SpawnSystem.class)***.
4. To call your actors you will need to send the data type you defined as a protobuf.
5. Use the invoke method to perform the actions you defined on your actors. This method must take the following arguments:
   * **name**: The name of the actor registered in the Spawn ActorSystem. This is done via ActorEntity annotation on your Actor.
   * **method**: The name of the method/action defined in your actor. This is done via the Command annotation on your Actor.
   * **arg**: The actual payload to be sent to the Actor action you want to call.
   * **returnType**: The return type of the action performed by the Actor (the actual type that will be returned and not the Value type used only as a factory for the return type).

The complete example code can be found [here](https://github.com/eigr-labs/spawn-springboot-sdk/tree/main/spawn-springboot-examples).

