package com.example.eoe;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * ゲーム画面右側のカード詳細パネル（View コンポーネント）。
 * カード名・属性チップ・ATK/コスト・イラスト枠・効果テキストを表示する。状態は持たない。
 */
final class CardDetailPanel {

    private Label name;
    private Label attr;
    private Label atk;
    private Label cost;
    private TextFlow effect;
    private StackPane illustration;

    private static final String PLACEHOLDER = "右側でカードを選択すると詳細を表示します。";

    /** パネル本体を構築する。 */
    VBox build() {
        name = new Label("カード未選択");
        name.setFont(Font.font(18));
        name.setStyle("-fx-font-weight:bold;");
        name.setWrapText(true);

        attr = new Label();
        attr.setVisible(false);
        attr.setManaged(false);

        atk = new Label();
        atk.setFont(Font.font(14));
        cost = new Label();
        cost.setFont(Font.font(14));
        HBox stats = new HBox(4, atk, cost);
        stats.setAlignment(Pos.CENTER_LEFT);

        Label illustPlaceholder = new Label("カードのイラスト（予定）");
        illustPlaceholder.setStyle("-fx-text-fill:#888888;");
        illustration = new StackPane(illustPlaceholder);
        illustration.setStyle("-fx-background-color:#e8e8e8; -fx-border-color:#000000; -fx-border-width:2;");
        illustration.setMinHeight(150);
        illustration.setPrefHeight(150);
        illustration.setMaxHeight(150);
        illustration.setMaxWidth(Double.MAX_VALUE);

        Separator divider = new Separator();
        divider.setStyle("-fx-background-color:#000000;");
        Label effectHeader = new Label("効果");
        effectHeader.setFont(Font.font(13));
        effectHeader.setStyle("-fx-font-weight:bold;");

        effect = new TextFlow();
        CardView.effectText(effect, PLACEHOLDER);
        ScrollPane effectScroll = new ScrollPane(effect);
        effectScroll.setFitToWidth(true);
        effectScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        effectScroll.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(effectScroll, Priority.ALWAYS);

        VBox box = new VBox(8, name, attr, stats, illustration, divider, effectHeader, effectScroll);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-border-color:#bbbbbb; -fx-border-width:1; -fx-background-color:#e0e0e0;");
        box.setPrefWidth(260);
        box.setMaxWidth(260);
        box.setMaxHeight(Double.MAX_VALUE);
        return box;
    }

    /** 未選択状態に戻す。 */
    void reset() {
        name.setText("カード未選択");
        attr.setVisible(false);
        attr.setManaged(false);
        atk.setText("");
        cost.setText("");
        cost.setStyle("");
        CardView.effectText(effect, PLACEHOLDER);
    }

    /**
     * カードを表示する。
     * @param effectiveCost 宇宙軽減後に実際に払うコスト。元コスト未満なら赤字で表示する。
     * @param viewerIsOwner 閲覧者がカード所有者か（非公開カードの名前表示判定）。
     */
    void show(Card card, int effectiveCost, boolean viewerIsOwner) {
        name.setText(card.getDisplayName(viewerIsOwner));
        applyAttributeChip(card);
        atk.setText(card.atk != null ? "ATK/" + card.atk + "　" : "");
        if (card.cost == null) {
            cost.setText("");
            cost.setStyle("");
        } else {
            Integer base = null;
            try {
                base = Integer.parseInt(card.cost.trim());
            } catch (NumberFormatException ignore) {
                // 「3 or all」等の非数値コストはそのまま表示。
            }
            if (base != null && effectiveCost < base) {
                cost.setText("C/" + effectiveCost);
                cost.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold;");
            } else {
                cost.setText("C/" + card.cost);
                cost.setStyle("");
            }
        }
        CardView.effectText(effect, card.description);
        // 追加効果（【再生】時のガード等）を青文字で追記。
        for (String added : card.addedEffects) {
            Text t = new Text("\n\n［追加効果］" + added);
            t.setStyle("-fx-fill:#1E90FF; -fx-font-weight:bold;");
            effect.getChildren().add(t);
        }
        // 装備カード名と効果を黄色文字で追記。
        for (Card eq : card.equipments) {
            String eff = eq.description;
            int nl = eff.indexOf('\n');
            if (nl >= 0) {
                eff = eff.substring(nl + 1); // 「【装備】」の行を除く
            }
            Text eqText = new Text("\n\n［装備］" + eq.name + "\n" + eff);
            eqText.setStyle("-fx-fill:#C9A100; -fx-font-weight:bold;");
            effect.getChildren().add(eqText);
        }
    }

    /** 属性チップを属性色で描き分ける（属性なしは非表示）。 */
    private void applyAttributeChip(Card card) {
        if (card.attribute == null) {
            attr.setVisible(false);
            attr.setManaged(false);
            return;
        }
        Attribute a = card.attribute;
        attr.setVisible(true);
        attr.setManaged(true);
        attr.setText(a.displayName);
        String border = a.borderColor != null
                ? "-fx-border-color:" + a.borderColor + "; -fx-border-width:2; -fx-border-radius:4;"
                : "";
        attr.setStyle(
                "-fx-background-color:" + a.fillColor + ";"
                        + "-fx-text-fill:" + a.textColor + ";"
                        + "-fx-font-weight:bold; -fx-padding:2 10 2 10; -fx-background-radius:4;"
                        + border);
    }
}
