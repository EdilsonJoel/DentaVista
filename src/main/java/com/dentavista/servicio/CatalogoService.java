package com.dentavista.servicio;

import com.dentavista.modelo.Tratamiento;
import com.dentavista.repositorio.TratamientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo de tratamientos y precios del consultorio.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CatalogoService {

    private final TratamientoRepository tratamientos;
    private final SesionService sesion;
    private final Validacion validacion;

    @Transactional(readOnly = true)
    public List<Tratamiento> activos() {
        return tratamientos.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Tratamiento> todos() {
        return tratamientos.findAllByOrderByNombreAsc();
    }

    public Tratamiento guardar(Tratamiento t) {
        sesion.exigirAdministrador();
        t.setCodigo(t.getCodigo() == null ? null : t.getCodigo().trim().toUpperCase());
        validacion.validar(t);
        tratamientos.findByCodigo(t.getCodigo())
                .filter(o -> !o.getId().equals(t.getId()))
                .ifPresent(o -> {
                    throw new NegocioException("El código " + t.getCodigo() + " ya lo usa \"" + o.getNombre() + "\".");
                });
        return tratamientos.save(t);
    }
}
