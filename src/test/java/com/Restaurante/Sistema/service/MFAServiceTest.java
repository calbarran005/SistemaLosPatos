package com.Restaurante.Sistema.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de {@link MFAService} (autenticación de dos factores TOTP).
 */
class MFAServiceTest {

    private final MFAService mfaService = new MFAService();

    @Test
    @DisplayName("generateSecretKey devuelve una clave no vacía")
    void generateSecretKey_devuelveClaveValida() {
        String secret = mfaService.generateSecretKey();

        assertThat(secret).isNotBlank();
    }

    @Test
    @DisplayName("dos claves generadas consecutivamente son distintas")
    void generateSecretKey_clavesDistintas() {
        String s1 = mfaService.generateSecretKey();
        String s2 = mfaService.generateSecretKey();

        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    @DisplayName("getQRCodeURL construye una URL otpauth válida")
    void getQRCodeURL_formatoCorrecto() {
        String url = mfaService.getQRCodeURL("user@patos.com", "ABC123");

        assertThat(url)
                .startsWith("otpauth://totp/LosPatos:user@patos.com")
                .contains("secret=ABC123")
                .contains("issuer=LosPatos");
    }

    @Test
    @DisplayName("verifyCode rechaza un código incorrecto")
    void verifyCode_rechazaCodigoInvalido() {
        String secret = mfaService.generateSecretKey();

        // Un código fijo casi con total seguridad no coincide con el TOTP actual
        boolean valido = mfaService.verifyCode(secret, 0);

        assertThat(valido).isFalse();
    }
}
