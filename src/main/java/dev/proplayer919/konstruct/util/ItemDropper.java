package dev.proplayer919.konstruct.util;

import dev.proplayer919.konstruct.CustomPlayer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.ItemEntity;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;

import java.time.temporal.ChronoUnit;
import java.util.random.RandomGenerator;

public class ItemDropper {
    public static void dropItemFromPlayer(ItemStack stack, CustomPlayer player, boolean randomly) {
        ItemEntity itemEntity = new ItemEntity(stack);
        itemEntity.setPickupDelay(2, ChronoUnit.SECONDS);

        if (randomly) {
            float multiplier = RandomGenerator.getDefault().nextFloat() * 4f;
            float angle = RandomGenerator.getDefault().nextFloat() * (float) Math.PI * 2f;
            itemEntity.setVelocity(new Vec(-Math.sin(angle) * multiplier, 0.2f, Math.cos(angle) * multiplier));
        } else {
            itemEntity.setVelocity(player.getPosition().direction().mul(-0.5));
        }

        itemEntity.setInstance(player.getInstance(), player.getPosition().add(0.5, 0.5, 0.5));
    }

    public static void dropItem(ItemStack stack, Instance instance, Pos position) {
        ItemEntity itemEntity = new ItemEntity(stack);
        itemEntity.setPickupDelay(300, ChronoUnit.MILLIS);

        float multiplier = RandomGenerator.getDefault().nextFloat() * 4f;
        float angle = RandomGenerator.getDefault().nextFloat() * (float) Math.PI * 2f;
        itemEntity.setVelocity(new Vec(-Math.sin(angle) * multiplier, 0.2f, Math.cos(angle) * multiplier));

        itemEntity.setInstance(instance, position.add(0.5, 0.5, 0.5));
    }
}
