package dev.by1337.bc.command;

import dev.by1337.bc.bd.User;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.by1337.blib.BLib;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.CommandException;
import org.by1337.blib.command.argument.ArgumentInteger;
import org.by1337.blib.command.argument.ArgumentString;
import org.by1337.blib.command.argument.ArgumentStrings;

import java.util.Random;

public class CommandRegistry {
    public static final Command<CommandContext> COMMAND = new Command<>("root");


    static {
        COMMAND.addSubCommand(
                new Command<CommandContext>("[TAKE_KEY]")
                        .aliases("[take_key]")
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
                        .aliases("[play]")
                        .argument(new ArgumentString<>("prizes"))
                        .argument(new ArgumentString<>("animation"))
                        .executor((sender, args) -> {
                            String prizes = (String) args.getOrThrow("prizes", "Use: [PLAY] <prizes> <animation>");
                            String animation = (String) args.getOrThrow("animation", "Use: [PLAY] <prizes> <animation>");

                            sender.caseBlock().playAnimation(sender.player(), prizes, animation);
                        })
        ).addSubCommand(
                new Command<CommandContext>("[MESSAGE]")
                        .aliases("[message]")
                        .argument(new ArgumentStrings<>("msg"))
                        .executor((sender, args) -> {
                            String msg = (String) args.getOrDefault("msg", "");
                            BLib.getApi().getMessage().sendMsg(sender.player(), msg);
                        })
        ).addSubCommand(
                new Command<CommandContext>("[BROADCAST]")
                        .aliases("[broadcast]")
                        .argument(new ArgumentStrings<>("msg"))
                        .executor((sender, args) -> {
                            String msg = (String) args.getOrDefault("msg", "");
                            BLib.getApi().getMessage().sendAllMsg(msg);
                        })
        ).addSubCommand(
                new Command<CommandContext>("[CONSOLE]")
                        .aliases("[console]")
                        .argument(new ArgumentStrings<>("cmd"))
                        .executor((sender, args) -> {
                            String msg = (String) args.getOrThrow("cmd", "Use: [CONSOLE] <cmd>");
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), msg);
                        })
        ).addSubCommand(
                new Command<CommandContext>("[GIVE]")
                        .aliases("[give]")
                        .argument(new ArgumentString<>("item"))
                        .argument(new ArgumentInteger<>("min"))
                        .argument(new ArgumentInteger<>("max"))
                        .executor((sender, args) -> {
                            String item = (String) args.getOrThrow("item", "Use: [GIVE] <item> <?min> <?max>");
                            int min = (Integer) args.getOrDefault("min", -1);
                            int max = (Integer) args.getOrDefault("max", -1);

                            ItemStack itemStack = BLib.getApi().getItemStackSerialize().deserialize(item);

                            if (min > 0 && max > 0){
                                if (min > max){
                                    throw new CommandException("{} не может быть меньше чем {}! Команда [GIVE] {} {} {}", max, min, itemStack, min, max);
                                }
                                Random random = new Random();
                                int amount = random.nextInt(max - min + 1) + min;
                                itemStack.setAmount(amount);
                            }

                            sender.player().getInventory().addItem(itemStack).forEach((slot, i) -> {
                                sender.player().getWorld().dropItem(sender.player().getLocation(), itemStack);
                            });
                        })
        )
        ;
    }
}
