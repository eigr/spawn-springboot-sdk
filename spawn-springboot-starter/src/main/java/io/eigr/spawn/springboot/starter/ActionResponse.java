package io.eigr.spawn.springboot.starter;

import com.google.protobuf.GeneratedMessageV3;
import lombok.*;

import java.util.Optional;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ActionResponse<Resp extends GeneratedMessageV3> {
    private Optional<Resp> value;
}
