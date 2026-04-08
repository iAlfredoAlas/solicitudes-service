package com.prueba.solicitudesservice.repository;

import com.prueba.solicitudesservice.entity.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    // Listar por empleado
    Page<Solicitud> findByEmployeeId(String employeeId, Pageable pageable);

    // Listar por empleado y status
    Page<Solicitud> findByEmployeeIdAndStatus(String employeeId, String status, Pageable pageable);

    // VALIDACION DE TRASLAPE
    @Query("""
        SELECT COUNT(s) > 0 FROM Solicitud s
        WHERE s.employeeId = :employeeId
        AND (
            s.startDate <= :endDate AND s.endDate >= :startDate
        )
    """)
    boolean existsOverlappingRequest(String employeeId,
                                     java.time.LocalDate startDate,
                                     java.time.LocalDate endDate);

}