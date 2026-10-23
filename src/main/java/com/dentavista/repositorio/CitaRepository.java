package com.dentavista.repositorio;

import com.dentavista.modelo.Cita;
import com.dentavista.modelo.EstadoCita;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Usuario;
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
