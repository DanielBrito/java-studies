package com.reactivespring.controller;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.stream.IntStream;

public class SinksTest {

    static Integer errorFunction() {
        throw new RuntimeException("Exception Occurred");
    }

    @Test
    void sink() {
        Sinks.Many<Integer> replaySinks = Sinks.many().replay().all();
        //Sinks.Many<Integer> replaySinks = Sinks.many().replay().latest();

        var emitResult = replaySinks.tryEmitNext(1);

        System.out.println("emitResult :  " + emitResult);

        replaySinks.emitNext(2, Sinks.EmitFailureHandler.FAIL_FAST);

        Flux<Integer> integerFlux = replaySinks.asFlux();

        integerFlux
                .subscribe(s->{
                    System.out.println("Subscriber 1 : " + s);
                });

        Flux<Integer> integerFlux1 = replaySinks.asFlux();

        integerFlux1
                .subscribe(s->{
                    System.out.println("Subscriber 2 : " + s);
                });

        replaySinks.tryEmitNext(3);
    }

    @Test
    void sink_memoryTest() {
        Sinks.Many<Integer> replaySinks = Sinks.many().replay().all();

        IntStream.rangeClosed(0,300)
                .forEach(replaySinks::tryEmitNext);


        Flux<Integer> integerFlux = replaySinks.asFlux();

        integerFlux
                .subscribe(s->{
                    System.out.println("Subscriber 1 : " + s);
                });

        replaySinks.tryEmitNext(301);

        Flux<Integer> integerFlux1 = replaySinks.asFlux();

        integerFlux1
                .subscribe(s->{
                    System.out.println("Subscriber 2 : " + s);
                });
    }

    @Test
    void sink_multicast() throws InterruptedException {
        Sinks.Many<Integer> multiCast = Sinks.many().multicast().onBackpressureBuffer();

        IntStream.rangeClosed(0,200)
                .forEach(multiCast::tryEmitNext);


        multiCast.tryEmitNext(301);
        multiCast.tryEmitNext(302);

        Flux<Integer> integerFlux = multiCast.asFlux();

        integerFlux
                .subscribe(s->{
                    System.out.println("Subscriber 1 : " + s);
                });

        multiCast.tryEmitNext(303);

        Flux<Integer> integerFlux1 = multiCast.asFlux();

        integerFlux1
                .subscribe(s->{
                    System.out.println("Subscriber 2 : " + s);
                });

        multiCast.tryEmitNext(4);
    }

    @Test
    void sink_unicast() throws InterruptedException {
        Sinks.Many<Integer> multiCast = Sinks.many().unicast().onBackpressureBuffer();

        IntStream.rangeClosed(0,200)
                .forEach(multiCast::tryEmitNext);

        multiCast.tryEmitNext(301);
        multiCast.tryEmitNext(302);

        Flux<Integer> integerFlux = multiCast.asFlux();
        integerFlux
                .subscribe(s->{
                    System.out.println("Subscriber 1 : " + s);
                });

        multiCast.tryEmitNext(303);

        //This will throw an error and unicast allows just one subscriber:
        Flux<Integer> integerFlux1 = multiCast.asFlux();

        integerFlux1
                .subscribe(s->{
                    System.out.println("Subscriber 2 : " + s);
                });

        multiCast.tryEmitNext(4);
    }
}
