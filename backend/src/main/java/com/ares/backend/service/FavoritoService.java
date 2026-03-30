package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.FavoritoRequest;
import com.ares.backend.dto.RecetaDetailResponse;
import com.ares.backend.dto.RecetaFavoritaResponse;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RecetaFavorita;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.RecetaFavoritaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de favoritos.
 *
 * @author Ares
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final RecetaFavoritaRepository favoritoRepository;
    private final UsuarioService usuarioService;
    private final RecetaService recetaService;

    /**
     * Marca una receta como favorita para un usuario.
     *
     * @param request Datos del favorito (usuarioId, recetaId)
     * @return Favorito creado
     * @throws IllegalArgumentException Si ya existe el favorito
     */
    @Transactional
    public RecetaFavoritaResponse marcarFavorito(FavoritoRequest request) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        Receta receta = recetaService.buscarPorId(request.getRecetaId());

        // Verificar si ya existe
        if (favoritoRepository.existsByUsuarioAndReceta(usuario, receta)) {
            throw new IllegalArgumentException("La receta ya está marcada como favorita");
        }

        // Crear favorito
        RecetaFavorita favorito = new RecetaFavorita(usuario, receta);
        favorito = favoritoRepository.save(favorito);

        // Construir respuesta
        RecetaDetailResponse recetaDetail = recetaService.obtenerPorId(receta.getId());
        recetaDetail.setEsFavorita(true);

        return new RecetaFavoritaResponse(favorito, recetaDetail);
    }

    /**
     * Desmarca una receta como favorita.
     *
     * @param request Datos del favorito (usuarioId, recetaId)
     * @throws IllegalArgumentException Si el favorito no existe
     */
    @Transactional
    public void desmarcarFavorito(FavoritoRequest request) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        Receta receta = recetaService.buscarPorId(request.getRecetaId());

        // Verificar si existe
        if (!favoritoRepository.existsByUsuarioAndReceta(usuario, receta)) {
            throw new IllegalArgumentException("La receta no está marcada como favorita");
        }

        // Eliminar favorito
        favoritoRepository.deleteByUsuarioAndReceta(usuario, receta);
    }

    /**
     * Obtiene todas las recetas favoritas del usuario autenticado.
     *
     * @return Lista de recetas favoritas con sus detalles
     */
    public List<RecetaFavoritaResponse> obtenerFavoritosUsuario() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        List<RecetaFavorita> favoritos = favoritoRepository.findByUsuarioOrderByFechaMarcadoDesc(usuario);

        return favoritos.stream()
                .map(favorito -> {
                    RecetaDetailResponse recetaDetail = recetaService.obtenerPorId(favorito.getReceta().getId());
                    recetaDetail.setEsFavorita(true);
                    return new RecetaFavoritaResponse(favorito, recetaDetail);
                })
                .collect(Collectors.toList());
    }

    /**
     * Verifica si una receta es favorita del usuario autenticado.
     *
     * @param recetaId ID de la receta
     * @return true si es favorita, false en caso contrario
     */
    public boolean esFavorita(Long recetaId) {
        Long usuarioId = SecurityUtils.getUsuarioId();
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        Receta receta = recetaService.buscarPorId(recetaId);
        return favoritoRepository.existsByUsuarioAndReceta(usuario, receta);
    }

    /**
     * Obtiene los IDs de todas las recetas favoritas del usuario autenticado.
     * Útil para marcar favoritos en listados.
     *
     * @return Lista de IDs de recetas favoritas
     */
    public List<Long> obtenerIdsFavoritosUsuario() {
        Long usuarioId = SecurityUtils.getUsuarioId();
        return favoritoRepository.findRecetaIdsByUsuarioId(usuarioId);
    }
}