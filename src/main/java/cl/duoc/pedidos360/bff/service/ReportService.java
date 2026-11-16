package cl.duoc.pedidos360.bff.service;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import cl.duoc.pedidos360.bff.client.ReportClient;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/** Punto de composición del dominio reportería (hoy delega 1:1 en el cliente). Los KPIs los calcula report. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportClient reportClient;

    public JsonNode kpis(MultiValueMap<String, String> params) {
        return reportClient.kpis(params);
    }

    public JsonNode ventasPorHora(MultiValueMap<String, String> params) {
        return reportClient.ventasPorHora(params);
    }

    public JsonNode leadTime(MultiValueMap<String, String> params) {
        return reportClient.leadTime(params);
    }

    public JsonNode topProductos(MultiValueMap<String, String> params) {
        return reportClient.topProductos(params);
    }

    public JsonNode estadosActivos() {
        return reportClient.estadosActivos();
    }
}
