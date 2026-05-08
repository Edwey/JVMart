package com.jvmart.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Keeps JDK 25 sealed + switch patterns honest without JavaFX or DB. */
class ServiceResultTest {

    @Test
    void successValueViaSwitch() {
        ServiceResult<Integer> r = new ServiceResult.Success<>(42);
        int v = switch (r) {
            case ServiceResult.Success<Integer> s -> s.value();
            case ServiceResult.Failure<Integer> ignored -> -1;
        };
        assertEquals(42, v);
    }

    @Test
    void failureMessageViaSwitch() {
        ServiceResult<String> r = new ServiceResult.Failure<>("not found");
        String msg = switch (r) {
            case ServiceResult.Success<?> ignored -> "bad";
            case ServiceResult.Failure<String> f -> f.message();
        };
        assertEquals("not found", msg);
    }

    @Test
    void recordConstructorsExposeState() {
        var s = new ServiceResult.Success<>("ok");
        var f = new ServiceResult.Failure<Integer>("err");
        assertEquals("ok", s.value());
        assertEquals("err", f.message());
    }
}
