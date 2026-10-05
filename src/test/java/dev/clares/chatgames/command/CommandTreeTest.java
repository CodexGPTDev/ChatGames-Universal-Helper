package dev.clares.chatgames.command;

import com.mojang.brigadier.CommandDispatcher;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandTreeTest {
    @Test void suggestsCommandsModesTogglesAndHelpPages() throws Exception {
        CommandDispatcher<Object> dispatcher=new CommandDispatcher<>();
        dispatcher.register(CommandTree.build(command->1));
        assertEquals(Set.of("on","off","help","status","testlog","last","cache","ai","debug","stats","game","settime","test","simulate","gui","menu"),suggest(dispatcher,"cgedit "));
        assertEquals(Set.of("help"),suggest(dispatcher,"cgedit h"));
        assertEquals(Set.of("unreverse","unscramble"),suggest(dispatcher,"cgedit game un"));
        assertEquals(Set.of("on","off"),suggest(dispatcher,"cgedit game unscramble "));
        assertEquals(Set.of("on","off"),suggest(dispatcher,"cgedit stats "));
        assertEquals(Set.of("1","2","3"),suggest(dispatcher,"cgedit help "));
        assertEquals(Set.of("stats"),suggest(dispatcher,"cgedit cache "));
        assertTrue(suggest(dispatcher,"cgedit settime ").containsAll(Set.of("math","variable","fillout","random")));
        assertTrue(suggest(dispatcher,"cgedit settime math ").containsAll(Set.of("random","default","0s","1s","2s","2000ms")));
        assertTrue(suggest(dispatcher,"cgedit settime variable random 3s ").contains("5s"));
        assertEquals(Set.of("status"),suggest(dispatcher,"cgedit ai "));
    }
    @Test void executionPreservesTextAndSupportsBothDelayForms() throws Exception {
        List<String> executed=new ArrayList<>();
        CommandDispatcher<Object> dispatcher=new CommandDispatcher<>();
        dispatcher.register(CommandTree.build(command->{executed.add(command);return 1;}));
        for(String command:List.of("gui","menu","on","off","help 2","game trivia on","test unscramble uMics icsD","test trivia ¿En qué planeta vivimos?","simulate You have 20 seconds to unscramble: `Cperere`","settime 2s","settime random 1.8s 2.4s")) {
            assertEquals(1,dispatcher.execute("cgedit "+command,new Object()));
            assertEquals(command,executed.getLast());
        }
        assertEquals(1,dispatcher.execute("cgedit",new Object()));
        assertEquals("help",executed.getLast());
    }
    private static Set<String> suggest(CommandDispatcher<Object> dispatcher,String input) throws Exception {
        var parsed=dispatcher.parse(input,new Object());
        var suggestions=dispatcher.getCompletionSuggestions(parsed).get();
        Set<String> result=new HashSet<>();
        suggestions.getList().forEach(item->result.add(item.getText()));
        return result;
    }
}
