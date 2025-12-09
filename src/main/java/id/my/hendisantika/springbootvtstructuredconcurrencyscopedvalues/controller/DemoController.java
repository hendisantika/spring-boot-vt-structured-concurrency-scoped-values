package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.controller;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.RequestContext;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.ScopedValues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-vt-structured-concurrency-scoped-values
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 09/12/25
 * Time: 08.30
 * To change this template use File | Settings | File Templates.
 */

/**
 * Demo controller showcasing Virtual Threads, Structured Concurrency, and Scoped Values.
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * Demonstrates that the request is being handled by a Virtual Thread.
     */
    @GetMapping("/thread-info")
    public ResponseEntity<Map<String, Object>> getThreadInfo() {
        Thread currentThread = Thread.currentThread();
        RequestContext context = ScopedValues.currentRequestContext();

        Map<String, Object> info = new HashMap<>();
        info.put("threadName", currentThread.getName());
        info.put("threadId", currentThread.threadId());
        info.put("isVirtual", currentThread.isVirtual());
        info.put("threadClass", currentThread.getClass().getName());
        info.put("requestId", context.requestId());
        info.put("correlationId", context.correlationId());
        info.put("timestamp", context.timestamp().toString());

        log.info("Thread info requested - Virtual: {}, Thread: {}",
                currentThread.isVirtual(), currentThread.getName());

        return ResponseEntity.ok(info);
    }

    /**
     * Demonstrates ScopedValue propagation across virtual threads in Structured Concurrency.
     */
    @GetMapping("/scoped-value-demo")
    public ResponseEntity<Map<String, Object>> scopedValueDemo() {
        RequestContext parentContext = ScopedValues.currentRequestContext();
        String parentThreadName = Thread.currentThread().getName();
        boolean parentIsVirtual = Thread.currentThread().isVirtual();

        log.info("Parent thread: {} (virtual: {}), requestId: {}",
                parentThreadName, parentIsVirtual, parentContext.requestId());

        Map<String, Object> result = new HashMap<>();
        result.put("parentThread", parentThreadName);
        result.put("parentIsVirtual", parentIsVirtual);
        result.put("parentRequestId", parentContext.requestId());

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // Fork multiple tasks - each will inherit the ScopedValue
            var task1 = scope.fork(() -> {
                RequestContext ctx = ScopedValues.currentRequestContext();
                Thread t = Thread.currentThread();
                log.info("Task 1 - Thread: {} (virtual: {}), requestId: {}",
                        t.getName(), t.isVirtual(), ctx.requestId());
                Thread.sleep(100);
                return Map.of(
                        "taskName", "task1",
                        "threadName", t.getName(),
                        "isVirtual", t.isVirtual(),
                        "requestId", ctx.requestId(),
                        "sameAsParent", ctx.requestId().equals(parentContext.requestId())
                );
            });

            var task2 = scope.fork(() -> {
                RequestContext ctx = ScopedValues.currentRequestContext();
                Thread t = Thread.currentThread();
                log.info("Task 2 - Thread: {} (virtual: {}), requestId: {}",
                        t.getName(), t.isVirtual(), ctx.requestId());
                Thread.sleep(150);
                return Map.of(
                        "taskName", "task2",
                        "threadName", t.getName(),
                        "isVirtual", t.isVirtual(),
                        "requestId", ctx.requestId(),
                        "sameAsParent", ctx.requestId().equals(parentContext.requestId())
                );
            });

            var task3 = scope.fork(() -> {
                RequestContext ctx = ScopedValues.currentRequestContext();
                Thread t = Thread.currentThread();
                log.info("Task 3 - Thread: {} (virtual: {}), requestId: {}",
                        t.getName(), t.isVirtual(), ctx.requestId());
                Thread.sleep(75);
                return Map.of(
                        "taskName", "task3",
                        "threadName", t.getName(),
                        "isVirtual", t.isVirtual(),
                        "requestId", ctx.requestId(),
                        "sameAsParent", ctx.requestId().equals(parentContext.requestId())
                );
            });

            scope.join();
            scope.throwIfFailed();

            result.put("childTasks", java.util.List.of(task1.get(), task2.get(), task3.get()));
            result.put("message", "All child tasks inherited the same requestId via ScopedValue!");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("error", "Interrupted");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Demonstrates parallel processing with different completion times.
     */
    @GetMapping("/parallel-processing")
    public ResponseEntity<Map<String, Object>> parallelProcessingDemo() {
        long startTime = System.currentTimeMillis();
        String requestId = ScopedValues.currentRequestContext().requestId();

        log.info("Starting parallel processing demo [requestId={}]", requestId);

        Map<String, Object> result = new HashMap<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var fastTask = scope.fork(() -> {
                Thread.sleep(50);
                return Map.of("task", "fast", "duration", 50, "thread", Thread.currentThread().getName());
            });

            var mediumTask = scope.fork(() -> {
                Thread.sleep(100);
                return Map.of("task", "medium", "duration", 100, "thread", Thread.currentThread().getName());
            });

            var slowTask = scope.fork(() -> {
                Thread.sleep(200);
                return Map.of("task", "slow", "duration", 200, "thread", Thread.currentThread().getName());
            });

            scope.join();
            scope.throwIfFailed();

            long totalTime = System.currentTimeMillis() - startTime;

            result.put("tasks", java.util.List.of(fastTask.get(), mediumTask.get(), slowTask.get()));
            result.put("totalTimeMs", totalTime);
            result.put("requestId", requestId);
            result.put("explanation",
                    "Tasks ran in parallel. Total time (~200ms) is close to slowest task, " +
                            "not sum of all tasks (350ms). This is the power of Structured Concurrency!");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("error", "Interrupted");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Health check endpoint that confirms virtual threads are enabled.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Thread currentThread = Thread.currentThread();

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("virtualThreadsEnabled", currentThread.isVirtual());
        health.put("javaVersion", System.getProperty("java.version"));
        health.put("javaVendor", System.getProperty("java.vendor"));

        return ResponseEntity.ok(health);
    }
}
