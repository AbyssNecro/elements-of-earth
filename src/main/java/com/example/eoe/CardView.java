package com.example.eoe;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * カード表示まわりの View ヘルパ（ゲーム画面・デッキ画面で共有）。
 * 状態を持たない描画専用ユーティリティ。
 */
final class CardView {

    private CardView() {
    }

    /** キーワード（用語）の説明。効果テキストのツールチップに使用する。 */
    static final Map<String, String> KEYWORD_GLOSSARY = new HashMap<>();
    static {
        KEYWORD_GLOSSARY.put("顕現", "エンティティをレムナント以外からエリアに出すこと。及びその時の効果。");
        KEYWORD_GLOSSARY.put("再生", "レムナントからエンティティをエリアに出すこと。及びその時の効果。【顕現】した時の効果は発動しない。");
        KEYWORD_GLOSSARY.put("封印", "カードを封印エリアに裏側で置くこと。");
        KEYWORD_GLOSSARY.put("ガード", "相手の攻撃対象を自身に変更できる。");
        KEYWORD_GLOSSARY.put("拘束", "攻撃ができない。");
        KEYWORD_GLOSSARY.put("幻影", "相手は効果の対象にできない。");
        KEYWORD_GLOSSARY.put("不可侵", "攻撃対象にならない。");
        KEYWORD_GLOSSARY.put("コスモ", "レムナントに火山、海、森、空、宇宙、スキル（通常、金枠どちらでも可）が各1種類以上ある状態。");
    }

    /** 指定の TextFlow に効果テキストを描く（【キーワード】は赤＋マウスオーバーで用語説明）。 */
    static void effectText(TextFlow flow, String description) {
        flow.getChildren().clear();
        if (description == null) {
            return;
        }
        int i = 0;
        while (i < description.length()) {
            int open = description.indexOf('【', i);
            if (open < 0) {
                flow.getChildren().add(plain(description.substring(i)));
                break;
            }
            if (open > i) {
                flow.getChildren().add(plain(description.substring(i, open)));
            }
            int close = description.indexOf('】', open);
            if (close < 0) {
                flow.getChildren().add(plain(description.substring(open)));
                break;
            }
            String keyword = description.substring(open + 1, close);
            Text kw = new Text(description.substring(open, close + 1));
            kw.setStyle("-fx-fill:#E53935; -fx-font-weight:bold;");
            String def = KEYWORD_GLOSSARY.get(keyword);
            if (def != null) {
                Tooltip tip = new Tooltip(def);
                tip.setWrapText(true);
                tip.setMaxWidth(260);
                tip.setShowDelay(Duration.ZERO);
                tip.setShowDuration(Duration.INDEFINITE);
                tip.setHideDelay(Duration.ZERO);
                Tooltip.install(kw, tip);
            }
            flow.getChildren().add(kw);
            i = close + 1;
        }
    }

    private static Text plain(String s) {
        Text t = new Text(s);
        t.setStyle("-fx-fill:#333333;");
        return t;
    }

    /** コスト支払い対象などの黄グロー。 */
    static DropShadow selectionGlow() {
        DropShadow glow = new DropShadow(20, Color.web("#FFD700"));
        glow.setSpread(0.6);
        return glow;
    }

    /** 行動可能カードの縁を光らせる青いグロー。 */
    static DropShadow actionGlow() {
        DropShadow glow = new DropShadow(11, Color.web("#1E90FF"));
        glow.setSpread(0.7);
        return glow;
    }

    /** 攻撃中のエンティティを示す赤いグロー。 */
    static DropShadow attackGlow() {
        DropShadow glow = new DropShadow(20, Color.web("#FF3030"));
        glow.setSpread(0.65);
        return glow;
    }

    /** 攻撃対象を示す青いグロー（行動可能の青より強め）。 */
    static DropShadow targetGlow() {
        DropShadow glow = new DropShadow(20, Color.web("#1E90FF"));
        glow.setSpread(0.65);
        return glow;
    }

    /** 手札カードのセルを属性のカラーコードで塗り分ける（枠色があればそれを使用）。 */
    static void applyCardColor(StackPane cell, Label label, Card card) {
        if (card == null || card.attribute == null) {
            cell.setStyle("-fx-border-color:#999999; -fx-border-width:1; -fx-background-color:#ffffff;");
            return;
        }
        Attribute a = card.attribute;
        String border = a.borderColor != null
                ? "-fx-border-color:" + a.borderColor + "; -fx-border-width:3;"
                : "-fx-border-color:#999999; -fx-border-width:1;";
        cell.setStyle("-fx-background-color:" + a.fillColor + ";" + border);
        label.setStyle("-fx-text-fill:" + a.textColor + ";");
    }

    /** 装備カード1枚の表示。縦向きのままエンティティの裏に重ね、右下にはみ出させる。 */
    static Node equipVisual(Card eq) {
        Label label = new Label(eq.name);
        label.setFont(Font.font(8));
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        StackPane card = new StackPane(label);
        card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Attribute ea = eq.attribute;
        String border = (ea != null && ea.borderColor != null)
                ? "-fx-border-color:" + ea.borderColor + "; -fx-border-width:2;"
                : "-fx-border-color:#999999; -fx-border-width:1;";
        card.setStyle("-fx-background-color:" + (ea != null ? ea.fillColor : "#dddddd") + ";" + border);
        if (ea != null) {
            label.setStyle("-fx-text-fill:" + ea.textColor + ";");
        }
        card.setTranslateX(16);
        card.setTranslateY(16);
        return card;
    }
}
