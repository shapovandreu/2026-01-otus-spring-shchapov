package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Question;

import java.util.List;

@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final IOService ioService;

    private final QuestionDao questionDao;

    @Override
    public void executeTest() {
        ioService.printFormattedLine("%nPlease answer the questions below");
        List<Question> questionList = questionDao.findAll();
        for (int i = 0; i < questionList.size(); i++) {
            printQuestion(i + 1, questionList.get(i));
        }
    }

    private void printQuestion(int numberQuestion, Question question) {
        ioService.printFormattedLine("%n%d. %s", numberQuestion, question.text());

        var answers = question.answers();
        for (int i = 0; i < answers.size(); i++) {
            ioService.printFormattedLine("%d) %s", i + 1, answers.get(i).text());
        }
    }

}