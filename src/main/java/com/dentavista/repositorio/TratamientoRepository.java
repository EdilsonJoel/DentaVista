package com.dentavista.repositorio;

import com.dentavista.modelo.Tratamiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface TratamientoRepository extends JpaRepository<Tratamiento, Long> {

    Optional<Tratamiento> findByCodigo(String codigo);

    List<Tratamiento> findByActivoTrueOrderByNombreAsc();

    List<Tratamiento> findAllByOrderByNombreAsc();
}
