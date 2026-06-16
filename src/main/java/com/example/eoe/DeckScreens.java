package com.example.eoe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextFlow;

/**
 * タイトル画面・デッキ一覧・デッキ作成/編成・デッキ確認の各画面（View ＋ 画面内コントローラ）。
 * ゲーム本体（App）から分離し、画面の差し替えとゲーム開始はコールバックで委譲する。
 */
final class DeckScreens {

    private final DeckStore deckStore;
    private final Consumer<Deck> onStartGame;  // シングル開始（引数はNPCのデッキ）
    private final Consumer<Node> setScreen;    // 画面ホストの中身を差し替える

    private List<Card> deckCatalog;            // 全36種のテンプレート（各1枚）
    private Map<String, Card> deckCatalogByName;

    // デッキ編成画面の右側カード詳細パネルの部品。
    private Label dbName;
    private Label dbAttr;
    private Label dbAtk;
    private Label dbCost;
    private TextFlow dbEffect;

    DeckScreens(DeckStore deckStore, Consumer<Deck> onStartGame, Consumer<Node> setScreen) {
        this.deckStore = deckStore;
        this.onStartGame = onStartGame;
        this.setScreen = setScreen;
    }

    // ----- 共通レイアウト -----

    private StackPane screenContainer(Node content) {
        StackPane c = new StackPane(content);
        c.setMinSize(App.DESIGN_WIDTH, App.DESIGN_HEIGHT);
        c.setPrefSize(App.DESIGN_WIDTH, App.DESIGN_HEIGHT);
        c.setMaxSize(App.DESIGN_WIDTH, App.DESIGN_HEIGHT);
        c.setStyle("-fx-background-color:#f3f4f6;");
        return c;
    }

    private Button menuButton(String text) {
        Button b = new Button(text);
        b.setFont(Font.font(20));
        b.setPrefWidth(360);
        b.setPrefHeight(56);
        b.setStyle("-fx-background-color:#ffffff; -fx-border-color:#888; -fx-border-width:2; -fx-background-radius:6; -fx-border-radius:6;");
        return b;
    }

