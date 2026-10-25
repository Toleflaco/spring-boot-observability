package dev.tole.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);
    private final Counter itemsNotFound;
    private final Timer slowWait;

    public DemoController(MeterRegistry registry) {
        this.itemsNotFound = Counter.builder("items.not.found")
                .description("Búsqueda de items que no existen")
                .register(registry);
        this.slowWait = Timer.builder("slow.wait")
                .description("Tiempo de espera simulado en /slow")
                .register(registry);
    }

    @GetMapping("/ok")
    String ok() {
        log.info("Petición OK atendida");
        return "ok";
    }

    @GetMapping("/items/{id}")
    ResponseEntity<String> items(@PathVariable Long id) {
        if (id >= 1 && id <= 3) {
            log.atInfo()
                    .addKeyValue("itemId", id)
                    .addKeyValue("status", 200)
                    .log("Item encontrado");
            return ResponseEntity.ok("Item " + id);
        }
        log.atInfo()
                .addKeyValue("itemId", id)
                .addKeyValue("status", 404)
                .log("Item no encontrado");
        itemsNotFound.increment();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/error")
    String error() {
        throw new IllegalStateException("Fallo simulado en /error");
    }

    @GetMapping("/slow")
    ResponseEntity<String> slow() {
        long delayMs = ThreadLocalRandom.current().nextLong(100, 2001);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // restaura el aviso: no se lo "come"
            throw new IllegalStateException("Espera interrumpida", e);
        }
        slowWait.record(Duration.ofMillis(delayMs));
        log.atInfo()
                .addKeyValue("delay_ms", delayMs)
                .log("Respuesta lenta simulada");
        return ResponseEntity.ok("Respuesta lenta simulada");

    }
}
