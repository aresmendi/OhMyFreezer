-- V7: backstop de BD real para D3 ("como mucho una credencial TPV activa
-- por negocio"), cerrando la ventana de concurrencia que R4-001 detectó en
-- TpvApiKeyAdminService.emitir(): el invariante se comprobaba SOLO en
-- código de aplicación (buscar activa -> revocar -> insertar) dentro de un
-- único @Transactional, sin ningún respaldo a nivel de base de datos. Bajo
-- READ_COMMITTED, dos emitir() concurrentes para el mismo negocio pueden
-- leer el mismo estado (incluida la PRIMERA emisión, sin ninguna fila
-- previa que un lock pesimista pudiera bloquear) y ambos insertar una fila
-- activa nueva, sin que nada lo detecte.
--
-- negocio_id_activo espeja negocio_id SOLO mientras activa=true (ver
-- TpvApiKey constructor / TpvApiKey.revocar()). MySQL y H2 (MODE=MySQL)
-- tratan cada NULL como un valor distinto en un índice único, exactamente
-- igual que external_id_original en ventas_tpv (V6): como mucho una fila
-- por negocio puede tener aquí un valor no nulo. Dos inserciones
-- concurrentes de una fila activa para el mismo negocio ya no pueden
-- ambas tener éxito: la perdedora de la carrera revienta con
-- DataIntegrityViolationException al hacer save(), en vez de colar una
-- segunda credencial activa silenciosamente.
ALTER TABLE tpv_api_keys ADD COLUMN negocio_id_activo BIGINT NULL;

-- Backfill defensivo: si esta migración se aplicara sobre una BD con datos
-- preexistentes (dev/staging), las filas ya activas deben quedar cubiertas
-- por el nuevo índice desde el primer momento, no solo las emitidas después.
UPDATE tpv_api_keys SET negocio_id_activo = negocio_id WHERE activa = TRUE;

ALTER TABLE tpv_api_keys ADD CONSTRAINT uk_tpv_api_keys_negocio_activo UNIQUE (negocio_id_activo);
