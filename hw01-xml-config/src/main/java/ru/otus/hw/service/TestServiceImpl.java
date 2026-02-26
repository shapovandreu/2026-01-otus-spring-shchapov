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
        ioService.printFormattedLine("%nPlease answer the questions below%n");
        List<Question> questionList = questionDao.findAll();
        for (int i = 0; i < questionList.size(); i++) {
            ioService.printFormattedLine("%d. %s%n", i + 1, questionList.get(i).toString());
        }
    }

}