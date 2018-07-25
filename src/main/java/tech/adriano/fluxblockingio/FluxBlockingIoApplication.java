package tech.adriano.fluxblockingio;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootApplication
public class FluxBlockingIoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FluxBlockingIoApplication.class, args);
    }

    @Bean
    ApplicationRunner runner() {
        return args -> {
            Flux<String> intervalFlux = Flux
                    .interval(Duration.ofMillis(100))
                    .log()
                    .map(String::valueOf);

            intervalFlux.onBackpressureDrop(this::dropped)
                    .doOnError(throwable -> log.error(throwable.getMessage()))
                    .log()
                    .flatMap(this::nonblocking)
                    .log()
                    .subscribe(log::info);
        };
    }

    private void dropped(String s) {
        log.info("dropped ='{}'", s);
    }

    private Mono<String> blocking(String s) {
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Mono.just(String.format("processing blocking %s", s));
    }

    private Mono<String> nonblocking(String s) {
        Mono<String> mono = Mono.fromCallable(() -> {
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return String.format("processing nonblocking %s", s);
        });

        return mono.subscribeOn(Schedulers.newElastic("chat"));
    }
}
