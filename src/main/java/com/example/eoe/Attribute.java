package com.example.eoe;

/**
 * カードの属性。文字色（カラーコード）と表示用のチップ色を保持する。
 * 火山/海/森/空/宇宙 はエンティティの属性、スキル/aスキル はスキル系。
 */
public enum Attribute {
    VOLCANO("火山", "#FF4500", "#FFFFFF", null),
    SEA("海", "#1E90FF", "#FFFFFF", null),
    FOREST("森", "#228B22", "#FFFFFF", null),
    SKY("空", "#F8F8FF", "#333333", "#CCCCCC"),
    COSMOS("宇宙", "#2F2F2F", "#FFFFFF", null),
    SKILL("スキル", "#FFD700", "#333333", null),
    A_SKILL("aスキル", "#FFD700", "#333333", "#DAA520");

    final String displayName;
    final String fillColor;
    final String textColor;
    final String borderColor;

    Attribute(String displayName, String fillColor, String textColor, String borderColor) {
        this.displayName = displayName;
        this.fillColor = fillColor;
        this.textColor = textColor;
        this.borderColor = borderColor;
    }
}
