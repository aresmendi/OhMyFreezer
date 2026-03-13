package com.ares.backend.service;

import com.ares.backend.dto.DatoEstadisticaDTO;
import com.ares.backend.dto.EstadisticaRecetaResponse;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RegistroUsoReceta;
import com.ares.backend.repository.RecetaRepository;
import com.ares.backend.repository.RegistroUsoRecetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de estadísticas.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class EstadisticaService {

    private final RegistroUsoRecetaRepository registroUsoRecetaRepository;
    private final RecetaRepository recetaRepository;

    /**
     * Obtiene las estadísticas de uso de una receta en un rango de fechas.
     *
     * @param recetaId ID de la receta
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Estadísticas de la receta
     * @throws IllegalArgumentException Si la receta no existe
     */
    public EstadisticaRecetaResponse obtenerEstadisticasReceta(Long recetaId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        Receta receta = recetaRepository.findById(recetaId)
                .orElseThrow(() -> new IllegalArgumentException("Receta no encontrada"));

        // Obtener registros de uso en el rango de fechas
        List<RegistroUsoReceta> registros = registroUsoRecetaRepository
                .findByRecetaIdAndFechaElaboracionBetween(recetaId, fechaInicio, fechaFin);

        // Total y completadas
        int totalElaboraciones = registros.size();
        int elaboracionesCompletadas = (int) registros.stream()
                .filter(RegistroUsoReceta::getCompletada)
                .count();

        // solo elaboraciones completadas agrupadas por fecha
        Map<LocalDate, Long> usosPorFecha = registros.stream()
                .filter(RegistroUsoReceta::getCompletada)
                .collect(Collectors.groupingBy(
                        r -> r.getFechaElaboracion().toLocalDate(),
                        Collectors.counting()
                ));

        List<DatoEstadisticaDTO> datos = usosPorFecha.entrySet().stream()
                .map(entry -> new DatoEstadisticaDTO(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(DatoEstadisticaDTO::getFecha))
                .collect(Collectors.toList());

        return new EstadisticaRecetaResponse(
                recetaId,
                receta.getNombre(),
                datos,
                totalElaboraciones,
                elaboracionesCompletadas
        );
    }

    /**
     * Obtiene las estadísticas de todas las recetas en un rango de fechas.
     *
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Lista de estadísticas de todas las recetas
     */
    public List<EstadisticaRecetaResponse> obtenerEstadisticasTodasRecetas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Receta> recetas = recetaRepository.findAll();
        List<EstadisticaRecetaResponse> estadisticas = new ArrayList<>();

        for (Receta receta : recetas) {
            EstadisticaRecetaResponse estadistica = obtenerEstadisticasReceta(receta.getId(), fechaInicio, fechaFin);
            estadisticas.add(estadistica);
        }

        return estadisticas;
    }
}