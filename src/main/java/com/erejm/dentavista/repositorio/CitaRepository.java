package com.erejm.dentavista.repositorio;

import com.erejm.dentavista.modelo.Cita;
import com.erejm.dentavista.modelo.EstadoCita;
import com.erejm.dentavista.modelo.Paciente;
import com.erejm.dentavista.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByInicioBetweenOrderByInicioAsc(LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findByOdontologoAndInicioBetweenOrderByInicioAsc(Usuario odontologo, LocalDateTime desde, LocalDateTime hasta);

    List<Cita> findByPacienteOrderByInicioDesc(Paciente paciente);

    List<Cita> findByPacienteAndInicioBetween(Paciente paciente, LocalDateTime desde, LocalDateTime hasta);

    long countByEstado(EstadoCita estado);
}
