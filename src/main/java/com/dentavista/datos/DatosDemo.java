package com.dentavista.datos;

import com.dentavista.dominio.EstadoDental;
import com.dentavista.dominio.Odontograma;
import com.dentavista.dominio.Pieza;
import com.dentavista.dominio.Superficie;
import com.dentavista.modelo.Consulta;
import com.dentavista.modelo.EstadoItem;
import com.dentavista.modelo.ItemPlan;
import com.dentavista.modelo.Paciente;
import com.dentavista.modelo.Rol;
import com.dentavista.modelo.Tratamiento;
import com.dentavista.modelo.Usuario;
import com.dentavista.repositorio.ConsultaRepository;
import com.dentavista.repositorio.PacienteRepository;
import com.dentavista.repositorio.TratamientoRepository;
import com.dentavista.repositorio.UsuarioRepository;
import com.dentavista.servicio.ClinicaService;
import com.dentavista.servicio.PlanService;
import com.dentavista.servicio.Seguridad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static com.dentavista.dominio.EstadoDental.*;
import static com.dentavista.dominio.Superficie.*;

/**
 * Datos iniciales y de ejemplo.
 * <ul>
 *   <li><b>Catálogo base</b> de tratamientos con precios: siempre se crea al configurar
 *       el consultorio (luego el administrador ajusta los precios).</li>
 *   <li><b>Pacientes de ejemplo</b> (opcional, desde el asistente de primer uso): personal
 *       de ejemplo y 12 pacientes con historia clínica real de varios meses, citas, pagos y
 *       recetas, para presentar el sistema. Las fechas son relativas a hoy.</li>
 * </ul>
 * Con {@code app.demo.enabled=true} una base vacía se llena sola (pruebas y capturas).
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatosDemo implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final TratamientoRepository tratamientos;
    private final ConsultaRepository consultas;
    private final ClinicaService clinica;
    private final PlanService plan;
    private final TransactionTemplate tx;
    private final com.dentavista.repositorio.CitaRepository citas;
    private final com.dentavista.repositorio.ItemPlanRepository itemsPlan;
    private final com.dentavista.servicio.PagoService pagosService;
    private final com.dentavista.repositorio.PagoRepository pagosRepo;
    private final com.dentavista.servicio.RecetaService recetasService;
    private final com.dentavista.servicio.DatosClinica datosClinica;

    @org.springframework.beans.factory.annotation.Value("${app.demo.enabled:false}")
    private boolean demoAutomatica;

    private final Map<String, Tratamiento> catalogo = new TreeMap<>();
    private Usuario draParedes;
    private Usuario drMamani;

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) {
            if (!datosClinica.configurado()) {
                // Bases de versiones anteriores: el consultorio estaba en application.properties.
                tx.executeWithoutResult(s -> datosClinica.guardar(consultorioDeEjemplo()));
            }
            completarVersionesAnteriores();
            return;
        }
        if (!demoAutomatica) {
            return; // producto: la primera vez se abre el asistente de configuración
        }
        tx.executeWithoutResult(s -> {
            datosClinica.guardar(consultorioDeEjemplo());
            crearUsuarios(true);
            crearCatalogo();
            crearPacientes();
            crearAccesoPaciente();
            crearCitas();
            crearPagosYRecetas();
        });
        log.info("Datos de demostración cargados: {} pacientes", pacientes.count());
    }

    /**
     * Bases creadas con una versión anterior: se agregan solo los módulos nuevos
     * (acceso del paciente, agenda, pagos y recetas) sin tocar lo que el usuario cargó.
     */
    private void completarVersionesAnteriores() {
        if (pacientes.findByDni("72845163").isEmpty()) {
            return; // no es la base de demostración
        }
        tx.executeWithoutResult(s -> {
            draParedes = usuarios.findByUsuarioIgnoreCase("dra.paredes").orElse(null);
            drMamani = usuarios.findByUsuarioIgnoreCase("dr.mamani").orElse(null);
            if (draParedes == null || drMamani == null) {
                return;
            }
            if (usuarios.findByUsuarioIgnoreCase("72845163").isEmpty()) {
                crearAccesoPaciente();
            }
            if (citas.count() == 0) {
                crearCitas();
            }
            if (pagosRepo.count() == 0) {
                crearPagosYRecetas();
                log.info("Demostración actualizada con pagos y recetas");
            }
        });
    }

    // --------------------------------------------------------------- usuarios

    /** Catálogo base de tratamientos (solo si aún no hay ninguno). */
    public void cargarCatalogoBase() {
        if (tratamientos.count() == 0) {
            tx.executeWithoutResult(s -> crearCatalogo());
        }
    }

    /** Personal y pacientes de ejemplo, para conocer DentaVista (asistente de primer uso). */
    public void cargarEjemplos() {
        tx.executeWithoutResult(s -> {
            if (catalogo.isEmpty()) {
                tratamientos.findAll().forEach(t -> catalogo.put(t.getCodigo(), t));
            }
            crearUsuarios(false);
            crearPacientes();
            crearAccesoPaciente();
            crearCitas();
            crearPagosYRecetas();
        });
        log.info("Pacientes de ejemplo cargados: {}", pacientes.count());
    }

    private static com.dentavista.modelo.Consultorio consultorioDeEjemplo() {
        return com.dentavista.modelo.Consultorio.builder()
                .nombre("Clínica Dental Sonrisa Andina")
                .ruc("20605418731")
                .direccion("Jr. Lima 452, Juliaca - Puno")
                .telefono("(051) 32-4580")
                .email("citas@sonrisaandina.pe")
                .build();
    }

    private void crearUsuarios(boolean conAdministrador) {
        if (conAdministrador) {
            usuarios.save(usuario("admin", "admin123", "Administrador del sistema", null, Rol.ADMIN));
        }
        draParedes = usuarios.save(usuario("dra.paredes", "dentista123", "Dra. Lucía Paredes Quispe", "COP 28417", Rol.ODONTOLOGO));
        drMamani = usuarios.save(usuario("dr.mamani", "dentista123", "Dr. Rubén Mamani Condori", "COP 30552", Rol.ODONTOLOGO));
        usuarios.save(usuario("recepcion", "recepcion123", "Milagros Apaza Ticona", null, Rol.ASISTENTE));
    }

    private static Usuario usuario(String login, String clave, String nombre, String cop, Rol rol) {
        return Usuario.builder()
                .usuario(login)
                .contrasena(Seguridad.hashear(clave))
                .nombre(nombre)
                .colegiatura(cop)
                .email(login.replace(".", "") + "@sonrisaandina.pe")
                .rol(rol)
                .activo(true)
                .build();
    }

    // ------------------------------------------------------- acceso y agenda

    /** La paciente de la demo puede entrar con su DNI y ver su propio odontograma. */
    private void crearAccesoPaciente() {
        Paciente valeria = pacientes.findByDni("72845163").orElseThrow();
        usuarios.save(Usuario.builder()
                .usuario("72845163")
                .contrasena(Seguridad.hashear("paciente123"))
                .nombre(valeria.nombreCompleto())
                .email(valeria.getEmail())
                .rol(Rol.PACIENTE)
                .paciente(valeria)
                .activo(true)
                .build());
    }

    /**
     * Agenda de dos semanas: citas atendidas en los días pasados, confirmadas en los
     * próximos y algunas solicitudes de pacientes por confirmar.
     */
    private void crearCitas() {
        Random r = new Random(1510);
        List<Paciente> todos = pacientes.findAll();
        Paciente valeria = pacientes.findByDni("72845163").orElseThrow();
        Usuario[] doctores = {draParedes, drMamani};
        java.time.LocalTime[] manana = {java.time.LocalTime.of(8, 30), java.time.LocalTime.of(9, 30),
                java.time.LocalTime.of(10, 30), java.time.LocalTime.of(11, 30), java.time.LocalTime.of(12, 0)};
        java.time.LocalTime[] tarde = {java.time.LocalTime.of(15, 30), java.time.LocalTime.of(16, 30), java.time.LocalTime.of(17, 30)};
        String[] motivos = {"Control semestral", "Dolor en molar", "Limpieza", "Revisión de restauración", "Sensibilidad al frío"};
        LocalDateTime ahora = LocalDateTime.now();
        int solicitadas = 0;
        for (int dia = -4; dia <= 9; dia++) {
            LocalDate fecha = LocalDate.now().plusDays(dia);
            if (fecha.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                continue;
            }
            java.util.Set<Long> ocupados = new java.util.HashSet<>();
            for (Usuario doc : doctores) {
                List<java.time.LocalTime> horas = new ArrayList<>(List.of(manana));
                if (fecha.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
                    horas.addAll(List.of(tarde));
                }
                java.util.Collections.shuffle(horas, r);
                int n = 2 + r.nextInt(3);
                for (java.time.LocalTime h : horas.subList(0, Math.min(n, horas.size()))) {
                    Paciente p = todos.get(r.nextInt(todos.size()));
                    if (p.equals(valeria) || !ocupados.add(p.getId())) {
                        continue;
                    }
                    LocalDateTime inicio = fecha.atTime(h);
                    // Quien ya terminó su plan no tiene citas por venir: queda "para dar de alta".
                    if (inicio.isAfter(ahora) && itemsPlan.findByPacienteAndEstado(p, EstadoItem.PENDIENTE).isEmpty()) {
                        continue;
                    }
                    ItemPlan item = r.nextBoolean() ? itemsPlan.findByPacienteAndEstado(p, EstadoItem.PENDIENTE).stream()
                            .findFirst().orElse(null) : null;
                    com.dentavista.modelo.EstadoCita estado;
                    if (inicio.isBefore(ahora)) {
                        estado = r.nextInt(10) == 0 ? com.dentavista.modelo.EstadoCita.NO_ASISTIO
                                : com.dentavista.modelo.EstadoCita.ATENDIDA;
                    } else if (solicitadas < 2 && dia >= 2 && r.nextBoolean()) {
                        estado = com.dentavista.modelo.EstadoCita.SOLICITADA;
                        solicitadas++;
                    } else {
                        estado = com.dentavista.modelo.EstadoCita.CONFIRMADA;
                    }
                    cita(p, doc, inicio, h.getHour() == 12 ? 30 : (item != null && item.getPrecio().intValue() > 300 ? 60 : 30),
                            item == null ? motivos[r.nextInt(motivos.length)] : null, item, estado,
                            estado == com.dentavista.modelo.EstadoCita.SOLICITADA ? p.nombreCompleto() + " (portal)" : "Milagros Apaza Ticona");
                }
            }
        }
        // Valeria: su última consulta y su próxima cita (para el portal del paciente).
        LocalDate proximo = LocalDate.now().plusDays(2);
        if (proximo.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            proximo = proximo.plusDays(1);
        }
        ItemPlan pendiente = itemsPlan.findByPacienteAndEstado(valeria, EstadoItem.PENDIENTE).stream()
                .filter(i -> i.getPieza() != null).findFirst().orElse(null);
        cita(valeria, draParedes, proximo.atTime(9, 0), 30, null, pendiente, com.dentavista.modelo.EstadoCita.CONFIRMADA,
                "Milagros Apaza Ticona");
        cita(valeria, draParedes, LocalDate.now().minusDays(6).atTime(10, 0), 30, "Control: sensibilidad al frío", null,
                com.dentavista.modelo.EstadoCita.ATENDIDA, "Valeria Quispe Huanca (portal)");
    }

    /**
     * Caja y recetas de la demo: la mayoría de pacientes pagó parte de lo realizado
     * (unos deben, otros pagaron por adelantado) y algunas consultas tienen receta.
     */
    private void crearPagosYRecetas() {
        Random r = new Random(77);
        com.dentavista.modelo.MedioPago[] medios = com.dentavista.modelo.MedioPago.values();
        for (Paciente p : pacientes.findAll()) {
            java.math.BigDecimal realizado = itemsPlan.findByPacienteAndEstado(p, EstadoItem.REALIZADO).stream()
                    .map(ItemPlan::getPrecio).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            if (realizado.signum() == 0) {
                continue;
            }
            // Quien terminó todo su plan lo pagó completo (queda "para dar de alta");
            // el resto, entre el 60 % y el 100 % de lo realizado, en dos abonos.
            boolean terminado = itemsPlan.findByPacienteAndEstado(p, EstadoItem.PENDIENTE).isEmpty();
            double fraccion = terminado ? 1.0 : 0.6 + r.nextInt(5) * 0.1;
            java.math.BigDecimal total = realizado.multiply(java.math.BigDecimal.valueOf(fraccion))
                    .setScale(0, java.math.RoundingMode.DOWN);
            java.math.BigDecimal primero = total.multiply(new java.math.BigDecimal("0.6")).setScale(0, java.math.RoundingMode.DOWN);
            List<Consulta> cs = consultas.findByPacienteOrderByFechaAsc(p);
            LocalDateTime f1 = cs.get(0).getFecha().plusHours(1);
            LocalDateTime f2 = cs.get(cs.size() - 1).getFecha().plusHours(1);
            pago(p, primero, medios[r.nextInt(medios.length)], f1, "Primer abono");
            pago(p, total.subtract(primero), medios[r.nextInt(medios.length)], f2, null);
        }
        // Recetas en consultas con endodoncia, extracción o dolor.
        receta(pacientes.findByDni("72845163").orElseThrow(), "Endodoncia pieza 46",
                List.of(new com.dentavista.modelo.Receta.Item("Clindamicina 300 mg", "1 cápsula cada 8 horas por 7 días"),
                        new com.dentavista.modelo.Receta.Item("Paracetamol 500 mg", "1 tableta cada 8 horas si hay dolor")),
                "Alérgica a penicilina: se indica clindamicina. Evitar masticar del lado tratado 24 horas.");
        receta(pacientes.findByDni("09214587").orElseThrow(), "Extracción 17",
                List.of(new com.dentavista.modelo.Receta.Item("Amoxicilina 500 mg", "1 cápsula cada 8 horas por 7 días"),
                        new com.dentavista.modelo.Receta.Item("Paracetamol 500 mg", "1 tableta cada 8 horas por 3 días"),
                        new com.dentavista.modelo.Receta.Item("Clorhexidina 0.12 % enjuague bucal",
                                "Enjuagar 15 ml por 30 segundos, 2 veces al día por 10 días")),
                "Paciente anticoagulado: se evita ibuprofeno. Compresas frías 24 horas, dieta blanda.");
    }

    private void pago(Paciente p, java.math.BigDecimal monto, com.dentavista.modelo.MedioPago medio, LocalDateTime fecha,
                      String nota) {
        if (monto.signum() <= 0) {
            return;
        }
        pagosService.guardar(com.dentavista.modelo.Pago.builder().paciente(p).monto(monto).medio(medio).fecha(fecha)
                .nota(nota).registradoPor("Milagros Apaza Ticona").build());
    }

    /** Receta en la consulta cuyo motivo coincide (la de la endodoncia, la de la extracción…). */
    private void receta(Paciente p, String motivo, List<com.dentavista.modelo.Receta.Item> items, String indicaciones) {
        consultas.findByPacienteOrderByFechaAsc(p).stream()
                .filter(c -> c.getMotivo() != null && c.getMotivo().contains(motivo))
                .findFirst()
                .ifPresent(c -> recetasService.guardar(com.dentavista.modelo.Receta.builder()
                        .paciente(p).odontologo(c.getOdontologo()).consulta(c).fecha(c.getFecha().plusMinutes(40))
                        .items(new ArrayList<>(items)).indicaciones(indicaciones).build()));
    }

    private void cita(Paciente p, Usuario doc, LocalDateTime inicio, int duracion, String motivo, ItemPlan item,
                      com.dentavista.modelo.EstadoCita estado, String creadaPor) {
        citas.save(com.dentavista.modelo.Cita.builder()
                .paciente(p).odontologo(doc).inicio(inicio).duracion(duracion).motivo(motivo).itemPlan(item)
                .estado(estado).creadaPor(creadaPor).creadaEn(inicio.minusDays(3)).build());
    }

    // --------------------------------------------------------------- catalogo

    private void crearCatalogo() {
        tratamiento("PRO", "Profilaxis y destartraje", "Limpieza profesional con ultrasonido y pulido", "120", null, false, false);
        tratamiento("OBT", "Obturación con resina", "Restauración estética por cara afectada", "90", OBTURADO, true, true);
        tratamiento("SEL", "Sellante de fosas y fisuras", "Prevención de caries en caras oclusales", "45", SELLANTE, true, true);
        tratamiento("REC", "Reconstrucción por fractura", "Reconstrucción con resina de alta resistencia", "150", OBTURADO, true, true);
        tratamiento("END", "Endodoncia", "Tratamiento de conductos (unirradicular / multirradicular)", "480", ENDODONCIA, false, true);
        tratamiento("EXO", "Extracción simple", "Exodoncia con anestesia local", "150", AUSENTE, false, true);
        tratamiento("COR", "Corona de porcelana", "Corona metal-porcelana o zirconio", "950", CORONA, false, true);
        tratamiento("IMP", "Implante dental", "Implante de titanio con corona", "3200", IMPLANTE, false, true);
        tratamiento("RXP", "Radiografía periapical", "Radiografía digital de una pieza", "30", null, false, true);
        tratamiento("BLA", "Blanqueamiento dental", "Blanqueamiento en consultorio (2 sesiones)", "600", null, false, false);
    }

    private void tratamiento(String codigo, String nombre, String desc, String precio, EstadoDental resultado,
                             boolean porCara, boolean requierePieza) {
        catalogo.put(codigo, tratamientos.save(Tratamiento.builder()
                .codigo(codigo)
                .nombre(nombre)
                .descripcion(desc)
                .precio(new BigDecimal(precio))
                .estadoResultante(resultado)
                .porCara(porCara)
                .requierePieza(requierePieza)
                .activo(true)
                .build()));
    }

    // --------------------------------------------------------------- pacientes

    private void crearPacientes() {
        valeria();
        Random r = new Random(20261014);
        aleatorio(r, "41258963", "Jorge Luis", "Mamani Ccama", 1980, "M", null, "Hipertensión arterial controlada (losartán)", drMamani);
        aleatorio(r, "70125478", "Ana Lucía", "Flores Torres", 1995, "F", null, null, draParedes);
        carlos();
        aleatorio(r, "43698521", "Rosa Elena", "Apaza Ticona", 1987, "F", null, "Diabetes tipo 2 (metformina)", draParedes);
        aleatorio(r, "74521036", "Diego", "Fernández Rojas", 2003, "M", null, null, drMamani);
        aleatorio(r, "78963210", "Lucía", "Huamán Choque", 2010, "F", null, null, draParedes);
        aleatorio(r, "29874563", "Miguel Ángel", "Ccallo Yupanqui", 1973, "M", "Látex", null, drMamani);
        aleatorio(r, "72036985", "Sofía", "Ramírez Vilca", 1998, "F", "Ibuprofeno", null, draParedes);
        aleatorio(r, "45632178", "Fernando", "Tito Calla", 1991, "M", null, null, drMamani);
        aleatorio(r, "47125896", "Gabriela", "Choque Mendoza", 1985, "F", null, "Embarazo 2.º trimestre", draParedes);
        aleatorio(r, "71458236", "Renzo", "Vargas Pilco", 1999, "M", null, null, drMamani);
    }

    private Paciente paciente(String dni, String nombres, String apellidos, LocalDate nacimiento, String sexo,
                              String alergias, String antecedentes, String telefono) {
        return pacientes.save(Paciente.builder()
                .dni(dni)
                .nombres(nombres)
                .apellidos(apellidos)
                .fechaNacimiento(nacimiento)
                .sexo(sexo)
                .telefono(telefono)
                .email(nombres.split(" ")[0].toLowerCase()
                        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                        + "." + apellidos.split(" ")[0].toLowerCase().replace("á", "a").replace("é", "e") + "@gmail.com")
                .direccion("Juliaca, Puno")
                .alergias(alergias)
                .antecedentes(antecedentes)
                .fechaRegistro(LocalDateTime.now().minusDays(420))
                .activo(true)
                .build());
    }

    /** Paciente estrella de la demo: historia de un año con todos los tipos de hallazgo. */
    private void valeria() {
        Paciente p = paciente("72845163", "Valeria", "Quispe Huanca", LocalDate.of(1996, 4, 12), "F",
                "Penicilina", null, "951 284 736");

        Visita v1 = visita(p, draParedes, 390, "Primera consulta: dolor al masticar del lado derecho");
        v1.pieza(18, AUSENTE).pieza(28, AUSENTE).pieza(38, AUSENTE).pieza(48, AUSENTE)
                .cara(16, OCLUSAL, CARIES).cara(16, MESIAL, CARIES).cara(15, DISTAL, CARIES)
                .cara(26, OCLUSAL, CARIES).cara(36, OCLUSAL, CARIES)
                .pieza(46, ENDODONCIA_INDICADA).cara(21, OCLUSAL, FRACTURA)
                .realiza("PRO", null, null);
        visita(p, draParedes, 362, "Restauraciones cuadrante 1")
                .realiza("OBT", 16, OCLUSAL).realiza("OBT", 16, MESIAL).realiza("OBT", 15, DISTAL);
        visita(p, drMamani, 334, "Endodoncia pieza 46")
                .realiza("END", 46, PIEZA);
        visita(p, draParedes, 300, "Corona 46 y reconstrucción 21")
                .realiza("COR", 46, PIEZA).realiza("REC", 21, OCLUSAL);
        visita(p, draParedes, 152, "Control semestral")
                .cara(37, OCLUSAL, CARIES).cara(47, OCLUSAL, CARIES).cara(24, MESIAL, CARIES)
                .cara(35, OCLUSAL, SELLANTE).cara(45, OCLUSAL, SELLANTE)
                .realiza("OBT", 26, OCLUSAL).realiza("PRO", null, null);
        Visita v6 = visita(p, draParedes, 6, "Control: sensibilidad al frío");
        v6.cara(14, DISTAL, CARIES).realiza("OBT", 36, OCLUSAL);
        pendientesDesdeOdontograma(p, v6.fecha);
        plan.guardar(plan.nuevo(p, catalogo.get("BLA"), null, null, v6.fecha, "Solicitado por la paciente"));
    }

    /** Adulto mayor con piezas perdidas, implante y prótesis. */
    private void carlos() {
        Paciente p = paciente("09214587", "Carlos Alberto", "Condori Pari", LocalDate.of(1964, 9, 3), "M",
                null, "Anticoagulado (warfarina): coordinar con cardiología antes de extracciones", "954 120 877");
        Visita v1 = visita(p, drMamani, 410, "Evaluación integral y plan de rehabilitación");
        v1.pieza(18, AUSENTE).pieza(28, AUSENTE).pieza(38, AUSENTE).pieza(48, AUSENTE)
                .pieza(36, AUSENTE).pieza(46, AUSENTE).pieza(17, EXTRACCION_INDICADA)
                .pieza(26, ENDODONCIA_INDICADA).cara(25, MESIAL, CARIES).cara(14, VESTIBULAR, CARIES)
                .pieza(11, CORONA).pieza(21, CORONA).cara(34, OCLUSAL, OBTURADO).cara(44, OCLUSAL, OBTURADO)
                .realiza("PRO", null, null);
        visita(p, drMamani, 380, "Extracción 17").realiza("EXO", 17, PIEZA);
        visita(p, drMamani, 300, "Endodoncia 26").realiza("END", 26, PIEZA);
        visita(p, drMamani, 200, "Colocación de implante 46").realiza("IMP", 46, PIEZA);
        Visita v5 = visita(p, drMamani, 45, "Control de implante");
        v5.cara(25, MESIAL, OBTURADO).cara(13, VESTIBULAR, CARIES).realiza("PRO", null, null);
        pendientesDesdeOdontograma(p, v5.fecha);
        plan.guardar(plan.nuevo(p, catalogo.get("IMP"), 36, PIEZA, v5.fecha, "Rehabilitación de pieza perdida"));
        plan.guardar(plan.nuevo(p, catalogo.get("COR"), 26, PIEZA, v5.fecha, "Proteger pieza endodonciada"));
    }

    /** Historia verosímil generada con una semilla fija (siempre la misma). */
    private void aleatorio(Random r, String dni, String nombres, String apellidos, int anio, String sexo,
                           String alergias, String antecedentes, Usuario doctor) {
        Paciente p = paciente(dni, nombres, apellidos,
                LocalDate.of(anio, 1 + r.nextInt(12), 1 + r.nextInt(27)), sexo, alergias, antecedentes,
                "9" + (10 + r.nextInt(89)) + " " + (100 + r.nextInt(899)) + " " + (100 + r.nextInt(899)));
        int edad = LocalDate.now().getYear() - anio;
        int inicio = 120 + r.nextInt(300);
        int dias3 = 2 + r.nextInt(12);
        Visita v1 = visita(p, doctor, inicio, edad < 18 ? "Evaluación y prevención" : "Primera consulta");

        // Terceros molares: en adolescentes aún no erupcionan; en adultos, a veces extraídos.
        for (int m : new int[]{18, 28, 38, 48}) {
            if (edad < 18 || r.nextDouble() < 0.55) {
                v1.pieza(m, AUSENTE);
            }
        }
        List<int[]> caries = new ArrayList<>();
        int[] posteriores = {14, 15, 16, 17, 24, 25, 26, 27, 34, 35, 36, 37, 44, 45, 46, 47};
        int nCaries = 2 + r.nextInt(edad > 40 ? 5 : 4);
        for (int k = 0; k < nCaries; k++) {
            int pieza = posteriores[r.nextInt(posteriores.length)];
            Superficie cara = r.nextDouble() < 0.6 ? OCLUSAL : (r.nextBoolean() ? MESIAL : DISTAL);
            v1.cara(pieza, cara, CARIES);
            caries.add(new int[]{pieza, cara.ordinal()});
        }
        if (edad < 25) {
            v1.cara(36, OCLUSAL, SELLANTE).cara(46, OCLUSAL, SELLANTE);
        }
        if (edad > 40 && r.nextBoolean()) {
            v1.pieza(posteriores[r.nextInt(posteriores.length)], ENDODONCIA_INDICADA);
        }
        if (r.nextDouble() < 0.3) {
            v1.cara(11 + r.nextInt(2), OCLUSAL, FRACTURA);
        }
        v1.realiza("PRO", null, null);

        // Segunda visita: se obturan algunas caries.
        int dias2 = Math.max(30, inicio - 25 - r.nextInt(40));
        Visita v2 = visita(p, doctor, dias2, "Restauraciones");
        for (int k = 0; k < caries.size(); k++) {
            if (r.nextDouble() < 0.6) {
                int[] c = caries.get(k);
                v2.realizaSiPendiente("OBT", c[0], Superficie.values()[c[1]]);
            }
        }
        // Control reciente con algún hallazgo nuevo.
        if (r.nextDouble() < 0.7) {
            Visita v3 = visita(p, doctor, dias3, "Control");
            // En el control se resuelve alguna caries que quedó pendiente.
            for (int[] c : caries) {
                if (clinica.odontograma(p).pieza(c[0]).cara(Superficie.values()[c[1]]) == CARIES) {
                    v3.realiza("OBT", c[0], Superficie.values()[c[1]]);
                    break;
                }
            }
            v3.cara(posteriores[r.nextInt(posteriores.length)], OCLUSAL, CARIES);
            pendientesDesdeOdontograma(p, v3.fecha);
        } else {
            pendientesDesdeOdontograma(p, v2.fecha);
        }
    }

    // ------------------------------------------------------------ utilidades

    private Visita visita(Paciente p, Usuario doctor, int diasAtras, String motivo) {
        LocalDateTime fecha = LocalDate.now().minusDays(diasAtras)
                .atTime(LocalTime.of(9 + (diasAtras % 8), (diasAtras * 7) % 60));
        Consulta c = consultas.save(Consulta.builder()
                .paciente(p)
                .odontologo(doctor)
                .fecha(fecha)
                .motivo(motivo)
                .build());
        return new Visita(p, c, fecha);
    }

    /** Crea tratamientos pendientes para todas las patologías abiertas (como el botón "Generar plan"). */
    private void pendientesDesdeOdontograma(Paciente p, LocalDateTime fecha) {
        Odontograma o = clinica.odontograma(p);
        for (Odontograma.Patologia pat : o.patologias()) {
            Tratamiento t = catalogo.get(pat.estado().getTratamientoSugerido());
            if (t != null) {
                Superficie s = t.isPorCara() ? pat.superficie() : PIEZA;
                plan.guardar(plan.nuevo(p, t, pat.pieza(), s, fecha,
                        pat.estado().getEtiqueta() + " en " + pat.superficie().etiqueta(pat.pieza()).toLowerCase()));
            }
        }
    }

    /** Una visita: registra hallazgos y tratamientos realizados en orden, minuto a minuto. */
    private final class Visita {
        private final Paciente paciente;
        private final Consulta consulta;
        private final LocalDateTime fecha;
        private int minuto;

        private Visita(Paciente paciente, Consulta consulta, LocalDateTime fecha) {
            this.paciente = paciente;
            this.consulta = consulta;
            this.fecha = fecha;
        }

        private LocalDateTime siguiente() {
            return fecha.plusMinutes(minuto++);
        }

        Visita cara(int pieza, Superficie s, EstadoDental e) {
            Pieza.validar(pieza);
            clinica.registrarEn(paciente, consulta, siguiente(), pieza, s, e, null);
            return this;
        }

        Visita pieza(int pieza, EstadoDental e) {
            return cara(pieza, PIEZA, e);
        }

        Visita realiza(String codigo, Integer pieza, Superficie s) {
            Tratamiento t = catalogo.get(codigo);
            ItemPlan item = plan.guardar(plan.nuevo(paciente, t, pieza, pieza == null ? null : s, fecha.minusDays(2), null));
            plan.aplicarRealizacion(item, consulta, siguiente());
            return this;
        }

        /** Solo si la cara sigue con caries (el generador puede repetir una pieza). */
        Visita realizaSiPendiente(String codigo, int pieza, Superficie s) {
            EstadoDental actual = clinica.odontograma(paciente).pieza(pieza).cara(s);
            if (actual == CARIES) {
                realiza(codigo, pieza, s);
            }
            return this;
        }
    }
}
