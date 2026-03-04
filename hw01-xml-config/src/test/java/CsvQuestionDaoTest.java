import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

        assertAll("Проверка первого вопроса",
                () -> assertQuestion(questions.get(0), "Is there life on Mars?", 3),
                () -> assertCorrectAnswer(questions.get(0).answers().get(0),
                        "Science doesn't know this yet")
        );

        assertAll("Проверка второго вопроса",
                () -> assertQuestion(questions.get(1),
                        "How should resources be loaded form jar in Java?", 3),
                () -> assertCorrectAnswer(questions.get(1).answers().get(0),
                        "ClassLoader#geResourceAsStream or ClassPathResource#getInputStream")
        );

        assertAll("Проверка третьего вопроса",
                () -> assertQuestion(questions.get(2),
                        "Which option is a good way to handle the exception?", 4),
                () -> assertCorrectAnswer(questions.get(2).answers().get(2),
                        "Rethrow with wrapping in business exception (for example QuestionReadException)")
        );
    }

    @DisplayName("корректно читать данные вопроса из CSV строки")
    @ParameterizedTest
    @CsvSource({
            "0, 'Is there life on Mars?', 3, 0, 'Science doesn''t know this yet'",
            "1, 'How should resources be loaded form jar in Java?', 3, 0, 'ClassLoader#geResourceAsStream or ClassPathResource#getInputStream'",
            "2, 'Which option is a good way to handle the exception?', 4, 2, 'Rethrow with wrapping in business exception (for example QuestionReadException)'"
    })
    void shouldCorrectlyReadQuestionFromCsv(
            int questionIndex,
            String questionText,
            int answersCount,
            int correctAnswerIndex,
            String correctAnswerText) {

        String testFileName = "questions-test.csv";
        when(fileNameProvider.getTestFileName()).thenReturn(testFileName);

        List<Question> questions = csvQuestionDao.findAll();

        assertNotNull(questions);
        assertTrue(questions.size() > questionIndex);

        assertQuestion(questions.get(questionIndex), questionText, answersCount);
        assertCorrectAnswer(questions.get(questionIndex).answers().get(correctAnswerIndex),
                correctAnswerText);
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

    private void assertQuestion(Question question, String expectedText, int expectedSizeAnswer) {
        assertEquals(expectedText, question.text());
        assertEquals(expectedSizeAnswer, question.answers().size());
    }

    private void assertCorrectAnswer(Answer answer, String expectedText) {
        assertEquals(expectedText, answer.text());
        assertTrue(answer.isCorrect());
    }
}