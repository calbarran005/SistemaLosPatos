package com.Restaurante.Sistema.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios de {@link QRCodeService} (generación de imágenes QR).
 */
class QRCodeServiceTest {

    private final QRCodeService qrCodeService = new QRCodeService();

    @Test
    @DisplayName("generarQR produce una imagen PNG no vacía")
    void generarQR_devuelvePngValido() {
        byte[] imagen = qrCodeService.generarQR("https://lospatos.com/mesa/1", 200, 200);

        assertThat(imagen).isNotEmpty();
        // Firma de un archivo PNG: 0x89 'P' 'N' 'G'
        assertThat(imagen[0] & 0xFF).isEqualTo(0x89);
        assertThat(imagen[1]).isEqualTo((byte) 'P');
        assertThat(imagen[2]).isEqualTo((byte) 'N');
        assertThat(imagen[3]).isEqualTo((byte) 'G');
    }

    @Test
    @DisplayName("generarQR lanza excepción ante dimensiones inválidas")
    void generarQR_dimensionesInvalidasLanzaExcepcion() {
        assertThatThrownBy(() -> qrCodeService.generarQR("contenido", -10, -10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error generando QR");
    }
}
