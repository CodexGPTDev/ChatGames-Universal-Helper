package dev.clares.chatgames.game;

import dev.clares.chatgames.ai.*;
import dev.clares.chatgames.chat.*;
import dev.clares.chatgames.config.*;
import dev.clares.chatgames.solver.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

public final class GameManager implements AutoCloseable {
    private final ConfigManager config;
    private final Consumer<Runnable> clientThread;
    private final BooleanSupplier playerReady;
    private final AnswerSender sender;
    private final CompletableFuture<WordSolver> words;
    private final TriviaSolver trivia;
    private final AIProvider ai;
    private final ScheduledExecutorService queue = Executors.newSingleThreadScheduledExecutor(r -> { Thread t=new Thread(r,"ChatGames-logic"); t.setDaemon(true); return t; });
    private final ExecutorService aiPool = Executors.newSingleThreadExecutor(r -> {Thread t=new Thread(r,"ChatGames-ai"); t.setDaemon(true); return t; });
    private final Map<String,Long> seen = new HashMap<>();
    private final Map<String,Long> sentAnswers = new ConcurrentHashMap<>();
    private final Consumer<String> output;
    private final Object responseLock = new Object();
    private final AtomicLong automationEpoch = new AtomicLong();
    private final Map<GameType,Long> gameEpochs = new ConcurrentHashMap<>();
    private volatile GameSession current,last;
    private GameSession completedSession;
    private long completedAt;
    private ChallengeBuffer buffer;
    private long bufferEpoch;
    private ChallengeBuffer simulationBuffer;
    private volatile ScheduledFuture<?> pending;
    private volatile String serverKey="";
    public GameManager(ConfigManager config,MinecraftClient client,Consumer<String> output) {
        this(config,output,task->client.execute(task),()->client.player!=null,new ChatAnswerSender(client));
    }
    GameManager(ConfigManager config,Consumer<String> output,Consumer<Runnable> clientThread,BooleanSupplier playerReady,AnswerSender sender) {
        this.config=config;this.output=output;this.clientThread=clientThread;this.playerReady=playerReady;this.sender=sender;
        words=CompletableFuture.supplyAsync(() -> new WordSolver(config.dir()),aiPool);
        trivia=new TriviaSolver(config.dir()); ai=new HttpAIProvider(config.get().ai);
        queue.scheduleAtFixedRate(this::flush, 100,100,TimeUnit.MILLISECONDS);
    }
    public GameSession last() { return last; }
    public GameSession current() { return current; }
    public int cacheSize() { WordSolver loaded=words.getNow(null); return (loaded==null?0:loaded.cacheSize())+trivia.cacheSize(); }
    public void onLog(String line) {
        long epoch=automationEpoch.get();
        if (!config.get().enabled) return;
        queue.execute(() -> {if(epoch==automationEpoch.get())accept(line);});
    }
    public void onRich(Text text) {
        long epoch=automationEpoch.get();
        if (!config.get().enabled) return;
        queue.execute(() -> {
        if (!config.get().enabled || epoch!=automationEpoch.get()) return;
        String line=text.getString();
        // Both input sources must close the same session before inspecting rich payloads.
        if (line.startsWith("[CGH]")) return;
        if (GameDetector.winner(line) || GameDetector.capitalizationHint(line)) { accept(line); return; }
        if (GameDetector.administrative(line)) return;
        List<ComponentInspector.Rich> rich=ComponentInspector.inspect(text);
        if (rich.isEmpty()) return;
        boolean click=line.toLowerCase(Locale.ROOT).matches("(?s).*(?:click|haz clic|pulsa).*"), hover=line.toLowerCase(Locale.ROOT).matches("(?s).*(?:hover|pasa el cursor|escribe|write|type).*" );
        if (click && config.get().game("clickable")) {
            for (var r:rich) if (!r.command().isEmpty()) {
                if (!openRich(GameType.CLICKABLE,line,r.command(),epoch)) return;
                GameSession s=current;
                schedule(s, () -> sender.clickCommand(r.command()));
                return;
            }
        }
        if (hover && config.get().game("hoverable")) {
            for (var r:rich) if (!r.hover().isBlank()) {if (openRich(GameType.HOVERABLE,line,r.hover(),epoch)) schedule(current,()->sender.send(r.hover())); return;}
        }
    }); }
    /** Commands invalidate old work immediately, including callbacks already on the client thread. */
    public void setEnabled(boolean enabled) {
        synchronized(responseLock) {
            if (config.get().enabled==enabled) return;
            config.get().enabled=enabled;
            automationEpoch.incrementAndGet();
            cancel();
            queue.execute(() -> {cancel();buffer=null;completedSession=null;seen.clear();sentAnswers.clear();});
        }
        config.save();
    }
    public void setGameEnabled(GameType type,boolean enabled) {
        synchronized(responseLock) {
            config.get().games.put(type.name().toLowerCase(Locale.ROOT),enabled);
            if (!enabled) {
                gameEpochs.merge(type,1L,Long::sum);
                if(current!=null && current.type==type)cancel();
            }
            queue.execute(() -> {
                // Buffered prompts belong to the old control state and must not resume on re-enable.
                if(buffer!=null && GameDetector.detect(buffer.text(),true).map(d->d.type()==type).orElse(false))buffer=null;
                seen.keySet().removeIf(key->key.contains("|"+type+"|"));
                if (!enabled && current!=null && current.type==type)cancel();
            });
        }
        config.save();
    }
    private void tagSession(GameSession s,long epoch) {
        s.automationEpoch=epoch;s.gameEpoch=gameEpochs.getOrDefault(s.type,0L);
    }
    private boolean canRespond(GameSession s) {
        return config.get().enabled && config.get().game(s.type.name())
            && s.automationEpoch==automationEpoch.get() && s.gameEpoch==gameEpochs.getOrDefault(s.type,0L);
    }
    private boolean openRich(GameType t,String line,String value,long epoch) {
        String key=serverKey+"|rich|"+t+"|"+line+"|"+value;
        if (seen.putIfAbsent(key,System.currentTimeMillis()) != null) return false;
        synchronized(responseLock) {
            if(epoch!=automationEpoch.get() || !config.get().enabled || !config.get().game(t.name()))return false;
            cancel(); completedSession=null; current=new GameSession(); current.type=t; current.question=line; current.answer=value; current.source="COMPONENT"; current.confidence=DetectionConfidence.HIGH; current.status=GameState.WAITING; last=current;
            tagSession(current,epoch);
        }
        debug("Detected "+t+" / "+value);
        return true;
    }
    private void accept(String line) {
        if (!config.get().enabled) return;
        if (GameDetector.administrative(line)) {
            cancel();buffer=null;completedSession=null;seen.clear();sentAnswers.clear();return;
        }
        debug("Log message: "+line);
        if (GameDetector.winner(line)) {
            // Some servers announce a winner header, then the answer on a later line.
            if (current!=null) {completedSession=current;completedAt=System.currentTimeMillis();}
            GameSession completed=System.currentTimeMillis()-completedAt<5000?completedSession:null;
            if (completed!=null) GameDetector.announcedAnswer(line).ifPresent(solution->remember(completed,solution));
            debug("Winner detected");buffer=null;cancel();return;
        }
        if (GameDetector.capitalizationHint(line)) {
            GameSession s=current;
            // Only the server's explicit rejection permits one capitalization correction.
            if (s!=null && s.status==GameState.ANSWERED && !s.capitalizationRetried
                    && EnumSet.of(GameType.FILLOUT,GameType.UNSCRAMBLE).contains(s.type)
                    && !s.answer.isEmpty() && Character.isLetter(s.answer.charAt(0))) {
                s.capitalizationRetried=true;
                char first=s.answer.charAt(0);
                s.answer=(Character.isUpperCase(first)?Character.toLowerCase(first):Character.toUpperCase(first))+s.answer.substring(1);
                schedule(s,()->sender.send(s.answer));
            }
            return;
        }
        if (current!=null && current.type==GameType.GUESS_THE_NUMBER && current.status==GameState.ANSWERED && (GameDetector.hintHigher(line) || GameDetector.hintLower(line))) {
            if (GameDetector.hintHigher(line)) current.lower=Integer.parseInt(current.answer)+1; else current.upper=Integer.parseInt(current.answer)-1;
            if (current.lower<=current.upper && current.guesses<config.get().maxGuesses) {
                current.answer=Integer.toString(current.lower+(current.upper-current.lower)/2); schedule(current,()->sender.send(current.answer));
            } return;
        }
        if (GameDetector.banner(line)) {
            if (buffer!=null && buffer.size()>1) flushNow();
            if (buffer==null) { buffer=new ChallengeBuffer();bufferEpoch=automationEpoch.get(); debug("Session opened"); }
            buffer.add(line); return;
        }
        boolean context=buffer!=null;
        if (context) { buffer.add(line); return; }
        if (GameDetector.detect(line,false).isPresent()) { buffer=new ChallengeBuffer();bufferEpoch=automationEpoch.get(); buffer.add(line); }
    }
    private void flush() { if (buffer!=null && buffer.ready(System.currentTimeMillis())) flushNow(); seen.entrySet().removeIf(e -> System.currentTimeMillis()-e.getValue()>60000); sentAnswers.entrySet().removeIf(e -> System.currentTimeMillis()-e.getValue()>30000); }
    private void flushNow() {
        ChallengeBuffer b=buffer; buffer=null;
        long epoch=bufferEpoch;
        if (b==null || !config.get().enabled || epoch!=automationEpoch.get()) return;
        Optional<GameDetector.Detection> d=GameDetector.detect(b.text(), b.size()>1 || GameDetector.banner(b.text()));
        if (d.isEmpty()) { debug("No confident challenge in: "+b.text()); return; }
        GameDetector.Detection detected=d.get();
        if (detected.confidence()==DetectionConfidence.LOW || !config.get().game(detected.type().name())) {
            if (!EnumSet.of(GameType.HUNT,GameType.MINE,GameType.PLACE,GameType.FISH,GameType.EAT,GameType.CRAFT,GameType.FURNACE).contains(detected.type())) return;
        }
        String key=serverKey+"|"+detected.type()+"|"+TriviaSolver.normalize(detected.question());
        if (seen.containsKey(key)) return;
        seen.put(key,System.currentTimeMillis());
        cancel();completedSession=null;
        GameSession s=new GameSession(); s.type=detected.type(); s.question=detected.question(); s.normalizedQuestion=key; s.confidence=detected.confidence(); s.status=GameState.SOLVING; current=last=s;
        tagSession(s,epoch);s.wordContext=GameDetector.wordContext(b.text());
        debug("Detected "+s.type+" / "+s.question);
        if (EnumSet.of(GameType.HUNT,GameType.MINE,GameType.PLACE,GameType.FISH,GameType.EAT,GameType.CRAFT,GameType.FURNACE).contains(s.type)) {s.status=GameState.DETECTED; output.accept("Objetivo "+s.type+": "+s.question); return;}
        if (s.type==GameType.GUESS_THE_NUMBER) {
            String[] ends=s.question.split("-"); s.lower=Integer.parseInt(ends[0]); s.upper=Integer.parseInt(ends[1]);
            s.answer=Integer.toString(s.lower+(s.upper-s.lower)/2); s.source="LOCAL";
            schedule(s,()->sender.send(s.answer)); return;
        }
        Optional<String> answer=s.type==GameType.FILLOUT?solveFillout(s.question,s.wordContext):solveLocal(s.type,s.question);
        if (answer.isPresent()) { s.answer=answer.get(); s.source="LOCAL"; schedule(s,()->sender.send(s.answer)); return; }
        if (config.get().ai.enabled && EnumSet.of(GameType.TRIVIA,GameType.UNSCRAMBLE,GameType.FILLOUT).contains(s.type)) {
            aiPool.submit(() -> {
                String instruction=s.type==GameType.TRIVIA ? "Responde únicamente con la respuesta más corta correcta. No expliques nada." : s.type==GameType.UNSCRAMBLE ? "Ordena las letras en una palabra española o inglesa probable. Solo la palabra." : "Completa la palabra; los guiones son letras desconocidas. Solo la palabra.";
                Optional<String> result=ai.ask(instruction,s.question);
                queue.execute(() -> {
                    if (current!=s || s.status==GameState.CANCELLED || !canRespond(s)) return;
                    Optional<String> valid=result.filter(a -> switch(s.type) {
                        case TRIVIA -> TriviaSolver.validAnswer(s.question,a);
                        case UNSCRAMBLE -> WordSolver.matchesUnscramble(s.question,a);
                        case FILLOUT -> WordSolver.matchesFillout(s.question,a);
                        default -> false;
                    });
                    if (valid.isEmpty()) {s.status=GameState.FINISHED;debug("No reliable AI answer");return;}
                    s.answer=valid.get();s.source="AI";
                    if(s.type==GameType.TRIVIA)trivia.put(s.question,s.answer);
                    schedule(s,()->sender.send(s.answer));
                });
            });
        } else { s.status=GameState.FINISHED; debug("No reliable answer"); }
    }
    public Optional<String> solveLocal(GameType t,String input) {
        try { return switch(t) {
            case MATH -> Optional.of(MathParser.solve(input));
            case VARIABLE -> SymbolVariableSolver.hasTarget(input)?SymbolVariableSolver.solve(input):VariableSolver.solve(input);
            case TRIVIA -> trivia.answer(input);
            case REACTION,RANDOM -> Optional.of(input);
            case UNREVERSE -> Optional.of(new StringBuilder(input).reverse().toString());
            case UNSCRAMBLE -> words.get(3,TimeUnit.SECONDS).unscramble(input);
            case FILLOUT -> words.get(3,TimeUnit.SECONDS).fillout(input);
            default -> Optional.empty();
        }; } catch (Exception e) { return Optional.empty(); }
    }
    private Optional<String> solveFillout(String input,String context) {
        try {return words.get(3,TimeUnit.SECONDS).fillout(input,context);}catch(Exception ignored){return Optional.empty();}
    }
    private void remember(GameSession session,String solution) {
        switch (session.type) {
            case UNSCRAMBLE -> words.thenAccept(dictionary->dictionary.rememberUnscramble(session.question,solution));
            case FILLOUT -> words.thenAccept(dictionary->dictionary.rememberFillout(session.question,solution,session.wordContext));
            case TRIVIA -> trivia.put(session.question,solution);
            default -> { }
        }
    }
    private long delay(GameType type) {return config.get().delayFor(type.name()).sampleMillis();}
    private void schedule(GameSession s,Callable<Boolean> send) {if(s!=null)schedule(s,send,delay(s.type));}
    private void schedule(GameSession s,Callable<Boolean> send,long ms) {
        if (s==null || s!=current || !canRespond(s)) return;
        if (pending != null) pending.cancel(false);
        long scheduleVersion = s.prepareSend(System.currentTimeMillis()+ms);
        debug("Answer: "+s.answer+" / Delay: "+ms+"ms / Scheduled");
        pending=queue.schedule(() -> clientThread.accept(() -> {
            synchronized(responseLock) {
            if (s!=current || !canRespond(s) || !playerReady.getAsBoolean() || !s.claimSend(scheduleVersion)) return;
            // Reserve the single send on the client thread before invoking the network handler.
            if (s!=current || s.status!=GameState.SOLVING || !canRespond(s)) return;
            String answerKey=serverKey+"|"+s.answer.strip();
            if (s.type!=GameType.GUESS_THE_NUMBER && s.type!=GameType.CLICKABLE && System.currentTimeMillis()-sentAnswers.getOrDefault(answerKey,0L)<30000) {
                s.finishSend(scheduleVersion,false);
                debug("Repeated answer suppressed: "+s.answer);
                return;
            }
            boolean sent=false;
            try { sent=send.call(); } catch(Exception ignored) { }
            if (s!=current || s.scheduleVersion!=scheduleVersion) return;
            s.finishSend(scheduleVersion,sent);
            if (sent) {s.guesses++;if(s.type!=GameType.GUESS_THE_NUMBER && s.type!=GameType.CLICKABLE) sentAnswers.put(answerKey,System.currentTimeMillis());}
            debug(sent?"Sent: "+s.answer:"Send cancelled");
            if (sent && config.get().stats) output.accept("Respuesta enviada ("+s.type+"): "+s.answer);
            }
        }),ms,TimeUnit.MILLISECONDS);
    }
    public void cancel() { synchronized(responseLock) {if (pending!=null) pending.cancel(false); if (current!=null && current.status!=GameState.FINISHED) {current.cancelSend();debug("Scheduled response cancelled");} current=null;} }
    public void resetServer(String server) {queue.execute(() -> {cancel();buffer=null;completedSession=null;seen.clear();sentAnswers.clear();serverKey=server;});}
    public void simulate(String line) {queue.execute(() -> {
        if (GameDetector.winner(line)) { simulationBuffer=null; output.accept("Simulación cancelada por ganador"); return; }
        if (simulationBuffer==null || GameDetector.banner(line) && simulationBuffer.size()>0) simulationBuffer=new ChallengeBuffer();
        simulationBuffer.add(line);
        String block=simulationBuffer.text();
        GameDetector.detect(block,true).ifPresentOrElse(d -> {
            output.accept("Simulación: "+d.type()+" / "+d.question()+" → "+solveLocal(d.type(),d.question()).orElse("sin respuesta local"));
            simulationBuffer=null;
        },()->output.accept("Simulación: esperando más líneas"));
    });}
    private void debug(String text) {if(config.get().debug) output.accept(text);}
    @Override public void close() {queue.shutdownNow();aiPool.shutdownNow();}
}
