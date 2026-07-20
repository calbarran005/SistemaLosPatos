package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.ClienteJuridico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteJuridicoRepository extends JpaRepository<ClienteJuridico, Integer> {
    java.util.Optional<ClienteJuridico> findByRuc(String ruc);
}
