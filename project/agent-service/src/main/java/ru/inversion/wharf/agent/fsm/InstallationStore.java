package ru.inversion.wharf.agent.fsm;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class InstallationStore {

    private final ConcurrentHashMap<UUID, Installation> byId = new ConcurrentHashMap<>();

    public Optional<Installation> find(UUID installationId) {
        return Optional.ofNullable(byId.get(installationId));
    }

    public void save(Installation installation) {
        byId.put(installation.installationId(), installation);
    }

    public Collection<Installation> all() {
        return byId.values();
    }
}
