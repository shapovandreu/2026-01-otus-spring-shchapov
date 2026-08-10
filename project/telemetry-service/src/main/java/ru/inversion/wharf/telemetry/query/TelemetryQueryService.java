package ru.inversion.wharf.telemetry.query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.inversion.wharf.telemetry.domain.TelemetryDocument;
import ru.inversion.wharf.telemetry.repository.TelemetryRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class TelemetryQueryService {

    private final ReactiveMongoTemplate mongo;
    private final TelemetryRepository repository;

    public TelemetryQueryService(ReactiveMongoTemplate mongo, TelemetryRepository repository) {
        this.mongo = mongo;
        this.repository = repository;
    }

    public Flux<TelemetryDocument> eventsByInstallation(UUID installationId, Pageable page) {
        return repository.findByInstallationIdOrderByReceivedAtDesc(installationId, page);
    }

    public Flux<TelemetryDocument> eventsByInstallationInOrg(UUID orgId, UUID installationId, Pageable page) {
        return repository.findByOrgIdAndInstallationIdOrderByReceivedAtDesc(orgId, installationId, page);
    }

    public Flux<TelemetryDocument> eventsByOrg(UUID orgId, Pageable page) {
        return repository.findByOrgIdOrderByReceivedAtDesc(orgId, page);
    }

    public Flux<InstallationStatus> installationStatuses(UUID orgId, long skip, int limit) {
        List<AggregationOperation> stages = new ArrayList<>();
        if (orgId != null) {
            stages.add(match(where("orgId").is(orgId)));
        }
        stages.add(sort(Sort.by(Sort.Direction.DESC, "receivedAt")));
        stages.add(group("installationId")
                .first("installationId").as("installationId")
                .first("orgId").as("orgId")
                .first("productId").as("productId")
                .first("type").as("lastType")
                .first("state").as("lastState")
                .first("releaseId").as("lastReleaseId")
                .first("message").as("lastMessage")
                .first("receivedAt").as("lastSeen"));
        stages.add(sort(Sort.by(Sort.Direction.DESC, "lastSeen")));
        if (skip > 0) {
            stages.add(skip(skip));
        }
        stages.add(limit(limit));

        Aggregation aggregation = newAggregation(stages);
        return mongo.aggregate(aggregation, "telemetry_events", InstallationStatus.class);
    }
}
