/**
 * Entidades JPA del dominio OhMyFreezer.
 * <p>
 * Define aquí (a nivel de paquete) el {@code @FilterDef} del filtro Hibernate
 * de multi-tenancy {@code negocioFilter}. Hibernate 6 no permite declarar el
 * mismo {@code @FilterDef} en más de una entidad (lanza
 * {@code AnnotationException: Multiple '@FilterDef' annotations define a
 * filter named 'negocioFilter'}), así que la definición vive una sola vez
 * aquí y cada entidad tenant-owned solo aplica {@code @Filter(name =
 * "negocioFilter", ...)} referenciándola.
 */
@FilterDef(name = "negocioFilter", parameters = @ParamDef(name = "negocioId", type = Long.class))
package com.ares.backend.entity;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
