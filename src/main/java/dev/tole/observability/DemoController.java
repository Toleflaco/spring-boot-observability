package dev.tole.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

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
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/error")
    String error() {
        throw new IllegalStateException("Fallo simulado en /error");
    }

    @GetMapping("/slow")
    ResponseEntity<String> slow () throws InterruptedException {
        long delayMs = ThreadLocalRandom.current().nextLong(100,2001);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // restaura el aviso: no se lo "come"
            throw new IllegalStateException("Espera interrumpida", e);
        }
        log.atInfo()
                .addKeyValue("delay_ms",delayMs)
                .log("Respuesta lenta simulada");
        return ResponseEntity.ok("Respuesta lenta simulada");
    }
}
