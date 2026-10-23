package com.dentavista.repositorio;

import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByPacienteOrderByFechaDesc(Paciente paciente);

    List<Consulta> findByPacienteOrderByFechaAsc(Paciente paciente);

    Optional<Consulta> findFirstByPacienteAndFechaBetweenOrderByFechaDesc(
            Paciente paciente, LocalDateTime desde, LocalDateTime hasta);

    Optional<Consulta> findFirstByPacienteOrderByFechaDesc(Paciente paciente);

    long countByFechaBetween(LocalDateTime desde, LocalDateTime hasta);

    List<Consulta> findTop8ByOrderByFechaDesc();
}
