package com.prueba.solicitudesservice.service;

import com.prueba.solicitudesservice.entity.Solicitud;
import com.prueba.solicitudesservice.exception.ApiException;
import com.prueba.solicitudesservice.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudRepository repository;

    // CREAR SOLICITUD
    public Solicitud create(Solicitud solicitud) {

        // VALIDACIONES OBLIGATORIAS (400)
        if (solicitud.getEmployeeId() == null || solicitud.getEmployeeId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "employeeId es obligatorio", "BAD_REQUEST");
        }

        if (solicitud.getType() == null || solicitud.getType().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "type es obligatorio", "BAD_REQUEST");
        }

        if (solicitud.getStartDate() == null || solicitud.getEndDate() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startDate y endDate son obligatorios", "BAD_REQUEST");
        }

        // VALIDAR TYPE
        if (!solicitud.getType().equals("VACACIONES") && !solicitud.getType().equals("PERMISO")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "type inválido", "BAD_REQUEST");
        }

        // VALIDAR FECHAS
        if (solicitud.getEndDate().isBefore(solicitud.getStartDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate no puede ser menor que startDate", "BAD_REQUEST");
        }

        // VALIDAR TRASLAPE (409)
        boolean overlap = repository.existsOverlappingRequest(
                solicitud.getEmployeeId(),
                solicitud.getStartDate(),
                solicitud.getEndDate()
        );

        if (overlap) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una solicitud en ese rango de fechas", "OVERLAP");
        }

        // CALCULAR DIAS
        BigDecimal days = calculateDays(
                solicitud.getType(),
                solicitud.getStartDate(),
                solicitud.getEndDate()
        );

        solicitud.setDaysRequested(days);

        // VALIDAR LIMITES (400)
        if ("VACACIONES".equals(solicitud.getType()) && days.intValue() > 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vacaciones exceden máximo permitido", "LIMIT_EXCEEDED");
        }

        if ("PERMISO".equals(solicitud.getType()) && days.intValue() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Permiso excede máximo permitido", "LIMIT_EXCEEDED");
        }

        // SET DEFAULTS
        solicitud.setStatus("PENDIENTE");
        solicitud.setCreatedAt(LocalDateTime.now());

        return repository.save(solicitud);
    }

    // LISTAR
    public Page<Solicitud> findByEmployee(String employeeId, String status, int page, int size) {

        // validar employeeId
        if (employeeId == null || employeeId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "employeeId es obligatorio", "BAD_REQUEST");
        }

        // validar status
        if (status != null && !status.isBlank()) {
            if (!status.equals("PENDIENTE") &&
                    !status.equals("APROBADA") &&
                    !status.equals("RECHAZADA")) {

                throw new ApiException(HttpStatus.BAD_REQUEST, "status inválido", "BAD_REQUEST");
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());

        if (status != null && !status.isBlank()) {
            return repository.findByEmployeeIdAndStatus(employeeId, status, pageable);
        }

        return repository.findByEmployeeId(employeeId, pageable);
    }

    // APROBAR / RECHAZAR
    public Solicitud decision(Long id, String status, String comment) {

        Solicitud solicitud = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud no encontrada", "NOT_FOUND"));

        // VALIDAR STATUS ENTRADA
        if (status == null || status.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status es obligatorio", "BAD_REQUEST");
        }

        if (!status.equals("APROBADA") && !status.equals("RECHAZADA")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status inválido", "BAD_REQUEST");
        }

        // VALIDAR TRANSICION
        if (!"PENDIENTE".equals(solicitud.getStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Solo solicitudes pendientes pueden cambiar estado",
                    "INVALID_STATE");
        }

        solicitud.setStatus(status);
        solicitud.setDecisionComment(comment);

        return repository.save(solicitud);
    }

    // RESUMEN
    public SummaryResponse getSummary(String employeeId, int year) {

        // VALIDACIONES (400)
        if (employeeId == null || employeeId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "employeeId es obligatorio", "BAD_REQUEST");
        }

        if (year <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "year inválido", "BAD_REQUEST");
        }

        var solicitudes = repository.findByEmployeeAndYear(employeeId, year);

        BigDecimal vacaciones = BigDecimal.ZERO;
        BigDecimal permisos = BigDecimal.ZERO;

        for (Solicitud s : solicitudes) {

            // solo contar aprobadas
            if (!"APROBADA".equals(s.getStatus())) continue;

            if ("VACACIONES".equals(s.getType())) {
                vacaciones = vacaciones.add(s.getDaysRequested());
            }

            if ("PERMISO".equals(s.getType())) {
                permisos = permisos.add(s.getDaysRequested());
            }
        }

        return new SummaryResponse(employeeId, year, vacaciones, permisos);
    }

    // CALCULO DE DIAS
    private BigDecimal calculateDays(String type, LocalDate start, LocalDate end) {

        int days = 0;

        LocalDate date = start;

        while (!date.isAfter(end)) {

            if ("VACACIONES".equals(type)) {
                // Solo lunes a viernes
                if (!(date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                        date.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                    days++;
                }
            } else {
                // PERMISO = todos los días
                days++;
            }

            date = date.plusDays(1);
        }

        return BigDecimal.valueOf(days);
    }

    // DTO
    public record SummaryResponse(
            String employeeId,
            int year,
            BigDecimal vacacionesUsed,
            BigDecimal permisoUsed
    ) {}
}