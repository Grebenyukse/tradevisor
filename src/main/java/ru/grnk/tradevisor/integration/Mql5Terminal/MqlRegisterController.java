package ru.grnk.tradevisor.integration.Mql5Terminal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/advisor")
@Slf4j
public class MqlRegisterController {

    private final Map<String, AdvisorInfo> registeredAdvisors = new ConcurrentHashMap<>();

    public static record AdvisorInfo(
            String advisorId,
            String rootPath,
            String incomingRequestsDir,
            String outgoingResponsesDir,
            String outgoingRequestsDir,
            String incomingResponsesDir,
            LocalDateTime registrationTime,
            boolean isActive) {
    }

    ;


    public static record RegistrationRequest(
            String advisorId,
            String rootPath,
            String incomingRequestsDir,
            String outgoingResponsesDir,
            String outgoingRequestsDir,
            String incomingResponsesDir) {
    }

    public static record RegistrationResponse(boolean success, String message, String advisorId) {
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> registerAdvisor(@RequestBody RegistrationRequest request) {
        try {
            log.info("Получен запрос на регистрацию советника: ID={}, RootPath={}",
                    request.advisorId(), request.rootPath());
            if (request.advisorId() == null || request.advisorId().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new RegistrationResponse(false, "Advisor ID is required", null));
            }

            if (request.rootPath() == null || request.rootPath().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new RegistrationResponse(false, "Root path is required", null));
            }
            AdvisorInfo advisorInfo = new AdvisorInfo(
                    request.advisorId(),
                    request.rootPath(),
                    request.incomingRequestsDir(),
                    request.outgoingResponsesDir(),
                    request.outgoingRequestsDir(),
                    request.incomingResponsesDir(),
                    LocalDateTime.now(),
                    true
            );
            registeredAdvisors.put(request.advisorId(), advisorInfo);
            log.info("Советник {} успешно зарегистрирован", request.advisorId());
            return ResponseEntity.ok(new RegistrationResponse(true,
                    "Advisor registered successfully", request.advisorId()));

        } catch (Exception e) {
            log.error("Ошибка при регистрации советника: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RegistrationResponse(false, "Internal server error: " + e.getMessage(), null));
        }
    }

    /**
     * Получение информации о зарегистрированном советнике
     */
    @GetMapping("/{advisorId}")
    public ResponseEntity<AdvisorInfo> getAdvisorInfo(@PathVariable String advisorId) {
        AdvisorInfo info = registeredAdvisors.get(advisorId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    /**
     * Получение списка всех зарегистрированных советников
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, AdvisorInfo>> getAllAdvisors() {
        return ResponseEntity.ok(registeredAdvisors);
    }

    /**
     * Удаление регистрации советника
     */
    @DeleteMapping("/{advisorId}")
    public ResponseEntity<RegistrationResponse> unregisterAdvisor(@PathVariable String advisorId) {
        AdvisorInfo removed = registeredAdvisors.remove(advisorId);
        if (removed == null) {
            // Используем правильный способ создания ResponseEntity с телом для not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RegistrationResponse(false, "Advisor not found", advisorId));
        }

        log.info("Советник {} удален из реестра", advisorId);
        return ResponseEntity.ok(new RegistrationResponse(true,
                "Advisor unregistered successfully", advisorId));
    }

    /**
     * Проверка активности советника
     */
    @GetMapping("/{advisorId}/status")
    public ResponseEntity<Map<String, Object>> checkAdvisorStatus(@PathVariable String advisorId) {
        AdvisorInfo info = registeredAdvisors.get(advisorId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("advisorId", advisorId);
        status.put("isActive", info.isActive());
        status.put("registrationTime", info.registrationTime());
        status.put("lastSeen", LocalDateTime.now()); // В реальной реализации можно отслеживать heartbeat

        return ResponseEntity.ok(status);
    }
}

