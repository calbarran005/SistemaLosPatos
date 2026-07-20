package com.Restaurante.Sistema.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

@Service
public class MFAService {

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /**
     * Genera una nueva clave secreta para un usuario.
     */
    public String generateSecretKey() {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * Genera la URL del código QR para que el usuario la escanee.
     */
    public String getQRCodeURL(String email, String secretKey) {
        // Usamos un nombre sin espacios para el emisor técnico para máxima compatibilidad
        return String.format("otpauth://totp/LosPatos:%s?secret=%s&issuer=LosPatos", email, secretKey);
    }

    /**
     * Valida el código de 6 dígitos introducido por el usuario.
     */
    public boolean verifyCode(String secretKey, int code) {
        return gAuth.authorize(secretKey, code);
    }
}
