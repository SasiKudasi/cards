package ru.testtask.cards.utilits.encription;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CardEncryptionUtil {
    private static final String SECRET_KEY = "1234567890123456"; // 16 символов = 128-bit ключ
    private static final String ALGORITHM = "AES";

    public static String encrypt(String cardNumber) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(cardNumber.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка при шифровании номера карты", ex);
        }
    }

    public static String decrypt(String encryptedCardNumber) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedCardNumber);
            return new String(cipher.doFinal(decodedBytes));
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка при дешифровке номера карты", ex);
        }
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
