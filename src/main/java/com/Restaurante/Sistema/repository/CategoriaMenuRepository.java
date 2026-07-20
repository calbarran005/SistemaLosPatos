package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.CategoriaMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaMenuRepository extends JpaRepository<CategoriaMenu, Integer> {
}
