package ru.otus.hw.batch;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IdMappingRegistry {

    private final Map<Long, String> authorIds = new ConcurrentHashMap<>();

    private final Map<Long, String> genreIds = new ConcurrentHashMap<>();

    private final Map<Long, String> bookIds = new ConcurrentHashMap<>();

    public String mapAuthor(long sourceId) {
        return register(authorIds, sourceId);
    }

    public String mapGenre(long sourceId) {
        return register(genreIds, sourceId);
    }

    public String mapBook(long sourceId) {
        return register(bookIds, sourceId);
    }

    public String resolveAuthor(long sourceId) {
        return resolve(authorIds, sourceId, "author");
    }

    public String resolveGenre(long sourceId) {
        return resolve(genreIds, sourceId, "genre");
    }

    public String resolveBook(long sourceId) {
        return resolve(bookIds, sourceId, "book");
    }

    public void clear() {
        authorIds.clear();
        genreIds.clear();
        bookIds.clear();
    }

    private String register(Map<Long, String> mapping, long sourceId) {
        String targetId = new ObjectId().toString();
        mapping.put(sourceId, targetId);
        return targetId;
    }

    private String resolve(Map<Long, String> mapping, long sourceId, String entity) {
        String targetId = mapping.get(sourceId);
        if (targetId == null) {
            throw new IllegalStateException(
                    "No migrated %s found for source id %d".formatted(entity, sourceId));
        }
        return targetId;
    }
}
