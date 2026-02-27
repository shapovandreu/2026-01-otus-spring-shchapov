import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.CsvQuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.exceptions.QuestionReadException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        assertQuestion(
                questions.get(0),
                "Is there life on Mars?",
                3
        );
        assertCorrectAnswer(
                questions.get(0).answers().get(0),
                "Science doesn't know this yet"
        );

        assertQuestion(
                questions.get(1),
                "How should resources be loaded form jar in Java?",
                3
        );
        assertCorrectAnswer(
                questions.get(1).answers().get(0),
                "ClassLoader#geResourceAsStream or ClassPathResource#getInputStream"
        );

        assertQuestion(
                questions.get(2),
                "Which option is a good way to handle the exception?",
                4
        );
        assertCorrectAnswer(
                questions.get(2).answers().get(2),
                "Rethrow with wrapping in business exception (for example QuestionReadException)"
        );
    }

    private void assertQuestion(Question question, String asserText, int assertSizeAnswer) {
        assertEquals(asserText, question.text());
        assertEquals(assertSizeAnswer, question.answers().size());
    }

    private void assertCorrectAnswer(Answer answer, String assertText) {
        assertEquals(assertText, answer.text());
        assertTrue(answer.isCorrect());
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