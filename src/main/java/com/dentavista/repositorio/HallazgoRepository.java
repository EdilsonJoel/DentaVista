package com.dentavista.repositorio;

import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.Hallazgo;
import com.dentavista.modelo.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface HallazgoRepository extends JpaRepository<Hallazgo, Long> {

    List<Hallazgo> findByPacienteOrderByFechaAscIdAsc(Paciente paciente);

    List<Hallazgo> findByPacienteAndPiezaOrderByFechaDescIdDesc(Paciente paciente, int pieza);

    List<Hallazgo> findByConsultaOrderByIdAsc(Consulta consulta);

    Optional<Hallazgo> findFirstByPacienteAndConsultaOrderByIdDesc(Paciente paciente, Consulta consulta);

}
