package com.example.eoe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * デッキの保存領域。5つの保存スロットと、ゲームで使用中のデッキ番号を持つ。
 * decks.json（作業ディレクトリ直下）に永続化する。
 */
public class DeckStore {
    static final int SLOTS = 5;

    /** 保存スロット（null は空きスロット）。 */
    Deck[] slots = new Deck[SLOTS];
    /** 使用中デッキのスロット番号（-1 は未選択）。 */
    int activeIndex = -1;

    private static File file() {
        return new File(System.getProperty("user.dir"), "decks.json");
    }

    /** decks.json を読み込む（無ければ空のストア）。 */
    static DeckStore load() {
        File f = file();
        if (f.exists()) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                DeckStore s = new Gson().fromJson(r, DeckStore.class);
                if (s != null) {
                    if (s.slots == null || s.slots.length != SLOTS) {
                        Deck[] fixed = new Deck[SLOTS];
                        if (s.slots != null) {
                            for (int i = 0; i < Math.min(SLOTS, s.slots.length); i++) {
                                fixed[i] = s.slots[i];
                            }
                        }
                        s.slots = fixed;
                    }
                    return s;
                }
            } catch (Exception e) {
                System.err.println("decks.json の読み込みに失敗しました: " + e.getMessage());
            }
        }
        return new DeckStore();
    }

    /** decks.json へ保存する。 */
    void save() {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file()), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(this, w);
        } catch (Exception e) {
            System.err.println("decks.json の保存に失敗しました: " + e.getMessage());
        }
    }

    Deck active() {
        if (activeIndex >= 0 && activeIndex < SLOTS) {
            return slots[activeIndex];
        }
        return null;
    }
}
