package ru.grnk.tradevisor.acollect.news.dto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class NewsHasher {

    public static String calculateHash(String title,
                                       String description,
                                       String publishedAt) {
        // Обрабатываем NULL-значения
        String safeTitle = (title != null) ? title : "";
        String safeDescription = (description != null) ? description : "";
        String formattedDate =  (description != null) ? publishedAt : "";

        // Конкатенация полей
        String combined = safeTitle + safeDescription + formattedDate;

        // Вычисление MD5
        return md5Hash(combined);
    }

    private static String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
