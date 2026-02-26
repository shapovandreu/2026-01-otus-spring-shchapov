package ru.otus.hw.dao;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.dto.QuestionDto;
import ru.otus.hw.domain.Question;
import ru.otus.hw.exceptions.QuestionReadException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CsvQuestionDao implements QuestionDao {

    private static final String MSG_ERROR = "Error when reading a list of questions from a file";

    private final TestFileNameProvider fileNameProvider;

    @Override
    public List<Question> findAll() {
        Reader reader = createReaderForFileName(fileNameProvider.getTestFileName());

        CsvToBean<QuestionDto> csvToBean = buildQuestionCsvBuild(reader);

        List<QuestionDto> questionDtoList = csvToBean.parse();

        return toDomainObjects(questionDtoList);
    }

    private Reader createReaderForFileName(String fileName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        if (inputStream == null) {
            throw new QuestionReadException(MSG_ERROR);
        }

        return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    }

    private CsvToBean<QuestionDto> buildQuestionCsvBuild(Reader reader) {
        return new CsvToBeanBuilder<QuestionDto>(reader)
                .withType(QuestionDto.class)
                .withSkipLines(1)
                .withSeparator(';')
                .build();
    }

    private List<Question> toDomainObjects(List<QuestionDto> questionDtoList) {
        return questionDtoList.stream()
                .map(QuestionDto::toDomainObject)
                .collect(Collectors.toList());
    }

}
