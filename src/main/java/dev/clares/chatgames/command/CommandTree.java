package dev.clares.chatgames.command;

import com.mojang.brigadier.builder.*;
import com.mojang.brigadier.arguments.*;
import dev.clares.chatgames.game.GameType;
import java.util.Locale;
import java.util.function.ToIntFunction;

/** Real Brigadier nodes provide both argument validation and Minecraft's completion menu. */
public final class CommandTree {
    private CommandTree() { }
    public static <S> LiteralArgumentBuilder<S> build(ToIntFunction<String> run) {
        LiteralArgumentBuilder<S> root=LiteralArgumentBuilder.<S>literal("cgedit").executes(ctx->run.applyAsInt("help"));
        for (String name:new String[]{"on","off","status","testlog","last","gui","menu"})
            root.then(LiteralArgumentBuilder.<S>literal(name).executes(ctx->run.applyAsInt(name)));
        root.then(LiteralArgumentBuilder.<S>literal("help").executes(ctx->run.applyAsInt("help"))
            .then(RequiredArgumentBuilder.<S,Integer>argument("page",IntegerArgumentType.integer(1,HelpPages.count()))
                .suggests((ctx,builder)->{for(int i=1;i<=HelpPages.count();i++)builder.suggest(i);return builder.buildFuture();})
                .executes(ctx->run.applyAsInt("help "+IntegerArgumentType.getInteger(ctx,"page")))));
        root.then(LiteralArgumentBuilder.<S>literal("cache").executes(ctx->run.applyAsInt("cache stats"))
            .then(LiteralArgumentBuilder.<S>literal("stats").executes(ctx->run.applyAsInt("cache stats"))));
        root.then(LiteralArgumentBuilder.<S>literal("ai").executes(ctx->run.applyAsInt("ai status"))
            .then(LiteralArgumentBuilder.<S>literal("status").executes(ctx->run.applyAsInt("ai status"))));
        for (String name:new String[]{"debug","stats"}) root.then(toggle(name,run));
        LiteralArgumentBuilder<S> game=LiteralArgumentBuilder.literal("game"), test=LiteralArgumentBuilder.literal("test");
        for (GameType type:GameType.values()) {
            if (type==GameType.UNKNOWN) continue;
            String name=type.name().toLowerCase(Locale.ROOT);
            LiteralArgumentBuilder<S> mode=LiteralArgumentBuilder.literal(name);
            for (String value:new String[]{"on","off"})
                mode.then(LiteralArgumentBuilder.<S>literal(value).executes(ctx->run.applyAsInt("game "+name+" "+value)));
            game.then(mode);
            test.then(LiteralArgumentBuilder.<S>literal(name)
                .then(RequiredArgumentBuilder.<S,String>argument("text",StringArgumentType.greedyString())
                    .executes(ctx->run.applyAsInt("test "+name+" "+StringArgumentType.getString(ctx,"text")))));
        }
        root.then(game).then(test);
        root.then(LiteralArgumentBuilder.<S>literal("simulate")
            .then(RequiredArgumentBuilder.<S,String>argument("text",StringArgumentType.greedyString())
                .executes(ctx->run.applyAsInt("simulate "+StringArgumentType.getString(ctx,"text")))));
        LiteralArgumentBuilder<S> time=LiteralArgumentBuilder.<S>literal("settime").executes(ctx->run.applyAsInt("settime"));
        time.then(duration("settime",run));
        // "random 2s 3s" remains the general range; "random 2s" is a fixed RANDOM delay.
        LiteralArgumentBuilder<S> random=LiteralArgumentBuilder.<S>literal("random").executes(ctx->run.applyAsInt("settime random"));
        random.then(RequiredArgumentBuilder.<S,String>argument("min",StringArgumentType.word())
            .suggests((ctx,builder)->{builder.suggest("0s");builder.suggest("1s");builder.suggest("2s");builder.suggest("2000ms");return builder.buildFuture();})
            .executes(ctx->run.applyAsInt("settime random "+StringArgumentType.getString(ctx,"min")))
            .then(RequiredArgumentBuilder.<S,String>argument("max",StringArgumentType.word())
                .suggests((ctx,builder)->{builder.suggest("3s");builder.suggest("5s");return builder.buildFuture();})
                .executes(ctx->run.applyAsInt("settime random "+StringArgumentType.getString(ctx,"min")+" "+StringArgumentType.getString(ctx,"max")))));
        random.then(range("settime random",run)).then(inherit("settime random",run));
        time.then(random);
        for(GameType type:GameType.values()) {
            if(type==GameType.UNKNOWN || type==GameType.RANDOM)continue;
            String name=type.name().toLowerCase(Locale.ROOT), prefix="settime "+name;
            time.then(LiteralArgumentBuilder.<S>literal(name).executes(ctx->run.applyAsInt(prefix))
                .then(duration(prefix,run)).then(range(prefix,run)).then(inherit(prefix,run)));
        }
        root.then(time);
        return root;
    }
    private static <S> RequiredArgumentBuilder<S,String> duration(String prefix,ToIntFunction<String> run) {
        return RequiredArgumentBuilder.<S,String>argument("delay",StringArgumentType.word())
            .suggests((ctx,builder)->{builder.suggest("0s");builder.suggest("1s");builder.suggest("2s");builder.suggest("2000ms");return builder.buildFuture();})
            .executes(ctx->run.applyAsInt(prefix+" "+StringArgumentType.getString(ctx,"delay")));
    }
    private static <S> LiteralArgumentBuilder<S> range(String prefix,ToIntFunction<String> run) {
        return LiteralArgumentBuilder.<S>literal("random")
            .then(RequiredArgumentBuilder.<S,String>argument("min",StringArgumentType.word())
                .suggests((ctx,builder)->{builder.suggest("0s");builder.suggest("1s");builder.suggest("2s");builder.suggest("3s");return builder.buildFuture();})
                .then(RequiredArgumentBuilder.<S,String>argument("max",StringArgumentType.word())
                    .suggests((ctx,builder)->{builder.suggest("3s");builder.suggest("5s");return builder.buildFuture();})
                    .executes(ctx->run.applyAsInt(prefix+" random "+StringArgumentType.getString(ctx,"min")+" "+StringArgumentType.getString(ctx,"max")))));
    }
    private static <S> LiteralArgumentBuilder<S> inherit(String prefix,ToIntFunction<String> run) {
        return LiteralArgumentBuilder.<S>literal("default").executes(ctx->run.applyAsInt(prefix+" default"));
    }
    private static <S> LiteralArgumentBuilder<S> toggle(String name,ToIntFunction<String> run) {
        LiteralArgumentBuilder<S> node=LiteralArgumentBuilder.literal(name);
        for(String value:new String[]{"on","off"})
            node.then(LiteralArgumentBuilder.<S>literal(value).executes(ctx->run.applyAsInt(name+" "+value)));
        return node;
    }
}
