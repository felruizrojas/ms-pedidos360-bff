package cl.duoc.pedidos360.bff.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuerpo del PUT: solo precio y stock. Mismas reglas y mensajes que
 * ms-pedidos360-catalog.ActualizarProductoRequest (ver CrearProductoRequest para el porqué
 * de duplicarlas acá).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarProductoRequest {

    @NotNull(message = "El precio es obligatorio.")
    @Positive(message = "El precio debe ser un monto entero mayor que 0.")
    @DecimalMax(value = "999999999", message = "El precio no puede superar $999.999.999.")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio.")
    @PositiveOrZero(message = "El stock no puede ser negativo.")
    @Max(value = 100_000, message = "El stock no puede superar 100.000 unidades.")
    private Integer stock;
}