    private VBox menuScreen(String title, Runnable onBack, Node body) {
        Button back = new Button("← 戻る");
        back.setOnAction(e -> onBack.run());
        Label t = new Label(title);
        t.setFont(Font.font(22));
        t.setStyle("-fx-font-weight:bold;");
        HBox header = new HBox(12, back, t);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12));
        header.setStyle("-fx-background-color:#e5e7eb;");
        VBox root = new VBox(header, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        return root;
    }

    // ----- タイトル -----

    /** タイトル画面を表示する。 */
    void showTitle() {
        Label title = new Label("Elements of Earth");
        title.setFont(Font.font(52));
        title.setStyle("-fx-font-weight:bold; -fx-text-fill:#1f2937;");
        Label sub = new Label("- 新生の胎動 -");
        sub.setFont(Font.font(18));
        sub.setStyle("-fx-text-fill:#6b7280;");

        Button single = menuButton("シングル（NPC対戦）");
        single.setOnAction(e -> showNpcSelect());
        Button online = menuButton("オンライン（未実装）");
        online.setDisable(true);
        Button deck = menuButton("デッキ（作成・編成）");
        deck.setOnAction(e -> showDeckList());

        Deck active = deckStore.active();
        Label activeLabel = new Label("使用中デッキ: " + (active != null ? active.name : "なし（既定の全カードで対戦）"));
        activeLabel.setFont(Font.font(14));
        activeLabel.setStyle("-fx-text-fill:#374151;");

        VBox box = new VBox(16, title, sub, new Region(), single, online, deck, new Region(), activeLabel);
        box.setAlignment(Pos.CENTER);
        VBox.setVgrow(box.getChildren().get(2), Priority.NEVER);
        setScreen.accept(screenContainer(box));
    }

    /** シングル: 対戦相手（NPC）を選ぶ。指定 or ランダムでゲーム開始。 */
    private void showNpcSelect() {
        Label title = new Label("対戦相手（NPC）を選択");
        title.setFont(Font.font(24));
        title.setStyle("-fx-font-weight:bold;");

        Deck mine = deckStore.active();
        Label myDeck = new Label("あなたのデッキ: " + (mine != null ? mine.name : "なし（既定の全カード）"));
        myDeck.setStyle("-fx-text-fill:#374151;");

        VBox box = new VBox(12, title, myDeck, new Region());
        box.setAlignment(Pos.CENTER);

        List<Deck> npcs = NpcDecks.all();
        for (Deck npc : npcs) {
            Button b = menuButton(npc.name);
            b.setOnAction(e -> onStartGame.accept(npc));
            box.getChildren().add(b);
        }
        Button rand = menuButton("ランダム");
        rand.setOnAction(e -> onStartGame.accept(npcs.get((int) (Math.random() * npcs.size()))));
        box.getChildren().add(rand);

        setScreen.accept(screenContainer(menuScreen("シングル: 相手選択", this::showTitle, box)));
    }

    // ----- カタログ・検証 -----

    private List<Card> catalog() {
        if (deckCatalog == null) {
            deckCatalog = CardLoader.buildLibrary(Game.Player.PLAYER1);
            deckCatalogByName = new HashMap<>();
            for (Card c : deckCatalog) {
                deckCatalogByName.put(c.name, c);
            }
        }
        return deckCatalog;
    }

    private Card catalogCard(String name) {
        catalog();
        return deckCatalogByName.get(name);
    }

    /** 同名カードの上限（宇宙・aスキルは1枚、その他は3枚）。 */
    private int maxCopies(Card t) {
        if (t == null) {
            return 0;
        }
        return (t.attribute == Attribute.COSMOS || t.attribute == Attribute.A_SKILL) ? 1 : 3;
    }

    /** デッキのバトル使用上の警告一覧（空ならバトルで使用可）。 */
    List<String> deckWarnings(Deck d) {
        List<String> w = new ArrayList<>();
        int total = d.total();
        if (total > 40) {
            w.add("40枚を超えたデッキはバトルで使用できません（現在" + total + "枚）。");
        } else if (total < 40) {
            w.add("40枚に満たないデッキはバトルで使用できません（現在" + total + "枚）。");
        }
        int cosmos = 0;
        int askill = 0;
        for (Map.Entry<String, Integer> e : d.counts.entrySet()) {
            Card t = catalogCard(e.getKey());
            if (t == null) {
                continue;
            }
            if (t.attribute == Attribute.COSMOS) {
                cosmos += e.getValue();
            }
            if (t.attribute == Attribute.A_SKILL) {
                askill += e.getValue();
            }
        }
        if (cosmos > 2) {
            w.add("宇宙属性は合計2枚までです（現在" + cosmos + "枚）。");
        }
        if (askill > 2) {
            w.add("aスキルは合計2枚までです（現在" + askill + "枚）。");
        }
        return w;
    }

    /** バトルで使用可能なデッキか（App からも参照）。 */
    boolean isUsable(Deck d) {
        return deckWarnings(d).isEmpty();
    }

    // ----- デッキ一覧 -----

    private void showDeckList() {
        HBox slots = new HBox(14);
        slots.setAlignment(Pos.CENTER);
        slots.setPadding(new Insets(24));
        for (int i = 0; i < DeckStore.SLOTS; i++) {
            final int idx = i;
            Deck d = deckStore.slots[i];
            VBox tile = new VBox(8);
            tile.setAlignment(Pos.CENTER);
            tile.setPrefSize(200, 300);
            tile.setStyle("-fx-border-color:#888; -fx-border-width:2; -fx-background-color:#ffffff; -fx-border-radius:8; -fx-background-radius:8; -fx-cursor:hand;");
            if (d == null) {
                Label plus = new Label("＋");
                plus.setFont(Font.font(72));
                plus.setStyle("-fx-text-fill:#9ca3af;");
                Label hint = new Label("新規作成");
                hint.setStyle("-fx-text-fill:#9ca3af;");
                tile.getChildren().addAll(plus, hint);
                tile.setOnMouseClicked(e -> openDeckBuilder(idx, null));
            } else {
                if (deckStore.activeIndex == idx) {
                    Label a = new Label("★ 使用中");
                    a.setStyle("-fx-text-fill:#2563eb; -fx-font-weight:bold;");
                    tile.getChildren().add(a);
                }
                Label name = new Label(d.name != null ? d.name : "デッキ" + (idx + 1));
                name.setFont(Font.font(18));
                name.setWrapText(true);
                Label count = new Label(d.total() + " 枚");
                count.setFont(Font.font(15));
                boolean usable = isUsable(d);
                Label status = new Label(usable ? "バトル使用可" : "バトル使用不可");
                status.setStyle(usable ? "-fx-text-fill:#16a34a;" : "-fx-text-fill:#dc2626;");
                tile.getChildren().addAll(name, count, status);
                tile.setOnMouseClicked(e -> showDeckDetail(idx));
            }
            slots.getChildren().add(tile);
        }
        setScreen.accept(screenContainer(menuScreen("デッキ一覧", this::showTitle, slots)));
    }

    /** デッキ確認画面（レシピ表示＋使用／編成／削除）。 */
    private void showDeckDetail(int idx) {
        Deck d = deckStore.slots[idx];
        if (d == null) {
            showDeckList();
            return;
        }
        Label header = new Label((d.name != null ? d.name : "デッキ" + (idx + 1)) + "（" + d.total() + "枚）");
        header.setFont(Font.font(18));
        header.setStyle("-fx-font-weight:bold;");

        VBox warn = new VBox(2);
        for (String s : deckWarnings(d)) {
            Label l = new Label("⚠ " + s);
            l.setStyle("-fx-text-fill:#dc2626;");
            warn.getChildren().add(l);
        }

        VBox recipe = new VBox(3);
        recipe.setPadding(new Insets(8));
        for (Card t : catalog()) {
            int c = d.count(t.name);
            if (c > 0) {
                Label l = new Label(t.name + "  ×" + c);
                recipe.getChildren().add(l);
            }
        }
        ScrollPane sp = new ScrollPane(recipe);
        sp.setFitToWidth(true);

        Button use = new Button("このデッキを使用する");
        boolean usable = isUsable(d);
        use.setDisable(!usable);
        use.setStyle(usable ? "-fx-base:#2563eb;" : "");
        use.setOnAction(e -> {
            deckStore.activeIndex = idx;
            deckStore.save();
            showDeckList();
        });
        Button edit = new Button("編成する");
        edit.setOnAction(e -> openDeckBuilder(idx, d));
        Button del = new Button("削除");
        del.setOnAction(e -> {
            deckStore.slots[idx] = null;
            if (deckStore.activeIndex == idx) {
                deckStore.activeIndex = -1;
            }
            deckStore.save();
            showDeckList();
        });
        HBox actions = new HBox(12, use, edit, del);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(10, header, warn, sp, actions);
        body.setPadding(new Insets(16));
        VBox.setVgrow(sp, Priority.ALWAYS);
        setScreen.accept(screenContainer(menuScreen("デッキ確認", this::showDeckList, body)));
    }

    // ----- 作成／編成 -----

    private void openDeckBuilder(int slotIndex, Deck base) {
        Deck working = (base != null) ? base.copy() : new Deck("デッキ" + (slotIndex + 1));
        showDeckBuilder(slotIndex, working);
    }

    /** レムナント枠と同じ見た目の、属性色チップ（クリックで詳細表示）。 */
    private Label deckCardChip(Card c) {
        StringBuilder sb = new StringBuilder(c.name);
        if (c.atk != null) {
            sb.append("  ATK/").append(c.atk);
        }
        if (c.cost != null) {
            sb.append("  C/").append(c.cost);
        }
        Label item = new Label(sb.toString());
        item.setFont(Font.font(12));
        item.setWrapText(true);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setPadding(new Insets(4, 6, 4, 6));
        Attribute a = c.attribute;
        if (a != null) {
            String border = a.borderColor != null
                    ? "-fx-border-color:" + a.borderColor + "; -fx-border-width:2;"
                    : "-fx-border-color:#999999; -fx-border-width:1;";
            item.setStyle("-fx-cursor:hand; -fx-background-color:" + a.fillColor + "; -fx-text-fill:" + a.textColor + ";" + border);
        } else {
            item.setStyle("-fx-cursor:hand; -fx-background-color:#ffffff; -fx-border-color:#999999; -fx-border-width:1;");
        }
        item.setOnMouseClicked(e -> showDeckCardDetail(c));
        return item;
    }

    /** デッキ編成画面の右側カード詳細パネル（ゲームの詳細欄と同じ見た目・独立の部品）。 */
    private VBox buildDeckDetailPanel() {
        dbName = new Label("カード未選択");
        dbName.setFont(Font.font(18));
        dbName.setStyle("-fx-font-weight:bold;");
        dbName.setWrapText(true);
        dbAttr = new Label();
        dbAttr.setVisible(false);
        dbAttr.setManaged(false);
        dbAtk = new Label();
        dbAtk.setFont(Font.font(14));
        dbCost = new Label();
        dbCost.setFont(Font.font(14));
        HBox stats = new HBox(8, dbAtk, dbCost);
        stats.setAlignment(Pos.CENTER_LEFT);

        Label illust = new Label("カードのイラスト（予定）");
        illust.setStyle("-fx-text-fill:#888888;");
        StackPane illustBox = new StackPane(illust);
        illustBox.setStyle("-fx-background-color:#e8e8e8; -fx-border-color:#000000; -fx-border-width:2;");
        illustBox.setMinHeight(140);
        illustBox.setPrefHeight(140);
        illustBox.setMaxHeight(140);
        illustBox.setMaxWidth(Double.MAX_VALUE);

        Separator divider = new Separator();
        divider.setStyle("-fx-background-color:#000000;");
        Label effectHeader = new Label("効果");
        effectHeader.setFont(Font.font(13));
        effectHeader.setStyle("-fx-font-weight:bold;");

        dbEffect = new TextFlow();
        CardView.effectText(dbEffect, "左の一覧でカードを選ぶと、効果がここに表示されます。");
        ScrollPane effectScroll = new ScrollPane(dbEffect);
        effectScroll.setFitToWidth(true);
        effectScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        effectScroll.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-color:transparent;");
        VBox.setVgrow(effectScroll, Priority.ALWAYS);

        VBox box = new VBox(8, dbName, dbAttr, stats, illustBox, divider, effectHeader, effectScroll);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-border-color:#bbbbbb; -fx-border-width:1; -fx-background-color:#e0e0e0;");
        box.setPrefWidth(280);
        box.setMinWidth(280);
        box.setMaxWidth(280);
        box.setMaxHeight(Double.MAX_VALUE);
        return box;
    }

    /** デッキ編成の詳細欄にカードを表示する。 */
    private void showDeckCardDetail(Card c) {
        dbName.setText(c.name);
        Attribute a = c.attribute;
        if (a != null) {
            dbAttr.setVisible(true);
            dbAttr.setManaged(true);
            dbAttr.setText(a.displayName);
            String border = a.borderColor != null ? a.borderColor : "#999999";
            dbAttr.setStyle("-fx-background-color:" + a.fillColor + "; -fx-text-fill:" + a.textColor
                    + "; -fx-padding:2 8 2 8; -fx-border-color:" + border
                    + "; -fx-border-width:2; -fx-border-radius:4; -fx-background-radius:4; -fx-font-weight:bold;");
        } else {
            dbAttr.setVisible(false);
            dbAttr.setManaged(false);
        }
        dbAtk.setText(c.atk != null ? "ATK/" + c.atk + "　" : "");
        dbCost.setText(c.cost != null ? "C/" + c.cost : "");
        CardView.effectText(dbEffect, c.description);
    }

    /** デッキ作成／編成画面（左: 属性色一覧＋／－、右: カード詳細）。 */
    private void showDeckBuilder(int slotIndex, Deck working) {
        TextField nameField = new TextField(working.name != null ? working.name : "");
        nameField.setPrefWidth(220);

        Label totalLabel = new Label();
        totalLabel.setFont(Font.font(15));
        VBox warnBox = new VBox(2);

        Runnable refreshHeader = () -> {
            int total = working.total();
            totalLabel.setText("合計 " + total + " / 40 枚（最大60）" + (isUsable(working) ? " ✓使用可" : ""));
            warnBox.getChildren().clear();
            for (String s : deckWarnings(working)) {
                Label l = new Label("⚠ " + s);
                l.setStyle("-fx-text-fill:#dc2626;");
                warnBox.getChildren().add(l);
            }
        };

        VBox rows = new VBox(4);
        rows.setPadding(new Insets(8));
        for (Card t : catalog()) {
            Label cntLabel = new Label();
            cntLabel.setMinWidth(28);
            cntLabel.setAlignment(Pos.CENTER);
            cntLabel.setFont(Font.font(15));
            cntLabel.setStyle("-fx-font-weight:bold;");
            Runnable rowRefresh = () -> cntLabel.setText(working.count(t.name) + "/" + maxCopies(t));

            Button minus = new Button("－");
            minus.setOnAction(e -> {
                working.set(t.name, working.count(t.name) - 1);
                rowRefresh.run();
                refreshHeader.run();
            });
            Button plus = new Button("＋");
            plus.setOnAction(e -> {
                int c = working.count(t.name);
                if (c >= maxCopies(t)) {
                    return; // 同名上限（通常3／宇宙・aスキル1）
                }
                if (working.total() >= 60) {
                    return; // デッキ上限60
                }
                working.set(t.name, c + 1);
                rowRefresh.run();
                refreshHeader.run();
            });
            rowRefresh.run();

            Label chip = deckCardChip(t);
            HBox.setHgrow(chip, Priority.ALWAYS);
            HBox row = new HBox(8, chip, minus, cntLabel, plus);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(1, 6, 1, 6));
            rows.getChildren().add(row);
        }
        ScrollPane sp = new ScrollPane(rows);
        sp.setFitToWidth(true);
        HBox.setHgrow(sp, Priority.ALWAYS);

        VBox detailPanel = buildDeckDetailPanel();
        HBox listAndDetail = new HBox(10, sp, detailPanel);
        listAndDetail.setPadding(new Insets(0, 8, 8, 8));
        VBox.setVgrow(listAndDetail, Priority.ALWAYS);

        Button save = new Button("保存");
        save.setStyle("-fx-base:#16a34a;");
        save.setOnAction(e -> {
            String nm = nameField.getText() != null ? nameField.getText().trim() : "";
            working.name = nm.isEmpty() ? ("デッキ" + (slotIndex + 1)) : nm;
            deckStore.slots[slotIndex] = working;
            deckStore.save();
            showDeckList();
        });

        HBox top = new HBox(12, new Label("名前"), nameField, totalLabel, save);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(8));
        VBox body = new VBox(6, top, warnBox, listAndDetail);
        refreshHeader.run();
        setScreen.accept(screenContainer(menuScreen("デッキ編成", this::showDeckList, body)));
    }
}
