package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 4 application demonstrating:
 * - Virtual Threads (JEP 444, production-ready since JDK 21)
 * - Structured Concurrency (JEP 480, finalized in JDK 25)
 * - Scoped Values (JEP 481, finalized in JDK 25)
 * <p>
 * Virtual Threads are enabled via spring.threads.virtual.enabled=true
 * in application.properties, which configures Tomcat to use virtual
 * threads for handling HTTP requests.
 */
@Slf4j
@SpringBootApplication
public class SpringBootVtStructuredConcurrencyScopedValuesApplication {

    static void main(String[] args) {
        log.info("Starting application with Java {} on {} threads",
                System.getProperty("java.version"),
                Thread.currentThread().isVirtual() ? "virtual" : "platform");

        SpringApplication.run(SpringBootVtStructuredConcurrencyScopedValuesApplication.class, args);
    }
}
