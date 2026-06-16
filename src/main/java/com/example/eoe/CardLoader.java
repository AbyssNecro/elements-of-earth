package com.example.eoe;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * cards.json からカード定義を読み込み、指定プレイヤー所有のカードを生成する。
 * カードのマスターデータをコードから分離するためのローダー。
 */
final class CardLoader {

    private CardLoader() {
    }

    /** JSONの1カード分の定義（テンプレート）。 */
    private static final class CardDef {
        String name;
        String type;        // ENTITY / SKILL
        String attribute;   // VOLCANO ... A_SKILL
        String atk;         // null可（スキル）
        String cost;
        String category;    // スキルの分類（装備/サポート/妨害）。エンティティはnull
        String effect;
        String skillEffect; // 任意
    }

    /** 読み込んだ定義のキャッシュ。 */
    private static List<CardDef> defs;

    private static synchronized List<CardDef> loadDefs() {
        if (defs == null) {
            try (InputStream in = CardLoader.class.getResourceAsStream("/cards.json")) {
                if (in == null) {
                    throw new IllegalStateException("cards.json が見つかりません（resources直下に配置してください）");
                }
                Type listType = new TypeToken<List<CardDef>>() { }.getType();
                defs = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), listType);
            } catch (Exception e) {
                throw new RuntimeException("cards.json の読み込みに失敗しました: " + e.getMessage(), e);
            }
        }
        return defs;
    }

    /** 1つの定義から Card を生成する。 */
    private static Card makeCard(CardDef d, Game.Player owner) {
        CardType type = CardType.valueOf(d.type);
        Attribute attr = Attribute.valueOf(d.attribute);
        String description;
        if (type == CardType.SKILL && d.category != null) {
            description = "【" + d.category + "】\n" + d.effect;
        } else {
            description = d.effect;
        }
        Card c = new Card(d.name, description, type, attr, d.atk, d.cost, owner, Zone.LIBRARY);
        if (d.skillEffect != null) {
            c.skillEffect = SkillEffect.valueOf(d.skillEffect);
        }
        return c;
    }

    /** 指定プレイヤー所有の全カード（各1枚。カタログ／既定ライブラリ用、ゾーンはLIBRARY）を生成する。 */
    static List<Card> buildLibrary(Game.Player owner) {
        List<Card> lib = new ArrayList<>();
        for (CardDef d : loadDefs()) {
            lib.add(makeCard(d, owner));
        }
        return lib;
    }

    /** デッキ（カード名→枚数）から、指定プレイヤー所有のライブラリを生成する。 */
    static List<Card> buildDeckLibrary(Deck deck, Game.Player owner) {
        java.util.Map<String, CardDef> byName = new java.util.LinkedHashMap<>();
        for (CardDef d : loadDefs()) {
            byName.put(d.name, d);
        }
        List<Card> lib = new ArrayList<>();
        for (java.util.Map.Entry<String, Integer> e : deck.counts.entrySet()) {
            CardDef d = byName.get(e.getKey());
            if (d == null) {
                continue; // 未知のカード名はスキップ
            }
            for (int i = 0; i < e.getValue(); i++) {
                lib.add(makeCard(d, owner));
            }
        }
        return lib;
    }
}
