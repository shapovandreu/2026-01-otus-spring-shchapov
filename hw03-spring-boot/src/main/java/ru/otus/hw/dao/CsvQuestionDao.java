package ru.otus.hw.dao;

import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.dto.QuestionDto;
import ru.otus.hw.domain.Question;
import ru.otus.hw.exceptions.QuestionReadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CsvQuestionDao implements QuestionDao {
    private static final String ERROR_MESSAGE = "Error when reading a list of questions from a file";

    private final TestFileNameProvider fileNameProvider;

    @Override
    public List<Question> findAll() {
        String fileName = fileNameProvider.getTestFileName();

        try (Reader reader = createReaderForFileName(fileName)) {
            return readQuestionCsvFile(reader);
        } catch (IOException e) {
            throw new QuestionReadException(ERROR_MESSAGE, e);
        }
    }

    private Reader createReaderForFileName(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new QuestionReadException(ERROR_MESSAGE);
        }

        return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    }

    private List<Question> readQuestionCsvFile(Reader reader) {
        try {
            return new CsvToBeanBuilder<QuestionDto>(reader)
                    .withType(QuestionDto.class)
                    .withSkipLines(1)
                    .withSeparator(';')
                    .build()
                    .stream()
                    .map(QuestionDto::toDomainObject)
                    .toList();
        } catch (Exception e) {
            throw new QuestionReadException(ERROR_MESSAGE, e);
        }
    }

}