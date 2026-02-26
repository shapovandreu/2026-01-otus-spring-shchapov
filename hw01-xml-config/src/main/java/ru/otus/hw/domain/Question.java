package ru.otus.hw.domain;

import org.apache.commons.collections4.ListUtils;

import java.util.List;

public record Question(String text, List<Answer> answers) {
    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer(text);
        List<Answer> answerList = ListUtils.emptyIfNull(answers);
        for (int i = 0; i < answerList.size(); i++) {
            stringBuffer.append("\n").append(i + 1).append(") ").append(answerList.get(i).text());
        }
        return stringBuffer.toString();
    }
}
