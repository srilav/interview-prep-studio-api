package dev.prepstudio.ai;

public record LlmTurn(Speaker speaker, String text) {

    public enum Speaker { USER, ASSISTANT }

    public static LlmTurn user(String text) {
        return new LlmTurn(Speaker.USER, text);
    }

    public static LlmTurn assistant(String text) {
        return new LlmTurn(Speaker.ASSISTANT, text);
    }
}
