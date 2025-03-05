package dev.by1337.bc.command;

import dev.by1337.bc.bd.User;
import org.bukkit.Bukkit;
import org.by1337.blib.BLib;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.CommandException;
import org.by1337.blib.command.argument.ArgumentString;
import org.by1337.blib.command.argument.ArgumentStrings;

public class CommandRegistry {
    public static final Command<CommandContext> COMMAND = new Command<>("root");


    static {
        COMMAND.addSubCommand(
                new Command<CommandContext>("[TAKE_KEY]")
                        .argument(new ArgumentString<>("key"))
                        .executor((sender, args) -> {
                            String key = (String) args.getOrThrow("key", "Use: [TAKE_KEY] <key>");
                            User user = sender.caseBlock().getDatabase().getUser(sender.player());
                            var list = user.getKeysOfType(key);
                            if (list.isEmpty()){
                                throw new CommandException("Player {} does not have a key of type {}", sender.player(), key);
                            }
                            user.removeKey(list.get(0));
                        })
        ).addSubCommand(
                new Command<CommandContext>("[PLAY]")
                        .argument(new ArgumentString<>("prizes"))
                        .argument(new ArgumentString<>("animation"))
                        .executor((sender, args) -> {
                            String prizes = (String) args.getOrThrow("prizes", "Use: [PLAY] <prizes> <animation>");
                            String animation = (String) args.getOrThrow("animation", "Use: [PLAY] <prizes> <animation>");

                            sender.caseBlock().playAnimation(sender.player(), prizes, animation);
                        })
        ).addSubCommand(
                new Command<CommandContext>("[MESSAGE]")
                        .argument(new ArgumentStrings<>("msg"))
                        .executor((sender, args) -> {
                            String msg = (String) args.getOrDefault("msg", "");
                            BLib.getApi().getMessage().sendMsg(sender.player(), msg);
                        })
        ).addSubCommand(
                new Command<CommandContext>("[BROADCAST]")
                        .argument(new ArgumentStrings<>("msg"))
                        .executor((sender, args) -> {
                            String msg = (String) args.getOrDefault("msg", "");
                            BLib.getApi().getMessage().sendAllMsg(msg);
                        })
        ).addSubCommand(
                new Command<CommandContext>("[CONSOLE]")
                        .argument(new ArgumentStrings<>("cmd"))
                        .executor((sender, args) -> {
                            String msg = (String) args.getOrThrow("cmd", "Use: [CONSOLE] <cmd>");
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), msg);
                        })
        )
        ;
    }
}
