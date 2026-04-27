import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.CsvQuestionDao;
import ru.otus.hw.domain.Question;
import ru.otus.hw.exceptions.QuestionReadException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = CsvQuestionDao.class)
@DisplayName("Класс CsvQuestionDao должен")
public class CsvQuestionDaoTest {

    @MockitoBean
    private TestFileNameProvider fileNameProvider;
    private CsvQuestionDao csvQuestionDao;

    @BeforeEach
    void setUp() {
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