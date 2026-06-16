package com.example.eoe;

import java.util.ArrayList;
import java.util.List;

/**
 * ゲーム中の1枚のカード（エンティティ／スキル）を表す。
 * フィールドは同一パッケージ（App など）から直接アクセスする想定でパッケージプライベート。
 */
public class Card {
    final String name;
    final String description;
    final CardType type;
    final Attribute attribute;
    /** ATK表記。null はATKを持たない（スキル等）ことを表す。 */
    final String atk;
    /** コスト表記。「3」や「3 or all」など文字列で保持。null はコストなし。 */
    final String cost;
    final Game.Player owner;
    Zone zone;
    boolean revealed;
    /** スキルの効果種別（既定はNONE）。 */
    SkillEffect skillEffect = SkillEffect.NONE;
    /** このエンティティに装備されている装備カード（エンティティ用）。 */
    final List<Card> equipments = new ArrayList<>();
    /** 実行中に付与された追加効果（例: 【再生】時に付く【ガード】）。詳細欄に青文字で表示。 */
    final List<String> addedEffects = new ArrayList<>();
    /** ターン終了まで持続するATK修正（スキル等による±）。 */
    int tempAtkMod = 0;
    /** 永続のATK修正（ヴォイド・ゲートの累積強化など）。破壊で失われる。 */
    int permAtkMod = 0;

    Card(String name, String description, CardType type, Attribute attribute,
         String atk, String cost, Game.Player owner, Zone zone) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.attribute = attribute;
        this.atk = atk;
        this.cost = cost;
        this.owner = owner;
        this.zone = zone;
        this.revealed = owner == Game.Player.PLAYER1 || zone != Zone.HAND;
    }

    /** ゾーン枠・非公開カードなど、属性/ATK/コストを持たないカード用。 */
    Card(String name, String description, CardType type, Game.Player owner, Zone zone) {
        this(name, description, type, null, null, null, owner, zone);
    }

    public String getDisplayName(boolean viewerIsOwner) {
        if (viewerIsOwner || revealed || zone != Zone.HAND) {
            return name;
        }
        return "?";
    }
}
