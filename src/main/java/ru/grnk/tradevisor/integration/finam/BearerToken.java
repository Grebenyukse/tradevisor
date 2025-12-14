package ru.grnk.tradevisor.integration.finam;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.Base64;
import java.util.concurrent.Executor;

public class BearerToken extends CallCredentials {

    private final String token;

    public BearerToken(String token) {
        validateToken(token);
        this.token = token;
    }

    private void validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("invalid jwt structure");
            }
            Base64.getDecoder().decode(parts[0]);
            Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("token is invalid base64: " + e.getMessage(), e);
        }
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier metadataApplier) {
        executor.execute(() -> {
            try {
                Metadata headers = new Metadata();
                headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                        token
                        );
                metadataApplier.apply(headers);
            } catch (Throwable e) {
                metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }
}
