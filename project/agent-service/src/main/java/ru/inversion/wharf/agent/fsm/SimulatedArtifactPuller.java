package ru.inversion.wharf.agent.fsm;

import ru.inversion.wharf.agent.verify.ManifestDoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedArtifactPuller implements ArtifactPuller {

    private static final Logger log = LoggerFactory.getLogger(SimulatedArtifactPuller.class);

    @Override
    public void pull(ManifestDoc manifest) {
        log.debug("PULLING (симуляция): релиз {} версии {} — забирать нечего, реестра на стенде нет",
                manifest.releaseId(), manifest.version());
    }
}
