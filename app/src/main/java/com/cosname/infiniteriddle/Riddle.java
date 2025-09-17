package com.cosname.infiniteriddle;

/**
 * Represents a single riddle with its question, answer, hint, and difficulty level.
 */
public class Riddle {
    private final String question;
    private final String answer;
    private final String hint;
    private final int difficulty;

    /**
     * @param question   The text of the riddle question
     * @param answer     The correct answer for the riddle
     * @param hint       A hint to help solve the riddle
     * @param difficulty Difficulty level from 1 (easiest) to 5 (hardest)
     */
    public Riddle(String question, String answer, String hint, int difficulty) {
        this.question = question;
        this.answer = answer;
        this.hint = hint;
        // ensure difficulty stays within 1-5
        this.difficulty = Math.max(1, Math.min(difficulty, 5));
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public String getHint() {
        return hint;
    }
    public String getDifficultyStars() {
        StringBuilder stars = new StringBuilder();
        // filled stars
        for (int i = 0; i < difficulty; i++) {
            stars.append("★");
            if (i < 4) stars.append(" ");
        }
        // empty stars
        for (int i = difficulty; i < 5; i++) {
            if (stars.length() > 0) stars.append(" ");
            stars.append("☆");
        }
        return stars.toString();
    }
    public int getDifficulty() {
        return difficulty;
    }

}
