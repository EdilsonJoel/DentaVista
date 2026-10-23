# DentaVista — Gestión clínica dental con odontograma 3D

**Equipo EREJM** · Everson Ricardo · Edilson Joel · Michael — Universidad Nacional de Juliaca (UNAJ)

> **Una sola idea, bien resuelta:** el odontólogo registra el estado de la boca del paciente
> haciendo clic **sobre la cara exacta de cada diente en un modelo 3D**, y el sistema
> convierte ese diagnóstico en un **plan de tratamiento con presupuesto** y en una
> **historia clínica que se puede recorrer en el tiempo**.

![Ficha del paciente](docs/capturas/03-odontograma.png)

## Descargar

**[⬇ Descargar el instalador para Windows](https://github.com/EdilsonJoel/DentaVista/releases/latest)**:
baja `DentaVista-2.1.0-Setup.exe`, ábrelo y sigue el asistente (crea el acceso directo en el escritorio).
Trae su propio Java, **no pide permisos de administrador** y se desinstala desde *Agregar o quitar programas*.
La primera vez se abre **Configurar consultorio**: registras los datos de la clínica y creas la cuenta del
administrador (con la opción de cargar pacientes de ejemplo para conocer el sistema).

> Si Windows muestra *"Windows protegió su PC"* (pasa con todo programa nuevo sin firma digital),
> pulsa **Más información → Ejecutar de todas formas**.

## El problema

En la mayoría de consultorios el odontograma se sigue llenando en papel o en planillas 2D
que el paciente no entiende: no sabe qué diente tiene caries, qué se le va a hacer ni
cuánto le va a costar. DentaVista gira alrededor de **ese** flujo (la agenda, los pagos y las
recetas existen solo para servirlo; no hay inventario, planillas ni contabilidad):

1. **Diagnosticar**: clic en la cara del diente (oclusal, vestibular, palatina/lingual,
   mesial, distal) sobre el modelo 3D o sobre el odontograma clínico 2D.
2. **Planificar**: el plan de tratamiento y el presupuesto se generan solos a partir de las
   patologías registradas (caries → obturación, extracción indicada → exodoncia…).
3. **Tratar**: al marcar un tratamiento como realizado, el odontograma se actualiza (la
   caries pasa a obturación, la pieza extraída queda ausente).
4. **Explicar**: línea de tiempo para ver la boca en cualquier consulta pasada, modo
   "abrir boca", giro automático y **pantalla completa** para mostrárselo al paciente, e informe
   clínico y presupuesto en PDF para entregar.

## Funcionalidades

| | |
|---|---|
| **Odontograma 3D** | 32 piezas con notación FDI, cada una dividida en sus 5 caras clínicas. Girar, acercar, desplazar, vistas frontal / oclusal superior / oclusal inferior / laterales, abrir la boca, enfocar una pieza, números flotantes, encías opcionales y **pantalla completa** (`F11`, `Esc` para salir) con la línea de tiempo incluida. |
| **Historial de una pieza** | Al seleccionar un diente, "Historial completo de la pieza" muestra su acercamiento 3D, su estado, lo realizado y lo pendiente (con importes) y la evolución consulta por consulta con el diagrama de sus caras tras cada una. Se exporta a **PDF**. La pestaña Historial también se filtra por pieza. |
| **Odontograma 2D** | El formato clínico estándar (siluetas + diagrama de cinco caras + símbolos de ausente, corona, endodoncia, implante), sincronizado con el 3D. |
| **Hallazgos** | Caries, fractura, obturación, sellante (por cara); endodoncia indicada / realizada, extracción indicada, corona, implante, ausente (pieza completa). **Doble clic en una cara = vuelve a sana** (en la silueta, restablece la pieza). Atajos `1`–`9`, `0` = sano, `Esc`, `Ctrl+Z` para deshacer. |
| **Validación clínica** | No deja marcar caries en una pieza ausente, ni una corona en una sola cara; el asistente de recepción solo puede consultar. |
| **Línea de tiempo** | Cada consulta es un punto; el botón ▶ reproduce la evolución del paciente consulta a consulta. |
| **Plan y presupuesto** | Generación automática desde el odontograma, tratamientos manuales, realizado / cancelado, avance del plan, precios congelados al presupuestar. |
| **Informe clínico (PDF)** | Datos y alertas del paciente, tres vistas 3D, odontograma clínico, hallazgos y consultas. Sin importes. |
| **Presupuesto (PDF)** | Una hoja: tratamientos con importes, realizado / por realizar, lo pagado y el saldo por pagar, con firmas. Sin imágenes, para que cada documento tenga un solo propósito. |
| **Agenda** | Calendario semanal (lunes a sábado) por odontólogo, con horario de atención y pausa; clic en un espacio libre para agendar. No permite cruces de horario. Cada cita puede ligarse a un tratamiento pendiente del plan; desde la cita se atiende al paciente y se abre su ficha. Una cita se puede **reprogramar** (otro día, hora u odontólogo) sin perder su tratamiento ni su motivo. |
| **Archivar pacientes** | Quien terminó su plan, lo pagó y no tiene citas aparece en **"Para dar de alta"**. Al archivarlo deja de aparecer en listas y avisos, se cancelan sus citas futuras y se desactiva su acceso; su historia clínica se conserva y se puede **reactivar**. No se archiva a quien todavía debe. |
| **Portal del paciente** | El paciente entra con su DNI, ve **su** odontograma 3D, su plan y presupuesto, **pide o reprograma citas** en los horarios libres (quedan "por confirmar" hasta que la recepción las confirma). El acceso se crea desde la ficha (botón de llave). |
| **Pagos y saldo** | Abonos en efectivo, Yape, Plin, tarjeta o transferencia. La app muestra cuánto debe el paciente por tratamientos ya realizados y cuánto lleva pagado del plan, y permite pagar por adelantado (nunca más que el total). Emite el recibo en PDF; un recibo mal hecho se anula, no se borra. |
| **Recordatorio por WhatsApp** | En cada cita, un botón abre WhatsApp con el mensaje listo: fecha, hora, odontólogo y tratamiento. |
| **Recetas** | Vademécum odontológico con la dosis habitual precargada. Cruza cada medicamento con las alergias (amoxicilina en alérgicos a la penicilina) y los antecedentes (AINE en anticoagulados o embarazadas) y exige confirmar las alertas. Emite la receta en PDF y queda en el historial de la consulta. |
| **Inicio** | Lo del día: citas de hoy, solicitudes por confirmar, lo que hay por cobrar (y a quién), pacientes con tratamientos por realizar sin cita (para llamarlos) y cuántos están para dar de alta. |
| **Formularios rápidos** | Máscaras de entrada: la fecha se escribe solo con números (`12051996` → 12/05/1996, también `120596`), el celular se formatea solo (951 284 736), el DNI acepta 8 dígitos, montos con 2 decimales, nombres con mayúscula inicial automática y búsqueda de paciente o pieza escribiendo parte del nombre. |
| **Primer uso** | Sin cuentas de prueba: la primera vez un asistente de dos pasos registra el consultorio (nombre, RUC, dirección, contacto: salen en todos los PDF) y la cuenta del administrador, con contraseña segura. El catálogo de tratamientos con precios se crea solo; los pacientes de ejemplo son opcionales. |
| **Cuentas del equipo** | El administrador crea cada cuenta en segundos: elige el rol (odontólogo, asistente, administrador, con lo que puede hacer cada uno), escribe el nombre y el usuario se sugiere solo (`lucia.paredes`). DentaVista genera una **contraseña temporal** para entregarla (con botón Copiar) y la persona crea la suya en su primer ingreso. También restablece contraseñas, desactiva cuentas y edita los datos del consultorio. Cada uno cambia su contraseña desde "Mi cuenta". |
| **Seguridad** | Contraseñas de 8+ caracteres con letras y números, guardadas con hash SHA-256 y sal; nunca se muestran ni se guardan en claro. El odontólogo debe registrar su colegiatura (COP), que aparece en recetas e informes. |
| **Extras** | Pantalla de carga inmediata al abrir, modo claro / oscuro (también el menú lateral), alertas médicas (alergias, anticoagulantes, embarazo) visibles en toda la ficha, roles (administrador, odontólogo, asistente, paciente). |

## Cómo ejecutar

Requisitos: **JDK 17 o superior** (probado con JDK 21). Maven viene incluido (`mvnw`).

```bash
./mvnw javafx:run
```

En Windows: `mvnw.cmd javafx:run`. Desde IntelliJ IDEA: abrir la carpeta como proyecto
Maven y ejecutar `com.dentavista.Launcher`.

La base de datos (SQLite) se crea sola en `%LOCALAPPDATA%\DentaVista`. La primera vez se abre el
asistente **Configurar consultorio** (datos de la clínica y cuenta del administrador); allí se puede
marcar *Cargar pacientes de ejemplo* para presentar el sistema con 12 pacientes con historia clínica de
hasta un año (fechas relativas a hoy), citas, pagos y recetas.

### Cuentas

No hay usuarios predeterminados. El administrador creado en el asistente entra a **Usuarios** y crea
la cuenta de cada persona del equipo; DentaVista le entrega una contraseña temporal que esa persona
cambia en su primer ingreso. El acceso de un paciente se crea desde su ficha (botón de llave).

Si se cargaron los ejemplos, existen además estas cuentas de ejemplo: `dra.paredes` / `dentista123`,
`dr.mamani` / `dentista123`, `recepcion` / `recepcion123` y la paciente `72845163` / `paciente123`.

### Pruebas

```bash
./mvnw test
```

65 pruebas: reglas del odontograma (dominio puro), reconocimiento del modelo 3D,
servicios clínicos, de plan, agenda (incluido reprogramar), pagos, recetas, archivo de pacientes e
historial de una pieza sobre SQLite real; primer uso (base vacía, sin cuentas de prueba), creación de
cuentas con contraseña temporal, seguridad y datos de ejemplo.

### Ejecutable de Windows

**Automático (recomendado):** al subir una etiqueta de versión, GitHub Actions
([`.github/workflows/publicar.yml`](.github/workflows/publicar.yml)) compila, corre las pruebas, arma el
ejecutable y lo publica en *Releases* listo para descargar:

```bash
git tag v2.1.0
git push origin v2.1.0
```

**Manual:**

```bash
./mvnw package -DskipTests
mkdir build\staging
copy target\dentavista-2.1.0.jar build\staging\
jpackage --type app-image --name DentaVista --input build/staging --main-jar dentavista-2.1.0.jar ^
  --icon build/icono.ico --dest dist --app-version 2.1.0 --vendor "DentaVista"
```

Genera `dist/DentaVista/DentaVista.exe` (incluye su propio Java; no requiere instalar nada). Para el
instalador, con [Inno Setup 6](https://jrsoftware.org/isinfo.php):

```bash
iscc /DVersion=2.1.0 build\instalador.iss
```

Genera `dist/DentaVista-2.1.0-Setup.exe`.

### ¿Y en el celular?

DentaVista es una aplicación de escritorio (JavaFX + Spring Boot + SQLite local): la base de datos vive
en la computadora del consultorio, por eso no se instala en Android ni iPhone. Funciona en Windows; en
macOS y Linux se ejecuta con `./mvnw javafx:run`.

## Arquitectura

```
com.dentavista
├── dominio      Lógica pura, sin Spring ni JavaFX: Pieza (FDI), Superficie,
│                EstadoDental y Odontograma (reconstrucción desde los registros)
├── modelo       Entidades JPA: Paciente, Consulta, Hallazgo, Tratamiento, ItemPlan, Usuario
├── repositorio  Spring Data JPA
├── servicio     ClinicaService, PlanService, PacienteService, CatalogoService,
│                UsuarioService, EstadisticaService, PdfService
├── tresd        Motor del odontograma 3D: Malla (OBJ + Catmull-Clark), ModeloDental
│                (reconocimiento de piezas y caras) y VisorDental3D (componente JavaFX)
├── ui           Componentes reutilizables: Odontograma2D, Notificador, Tema, Ui
├── vista        Pantallas (login y panel en FXML; ficha, plan, historial, etc.)
└── datos        Datos de demostración
```

Stack: **Java 17 · Spring Boot 3 · JPA/Hibernate · SQLite · JavaFX 21 (3D) · AtlantaFX ·
Ikonli · OpenPDF · JUnit 5**.

### Decisiones de diseño

- **El odontograma no se guarda, se reconstruye.** Solo se guardan los hallazgos con su
  fecha (`Hallazgo`); `Odontograma.reconstruir(registros, hasta)` los aplica en orden.
  Así la línea de tiempo es gratis y nunca se pierde información clínica (patrón
  *event sourcing*).
- **Del OBJ al modelo clínico, en Java puro** (`tresd.ModeloDental`):
  1. Se separa la malla en piezas conexas → 2 encías y 28 dientes.
  2. Cada diente se compara con su vecino en planta para saber si es superior o inferior;
     el lado da el cuadrante y la distancia al frente la posición → número FDI.
  3. Los terceros molares (18, 28, 38, 48) se generan a partir del segundo molar.
  4. Dos pasos de **subdivisión Catmull-Clark** convierten el modelo facetado en dientes lisos.
  5. Cada triángulo se clasifica en una de las 5 caras según hacia dónde mira respecto a la
     arcada (oclusal, vestibular, lingual, mesial, distal). Cada cara es una malla propia,
     por eso se puede pintar una caries solo en la cara mesial de un premolar.
- **Precio congelado**: el ítem del plan copia el precio del catálogo al crearse; cambiar
  precios no altera presupuestos ya entregados.
- **Errores de negocio legibles**: cada validación devuelve un mensaje que dice qué pasó y
  qué hacer, y en los formularios aparece dentro del diálogo sin perder lo escrito.

## Capturas

| | |
|---|---|
| ![Configurar consultorio](docs/capturas/13-configurar-consultorio.png) | ![Acceso](docs/capturas/01-acceso.png) |
| ![Inicio](docs/capturas/02-inicio.png) | ![Cuentas del equipo](docs/capturas/14-cuentas.png) |
| ![Boca abierta](docs/capturas/04-boca-abierta.png) | ![Vista oclusal](docs/capturas/05-vista-oclusal.png) |
| ![Línea de tiempo](docs/capturas/06-linea-de-tiempo.png) | ![Pantalla completa](docs/capturas/07-pantalla-completa.png) |
| ![Historial de una pieza](docs/capturas/15-historial-pieza.png) | ![Plan y pagos](docs/capturas/08-plan-y-pagos.png) |
| ![Agenda](docs/capturas/09-agenda.png) | ![Pacientes para dar de alta](docs/capturas/10-pacientes-alta.png) |

![Informe clínico y presupuesto en PDF](docs/capturas/12-documentos-pdf.png)

## Autores

**Equipo EREJM** — las iniciales aparecen en cada clase (`@author EREJM`), en el `pom.xml`, en el instalador
y en la documentación.

| Iniciales | Integrante |
|---|---|
| ER | Everson Ricardo |
| EJ | Edilson Joel |
| M | Michael |

---|---|
| ![Acceso](docs/capturas/01-acceso.png) | ![Inicio](docs/capturas/02-inicio.png) |
| ![Boca abierta](docs/capturas/04-boca-abierta.png) | ![Vista oclusal](docs/capturas/05-vista-oclusal.png) |
| ![Línea de tiempo](docs/capturas/06-linea-de-tiempo.png) | ![Pantalla completa](docs/capturas/07-pantalla-completa.png) |
| ![Plan y pagos](docs/capturas/08-plan-y-pagos.png) | ![Agenda](docs/capturas/09-agenda.png) |
| ![Pacientes para dar de alta](docs/capturas/10-pacientes-alta.png) | ![Modo oscuro](docs/capturas/11-modo-oscuro.png) |

![Informe clínico y presupuesto en PDF](docs/capturas/12-documentos-pdf.png)

---

Proyecto académico — Universidad Nacional de Juliaca (UNAJ), curso de programación en Java.
