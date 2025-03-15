package dev.by1337.bc.prize;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.virtualentity.api.virtual.item.VirtualItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.by1337.blib.BLib;
import org.by1337.blib.chat.placeholder.Placeholder;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.random.WeightedItem;
import org.by1337.bmenu.hook.ItemStackCreator;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Prize extends Placeholder implements WeightedItem<Prize> {

    public static final Codec<Prize> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("chance").forGetter(Prize::chance),
            Codec.STRING.fieldOf("display_name").forGetter(Prize::displayName),
            Codec.BOOL.optionalFieldOf("enchanted", false).forGetter(Prize::enchanted),
            Codec.STRING.listOf().fieldOf("give_commands").forGetter(Prize::giveCommands),
            Codec.STRING.fieldOf("material").forGetter(Prize::material)
    ).apply(instance, Prize::new));

    private double chance;
    private String displayName;
    private boolean enchanted;
    private List<String> giveCommands;
    private String material;
    private Component displayNameComponent;
    private ItemStack itemStack;

    public Prize(double chance, String displayName, boolean enchanted, List<String> giveCommands, String material) {
        this.chance = chance;
        this.displayName = displayName;
        this.enchanted = enchanted;
        this.giveCommands = giveCommands;
        this.material = material;
        if (BLib.getApi() == null) { // in test?
            displayNameComponent = null;
            itemStack = null;
        } else {
            displayNameComponent = BLib.getApi().getMessage().componentBuilder(displayName);
            itemStack = material.contains("-") ? ItemStackCreator.getItem(material) : new ItemStack(Material.valueOf(material.toUpperCase(Locale.ENGLISH)));
            itemStack.editMeta(m -> {
                if (enchanted) {
                    m.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
                    m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                m.displayName(displayNameComponent);
            });
        }

        registerPlaceholder("{prize_name}", this::displayName);
    }

    public VirtualItem createVirtualItem(Vec3d pos){
        VirtualItem item = VirtualItem.create();
        item.setPos(pos);
        item.setItem(itemStack());
        item.setCustomNameVisible(true);
        item.setCustomName(displayNameComponent);
        item.setNoGravity(true);
        item.setMotion(Vec3d.ZERO);
        return item;
    }

    @Override
    public Prize value() {
        return this;
    }

    @Override
    public double weight() {
        return chance;
    }

    public double chance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean enchanted() {
        return enchanted;
    }

    public void setEnchanted(boolean enchanted) {
        this.enchanted = enchanted;
    }

    public List<String> giveCommands() {
        return giveCommands;
    }

    public void setGiveCommands(List<String> giveCommands) {
        this.giveCommands = giveCommands;
    }

    public String material() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public Component displayNameComponent() {
        return displayNameComponent;
    }

    public void setDisplayNameComponent(Component displayNameComponent) {
        this.displayNameComponent = displayNameComponent;
    }

    @Contract(value = " -> new", pure = true)
    public ItemStack itemStack() {
        return itemStack.clone();
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prize prize = (Prize) o;
        return Double.compare(chance, prize.chance) == 0 && enchanted == prize.enchanted && Objects.equals(displayName, prize.displayName) && Objects.equals(giveCommands, prize.giveCommands) && Objects.equals(material, prize.material);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chance, displayName, enchanted, giveCommands, material);
    }
}
