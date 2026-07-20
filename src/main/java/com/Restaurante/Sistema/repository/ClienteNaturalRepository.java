package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.ClienteNatural;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteNaturalRepository extends JpaRepository<ClienteNatural, Integer> {
    java.util.Optional<ClienteNatural> findByDni(String dni);
}
