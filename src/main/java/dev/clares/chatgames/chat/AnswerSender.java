package dev.clares.chatgames.chat;

public interface AnswerSender {
    boolean send(String answer);
    boolean clickCommand(String command);
}
