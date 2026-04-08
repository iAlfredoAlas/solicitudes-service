package com.prueba.solicitudesservice.controller;

import com.prueba.solicitudesservice.entity.Solicitud;
import com.prueba.solicitudesservice.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService service;

    //  CREAR
    @PostMapping
    public ResponseEntity<Solicitud> create(@RequestBody Solicitud solicitud) {
        Solicitud created = service.create(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    //  LISTAR
    @GetMapping
    public ResponseEntity<Page<Solicitud>> list(
            @RequestParam String employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Solicitud> result = service.findByEmployee(employeeId, status, page, size);
        return ResponseEntity.ok(result);
    }

    //  APROBAR / RECHAZAR
    @PatchMapping("/{id}/decision")
    public Solicitud decision(
            @PathVariable Long id,
            @RequestBody DecisionRequest request
    ) {
        return service.decision(id, request.status(), request.decisionComment());
    }

    //  RESUMEN
    @GetMapping("/summary")
    public SolicitudService.SummaryResponse summary(
            @RequestParam String employeeId,
            @RequestParam int year
    ) {
        return service.getSummary(employeeId, year);
    }

    //  DTO
    public record DecisionRequest(
            String status,
            String decisionComment
    ) {}
}