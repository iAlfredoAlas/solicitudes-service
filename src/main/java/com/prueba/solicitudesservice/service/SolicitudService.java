package com.prueba.solicitudesservice.service;

import com.prueba.solicitudesservice.entity.Solicitud;
import com.prueba.solicitudesservice.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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

        // Validar fechas
        if (solicitud.getEndDate().isBefore(solicitud.getStartDate())) {
            throw new RuntimeException("endDate no puede ser menor que startDate");
        }

        // Validar traslape
        boolean overlap = repository.existsOverlappingRequest(
                solicitud.getEmployeeId(),
                solicitud.getStartDate(),
                solicitud.getEndDate()
        );

        if (overlap) {
            throw new RuntimeException("Ya existe una solicitud en ese rango de fechas");
        }

        // Calcular días
        BigDecimal days = calculateDays(solicitud.getType(),
                solicitud.getStartDate(),
                solicitud.getEndDate());

        solicitud.setDaysRequested(days);

        // Validar límites
        if ("VACACIONES".equals(solicitud.getType()) && days.intValue() > 10) {
            throw new RuntimeException("Vacaciones exceden máximo permitido");
        }

        if ("PERMISO".equals(solicitud.getType()) && days.intValue() > 5) {
            throw new RuntimeException("Permiso excede máximo permitido");
        }

        // Set estado inicial
        solicitud.setStatus("PENDIENTE");

        // Fecha creación
        solicitud.setCreatedAt(LocalDateTime.now());

        return repository.save(solicitud);
    }

    // LISTAR
    public Page<Solicitud> findByEmployee(String employeeId, String status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("startDate").descending());

        if (status != null && !status.isEmpty()) {
            return repository.findByEmployeeIdAndStatus(employeeId, status, pageable);
        }

        return repository.findByEmployeeId(employeeId, pageable);
    }

    // APROBAR / RECHAZAR
    public Solicitud decision(Long id, String status, String comment) {

        Solicitud solicitud = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if (!"PENDIENTE".equals(solicitud.getStatus())) {
            throw new RuntimeException("Solo solicitudes pendientes pueden cambiar estado");
        }

        solicitud.setStatus(status);
        solicitud.setDecisionComment(comment);

        return repository.save(solicitud);
    }

    // RESUMEN
    public SummaryResponse getSummary(String employeeId, int year) {

        var solicitudes = repository.findAll().stream()
                .filter(s -> s.getEmployeeId().equals(employeeId))
                .filter(s -> s.getStartDate().getYear() == year)
                .toList();

        BigDecimal vacaciones = BigDecimal.ZERO;
        BigDecimal permisos = BigDecimal.ZERO;

        for (Solicitud s : solicitudes) {
            if ("VACACIONES".equals(s.getType())) {
                vacaciones = vacaciones.add(s.getDaysRequested());
            } else if ("PERMISO".equals(s.getType())) {
                permisos = permisos.add(s.getDaysRequested());
            }
        }

        return new SummaryResponse(employeeId, year, vacaciones, permisos);
    }

    // CÁLCULO DE DÍAS
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