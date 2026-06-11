package com.rustrelics.stage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * HARDMODE permanente (Stage 3+). Se activa al entrar al Nether por primera vez
 * (ver StageTriggers) y NO TIENE VUELTA ATRAS: el stage solo sube.
 *
 * A diferencia de la Luna de Sangre (temporal, solo overworld), estos buffs son
 * permanentes y aplican a TODOS los mobs hostiles Y NEUTRALES en TODAS las
 * dimensiones:
 *   - mejora de vida maxima
 *   - mejora de danio de ataque
 *   - mejora de armadura (suma plana: funciona aunque la base sea 0)
 *   - drops mejorados (kills de jugador)
 *   - +XP bonus por kill
 *
 * Tag propio (rr_hm_buffed) distinto del de la luna de sangre → ambos se apilan:
 * un mob en luna de sangre durante hardmode recibe los dos buffs.
 */
public final class HardmodeBuffs {

    private static final String BUFFED_TAG = "rr_hm_buffed";
    private static final int HARDMODE_STAGE = 3;

    private static final double HEALTH_MULT = 2.5;
    private static final double DAMAGE_MULT = 1.10;
    private static final double XP_BONUS = 5.2;
    private static final float BONUS_LOOT_CHANCE = 0.10f; // ajustable: drop extra global
    private static final double ARMOR_BONUS = 2.02;

    private HardmodeBuffs() {}

    private static boolean hardmode(ServerLevel level) {
        return StageManager.getStage(level) >= HARDMODE_STAGE;
    }

    // --- Buff permanente (hostiles + neutrales) ---
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        // No aplicar buffs al jugador, solo a mobs hostiles y neutrales
        if (living instanceof ServerPlayer) {
            return;
        }
        if (!hardmode(level)) {
            return;
        }
        if (living.getTags().contains(BUFFED_TAG)) {
            return; // ya buffeado (evita compuesto en recarga de chunk)
        }
        living.addTag(BUFFED_TAG);

        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(health.getBaseValue() * HEALTH_MULT);
            living.setHealth(living.getMaxHealth());
        }
        AttributeInstance dmg = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.setBaseValue(dmg.getBaseValue() * DAMAGE_MULT);
        }
        // ARMOR existe en todos los LivingEntity (registerLivingAttributes lo anade).
        // Usamos suma plana porque la base es 0 en la mayoria de mobs.
        AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(armor.getBaseValue() + ARMOR_BONUS);
        }
    }

    // --- Drops mejorados (solo kills de jugador) ---
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (!hardmode(level)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) {
            return;
        }
        if (victim.getRandom().nextFloat() < BONUS_LOOT_CHANCE) {
            ItemEntity drop = new ItemEntity(
                level,
                victim.getX(),
                victim.getY() + 0.5,
                victim.getZ(),
                new ItemStack(Items.EMERALD)
            );
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        }
    }

    // --- Bonus de XP por kill ---
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (!hardmode(level)) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            killer.giveExperiencePoints((int) XP_BONUS);
        }
    }
}
