package ru.inversion.wharf.catalog.domain;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("release")
public record Release(
        @Id UUID id,
        UUID productId,
        String version,
        Channel channel,
        boolean published,
        String changelog,
        Instant createdAt) {

    public static Release draft(UUID productId, String version, Channel channel, String changelog, Instant now) {
        return new Release(null, productId, version, channel, false, changelog, now);
    }

    public Release publish() {
        return new Release(id, productId, version, channel, true, changelog, createdAt);
    }

    public Release inChannel(Channel newChannel) {
        return new Release(id, productId, version, newChannel, published, changelog, createdAt);
    }

    public Release withDraftDetails(String newVersion, Channel newChannel, String newChangelog) {
        return new Release(id, productId, newVersion, newChannel, published, newChangelog, createdAt);
    }
}
