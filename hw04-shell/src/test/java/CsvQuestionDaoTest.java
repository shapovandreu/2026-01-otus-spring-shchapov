import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.otus.hw.Application;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.CsvQuestionDao;
import ru.otus.hw.domain.Question;
import ru.otus.hw.exceptions.QuestionReadException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = Application.class)
@DisplayName("Класс CsvQuestionDao должен")
public class CsvQuestionDaoTest {

    private TestFileNameProvider fileNameProvider;
    private CsvQuestionDao csvQuestionDao;

    @BeforeEach
    void setUp() {
        fileNameProvider = mock(TestFileNameProvider.class);
        csvQuestionDao = new CsvQuestionDao(fileNameProvider);
    }

    @DisplayName("выбрасывать исключение при отсутствии файла")
    @Test
    void shouldThrowExceptionWhenFileNotFound() {
        String nonExistentFile = "non-existent-file.csv";
        when(fileNameProvider.getTestFileName()).thenReturn(nonExistentFile);

        assertThrows(QuestionReadException.class, () -> csvQuestionDao.findAll());
    }

    @DisplayName("корректно обрабатывать пустой файл (только заголовок)")
    @Test
    void shouldHandleEmptyFileWithOnlyHeader() {
        String testFileName = "questions-empty.csv";
        when(fileNameProvider.getTestFileName()).thenReturn(testFileName);

        List<Question> questions = csvQuestionDao.findAll();

        assertNotNull(questions);
        assertTrue(questions.isEmpty());
    }
}