-- V3: email pasa a ser el identificador global de login, reemplazando a
-- username en esa función (username sigue siendo único solo por negocio, ver
-- V2, y se mantiene para display/onboarding).
--
-- Motivo: desde V2 el username es único solo por negocio, no globalmente,
-- así que dos negocios distintos pueden tener un usuario con el mismo
-- username. UsuarioService.login() ya no puede desambiguar de forma segura
-- solo por username: se hace de email el identificador único en TODO el
-- sistema y login pasa a resolver por email.
--
-- Backfill defensivo: cualquier email NULL o vacío se rellena con un
-- placeholder derivado del propio id (ya único por definición), para que
-- esta migración nunca falle sobre datos preexistentes, sea cual sea su
-- estado real en dev/staging.
UPDATE usuarios
SET email = CONCAT('sin-email+', id, '@ohmyfreezer.invalid')
WHERE email IS NULL OR TRIM(email) = '';

ALTER TABLE usuarios MODIFY email VARCHAR(100) NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_email UNIQUE (email);
