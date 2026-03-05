package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Question;
import ru.otus.hw.domain.Student;
import ru.otus.hw.domain.TestResult;

import java.text.MessageFormat;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final IOService ioService;

    private final QuestionDao questionDao;

    @Override
    public TestResult executeTestFor(Student student) {
        ioService.printLine("");
        ioService.printFormattedLine("Please answer the questions below%n");
        var questions = questionDao.findAll();
        var testResult = new TestResult(student);

        for (var question: questions) {
            var isAnswerValid = askQuestion(question);
            testResult.applyAnswer(question, isAnswerValid);
        }

        return testResult;
    }

    private boolean askQuestion(Question question) {
        printQuestion(question);
        var numberAnswer = readNumberAnswer(question.answers().size());
        return question.answers().get(numberAnswer - 1).isCorrect();
    }

    private int readNumberAnswer(int sizeAnswers) {
        return ioService.readIntForRangeWithPrompt(
                1, sizeAnswers,
                MessageFormat.format("Please enter the response number(1-{0}).", sizeAnswers),
                MessageFormat.format("Enter a value between 1 and {0}", sizeAnswers));
    }

    private void printQuestion(Question question) {
        ioService.printFormattedLine("%n%s", question.text());

        var answers = question.answers();
        for (int i = 0; i < answers.size(); i++) {
            ioService.printFormattedLine("%d) %s", i + 1, answers.get(i).text());
        }
    }

}