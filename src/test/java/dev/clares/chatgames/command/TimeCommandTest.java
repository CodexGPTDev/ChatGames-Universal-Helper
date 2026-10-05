package dev.clares.chatgames.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.clares.chatgames.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TimeCommandTest {
    @TempDir Path temp;

    @Test void updating107UsesFastTimesAndKeepsOtherPreferences() throws Exception {
        Path dir=temp.resolve("config/chatgames-universal-helper");Files.createDirectories(dir);
        Files.writeString(dir.resolve("config.json"),"{\"configRevision\":7,\"enabled\":false,\"stats\":true,\"games\":{\"trivia\":false,\"math\":true},\"delay\":{\"mode\":\"fixed\",\"minMs\":7000,\"maxMs\":7000}}");
        Files.writeString(dir.resolve("trivia-cache.json"),"{\"What biome is home to bamboo?\":\"Mi respuesta del servidor\"}");
        ConfigManager config=new ConfigManager(temp);ModConfig c=config.get();
        assertFalse(c.enabled);assertTrue(c.stats);assertFalse(c.game("trivia"));
        for(String mode:c.games.keySet())check(c.delayFor(mode),"random",0,1000);
        assertEquals(9,c.configRevision);assertTrue(c.gameDelays.isEmpty());
        assertTrue(Files.readString(config.dir().resolve("trivia-cache.json")).contains("Mi respuesta del servidor"),"A timing-only update must keep learned answers");
        for(int i=0;i<10000;i++) {
            long math=c.delayFor("math").sampleMillis(),variable=c.delayFor("variable").sampleMillis();
            assertTrue(math>=0 && math<=1000);assertTrue(variable>=0 && variable<=1000);
        }
    }

    @Test void commandsSetPerModeRangesFixedTimesAndInheritanceAndPersistAfterRestart() throws Exception {
        ConfigManager config=new ConfigManager(temp);var commands=dispatcher(config);
        commands.execute("cgedit settime math random 2s 3s",new Object());
        commands.execute("cgedit settime variable random 3s 5s",new Object());
        commands.execute("cgedit settime fillout 3500ms",new Object());
        commands.execute("cgedit settime random 1.4s 2.4s",new Object());
        ConfigManager loaded=new ConfigManager(temp);ModConfig c=loaded.get();
        check(c.delayFor("math"),"random",2000,3000);check(c.delayFor("variable"),"random",3000,5000);
        check(c.delayFor("fillout"),"fixed",3500,3500);check(c.delayFor("unreverse"),"random",1400,2400);
        dispatcher(loaded).execute("cgedit settime math default",new Object());
        check(new ConfigManager(temp).get().delayFor("math"),"random",1400,2400);
        assertTrue(TimeCommand.execute(loaded,"").getFirst().contains("general"));
        assertTrue(TimeCommand.execute(loaded,"variable").getFirst().contains("3 s–5 s"));
    }

    @Test void legacyGlobalFormsAndRandomModeFormsRemainDistinct() throws Exception {
        ConfigManager config=new ConfigManager(temp);var commands=dispatcher(config);
        commands.execute("cgedit settime 2s",new Object());check(config.get().delayFor("trivia"),"fixed",2000,2000);
        commands.execute("cgedit settime random 1.8s 2.4s",new Object());check(config.get().delayFor("trivia"),"random",1800,2400);
        commands.execute("cgedit settime random 3200ms",new Object());check(config.get().delayFor("random"),"fixed",3200,3200);
        commands.execute("cgedit settime random random 4s 6s",new Object());check(config.get().delayFor("random"),"random",4000,6000);
        check(config.get().delayFor("trivia"),"random",1800,2400);
        commands.execute("cgedit settime random default",new Object());check(config.get().delayFor("random"),"random",1800,2400);
        commands.execute("cgedit settime",new Object());commands.execute("cgedit settime math",new Object());
    }

    @Test void invalidIntervalsDoNotPartiallyChangeOrSaveAnySetting() throws Exception {
        ConfigManager config=new ConfigManager(temp);String before=Files.readString(config.dir().resolve("config.json"));
        for(String input:List.of("math random 5s 3s","variable random 3s bogus","random 5s 3s","NaN","Infinity","1e50s","math -1ms","trivia 61s","variable random 3s 5s extra","default")) {
            assertThrows(IllegalArgumentException.class,()->TimeCommand.execute(config,input),input);
            assertEquals(before,Files.readString(config.dir().resolve("config.json")),input);
            check(config.get().delayFor("math"),"random",0,1000);
            check(config.get().delayFor("variable"),"random",0,1000);
        }
    }

    @Test void upgrading108ClearsOldIntervalsOnceAndZeroPersistsAfterRestart() throws Exception {
        Path dir=temp.resolve("config/chatgames-universal-helper");Files.createDirectories(dir);
        Files.writeString(dir.resolve("config.json"),"{\"configRevision\":8,\"enabled\":false,\"stats\":false,\"gameDelays\":{\"math\":{\"mode\":\"random\",\"minMs\":2000,\"maxMs\":3000},\"variable\":{\"mode\":\"random\",\"minMs\":3000,\"maxMs\":5000},\"fillout\":{\"mode\":\"fixed\",\"minMs\":9000,\"maxMs\":9000}}}");
        ConfigManager config=new ConfigManager(temp);assertFalse(config.get().enabled);
        for(String mode:config.get().games.keySet())check(config.get().delayFor(mode),"random",0,1000);
        var commands=dispatcher(config);
        commands.execute("cgedit settime 0s",new Object());
        commands.execute("cgedit settime math random 0s 1s",new Object());
        commands.execute("cgedit settime variable 0ms",new Object());
        ConfigManager loaded=new ConfigManager(temp);
        check(loaded.get().delayFor("trivia"),"fixed",0,0);assertEquals(0,loaded.get().delayFor("variable").sampleMillis());
        check(loaded.get().delayFor("math"),"random",0,1000);
        commands=dispatcher(loaded);commands.execute("cgedit settime random random 0s 0s",new Object());
        assertEquals(0,loaded.get().delayFor("random").sampleMillis());
    }

    private static CommandDispatcher<Object> dispatcher(ConfigManager config) {
        CommandDispatcher<Object> d=new CommandDispatcher<>();d.register(CommandTree.build(command->{
            assertTrue(command.startsWith("settime"));TimeCommand.execute(config,command.substring(7).strip());return 1;
        }));return d;
    }
    private static void check(ModConfig.Delay d,String mode,int min,int max) {
        assertEquals(mode,d.mode);assertEquals(min,d.minMs);assertEquals(max,d.maxMs);
    }
}
