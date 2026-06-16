package com.example.eoe;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 保存・編集されるデッキ。カード名 → 枚数のマップで内容を保持する。
 * Gson でそのままシリアライズできるよう単純な構造にしている。
 */
public class Deck {
    String name;
    /** カード名 → 枚数（0は保持しない）。追加順を保つため LinkedHashMap。 */
    final Map<String, Integer> counts = new LinkedHashMap<>();

    Deck() {
    }

    Deck(String name) {
        this.name = name;
    }

    /** デッキの総枚数。 */
    int total() {
        int n = 0;
        for (int v : counts.values()) {
            n += v;
        }
        return n;
    }

    /** 指定カードの枚数。 */
    int count(String cardName) {
        Integer v = counts.get(cardName);
        return v != null ? v : 0;
    }

    /** 指定カードの枚数を設定する（0以下なら除去）。 */
    void set(String cardName, int c) {
        if (c <= 0) {
            counts.remove(cardName);
        } else {
            counts.put(cardName, c);
        }
    }

    /** ディープコピー（編成のキャンセルに備えて編集用コピーを作る）。 */
    Deck copy() {
        Deck d = new Deck(name);
        d.counts.putAll(counts);
        return d;
    }
}
