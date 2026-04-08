package com.prueba.solicitudesservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String type; // VACACIONES | PERMISO

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "days_requested", nullable = false)
    private BigDecimal daysRequested;

    @Column(nullable = false)
    private String status; // PENDIENTE | APROBADA | RECHAZADA

    @Column(name = "decision_comment")
    private String decisionComment;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}