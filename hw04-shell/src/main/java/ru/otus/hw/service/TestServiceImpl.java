package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Question;
import ru.otus.hw.domain.Student;
import ru.otus.hw.domain.TestResult;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final LocalizedIOService ioService;

    private final QuestionDao questionDao;

    @Override
    public TestResult executeTestFor(Student student) {
        ioService.printLine("");
        ioService.printLineLocalized("TestService.answer.the.questions");
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
        int sizeAnswers = question.answers().size();
        var numberAnswer = ioService.readIntForRangeWithPromptLocalized(
                1, sizeAnswers,
                "TestService.input.response.number",
                "TestService.not.valid.input");
        return question.answers().get(numberAnswer - 1).isCorrect();
    }

    private void printQuestion(Question question) {
        ioService.printFormattedLine("%n%s", question.text());

        var answers = question.answers();
        for (int i = 0; i < answers.size(); i++) {
            ioService.printFormattedLine("%d) %s", i + 1, answers.get(i).text());
        }
    }

}
