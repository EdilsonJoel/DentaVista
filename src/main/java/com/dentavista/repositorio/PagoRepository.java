package com.dentavista.repositorio;

import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByPacienteOrderByFechaDesc(Paciente paciente);

    long countByReciboStartingWith(String prefijo);

    @Query("select coalesce(sum(p.monto), 0) from Pago p where p.paciente = :paciente and p.anulado = false")
    BigDecimal pagadoPor(@Param("paciente") Paciente paciente);

    @Query("select coalesce(sum(p.monto), 0) from Pago p where p.anulado = false and p.fecha between :desde and :hasta")
    BigDecimal cobradoEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(p) from Pago p where p.anulado = false and p.fecha between :desde and :hasta")
    long pagosEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    /** {pacienteId, total pagado} de todos los pacientes con pagos. */
    @Query("select p.paciente.id, sum(p.monto) from Pago p where p.anulado = false group by p.paciente.id")
    List<Object[]> pagadoPorPaciente();
}
