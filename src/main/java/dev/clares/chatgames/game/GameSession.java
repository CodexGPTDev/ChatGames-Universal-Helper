package dev.clares.chatgames.game;
import java.util.UUID;
public final class GameSession {
    public final String id = UUID.randomUUID().toString();
    public GameType type = GameType.UNKNOWN;
    public String question = "", normalizedQuestion = "", answer = "", source = "";
    public long startTime = System.currentTimeMillis(), scheduledSendTime;
    public long automationEpoch, gameEpoch;
    public String wordContext = "";
    public volatile GameState status = GameState.DETECTED;
    public volatile long scheduleVersion;
    public synchronized long prepareSend(long sendTime) {
        scheduledSendTime=sendTime;
        status=GameState.WAITING;
        return ++scheduleVersion;
    }
    public synchronized boolean claimSend(long version) {
        if (status!=GameState.WAITING || scheduleVersion!=version) return false;
        status=GameState.SOLVING;
        return true;
    }
    public synchronized void finishSend(long version,boolean sent) {
        if (scheduleVersion==version && status==GameState.SOLVING) status=sent?GameState.ANSWERED:GameState.CANCELLED;
    }
    public synchronized void cancelSend() { ++scheduleVersion; status=GameState.CANCELLED; }
    public DetectionConfidence confidence = DetectionConfidence.LOW;
    public int guesses, lower, upper;
    public boolean simulation;
    public boolean capitalizationRetried;
}
