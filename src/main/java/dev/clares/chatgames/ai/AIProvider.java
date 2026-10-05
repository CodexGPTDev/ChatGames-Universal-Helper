package dev.clares.chatgames.ai;
import java.util.Optional;
public interface AIProvider { Optional<String> ask(String instruction,String question); }
