package com.ares.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

/**
 * Credencial de autenticación de un TPV para un Negocio (tenant): como
 * mucho una está activa por negocio a la vez (re-emitir revoca la
 * anterior, D3 del diseño). El formato del secreto en claro es
 * {@code omf_tpv_<prefijo>_<secreto>} (D-D del diseño): {@link #prefijo}
 * se guarda en claro y con índice único global (lookup O(1) por
 * {@code TpvApiKeyRepository.findByPrefijoAndActivaTrue}); el secreto solo
 * se conserva como {@link #secretoHash} (bcrypt), nunca en claro.
 *
 * @author Ares
 * @version 1.0
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "secretoHash")
@Table(name = "tpv_api_keys")
@Filter(name = "negocioFilter", condition = "negocio_id = :negocioId")
public class TpvApiKey {

    /**
     * Identificador único de la credencial.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Negocio (tenant) al que pertenece esta credencial.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /**
     * Prefijo público del formato {@code omf_tpv_<prefijo>_<secreto>},
     * único GLOBALMENTE (no por negocio): el filtro resuelve el negocio a
     * partir de este valor, antes de saber a qué tenant pertenece la
     * petición.
     */
    @Column(nullable = false, unique = true, length = 16)
    private String prefijo;

    /**
     * Hash bcrypt del secreto. El secreto en claro solo se devuelve una vez
     * al emitir/reemitir la credencial; nunca se persiste en claro.
     */
    @Column(nullable = false, length = 72)
    private String secretoHash;

    /**
     * Usuario sintético del sistema que representa a este TPV en el
     * dominio (autoría de {@code RegistroUsoReceta}/{@code MovimientoStock}).
     * Provisto atómicamente junto con la primera credencial del negocio.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_sistema_id", nullable = false)
    private Usuario usuarioSistema;

    /**
     * Indica si la credencial sigue activa. Una credencial revocada no
     * autentica, aunque el prefijo siga existiendo en la tabla (histórico).
     */
    @Column(nullable = false)
    private Boolean activa = true;

    /**
     * Fecha y hora de emisión de la credencial.
     */
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Fecha y hora de revocación, si la credencial ya no está activa.
     */
    private LocalDateTime fechaRevocacion;

    /**
     * Espejo de {@code negocio.getId()} SOLO mientras la credencial está
     * activa; {@link #revocar()} lo pone a {@code null}. Existe
     * ÚNICAMENTE para soportar el índice único {@code
     * uk_tpv_api_keys_negocio_activo} (ver migración V7), backstop real de
     * BD para D3 ("como mucho una credencial activa por negocio"): MySQL y
     * H2 (MODE=MySQL) tratan cada NULL como un valor distinto en un índice
     * único (mismo patrón que {@code ventas_tpv.external_id_original} en
     * V6), así que como mucho una fila por negocio puede tener aquí un
     * valor no nulo — la constraint salta con
     * {@code DataIntegrityViolationException} si dos {@code emitir()}
     * concurrentes para el mismo negocio intentan dejar ambos una fila
     * activa, incluso cuando NINGUNA fila previa existía que un lock
     * pesimista pudiera bloquear (R4-001).
     */
    @Column(unique = true)
    private Long negocioIdActivo;

    /**
     * Constructor con parámetros para emitir una credencial nueva.
     *
     * @param negocio        Negocio (tenant) al que pertenece
     * @param prefijo        Prefijo público, único globalmente
     * @param secretoHash    Hash bcrypt del secreto
     * @param usuarioSistema Usuario sintético que representa al TPV
     */
    public TpvApiKey(Negocio negocio, String prefijo, String secretoHash, Usuario usuarioSistema) {
        this.negocio = negocio;
        this.prefijo = prefijo;
        this.secretoHash = secretoHash;
        this.usuarioSistema = usuarioSistema;
        this.activa = true;
        this.fechaCreacion = LocalDateTime.now();
        this.negocioIdActivo = negocio.getId();
    }

    /**
     * Revoca la credencial: apaga {@link #activa} y sella
     * {@link #fechaRevocacion} con el instante actual. Idempotente a nivel
     * de objeto (no vuelve a comprobar el estado previo); la atomicidad
     * frente a revocaciones concurrentes es responsabilidad de la capa de
     * persistencia, no de esta entidad. Limpia {@link #negocioIdActivo} a
     * {@code null} para liberar el hueco en el índice único D3.
     */
    public void revocar() {
        this.activa = false;
        this.fechaRevocacion = LocalDateTime.now();
        this.negocioIdActivo = null;
    }
}
