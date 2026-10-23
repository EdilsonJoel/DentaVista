package com.dentavista.tresd;

import com.dentavista.dominio.Pieza;
import com.dentavista.dominio.Superficie;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Convierte el modelo OBJ de la dentadura en un modelo <b>clinico</b>: cada diente
 * identificado con su numero FDI y cada triangulo asignado a una de las cinco caras
 * (oclusal, vestibular, lingual/palatina, mesial y distal).
 * <p>
 * El OBJ es una sola malla sin nombres por diente. El reconocimiento es geometrico:
 * </p>
 * <ol>
 *   <li>Se separa la malla en piezas conexas: 2 encias (las mas anchas) y 28 dientes.</li>
 *   <li>Cada diente se compara con su vecino mas cercano en planta: el que queda mas
 *       arriba pertenece a la arcada superior.</li>
 *   <li>El lado (x &lt; 0 = derecha del paciente) da el cuadrante y la distancia al
 *       frente de la boca da la posicion 1..7.</li>
 *   <li>Los terceros molares (muelas del juicio, 18/28/38/48) no vienen en el modelo:
 *       se generan copiando el segundo molar, desplazado y algo mas pequeno.</li>
 *   <li>Cada triangulo se clasifica segun hacia donde mira respecto a la arcada:
 *       hacia la arcada opuesta = oclusal, hacia afuera = vestibular, hacia la lengua =
 *       lingual, hacia la linea media = mesial y hacia atras = distal.</li>
 * </ol>
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class ModeloDental {

    /** Geometria lista para dibujar de un diente. */
    public record Diente(int fdi, Malla malla, List<int[]> triangulos, Superficie[] caraDeTriangulo,
                         double[][] normales, double[] centro) {
    }

    /** Geometria de una encia. */
    public record Encia(boolean superior, Malla malla, List<int[]> triangulos, double[][] normales) {
    }

    private static final String RECURSO = "/modelo/dentadura.obj";
    private static ModeloDental instancia;

    private final Map<Integer, Diente> dientes;
    private final List<Encia> encias;
    private final double[] centro;

    private ModeloDental(Map<Integer, Diente> dientes, List<Encia> encias, double[] centro) {
        this.dientes = dientes;
        this.encias = encias;
        this.centro = centro;
    }

    /** Modelo compartido por toda la aplicacion (se procesa una sola vez). */
    public static synchronized ModeloDental get() {
        if (instancia == null) {
            try (InputStream in = Objects.requireNonNull(
                    ModeloDental.class.getResourceAsStream(RECURSO), "Falta el recurso " + RECURSO)) {
                instancia = cargar(in, 2);
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer el modelo 3D de la dentadura", e);
            }
        }
        return instancia;
    }

    public Map<Integer, Diente> dientes() {
        return dientes;
    }

    public Diente diente(int fdi) {
        return dientes.get(fdi);
    }

    public List<Encia> encias() {
        return encias;
    }

    /** Centro geometrico de toda la dentadura (coordenadas del OBJ). */
    public double[] centro() {
        return centro;
    }

    // ------------------------------------------------------------------ carga

    /**
     * @param niveles pasos de subdivision Catmull-Clark para los dientes (0 = facetado)
     */
    public static ModeloDental cargar(InputStream obj, int niveles) throws IOException {
        // Se separa por conectividad ANTES de soldar: los dientes tocan la encia y,
        // soldando primero, quedarian unidos a ella. Despues se suelda cada pieza.
        Malla completa = Malla.leerObj(obj);
        List<Malla> piezas = new ArrayList<>();
        for (Malla p : completa.piezasConexas()) {
            piezas.add(p.soldar(1e-4));
        }

        // Las encias son las dos piezas mas anchas.
        piezas.sort(Comparator.comparingDouble(Malla::ancho).reversed());
        List<Malla> enciasCrudas = piezas.subList(0, 2);
        List<Malla> crudos = new ArrayList<>(piezas.subList(2, piezas.size()));

        // ---- 1. Arcada de cada diente (comparando con su vecino en planta).
        int n = crudos.size();
        double[][] c = new double[n][];
        for (int i = 0; i < n; i++) {
            c[i] = crudos.get(i).centro();
        }
        boolean[] superior = new boolean[n];
        for (int i = 0; i < n; i++) {
            int vecino = -1;
            double mejor = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (j == i) {
                    continue;
                }
                double d = sq(c[i][0] - c[j][0]) + sq(c[i][2] - c[j][2]);
                if (d < mejor) {
                    mejor = d;
                    vecino = j;
                }
            }
            superior[i] = c[i][1] > c[vecino][1];
        }

        // ---- 2. Cuadrante y posicion.
        Map<Integer, Malla> porFdi = new TreeMap<>();
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            boolean sup = cuadrante <= 2;
            boolean derecha = cuadrante == 1 || cuadrante == 4;
            List<Integer> grupo = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (superior[i] == sup && (c[i][0] < 0) == derecha) {
                    grupo.add(i);
                }
            }
            // Del frente (z mayor) hacia atras.
            grupo.sort(Comparator.comparingDouble(i -> -c[i][2]));
            for (int k = 0; k < grupo.size() && k < 8; k++) {
                porFdi.put(cuadrante * 10 + k + 1, crudos.get(grupo.get(k)));
            }
        }

        // ---- 3. Terceros molares sinteticos.
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            int m7 = cuadrante * 10 + 7;
            int m6 = cuadrante * 10 + 6;
            int m8 = cuadrante * 10 + 8;
            if (!porFdi.containsKey(m8) && porFdi.containsKey(m7) && porFdi.containsKey(m6)) {
                Malla segundo = porFdi.get(m7);
                double[] c7 = segundo.centro();
                double[] c6 = porFdi.get(m6).centro();
                double paso = 0.93;
                double[] c8 = {
                        c7[0] + (c7[0] - c6[0]) * paso,
                        c7[1] + (c7[1] - c6[1]) * paso * 0.5,
                        c7[2] + (c7[2] - c6[2]) * paso};
                porFdi.put(m8, segundo.transformada(c7, c8, 0.9));
            }
        }

        // ---- 4. Subdivision + clasificacion de caras.
        Map<Integer, double[]> centros = new TreeMap<>();
        porFdi.forEach((fdi, m) -> centros.put(fdi, m.centro()));

        Map<Integer, Diente> dientes = new TreeMap<>();
        for (Map.Entry<Integer, Malla> e : porFdi.entrySet()) {
            int fdi = e.getKey();
            Malla m = e.getValue();
            for (int k = 0; k < niveles; k++) {
                m = m.subdividir();
            }
            List<int[]> tris = m.triangulos();
            double[][] normales = m.normalesSuaves(tris);
            Superficie[] caras = clasificar(fdi, m, tris, centros);
            dientes.put(fdi, new Diente(fdi, m, tris, caras, normales, centros.get(fdi)));
        }

        List<Encia> encias = new ArrayList<>();
        for (Malla g : enciasCrudas) {
            Malla m = g.subdividir();
            List<int[]> tris = m.triangulos();
            boolean sup = g.centro()[1] > completa.centro()[1];
            encias.add(new Encia(sup, m, tris, m.normalesSuaves(tris)));
        }

        double[] b = completa.limites();
        double[] centro = {(b[0] + b[1]) / 2, (b[2] + b[3]) / 2, (b[4] + b[5]) / 2};
        return new ModeloDental(dientes, encias, centro);
    }

    // ----------------------------------------------------- caras de cada diente

    private static Superficie[] clasificar(int fdi, Malla m, List<int[]> tris, Map<Integer, double[]> centros) {
        double[] centro = centros.get(fdi);
        boolean sup = Pieza.esSuperior(fdi);

        // Oclusal: hacia la arcada opuesta.
        double[] oclusal = {0, sup ? -1 : 1, 0};

        // Mesial: a lo largo de la arcada hacia la linea media.
        int pos = Pieza.posicion(fdi);
        int cuad = Pieza.cuadrante(fdi);
        double[] anterior = pos == 1 ? centros.get(Pieza.espejo(fdi)) : centros.get(cuad * 10 + pos - 1);
        double[] posterior = pos == 8 ? centro : centros.get(cuad * 10 + pos + 1);
        if (anterior == null) {
            anterior = centro;
        }
        if (posterior == null) {
            posterior = centro;
        }
        double[] mesial = normalizar(new double[]{anterior[0] - posterior[0], 0, anterior[2] - posterior[2]});

        // Vestibular: perpendicular a la arcada, hacia afuera (lejos del centro de la arcada).
        double[] centroArcada = centroArcada(centros, sup);
        double[] vest = normalizar(new double[]{-mesial[2], 0, mesial[0]});
        double haciaFuera = (centro[0] - centroArcada[0]) * vest[0] + (centro[2] - centroArcada[2]) * vest[2];
        if (haciaFuera < 0) {
            vest = new double[]{-vest[0], 0, -vest[2]};
        }

        double[][] ejes = {vest, neg(vest), mesial, neg(mesial)};
        Superficie[] laterales = {Superficie.VESTIBULAR, Superficie.LINGUAL, Superficie.MESIAL, Superficie.DISTAL};
        double umbralOclusal = Pieza.esAnterior(fdi) ? 0.72 : 0.55;

        // Altura del diente: la cara oclusal solo puede estar en el tercio masticatorio.
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int i = 0; i < m.vertices(); i++) {
            double h = m.vertice(i)[1] * oclusal[1];
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        double alturaMinOclusal = min + (max - min) * 0.55;

        Superficie[] resultado = new Superficie[tris.size()];
        for (int t = 0; t < tris.size(); t++) {
            int[] tri = tris.get(t);
            double[] nrm = normalizar(m.normalTriangulo(tri));
            double[] a = m.vertice(tri[0]), b = m.vertice(tri[1]), cc = m.vertice(tri[2]);
            double[] pc = {(a[0] + b[0] + cc[0]) / 3, (a[1] + b[1] + cc[1]) / 3, (a[2] + b[2] + cc[2]) / 3};
            double[] dir = normalizar(new double[]{pc[0] - centro[0], pc[1] - centro[1], pc[2] - centro[2]});

            double altura = pc[1] * oclusal[1];
            if (punto(nrm, oclusal) > umbralOclusal && altura > alturaMinOclusal) {
                resultado[t] = Superficie.OCLUSAL;
                continue;
            }
            double mejor = -Double.MAX_VALUE;
            Superficie elegida = Superficie.VESTIBULAR;
            for (int k = 0; k < ejes.length; k++) {
                double puntaje = 0.55 * punto(nrm, ejes[k]) + 0.45 * punto(dir, ejes[k]);
                if (puntaje > mejor) {
                    mejor = puntaje;
                    elegida = laterales[k];
                }
            }
            resultado[t] = elegida;
        }
        return resultado;
    }

    private static double[] centroArcada(Map<Integer, double[]> centros, boolean superior) {
        double x = 0, z = 0;
        int n = 0;
        for (Map.Entry<Integer, double[]> e : centros.entrySet()) {
            if (Pieza.esSuperior(e.getKey()) == superior) {
                x += e.getValue()[0];
                z += e.getValue()[2];
                n++;
            }
        }
        return new double[]{x / n, 0, z / n};
    }

    private static double sq(double x) {
        return x * x;
    }

    private static double punto(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] neg(double[] a) {
        return new double[]{-a[0], -a[1], -a[2]};
    }

    private static double[] normalizar(double[] a) {
        double m = Math.sqrt(punto(a, a));
        return m < 1e-12 ? new double[]{0, 1, 0} : new double[]{a[0] / m, a[1] / m, a[2] / m};
    }
}
