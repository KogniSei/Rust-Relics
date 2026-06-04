package com.rustrelics.stage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Estado global de Rust & Relics, persistido en el data storage del overworld.
 *
 * Reemplaza el almacenamiento dual de KubeJS (server.persistentData + scoreboard)
 * por una unica fuente de verdad. El scoreboard rr_stage se mantiene como ESPEJO
 * de solo escritura (ver {@link StageManager}) para compatibilidad con datapacks
 * y los scripts KubeJS que quedan.
 */
public class StageSavedData extends SavedData {

    /** Nombre del fichero .dat dentro de data/ del overworld. */
    private static final String DATA_NAME = "rustrelics_stage";

    private int stage = 0;
    private int bloodmoon = 0;

    public StageSavedData() {
    }

    // ------------------------------------------------------------------
    // Acceso
    // ------------------------------------------------------------------

    /**
     * Obtiene (o crea) la instancia global. El estado vive SIEMPRE en el
     * overworld, sin importar desde que dimension se consulte.
     */
    public static StageSavedData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(StageSavedData::new, StageSavedData::load, null),
                DATA_NAME);
    }

    public int getStage() {
        return stage;
    }

    public int getBloodmoon() {
        return bloodmoon;
    }

    /** Fija el stage absoluto y marca para guardado. No notifica (eso lo hace StageManager). */
    public void setStage(int value) {
        if (this.stage != value) {
            this.stage = value;
            setDirty();
        }
    }

    public void setBloodmoon(int value) {
        if (this.bloodmoon != value) {
            this.bloodmoon = value;
            setDirty();
        }
    }

    // ------------------------------------------------------------------
    // Persistencia (firma 1.21.1: incluye HolderLookup.Provider)
    // ------------------------------------------------------------------

    public static StageSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        StageSavedData data = new StageSavedData();
        data.stage = tag.getInt("stage");
        data.bloodmoon = tag.getInt("bloodmoon");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("stage", stage);
        tag.putInt("bloodmoon", bloodmoon);
        return tag;
    }
}
