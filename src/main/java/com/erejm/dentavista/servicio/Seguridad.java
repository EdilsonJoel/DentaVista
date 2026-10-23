package com.erejm.dentavista.servicio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Hash de contraseñas con SHA-256 y sal aleatoria por usuario.
 * Se guarda {@code sal:hash} para poder verificar sin conocer la contraseña.
 *
 * @author EREJM (Everson Ricardo · Edilson Joel · Michael)
 */
public final class Seguridad {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Seguridad() {
    }

    public static String hashear(String contrasena) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        String hexSal = HexFormat.of().formatHex(sal);
        return hexSal + ":" + digest(hexSal + contrasena);
    }

    public static boolean coincide(String contrasena, String guardado) {
        if (guardado == null || !guardado.contains(":") || contrasena == null) {
            return false;
        }
        String sal = guardado.substring(0, guardado.indexOf(':'));
        // Comparación en tiempo constante: no filtra cuántos caracteres coinciden.
        return MessageDigest.isEqual(
                digest(sal + contrasena).getBytes(StandardCharsets.UTF_8),
                guardado.substring(guardado.indexOf(':') + 1).getBytes(StandardCharsets.UTF_8));
    }

    /** Política de contraseñas del personal y de los pacientes: 8+ caracteres con letras y números. */
    public static void validarClave(String clave) {
        if (clave == null || clave.length() < 8) {
            throw new NegocioException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (clave.chars().noneMatch(Character::isLetter) || clave.chars().noneMatch(Character::isDigit)) {
            throw new NegocioException("La contraseña debe combinar letras y números.");
        }
    }

    /** Contraseña temporal fácil de dictar y difícil de adivinar, p. ej. "Kmrt-5821". */
    public static String claveTemporal() {
        String letras = "abcdefghjkmnpqrstuvwxyz"; // sin i, l, o: no se confunden con 1 y 0
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            char c = letras.charAt(ALEATORIO.nextInt(letras.length()));
            sb.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        sb.append('-');
        for (int i = 0; i < 4; i++) {
            sb.append(2 + ALEATORIO.nextInt(8)); // dígitos 2-9
        }
        return sb.toString();
    }

    private static String digest(String texto) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
        }
    }
}
