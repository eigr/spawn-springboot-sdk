package io.eigr.spawn.springboot.starter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InvocationOpts {

    private boolean async;

    private Optional<Long> delay;

    private Optional<LocalDateTime> scheduledTo;

    public long getScheduleTimeInLong() {
        if (scheduledTo.isPresent()) {
            LocalDateTime ldt = scheduledTo.get();
            return ChronoUnit.MILLIS.between(LocalDateTime.now(), ldt);
        }

        throw new IllegalArgumentException("ScheduledTo is null");
    }
}
