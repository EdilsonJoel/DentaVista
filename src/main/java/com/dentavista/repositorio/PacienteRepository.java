package com.dentavista.repositorio;

import com.dentavista.modelo.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByDni(String dni);

    long countByActivoTrue();

    @Query("""
            select p from Paciente p
            where p.activo = :activo and (
                  :texto = ''
               or lower(p.nombres) like lower(concat('%', :texto, '%'))
               or lower(p.apellidos) like lower(concat('%', :texto, '%'))
               or lower(concat(p.nombres, ' ', p.apellidos)) like lower(concat('%', :texto, '%'))
               or p.dni like concat('%', :texto, '%'))
            order by p.apellidos, p.nombres
            """)
    List<Paciente> buscar(@Param("texto") String texto, @Param("activo") boolean activo);
}
