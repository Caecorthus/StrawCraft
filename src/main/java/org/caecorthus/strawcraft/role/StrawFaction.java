package org.caecorthus.strawcraft.role;

public enum StrawFaction {
    NONE("role.faction.strawcraft.none", "None", "无阵营"),
    GOOD("role.faction.strawcraft.good", "Good", "好人"),
    KILLER("role.faction.strawcraft.killer", "Killer", "杀手"),
    NEUTRAL("role.faction.strawcraft.neutral", "Neutral", "中立"),
    WITCH("role.faction.strawcraft.witch", "Witch", "魔女");

    private final String translationKey;
    private final String englishName;
    private final String chineseName;

    StrawFaction(String translationKey, String englishName, String chineseName) {
        this.translationKey = translationKey;
        this.englishName = englishName;
        this.chineseName = chineseName;
    }

    public String translationKey() {
        return translationKey;
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }
}
