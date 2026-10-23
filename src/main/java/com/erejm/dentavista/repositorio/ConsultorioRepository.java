package com.erejm.dentavista.repositorio;

import com.erejm.dentavista.modelo.Consultorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {

    Optional<Consultorio> findFirstByOrderByIdAsc();
}
