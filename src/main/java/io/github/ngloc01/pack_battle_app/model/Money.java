package io.github.ngloc01.pack_battle_app.model;

public class Money {
    private String sessionId;
    private int yourMoney;
    private int opponentMoney;

    // Default constructor (required for JSON serialization)
    public Money() {}

    // Constructor with parameters
    public Money(String sessionId, int yourMoney, int opponentMoney) {
        this.sessionId = sessionId;
        this.yourMoney = yourMoney;
        this.opponentMoney = opponentMoney;
    }

    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getYourMoney() {
        return yourMoney;
    }

    public void setYourMoney(int yourMoney) {
        this.yourMoney = yourMoney;
    }

    public int getOpponentMoney() {
        return opponentMoney;
    }

    public void setOpponentMoney(int opponentMoney) {
        this.opponentMoney = opponentMoney;
    }
}