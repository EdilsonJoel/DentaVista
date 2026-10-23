package com.dentavista.repositorio;

import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** @author EREJM (Everson Ricardo · Edilson Joel · Michael) */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsuarioIgnoreCase(String usuario);

    long countByRolAndActivoTrue(Rol rol);

    List<Usuario> findAllByOrderByNombreAsc();

    List<Usuario> findByRolAndActivoTrueOrderByNombreAsc(Rol rol);

    List<Usuario> findByRolNotOrderByNombreAsc(Rol rol);

    Optional<Usuario> findByPaciente(com.dentavista.modelo.Paciente paciente);
}
