package dev.clares.chatgames.gui;

import dev.clares.chatgames.config.*;
import dev.clares.chatgames.game.GameType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GuiSettingsTest {
    @TempDir Path temp;
    @Test void guiAndMenuCommandsRequestOnlyThePrivateScreen() throws Exception {
        ConfigManager config=new ConfigManager(temp);String before=Files.readString(config.dir().resolve("config.json"));
        java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();
        var commands=new dev.clares.chatgames.command.ClientCommands(config,null,null,s->fail("No chat response expected"),t->fail("No chat response expected"),requests::incrementAndGet);
        var run=commands.getClass().getDeclaredMethod("run",String.class);run.setAccessible(true);run.invoke(commands,"gui");run.invoke(commands,"menu");
        assertEquals(2,requests.get());assertEquals(before,Files.readString(config.dir().resolve("config.json")));
    }
    @Test void editingAndCancellingDoesNotChangeLiveOrSavedTimes() throws Exception {
        ConfigManager config=new ConfigManager(temp);String before=Files.readString(config.dir().resolve("config.json"));
        TimeDraft draft=new TimeDraft(config,"trivia");draft.preset(3000,5000);draft.minimum="4.5";
        assertEquals(0,config.get().delayFor("trivia").minMs);assertEquals(1000,config.get().delayFor("trivia").maxMs);
        assertEquals(before,Files.readString(config.dir().resolve("config.json")));
    }
    @Test void savingOwnRangePersistsAndKeepsOtherSettings() {
        ConfigManager config=new ConfigManager(temp);config.get().enabled=false;config.get().games.put("fillout",false);config.get().stats=true;config.save();
        TimeDraft draft=new TimeDraft(config,"trivia");draft.mode=TimeDraft.Mode.RANDOM;draft.minimum="0,5";draft.maximum="1500ms";draft.save();
        ModConfig loaded=new ConfigManager(temp).get();assertEquals(500,loaded.delayFor("trivia").minMs);assertEquals(1500,loaded.delayFor("trivia").maxMs);
        assertFalse(loaded.enabled);assertFalse(loaded.game("fillout"));assertTrue(loaded.stats);assertEquals(0,loaded.delayFor("math").minMs);
    }
    @Test void zeroAndFixedTimesIgnoreInactiveMaximumAndPersist() {
        ConfigManager config=new ConfigManager(temp);TimeDraft draft=new TimeDraft(config,"variable");draft.preset(0,0);draft.maximum="unused";draft.save();
        assertEquals(0,new ConfigManager(temp).get().delayFor("variable").sampleMillis());
    }
    @Test void invalidInputsCannotOverwriteSettings() throws Exception {
        ConfigManager config=new ConfigManager(temp);String before=Files.readString(config.dir().resolve("config.json"));
        for(String input:List.of("", "-1", "61", "NaN", "Infinity", "abc")) {
            TimeDraft draft=new TimeDraft(config,"math");draft.mode=TimeDraft.Mode.RANDOM;draft.minimum=input;draft.maximum="1";
            assertFalse(draft.error().isEmpty());assertThrows(IllegalArgumentException.class,draft::save);assertEquals(before,Files.readString(config.dir().resolve("config.json")));
        }
        TimeDraft reversed=new TimeDraft(config,"math");reversed.preset(5000,3000);assertThrows(IllegalArgumentException.class,reversed::save);
        assertEquals(before,Files.readString(config.dir().resolve("config.json")));
    }
    @Test void inheritingRemovesOnlyThatOverrideAndFollowsGeneralChanges() {
        ConfigManager config=new ConfigManager(temp);TimeDraft own=new TimeDraft(config,"math");own.preset(5000,5000);own.save();
        TimeDraft inherit=new TimeDraft(config,"math");inherit.mode=TimeDraft.Mode.INHERIT;inherit.minimum="unused";inherit.save();
        TimeDraft general=new TimeDraft(config,"");general.preset(100,900);general.save();
        ModConfig loaded=new ConfigManager(temp).get();assertFalse(loaded.gameDelays.containsKey("math"));assertEquals(100,loaded.delayFor("math").minMs);assertEquals(900,loaded.delayFor("math").maxMs);
    }
    @Test void failedDiskSaveRollsBackLiveInterval() throws Exception {
        ConfigManager config=new ConfigManager(temp);TimeDraft draft=new TimeDraft(config,"fillout");draft.preset(5000,5000);
        Path path=config.dir().resolve("config.json");Files.delete(path);Files.createDirectory(path);
        assertThrows(IllegalStateException.class,draft::save);assertFalse(config.get().gameDelays.containsKey("fillout"));assertEquals(0,config.get().delayFor("fillout").minMs);
    }
    @Test void catalogAndPagesCoverRequestedModesWithoutTouchingExcludedGames() {
        Set<GameType> offered=new HashSet<>();GameCatalog.ENTRIES.forEach(e->assertTrue(offered.add(e.type())));
        assertEquals(Set.of(GameType.MATH,GameType.VARIABLE,GameType.TRIVIA,GameType.UNSCRAMBLE,GameType.FILLOUT,GameType.UNREVERSE,GameType.REACTION,GameType.RANDOM,GameType.CLICKABLE,GameType.GUESS_THE_NUMBER),offered);
        for(int[] size:List.of(new int[]{320,240},new int[]{427,240},new int[]{640,360},new int[]{960,540})) {
            GuiLayout l=GuiLayout.main(size[0],size[1]);assertTrue(l.x()>=0);assertTrue(l.y()+l.height()<=size[1]);
            Set<Integer> seen=new HashSet<>();
            for(int page=0;page<l.pages(GameCatalog.ENTRIES.size());page++)for(int slot=0;slot<l.pageSize();slot++) {
                int index=page*l.pageSize()+slot;if(index<GameCatalog.ENTRIES.size())assertTrue(seen.add(index));
                assertTrue(l.cardX(slot)+l.cardWidth()<=size[0]);assertTrue(l.cardY(slot)+52<=l.footer());
            }
            assertEquals(GameCatalog.ENTRIES.size(),seen.size());
        }
    }
}
