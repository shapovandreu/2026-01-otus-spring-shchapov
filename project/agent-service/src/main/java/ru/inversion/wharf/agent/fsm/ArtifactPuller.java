package ru.inversion.wharf.agent.fsm;

import ru.inversion.wharf.agent.verify.ManifestDoc;

public interface ArtifactPuller {

    void pull(ManifestDoc manifest);
}
