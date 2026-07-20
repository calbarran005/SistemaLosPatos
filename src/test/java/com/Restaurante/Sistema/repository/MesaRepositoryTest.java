package com.Restaurante.Sistema.repository;

import com.Restaurante.Sistema.entity.Mesa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración de {@link MesaRepository} contra una base de datos
 * H2 en memoria ({@code @DataJpaTest} auto-configura el datasource embebido).
 */
@DataJpaTest
class MesaRepositoryTest {

    @Autowired
    private MesaRepository mesaRepository;

    private Mesa nuevaMesa(int numero, String qrToken) {
        Mesa mesa = new Mesa();
        mesa.setNumero(numero);
        mesa.setCapacidad(4);
        mesa.setEstado(Mesa.EstadoMesa.LIBRE);
        mesa.setQrToken(qrToken);
        return mesa;
    }

    @Test
    @DisplayName("save persiste la mesa y le asigna un id")
    void save_asignaId() {
        Mesa guardada = mesaRepository.save(nuevaMesa(1, "token-1"));

        assertThat(guardada.getId_mesa()).isNotNull();
    }

    @Test
    @DisplayName("findByNumero recupera la mesa por su número")
    void findByNumero_encuentraLaMesa() {
        mesaRepository.save(nuevaMesa(10, "token-10"));

        Optional<Mesa> encontrada = mesaRepository.findByNumero(10);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getCapacidad()).isEqualTo(4);
    }

    @Test
    @DisplayName("findByQrToken recupera la mesa por su token QR")
    void findByQrToken_encuentraLaMesa() {
        mesaRepository.save(nuevaMesa(20, "token-20"));

        Optional<Mesa> encontrada = mesaRepository.findByQrToken("token-20");

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getNumero()).isEqualTo(20);
    }

    @Test
    @DisplayName("findByNumero devuelve vacío si la mesa no existe")
    void findByNumero_vacioSiNoExiste() {
        Optional<Mesa> encontrada = mesaRepository.findByNumero(999);

        assertThat(encontrada).isEmpty();
    }
}
