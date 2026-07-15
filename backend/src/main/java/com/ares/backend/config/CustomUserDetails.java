package com.ares.backend.config;

import com.ares.backend.entity.Usuario;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    public Long getId() {
        return usuario.getId();
    }

    public boolean getEsJefeCocina() {
        return usuario.getEsJefeCocina();
    }

    /**
     * Negocio (tenant) al que pertenece el usuario autenticado. Se lee
     * siempre de la entidad Usuario recién recargada por JwtFilter — nunca
     * de la claim del JWT — porque es la fuente autoritativa e infalsificable.
     */
    public Long getNegocioId() {
        return usuario.getNegocio() != null ? usuario.getNegocio().getId() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String rol = usuario.getEsJefeCocina() ? "ROLE_JEFE" : "ROLE_COCINERO";
        return List.of(() -> rol);
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}