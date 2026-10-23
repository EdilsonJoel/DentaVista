package com.dentavista.tresd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Malla poligonal minima (vertices + caras de N lados) con las operaciones de
 * geometria que necesita el odontograma 3D: lectura de OBJ, soldado de vertices,
 * separacion en piezas conexas, subdivision Catmull-Clark y normales suaves.
 * Es Java puro (sin JavaFX) para poder probarla con pruebas unitarias.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Malla {

    /** Vertices: cada uno es {x, y, z}. */
    final List<double[]> v;
    /** Caras: indices a {@link #v}; triangulos, cuadrados o N-gonos. */
    final List<int[]> caras;

    public Malla(List<double[]> v, List<int[]> caras) {
        this.v = v;
        this.caras = caras;
    }

    public int vertices() {
        return v.size();
    }

    public double[] vertice(int i) {
        return v.get(i);
    }

    public List<int[]> caras() {
        return caras;
    }

    // ------------------------------------------------------------ lectura OBJ

    /** Lee las lineas {@code v} y {@code f} de un OBJ (ignora texturas, normales y grupos). */
    public static Malla leerObj(InputStream entrada) throws IOException {
        List<double[]> vs = new ArrayList<>();
        List<int[]> fs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.ISO_8859_1))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.strip();
                if (linea.startsWith("v ")) {
                    String[] t = linea.split("\\s+");
                    vs.add(new double[]{Double.parseDouble(t[1]), Double.parseDouble(t[2]), Double.parseDouble(t[3])});
                } else if (linea.startsWith("f ")) {
                    String[] t = linea.split("\\s+");
                    int[] cara = new int[t.length - 1];
                    for (int i = 1; i < t.length; i++) {
                        int idx = Integer.parseInt(t[i].split("/")[0]);
                        // OBJ admite indices negativos (relativos al final).
                        cara[i - 1] = idx > 0 ? idx - 1 : vs.size() + idx;
                    }
                    fs.add(cara);
                }
            }
        }
        return new Malla(vs, fs);
    }

    // ------------------------------------------------------ soldado y piezas

    /**
     * Une vertices que ocupan la misma posicion (los exportadores duplican vertices
     * en las costuras de textura); sin esto la subdivision abriria grietas.
     */
    public Malla soldar(double tolerancia) {
        Map<String, Integer> indice = new HashMap<>();
        int[] remapa = new int[v.size()];
        List<double[]> nuevos = new ArrayList<>();
        for (int i = 0; i < v.size(); i++) {
            double[] p = v.get(i);
            String clave = Math.round(p[0] / tolerancia) + ":" + Math.round(p[1] / tolerancia)
                    + ":" + Math.round(p[2] / tolerancia);
            Integer existente = indice.get(clave);
            if (existente == null) {
                existente = nuevos.size();
                nuevos.add(p.clone());
                indice.put(clave, existente);
            }
            remapa[i] = existente;
        }
        List<int[]> fs = new ArrayList<>();
        for (int[] c : caras) {
            int[] nc = new int[c.length];
            for (int i = 0; i < c.length; i++) {
                nc[i] = remapa[c[i]];
            }
            if (sinRepetidos(nc)) {
                fs.add(nc);
            }
        }
        return new Malla(nuevos, fs);
    }

    private static boolean sinRepetidos(int[] c) {
        for (int i = 0; i < c.length; i++) {
            for (int j = i + 1; j < c.length; j++) {
                if (c[i] == c[j]) {
                    return false;
                }
            }
        }
        return c.length >= 3;
    }

    /** Separa la malla en piezas conexas (cada diente y cada encia es una pieza). */
    public List<Malla> piezasConexas() {
        int[] padre = new int[v.size()];
        for (int i = 0; i < padre.length; i++) {
            padre[i] = i;
        }
        for (int[] c : caras) {
            for (int i = 1; i < c.length; i++) {
                unir(padre, c[0], c[i]);
            }
        }
        Map<Integer, List<int[]>> grupos = new HashMap<>();
        for (int[] c : caras) {
            grupos.computeIfAbsent(raiz(padre, c[0]), k -> new ArrayList<>()).add(c);
        }
        List<Malla> piezas = new ArrayList<>();
        for (List<int[]> grupo : grupos.values()) {
            Map<Integer, Integer> local = new HashMap<>();
            List<double[]> vs = new ArrayList<>();
            List<int[]> fs = new ArrayList<>();
            for (int[] c : grupo) {
                int[] nc = new int[c.length];
                for (int i = 0; i < c.length; i++) {
                    final int global = c[i];
                    nc[i] = local.computeIfAbsent(global, k -> {
                        vs.add(v.get(global).clone());
                        return vs.size() - 1;
                    });
                }
                fs.add(nc);
            }
            piezas.add(new Malla(vs, fs));
        }
        return piezas;
    }

    private static int raiz(int[] padre, int a) {
        while (padre[a] != a) {
            padre[a] = padre[padre[a]];
            a = padre[a];
        }
        return a;
    }

    private static void unir(int[] padre, int a, int b) {
        int ra = raiz(padre, a);
        int rb = raiz(padre, b);
        if (ra != rb) {
            padre[ra] = rb;
        }
    }

    // ------------------------------------------------------ medidas basicas

    public double[] centro() {
        double[] c = new double[3];
        for (double[] p : v) {
            c[0] += p[0];
            c[1] += p[1];
            c[2] += p[2];
        }
        int n = Math.max(1, v.size());
        return new double[]{c[0] / n, c[1] / n, c[2] / n};
    }

    /** {minX, maxX, minY, maxY, minZ, maxZ}. */
    public double[] limites() {
        double[] b = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
                -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double[] p : v) {
            for (int k = 0; k < 3; k++) {
                b[2 * k] = Math.min(b[2 * k], p[k]);
                b[2 * k + 1] = Math.max(b[2 * k + 1], p[k]);
            }
        }
        return b;
    }

    public double ancho() {
        double[] b = limites();
        return Math.max(b[1] - b[0], b[5] - b[4]);
    }

    /** Copia transformada: {@code p' = destino + (p - origen) * escala}. */
    public Malla transformada(double[] origen, double[] destino, double escala) {
        List<double[]> vs = new ArrayList<>(v.size());
        for (double[] p : v) {
            vs.add(new double[]{
                    destino[0] + (p[0] - origen[0]) * escala,
                    destino[1] + (p[1] - origen[1]) * escala,
                    destino[2] + (p[2] - origen[2]) * escala});
        }
        List<int[]> fs = new ArrayList<>(caras.size());
        for (int[] c : caras) {
            fs.add(c.clone());
        }
        return new Malla(vs, fs);
    }

    // ------------------------------------------------- subdivision Catmull-Clark

    /**
     * Un paso de subdivision Catmull-Clark: cada cara de N lados se convierte en N
     * cuadrilateros y la superficie se suaviza. Dos pasos convierten el modelo
     * "facetado" de baja resolucion en dientes redondeados y realistas.
     * Los bordes abiertos (la base del diente, oculta en la encia) se tratan con la
     * regla de borde para no encogerlos.
     */
    public Malla subdividir() {
        int nv = v.size();

        // 1. Punto de cara: promedio de sus vertices.
        double[][] puntoCara = new double[caras.size()][];
        for (int f = 0; f < caras.size(); f++) {
            puntoCara[f] = promedio(caras.get(f));
        }

        // 2. Aristas con sus caras vecinas.
        Map<Long, Arista> aristas = new HashMap<>();
        for (int f = 0; f < caras.size(); f++) {
            int[] c = caras.get(f);
            for (int i = 0; i < c.length; i++) {
                int a = c[i];
                int b = c[(i + 1) % c.length];
                aristas.computeIfAbsent(clave(a, b), k -> new Arista(Math.min(a, b), Math.max(a, b)))
                        .caras.add(f);
            }
        }

        List<double[]> nuevos = new ArrayList<>(nv + aristas.size() + caras.size());
        for (int i = 0; i < nv; i++) {
            nuevos.add(null); // se completan en el paso 4
        }

        // 3. Punto de arista.
        for (Arista ar : aristas.values()) {
            double[] p0 = v.get(ar.a);
            double[] p1 = v.get(ar.b);
            double[] pe;
            if (ar.caras.size() == 2) {
                double[] f0 = puntoCara[ar.caras.get(0)];
                double[] f1 = puntoCara[ar.caras.get(1)];
                pe = new double[3];
                for (int k = 0; k < 3; k++) {
                    pe[k] = (p0[k] + p1[k] + f0[k] + f1[k]) / 4.0;
                }
            } else {
                pe = new double[]{(p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2, (p0[2] + p1[2]) / 2};
            }
            ar.indice = nuevos.size();
            nuevos.add(pe);
        }

        // 4. Nueva posicion de cada vertice original.
        List<List<Integer>> carasDeVertice = new ArrayList<>(nv);
        List<List<Arista>> aristasDeVertice = new ArrayList<>(nv);
        for (int i = 0; i < nv; i++) {
            carasDeVertice.add(new ArrayList<>());
            aristasDeVertice.add(new ArrayList<>());
        }
        for (int f = 0; f < caras.size(); f++) {
            for (int idx : caras.get(f)) {
                carasDeVertice.get(idx).add(f);
            }
        }
        for (Arista ar : aristas.values()) {
            aristasDeVertice.get(ar.a).add(ar);
            aristasDeVertice.get(ar.b).add(ar);
        }
        for (int i = 0; i < nv; i++) {
            double[] p = v.get(i);
            List<Arista> suyas = aristasDeVertice.get(i);
            List<Arista> borde = suyas.stream().filter(a -> a.caras.size() != 2).toList();
            double[] r = new double[3];
            if (!borde.isEmpty()) {
                // Regla de borde: (6P + vecinos de borde) / 8 (con dos vecinos).
                if (borde.size() == 2) {
                    double[] a = v.get(borde.get(0).otro(i));
                    double[] b = v.get(borde.get(1).otro(i));
                    for (int k = 0; k < 3; k++) {
                        r[k] = (6 * p[k] + a[k] + b[k]) / 8.0;
                    }
                } else {
                    r = p.clone();
                }
            } else {
                int n = suyas.size();
                double[] fProm = new double[3];
                for (int f : carasDeVertice.get(i)) {
                    for (int k = 0; k < 3; k++) {
                        fProm[k] += puntoCara[f][k];
                    }
                }
                int nf = Math.max(1, carasDeVertice.get(i).size());
                double[] rProm = new double[3];
                for (Arista ar : suyas) {
                    double[] o = v.get(ar.otro(i));
                    for (int k = 0; k < 3; k++) {
                        rProm[k] += (p[k] + o[k]) / 2.0;
                    }
                }
                for (int k = 0; k < 3; k++) {
                    fProm[k] /= nf;
                    rProm[k] /= Math.max(1, n);
                    r[k] = (fProm[k] + 2 * rProm[k] + (n - 3) * p[k]) / n;
                }
            }
            nuevos.set(i, r);
        }

        // 5. Cada cara de N lados se divide en N cuadrilateros.
        List<int[]> fs = new ArrayList<>();
        for (int f = 0; f < caras.size(); f++) {
            int[] c = caras.get(f);
            int idxCara = nuevos.size();
            nuevos.add(puntoCara[f]);
            for (int i = 0; i < c.length; i++) {
                int actual = c[i];
                int siguiente = c[(i + 1) % c.length];
                int anterior = c[(i - 1 + c.length) % c.length];
                fs.add(new int[]{
                        actual,
                        aristas.get(clave(actual, siguiente)).indice,
                        idxCara,
                        aristas.get(clave(anterior, actual)).indice});
            }
        }
        return new Malla(nuevos, fs);
    }

    private double[] promedio(int[] cara) {
        double[] c = new double[3];
        for (int idx : cara) {
            double[] p = v.get(idx);
            c[0] += p[0];
            c[1] += p[1];
            c[2] += p[2];
        }
        return new double[]{c[0] / cara.length, c[1] / cara.length, c[2] / cara.length};
    }

    private static long clave(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return ((long) min << 32) | (max & 0xffffffffL);
    }

    private static final class Arista {
        final int a;
        final int b;
        final List<Integer> caras = new ArrayList<>(2);
        int indice;

        Arista(int a, int b) {
            this.a = a;
            this.b = b;
        }

        int otro(int x) {
            return x == a ? b : a;
        }
    }

    // ------------------------------------------------------ triangulos y normales

    /** Divide cada cara en triangulos (abanico). */
    public List<int[]> triangulos() {
        List<int[]> tris = new ArrayList<>();
        for (int[] c : caras) {
            for (int i = 1; i + 1 < c.length; i++) {
                tris.add(new int[]{c[0], c[i], c[i + 1]});
            }
        }
        return tris;
    }

    /** Normal (no normalizada; su modulo es el doble del area) de un triangulo. */
    public double[] normalTriangulo(int[] t) {
        double[] a = v.get(t[0]);
        double[] b = v.get(t[1]);
        double[] c = v.get(t[2]);
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        double wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
        return new double[]{uy * wz - uz * wy, uz * wx - ux * wz, ux * wy - uy * wx};
    }

    /** Normales suaves por vertice (promedio ponderado por area de los triangulos vecinos). */
    public double[][] normalesSuaves(List<int[]> tris) {
        double[][] n = new double[v.size()][3];
        for (int[] t : tris) {
            double[] nt = normalTriangulo(t);
            for (int idx : t) {
                n[idx][0] += nt[0];
                n[idx][1] += nt[1];
                n[idx][2] += nt[2];
            }
        }
        for (double[] ni : n) {
            double m = Math.sqrt(ni[0] * ni[0] + ni[1] * ni[1] + ni[2] * ni[2]);
            if (m > 1e-12) {
                ni[0] /= m;
                ni[1] /= m;
                ni[2] /= m;
            } else {
                ni[1] = 1;
            }
        }
        return n;
    }
}
