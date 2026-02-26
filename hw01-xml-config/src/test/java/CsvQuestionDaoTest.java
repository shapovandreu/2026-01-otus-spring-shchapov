import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.CsvQuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.exceptions.QuestionReadException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Класс CsvQuestionDao должен")
public class CsvQuestionDaoTest {

    private TestFileNameProvider fileNameProvider;
    private CsvQuestionDao csvQuestionDao;

    @BeforeEach
    void setUp() {
        fileNameProvider = mock(TestFileNameProvider.class);
        csvQuestionDao = new CsvQuestionDao(fileNameProvider);
    }

    @DisplayName("корректно читать CSV файл и возвращать список вопросов")
    @Test
    void shouldCorrectReadCsvFileAndReturnQuestionsList() {
        String testFileName = "questions-test.csv";
        when(fileNameProvider.getTestFileName()).thenReturn(testFileName);

        List<Question> questions = csvQuestionDao.findAll();

        assertNotNull(questions);
        assertEquals(3, questions.size());

        // Проверка первого вопроса
        Question firstQuestion = questions.get(0);
        assertEquals("Is there life on Mars?", firstQuestion.text());
        assertEquals(3, firstQuestion.answers().size());

        Answer firstAnswer = firstQuestion.answers().get(0);
        assertEquals("Science doesn't know this yet", firstAnswer.text());
        assertTrue(firstAnswer.isCorrect());

        // Проверка второго вопроса
        Question secondQuestion = questions.get(1);
        assertEquals("How should resources be loaded form jar in Java?", secondQuestion.text());
        assertEquals(3, secondQuestion.answers().size());

        Answer secondQuestionFirstAnswer = secondQuestion.answers().get(0);
        assertEquals("ClassLoader#geResourceAsStream or ClassPathResource#getInputStream",
                secondQuestionFirstAnswer.text());
        assertTrue(secondQuestionFirstAnswer.isCorrect());

        // Проверка третьего вопроса
        Question thirdQuestion = questions.get(2);
        assertEquals("Which option is a good way to handle the exception?", thirdQuestion.text());
        assertEquals(4, thirdQuestion.answers().size());

        Answer thirdQuestionThirdAnswer = thirdQuestion.answers().get(2);
        assertEquals("Rethrow with wrapping in business exception (for example QuestionReadException)",
                thirdQuestionThirdAnswer.text());
        assertTrue(thirdQuestionThirdAnswer.isCorrect());
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