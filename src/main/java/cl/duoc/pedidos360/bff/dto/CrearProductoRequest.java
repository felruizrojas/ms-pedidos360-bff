package cl.duoc.pedidos360.bff.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mismas reglas y mensajes que ms-pedidos360-catalog.ProductoRequest (fuente de verdad de
 * negocio) y que pedidos-360-frontend.producto-validators.ts: se duplican acá a propósito
 * para que el BFF falle rápido con un mensaje en español consistente, en vez de dejar pasar
 * un request inválido hasta el microservicio downstream (o, peor, devolver el mensaje default
 * de Bean Validation en inglés si algo viola solo las reglas más laxas de antes).
 * La unicidad del nombre NO se valida acá: requiere consultar la BD de catalog, así que esa
 * queda a cargo de catalog; el BFF ya reenvía ese 400 con `detalles` tal cual (ver
 * ValidacionDownstreamException / GlobalExceptionHandler).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearProductoRequest {

    private static final String NOMBRE_PATTERN = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü\\s]+$";
    private static final String DESCRIPCION_PATTERN = "^[A-Za-zÁÉÍÓÚÑÜáéíóúñü0-9\\s]*$";

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 25, message = "El nombre no puede superar los 25 caracteres.")
    @Pattern(regexp = NOMBRE_PATTERN, message = "El nombre solo admite letras y espacios (sin números ni símbolos).")
    private String nombre;

    @Size(max = 50, message = "La descripción no puede superar los 50 caracteres.")
    @Pattern(regexp = DESCRIPCION_PATTERN, message = "La descripción solo admite letras y números (sin símbolos especiales).")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio.")
    @Positive(message = "El precio debe ser un monto entero mayor que 0.")
    @DecimalMax(value = "999999999", message = "El precio no puede superar $999.999.999.")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio.")
    @PositiveOrZero(message = "El stock no puede ser negativo.")
    @Max(value = 100_000, message = "El stock no puede superar 100.000 unidades.")
    private Integer stock;
}
