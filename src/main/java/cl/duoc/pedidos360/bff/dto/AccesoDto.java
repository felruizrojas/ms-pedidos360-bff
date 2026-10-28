package cl.duoc.pedidos360.bff.dto;

import java.time.Instant;

/** Acceso registrado del usuario que llama (GET /api/users/me). */
public record AccesoDto(String id, String nombre, String email, Instant primerAcceso, Instant ultimoAcceso) {
}
