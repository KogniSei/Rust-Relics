package com.rustrelics.stage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Estado global de Rust & Relics, persistido en el data storage del overworld.
 *
 * Fuente unica de verdad. Los scoreboards (rr_stage, rr_bloodmoon) son solo
 * ESPEJOS de escritura para compatibilidad con datapacks y KubeJS.
 *
 * Optimizacion: la {@link SavedData.Factory} se cachea como constante. Antes se
 * asignaba una nueva en cada {@link #get} — y get() se invoca en caminos muy
 * calientes (cada spawn de mob, cada tick de los managers, cada cambio de
 * equipo). Reusarla elimina esa basura por-llamada.
 */
public class StageSavedData extends SavedData {

    private static final String DATA_NAME = "rustrelics_stage";

    private static final SavedData.Factory<StageSavedData> FACTORY =
            new SavedData.Factory<>(StageSavedData::new, StageSavedData::load, null);

    // --- Progresion global ---
    private int stage = 0;

    // --- Luna de Sangre (Stage 1+) ---
    private int bloodmoon = 0;
    private boolean bmChecked = false;
    private boolean bmDawnSent = false;

    // --- Eclipse Solar (Stage 4+) ---
    private long eclipseTicks = 0;
    private boolean eclipseChecked = false;
    private boolean eclipseDawnSent = false;

    // --- Stage Secreto: Warden ---
    private boolean wardenSlain = false;

    public StageSavedData() {
    }

    /**
     * Obtiene (o crea) la instancia global. El estado vive SIEMPRE en el
     * overworld, sin importar desde que dimension se consulte.
     */
    public static StageSavedData get(ServerLevel anyLevel) {
        return anyLevel.getServer().overworld().getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    // ------------------------------------------------------------------
    // Progresion
    // ------------------------------------------------------------------

    public int getStage() {
        return stage;
    }

    public void setStage(int value) {
        if (this.stage != value) {
            this.stage = value;
            setDirty();
        }
    }

    // ------------------------------------------------------------------
    // Luna de Sangre
    // ------------------------------------------------------------------

    public int getBloodmoon() {
        return bloodmoon;
    }

    public void setBloodmoon(int value) {
        if (this.bloodmoon != value) {
            this.bloodmoon = value;
            setDirty();
        }
    }

    public boolean isBmChecked() {
        return bmChecked;
    }

    public void setBmChecked(boolean value) {
        if (this.bmChecked != value) {
            this.bmChecked = value;
            setDirty();
        }
    }

    public boolean isBmDawnSent() {
        return bmDawnSent;
    }

    public void setBmDawnSent(boolean value) {
        if (this.bmDawnSent != value) {
            this.bmDawnSent = value;
            setDirty();
        }
    }

    // ------------------------------------------------------------------
    // Eclipse Solar
    // ------------------------------------------------------------------

    public long getEclipseTicks() {
        return eclipseTicks;
    }

    public void setEclipseTicks(long value) {
        if (this.eclipseTicks != value) {
            this.eclipseTicks = value;
            setDirty();
        }
    }

    public boolean isEclipseChecked() {
        return eclipseChecked;
    }

    public void setEclipseChecked(boolean value) {
        if (this.eclipseChecked != value) {
            this.eclipseChecked = value;
            setDirty();
        }
    }

    public boolean isEclipseDawnSent() {
        return eclipseDawnSent;
    }

    public void setEclipseDawnSent(boolean value) {
        if (this.eclipseDawnSent != value) {
            this.eclipseDawnSent = value;
            setDirty();
        }
    }

    // ------------------------------------------------------------------
    // Stage Secreto: Warden
    // ------------------------------------------------------------------

    public boolean isWardenSlain() {
        return wardenSlain;
    }

    public void setWardenSlain(boolean value) {
        if (this.wardenSlain != value) {
            this.wardenSlain = value;
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
        data.bmChecked = tag.getBoolean("bmChecked");
        data.bmDawnSent = tag.getBoolean("bmDawnSent");
        data.eclipseTicks = tag.getLong("eclipseTicks");
        data.eclipseChecked = tag.getBoolean("eclipseChecked");
        data.eclipseDawnSent = tag.getBoolean("eclipseDawnSent");
        data.wardenSlain = tag.getBoolean("wardenSlain");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("stage", stage);
        tag.putInt("bloodmoon", bloodmoon);
        tag.putBoolean("bmChecked", bmChecked);
        tag.putBoolean("bmDawnSent", bmDawnSent);
        tag.putLong("eclipseTicks", eclipseTicks);
        tag.putBoolean("eclipseChecked", eclipseChecked);
        tag.putBoolean("eclipseDawnSent", eclipseDawnSent);
        tag.putBoolean("wardenSlain", wardenSlain);
        return tag;
    }
}
