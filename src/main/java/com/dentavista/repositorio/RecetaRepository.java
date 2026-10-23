package com.dentavista.repositorio;

import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Receta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    List<Receta> findByPacienteOrderByFechaDesc(Paciente paciente);

}
