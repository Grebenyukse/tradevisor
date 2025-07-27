package ru.grnk.tradevisor.integration.finam.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FinamTokenDetailsRs(
        OffsetDateTime created_at,
        OffsetDateTime expires_at,
        List<MDPermission> md_permissions,
        List<String> account_ids
) {
}
