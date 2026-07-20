package com.Restaurante.Sistema.service;

import com.Restaurante.Sistema.entity.Mesa;
import com.Restaurante.Sistema.repository.MesaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link MesaService} usando Mockito.
 * No levantan el contexto de Spring ni necesitan base de datos.
 */
@ExtendWith(MockitoExtension.class)
class MesaServiceTest {

    @Mock
    private MesaRepository mesaRepository;

    @InjectMocks
    private MesaService mesaService;

    @Test
    @DisplayName("listarTodas delega en el repositorio")
    void listarTodas_devuelveLasMesasDelRepositorio() {
        Mesa m1 = new Mesa();
        Mesa m2 = new Mesa();
        when(mesaRepository.findAll()).thenReturn(List.of(m1, m2));

        List<Mesa> resultado = mesaService.listarTodas();

        assertThat(resultado).containsExactly(m1, m2);
        verify(mesaRepository).findAll();
    }

    @Test
    @DisplayName("guardar genera un qrToken cuando la mesa no tiene uno")
    void guardar_generaQrTokenSiEstaVacio() {
        Mesa mesa = new Mesa();
        mesa.setNumero(5);
        mesa.setCapacidad(4);
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> inv.getArgument(0));

        Mesa guardada = mesaService.guardar(mesa);

        assertThat(guardada.getQrToken()).isNotBlank();
        // Debe ser un UUID válido (36 caracteres con guiones)
        assertThat(guardada.getQrToken()).hasSize(36);
        verify(mesaRepository).save(mesa);
    }

    @Test
    @DisplayName("guardar respeta el qrToken existente")
    void guardar_noSobreescribeQrTokenExistente() {
        Mesa mesa = new Mesa();
        mesa.setQrToken("token-existente");
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> inv.getArgument(0));

        Mesa guardada = mesaService.guardar(mesa);

        assertThat(guardada.getQrToken()).isEqualTo("token-existente");
    }

    @Test
    @DisplayName("ocupar cambia el estado de la mesa a OCUPADA")
    void ocupar_poneEstadoOcupada() {
        Mesa mesa = new Mesa();
        mesa.setEstado(Mesa.EstadoMesa.LIBRE);

        mesaService.ocupar(mesa);

        assertThat(mesa.getEstado()).isEqualTo(Mesa.EstadoMesa.OCUPADA);
        verify(mesaRepository).save(mesa);
    }

    @Test
    @DisplayName("liberar pone la mesa en LIBRE cuando existe")
    void liberar_poneEstadoLibreSiExiste() {
        Mesa mesa = new Mesa();
        mesa.setEstado(Mesa.EstadoMesa.OCUPADA);
        when(mesaRepository.findById(1)).thenReturn(Optional.of(mesa));

        mesaService.liberar(1);

        ArgumentCaptor<Mesa> captor = ArgumentCaptor.forClass(Mesa.class);
        verify(mesaRepository).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(Mesa.EstadoMesa.LIBRE);
    }

    @Test
    @DisplayName("liberar no hace nada cuando la mesa no existe")
    void liberar_noGuardaSiNoExiste() {
        when(mesaRepository.findById(99)).thenReturn(Optional.empty());

        mesaService.liberar(99);

        verify(mesaRepository, never()).save(any());
    }

    @Test
    @DisplayName("eliminar delega el borrado por id en el repositorio")
    void eliminar_borraPorId() {
        mesaService.eliminar(7);

        verify(mesaRepository).deleteById(7);
    }
}
