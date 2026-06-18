package com.example.eoe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.beans.value.ChangeListener;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Elements of Earth（EoE）カードゲームのJavaFXエントリポイント。
 *
 * ゲーム画面全体の構成とイベント処理を担当する。
 * - 上部: ターン情報、フェーズ表示、メニュー
 * - 中央: 対戦相手のエリア、戦闘エリア
 * - 下部: プレイヤーの手札
 */
public class App extends Application {

    /** ゲーム状態を保持するモデル。 */
    private final Game game = new Game();

    /** 操作メッセージ（フィードバック）を表示するラベル。 */
    private final Label statusLabel = new Label();
    /** ターン・フェイズ表示ラベル。 */
    private final Label phaseLabel = new Label();
    /** メインステージ参照（全画面切替などで使用） */
    private Stage primaryStage;

    /** エレメントコア表示。 */
    private Label playerCoreLabel;
    private Label opponentCoreLabel;

    /** 手札カードの「顕現」ボタン（選択中のエンティティの上に表示）。 */
    private final Button manifestButton = new Button("顕現");
    private StackPane manifestButtonHost;
    /** 手札カード → 表示セルのマップ（顕現ボタンの配置先解決用）。 */
    private final Map<Card, StackPane> handCellMap = new HashMap<>();
    /** 画面下部のハンド表示の対象プレイヤー（通常はPLAYER1。スキルチェック中は防御側を一時公開）。 */
    private Game.Player handViewOwner = Game.Player.PLAYER1;

    /** 場のエンティティの「攻撃」ボタン（バトルフェイズに選択中の自エンティティの上に表示）。 */
    private final Button attackButton = new Button("攻撃");
    private StackPane attackButtonHost;
    /** 手札スキルの「発動」ボタン（スキルフェイズに選択中のスキルの上に表示）。 */
    private final Button skillButton = new Button("発動");
    private StackPane skillButtonHost;
    /** 攻撃フローの状態。 */
    private Card attacker;
    private boolean attackTargetActive;
    private Card attackTargetCard;
    private boolean attackTargetElement;
    private Node attackTargetNode;

    /** このスキルフェイズでスキルを使用済みか（スキルは1ターン1枚まで）。 */
    private boolean skillUsedThisPhase;
    /** 場の状態（ゾーン・ターン効果）を保持するモデル。 */
    private final MatchState state = new MatchState();

    /** スキル効果の対象選択フローの状態。 */
    private boolean skillTargetActive;
    private Card skillCard;
    private Card skillTargetCard;
    private Node skillTargetNode;
    /** コスト支払い後に発動するスキル（非nullならコスト確定時にスキルを解決）。 */
    private Card pendingSkill;
    /** コスト支払い後にレムナントから再生するエンティティ（リビングデッド・ナイト等の自己再生）。 */
    private Card pendingReviveEntity;
    /** ディスカード専用の選択中か（顕現/スキル/再生を伴わず手札をレムナントへ送るだけ）。 */
    private boolean pendingDiscardOnly;
    /** パイロ・インプの「コスト1枚払って1ドロー」選択中か（払ったらドロー）。 */
    private boolean pendingPyroDraw;
    /** 直近のバトルでパイロ・インプが直接ダメージを与え、ドロー選択を提示すべきか。 */
    private boolean pyroDrawOffered;
    /** 再生装備（死霊術師の呪縛）の対象選択フローの状態。 */
    private boolean reviveTargetActive;
    private Card reviveSkill;
    private Card reviveTarget;
    private Node reviveTargetNode;
    /** 顕現時効果の対象選択フローの状態。 */
    private boolean manifestEffectActive;
    private Card manifestEffectSource;
    private Card manifestEffectTarget;
    private Node manifestEffectNode;
    /** 顕現時【再生】（エルダー・トレント）の対象選択フローの状態。自レムナントの低コスト森属性を無償再生。 */
    private boolean manifestReviveActive;
    private Card manifestReviveTarget;
    private Node manifestReviveNode;
    /** スカイ・ファルコンの顕現時：自レムナントのスキルカードをデッキに戻す対象選択の状態。 */
    private boolean skyReturnActive;
    private Card skyReturnTarget;
    private Node skyReturnNode;
    /** 舞踊る妖精の顕現時：ハンドのコスト1以下の森属性を無償【顕現】する対象選択の状態。 */
    private boolean fairyManifestActive;
    private Card fairyManifestTarget;
    private Node fairyManifestNode;
    /** リサイクル・アースの2段階処理の状態。 */
    private boolean recycleToHandActive;   // ①ハンドに加える対象を自レムナントから選択中
    private boolean recycleReviveActive;    // ②（任意）再生する対象を自レムナントから選択中
    private boolean pendingRecycleCost;     // リサイクルのコスト支払い選択中
    private boolean recycleDoRevive;        // 追加コストを払い②再生まで行うか
    private Card recycleSkill;
    private Card recycleHandTarget;   // ①で選んだ対象（②選択中も保持）
    private Card recycleReviveTarget; // ②で選んだ再生対象
    private Node recycleNode;
    /** スキルチェック割り込みの状態（防御側が攻撃宣言に反応する）。 */
    private Card scAttacker;          // 攻撃宣言したエンティティ（保留中の攻撃）
    private Card scTarget;            // 攻撃対象エンティティ（エレメント攻撃なら null）
    private boolean scTargetElem;     // エレメントへの攻撃か
    private Game.Player scAtkP;       // 攻撃側
    private Game.Player scDefP;       // 防御側
    /** 相手バトルフェイズ中に防御側がスキルを発動済みか（スキルチェックは合計1枚まで）。 */
    private boolean defenderSkillCheckUsed;
    /** スキルを発動するプレイヤー（通常は手番。スキルチェックで防御側がスキルを使う間は防御側）。null=手番。 */
    private Game.Player skillCastPlayer;
    /** スキルチェック中の一般スキル（サポート/妨害）を解決中か。解決後に proceedToBattle へ戻る。 */
    private boolean skillCheckSkillActive;
    /** ゲーム終了（どちらかのコアが0）を表示済みか。 */
    private boolean gameOver;
    /** スキルチェックのリアクティブ解決の保留情報（コスト支払い確定後に発動）。 */
    private boolean pendingSkillCheckPay;   // 防御側のコスト支払い選択中
    private Card pendingSkillCheckCard;     // 発動するリアクティブカード
    private boolean pendingSkillCheckDestroy; // true=攻撃者を破壊 / false=ATK0
    private boolean pendingSkillCheckSeal;  // 発動カードを封印へ（true）/レムナントへ（false）

    /** 各スロットに対応する表示セル。refreshSlots で中身を同期する。 */
    private final StackPane[] playerSlotCells = new StackPane[3];
    private final StackPane[] opponentSlotCells = new StackPane[3];

    private final Pane animationLayer = new Pane();
    private HBox playerHandBox;
    private VBox battleFieldBox;

    /** レムナント閲覧パネル（公開情報。クリックで左側に縦スクロールリストを表示）。 */
    private VBox remnantListPanel;
    private Label remnantListTitle;
    private VBox remnantListContent;
    private Game.Player remnantListOwner;
    /** レムナント枠のラベル（枚数表示の更新用）。 */
    private Label playerRemnantCellLabel;
    private Label opponentRemnantCellLabel;
    /** 封印枠のラベル（枚数表示の更新用）。 */
    private Label playerSealCellLabel;
    private Label opponentSealCellLabel;
    /** ライブラリ枠のラベル（残り枚数表示の更新用）。 */
    private Label playerLibraryCellLabel;
    private Label opponentLibraryCellLabel;

    /** 選択（コスト支払い等）の二段階確認バー。 */
    private HBox confirmBar;
    private Label confirmPrompt;
    private Button confirmOkButton;

    /** リアクティブな選択（ガードチェック／スキルチェック）の非ブロッキング選択バー。 */
    private HBox choiceBar;
    private Label choicePrompt;
    private boolean choiceActive;

    /** バトル中の発光（攻撃エンティティ=赤／攻撃対象=青）の状態。 */
    private Card battleAtkCard;
    private Card battleTgtCard;
    private boolean battleTgtElem;
    private Game.Player battleDefP;
    /** ガードチェックで黄色く光らせているガード可能エンティティのセル。 */
    private final List<Node> guardGlowNodes = new ArrayList<>();
    /** 選択モードの状態。 */
    private boolean selectionActive;
    private int selectionRequired;
    private final List<Card> selectionChosen = new ArrayList<>();
    private final Map<Card, Node> selectionNodeMap = new HashMap<>();
    /** コスト支払いを行うプレイヤー（通常は手番プレイヤー。スキルチェック中は防御側）。null=手番プレイヤー。 */
    private Game.Player selectionPlayer;
    /** 顕現の保留情報（コスト支払い確定後に場へ出す）。 */
    private Card manifestPendingEntity;
    private int manifestPendingSlot;
    private Node manifestSourceNode;
    /** 相手ハンドは非公開のため、枚数のみを左上に表示する。 */
    private VBox opponentHandIndicator;
    private Label opponentHandCountLabel;

    private Card selectedCard;
    private Node selectedCardNode;

    /** ゲーム画面右側のカード詳細パネル（Viewコンポーネント）。 */
    private CardDetailPanel detailPanel;

    /** デザイン基準サイズ（16:9）。この上にUIを組み、ウィンドウに合わせて等比拡大縮小する。 */
    static final double DESIGN_WIDTH = 1280;
    static final double DESIGN_HEIGHT = 720;

    /** 画面遷移のホスト（固定1280×720。子に現在の画面を入れ替える）。 */
    private StackPane screenHost;
    /** ゲーム画面のルート（一度だけ構築して再利用する）。 */
    private Node gameScreenRoot;
    /** デッキ保存領域。 */
    private DeckStore deckStore;
    /** タイトル・デッキ画面（メニュー系のView/コントローラ）。 */
    private DeckScreens deckScreens;
    /** NPC（PLAYER2）のデッキと、NPC自動プレイの有効フラグ。 */
    private Deck npcDeck;
    private boolean npcEnabled;
    /** NPCのAIが思考・行動中か（その間は人間の操作をブロック）。 */
    private boolean npcThinking;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        deckStore = DeckStore.load();

        // 固定サイズ(16:9)の画面ホスト。子に現在の画面（タイトル／デッキ／ゲーム）を入れ替える。
        screenHost = new StackPane();
        screenHost.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        screenHost.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        screenHost.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        screenHost.setStyle("-fx-background-color:#ffffff;");

        // ウィンドウサイズに合わせて全体を等比スケール（ヒットテストも追従）。
        Scale scale = new Scale(1, 1, 0, 0);
        screenHost.getTransforms().add(scale);
        Group scaledContent = new Group(screenHost);
        StackPane outer = new StackPane(scaledContent);
        outer.setStyle("-fx-background-color:#1a1a1a;");

        ChangeListener<Number> rescale = (obs, oldV, newV) -> {
            double factor = Math.min(outer.getWidth() / DESIGN_WIDTH, outer.getHeight() / DESIGN_HEIGHT);
            if (factor > 0 && !Double.isNaN(factor) && !Double.isInfinite(factor)) {
                scale.setX(factor);
                scale.setY(factor);
            }
        };
        outer.widthProperty().addListener(rescale);
        outer.heightProperty().addListener(rescale);

        stage.setTitle("Elements of Earth");
        Scene scene = new Scene(outer, DESIGN_WIDTH, DESIGN_HEIGHT);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(640);
        stage.setMinHeight(400);

        deckScreens = new DeckScreens(deckStore, this::showGame, this::showScreen);
        deckScreens.showTitle(); // 最初はタイトル画面
        stage.show();
        rescale.changed(null, 0, 0);
    }

    /** 画面ホストの中身を入れ替える。 */
    private void showScreen(Node screen) {
        screenHost.getChildren().setAll(screen);
    }

    /** ゲーム画面を構築する（topPanel + 中央 + 詳細）。一度だけ作って再利用する。 */
    private Node buildGameScreen() {
        VBox topPanel = createTopPanel();
        VBox centerPanel = createCenterPanel();
        detailPanel = new CardDetailPanel();
        VBox detailNode = detailPanel.build();

        HBox mainArea = new HBox(12, centerPanel, detailNode);
        HBox.setHgrow(centerPanel, Priority.ALWAYS);
        mainArea.setPadding(new Insets(6));
        mainArea.setMaxWidth(Double.MAX_VALUE);

        animationLayer.setMouseTransparent(true);
        animationLayer.setPickOnBounds(false);
        animationLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane contentStack = new StackPane(mainArea, animationLayer);

        BorderPane designRoot = new BorderPane();
        designRoot.setTop(topPanel);
        designRoot.setCenter(contentStack);
        designRoot.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        designRoot.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        designRoot.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        designRoot.setStyle("-fx-background-color:#ffffff;");
        return designRoot;
    }

    /** ゲーム画面へ。PLAYER1は使用中デッキ、PLAYER2は指定NPCデッキで新規ゲームを開始する。 */
    private void showGame(Deck npcDeck) {
        this.npcDeck = npcDeck;
        this.npcEnabled = (npcDeck != null);
        if (gameScreenRoot == null) {
            gameScreenRoot = buildGameScreen();
        }
        showScreen(gameScreenRoot);
        startMatch();
    }

    /** モデルを初期化し、コイントスで先攻/後攻を決めてゲームを開始する。 */
    private void startMatch() {
        game.reset();
        initializeHands();
        coinTossAndStart();
    }

    /**
     * コイントスで先攻/後攻を決定し、結果を表示してからゲームを開始する。
     * 表（heads）=自分（PLAYER1）の先攻、裏=自分の後攻（相手が先攻）。
     * ゲーム開始時（先攻1ターン目のドローフェイズ前）に行われる。
     * オンライン実装時は、相手側にはこの結果の反対（先攻↔後攻）を表示する想定。
     */
    private void coinTossAndStart() {
        boolean heads = Math.random() < 0.5;
        Game.Player first = heads ? Game.Player.PLAYER1 : Game.Player.PLAYER2;
        game.setFirstPlayer(first);

        String coin = heads ? "表" : "裏";
        String result = heads ? "あなたの先攻です。" : "あなたの後攻です（相手が先攻）。";
        statusLabel.setText("コイントス: 【" + coin + "】 " + result);
        render();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("コイントス");
        alert.setHeaderText("コイントス: " + coin);
        alert.setContentText(result + "\n（表＝先攻 ／ 裏＝後攻）");
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }
        alert.showAndWait();

        // 相手（NPC）が先攻なら、NPCのターンから開始する。
        maybeRunNpcTurn();
    }

    /** 開発用: 現在の画面をPNGに保存するユーティリティ（通常は未使用）。 */
    @SuppressWarnings("unused")
    private void saveAppSnapshot(Scene scene) {
        try {
            WritableImage image = scene.snapshot(null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new java.io.File("C:/Users/n1251018/Documents/eoe/app_snapshot.png"));
        } catch (Exception ex) {
            System.err.println("Failed to save app snapshot: " + ex.getMessage());
        }
    }

    /**
     * 上部パネルを作成（ターン・フェイズ表示、Nextボタン、メニュー、メッセージ行）。
     */
    private VBox createTopPanel() {
        phaseLabel.setFont(Font.font(14));

        Button nextButton = new Button("Next");
        nextButton.setOnAction(e -> doNextPhase());

        Button newGameBtn = new Button("新規ゲーム");
        newGameBtn.setOnAction(e -> {
            if (selectionActive) {
                cancelSelection();
            }
            if (attackTargetActive) {
                cancelAttack();
            }
            if (skillTargetActive) {
                cancelSkillTarget();
            }
            if (reviveTargetActive) {
                cancelReviveTarget();
            }
            if (manifestEffectActive) {
                cancelManifestTarget();
            }
            if (manifestReviveActive) {
                cancelManifestRevive();
            }
            if (skyReturnActive) {
                cancelSkyReturn();
            }
            if (fairyManifestActive) {
                cancelFairyManifest();
            }
            if (recycleToHandActive || recycleReviveActive) {
                cancelRecycle();
            }
            startMatch();
        });

        Button fullScreenBtn = new Button("全画面切替");
        fullScreenBtn.setOnAction(e -> {
            if (primaryStage != null) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            }
        });

        Button titleBtn = new Button("タイトルへ");
        titleBtn.setOnAction(e -> deckScreens.showTitle());

        HBox controlRow = new HBox(12, phaseLabel, nextButton, newGameBtn, fullScreenBtn, titleBtn);
        controlRow.setAlignment(Pos.CENTER_LEFT);
        controlRow.setPadding(new Insets(8));

        statusLabel.setFont(Font.font(12));
        HBox messageRow = new HBox(statusLabel);
        messageRow.setAlignment(Pos.CENTER_LEFT);
        messageRow.setPadding(new Insets(0, 8, 6, 8));

        VBox panel = new VBox(controlRow, messageRow);
        panel.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
        return panel;
    }

    /** コア0またはライブラリアウトなら勝敗を表示する。終了後は true を返す。 */
    private boolean checkGameOver() {
        if (gameOver) {
            return true;
        }
        if (!game.isGameOver()) {
            return false;
        }
        gameOver = true;
        Game.Player w = game.getWinner();
        String msg = (w != null ? w.getDisplayName() : "") + " の勝利！";
        Game.Player decked = game.getDeckedOutLoser();
        String reason = decked != null
                ? decked.getDisplayName() + " がライブラリアウト（山札切れでドローできず）。"
                : "相手のエレメントコアが0になりました。";
        statusLabel.setText("★ " + msg + "（「新規ゲーム」で再開できます）");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ゲーム終了");
        alert.setHeaderText(null);
        alert.setContentText(msg + "\n" + reason);
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }
        alert.showAndWait();
        return true;
    }

    /** 操作の入口で呼ぶ: ゲーム終了済みならメッセージを出して true。 */
    private boolean blockedByGameOver() {
        if (gameOver) {
            statusLabel.setText("ゲームは終了しています。「新規ゲーム」で再開してください。");
            return true;
        }
        return false;
    }

    /** Next: 次のフェイズへ進む（エンド→相手のドローへ自動で手番交代）。 */
    private void doNextPhase() {
        if (blockedByGameOver()) {
            return;
        }
        if (npcThinking) {
            return; // NPCの行動中は操作不可
        }
        if (isBusy()) {
            statusLabel.setText("選択を完了してください（決定／キャンセル）。");
            return;
        }
        // 現フェイズにまだ可能な行動が残っていれば確認する。
        String remaining = remainingActionMessage();
        if (remaining != null && !confirmAdvancePhase(remaining)) {
            return; // 「いいえ」: 操作画面へ戻る
        }
        advancePhase();
        maybeRunNpcTurn();
    }

    /** フェイズ進行の本体（確認ダイアログなし。NPCの自動進行でも使用）。 */
    private void advancePhase() {
        Game.Phase before = game.getCurrentPhase();
        Game.Player drawer = game.getCurrentPlayer();
        game.nextPhase();
        selectedCard = null;
        selectedCardNode = null;
        if (game.getCurrentPhase() == Game.Phase.SKILL) {
            skillUsedThisPhase = false; // スキルフェイズ開始でリセット
        }
        if (game.getCurrentPhase() == Game.Phase.BATTLE) {
            state.attackedEntities.clear(); // バトルフェイズ開始で攻撃済みをリセット
            defenderSkillCheckUsed = false; // 防御側のスキルチェック使用回数（1枚/相手バトルフェイズ）をリセット
        }

        // ドローフェイズ終了時（ドロー→スタンバイ）に手番プレイヤーが2枚引く。
        if (before == Game.Phase.DRAW) {
            drawCards(drawer, 2);
            statusLabel.setText(drawer.getDisplayName() + " がドローフェイズ終了: 2枚ドロー");
            if (checkGameOver()) {
                render();
                return; // ライブラリアウトでゲーム終了
            }
        } else if (before == Game.Phase.END) {
            // ヴォイド・ゲート: 自分のエンドフェイズに ATK+2（永続累積）。
            for (Card c : slotsFor(drawer)) {
                if (c != null && "ヴォイド・ゲート".equals(c.name)) {
                    c.permAtkMod += 2;
                }
            }
            // ターン終了まで持続するATK修正をリセットする。
            clearTempAtkMods();
            state.mistGuardUsed.clear(); // ミスト・ミラージュの「1ターン1度」をリセット
            state.effectImmuneThisTurn.clear(); // ボルカニック・シールドの効果耐性をリセット
            state.tramplersThisTurn.clear(); // ホワイトフェザーの付与効果をリセット
            state.guardDisabledThisTurn.clear(); // アーマー・バインドのガード不可をリセット
            state.atkZeroedThisTurn.clear(); // コズミック・ノヴァのATK0をリセット
            // 死霊術師の呪縛: 装備されている間、所有者のターン終了時に自エレメントへ3ダメージ。
            int necroDamage = necroticBindingDamage(drawer);
            if (necroDamage > 0) {
                game.damageCore(drawer, necroDamage);
                if (checkGameOver()) {
                    render();
                    return; // 死霊術師の呪縛の自傷でゲーム終了
                }
            }
            // ターン終了時: 手番プレイヤーの手札が5枚になるよう引く（6枚以上はそのまま）。
            int handSize = handFor(drawer).size();
            if (handSize < 5) {
                drawCards(drawer, 5 - handSize);
                statusLabel.setText(drawer.getDisplayName() + " がターン終了: " + (5 - handSize) + "枚ドローして5枚に");
            } else {
                statusLabel.setText(drawer.getDisplayName() + " がターン終了（手札" + handSize + "枚）");
            }
            if (checkGameOver()) {
                render();
                return; // ライブラリアウトでゲーム終了
            }
        } else {
            statusLabel.setText(game.getCurrentPlayer().getDisplayName() + " の "
                    + game.getCurrentPhaseName() + "フェイズ");
        }
        updateManifestButton();
        updateAttackButton();
        updateSkillButton();
        render();
    }

    // ===== NPC（簡易AI・自動プレイ） =====

    private boolean isNpc(Game.Player p) {
        return npcEnabled && p == Game.Player.PLAYER2;
    }

    private void scheduleNpc(double ms, Runnable r) {
        javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(ms));
        // アニメーション処理中は showAndWait（ガード/スキルチェックのダイアログ）が禁止されるため、
        // runLater でパルス外に逃がして実行する。
        p.setOnFinished(e -> javafx.application.Platform.runLater(r));
        p.play();
    }

    /** 手番がNPC(PLAYER2)なら自動プレイを開始する。 */
    private void maybeRunNpcTurn() {
        if (npcEnabled && !gameOver && game.getCurrentPlayer() == Game.Player.PLAYER2 && !npcThinking) {
            npcThinking = true;
            statusLabel.setText("NPCのターン（思考中…）");
            scheduleNpc(800, this::npcAct);
        }
    }

    /** NPCの1アクション。フェイズに応じて行動し、次のアクションを遅延スケジュールする。 */
    private void npcAct() {
        if (gameOver || game.getCurrentPlayer() != Game.Player.PLAYER2) {
            npcThinking = false;
            return;
        }
        if (npcResolveStuck()) {        // 顕現時効果などの選択モードを自動解決
            scheduleNpc(500, this::npcAct);
            return;
        }
        if (isBusy()) {                 // 人間の防御側スキルチェック等の解決待ち
            scheduleNpc(400, this::npcAct);
            return;
        }
        switch (game.getCurrentPhase()) {
            case STANDBY:
                if (npcTryManifest()) {
                    scheduleNpc(700, this::npcAct);
                } else {
                    advancePhase();
                    scheduleNpc(500, this::npcAct);
                }
                break;
            case BATTLE:
                if (npcTryAttack()) {
                    scheduleNpc(800, this::npcAct);
                } else {
                    advancePhase();
                    scheduleNpc(500, this::npcAct);
                }
                break;
            case END:
                advancePhase(); // → PLAYER1のドローへ。手番が戻る
                npcThinking = false;
                updateManifestButton();
                updateAttackButton();
                updateSkillButton();
                statusLabel.setText("あなたのターンです。");
                break;
            default: // DRAW / SKILL は行動せず進める（簡易AIはスキル未使用）
                advancePhase();
                scheduleNpc(500, this::npcAct);
                break;
        }
    }

    /** NPCが選択モードに入っていたら自動解決する（解決したら true）。 */
    private boolean npcResolveStuck() {
        if (manifestEffectActive) {
            java.util.List<Card> cands = manifestTargetCandidates(Game.Player.PLAYER2, manifestEffectSource);
            if (!cands.isEmpty()) {
                setManifestTarget(cands.get(0), null);
                confirmManifestTarget();
            } else {
                cancelManifestTarget();
            }
            return true;
        }
        if (manifestReviveActive) {
            cancelManifestRevive();
            return true;
        }
        if (skyReturnActive) {
            cancelSkyReturn();
            return true;
        }
        if (fairyManifestActive) {
            cancelFairyManifest();
            return true;
        }
        return false;
    }

    /** NPCのスタンバイ: 払えるエンティティのうちATKが高いものを1体顕現する。 */
    private boolean npcTryManifest() {
        Game.Player npc = Game.Player.PLAYER2;
        if (firstEmptySlot(slotsFor(npc)) < 0) {
            return false;
        }
        java.util.List<Card> hand = handFor(npc);
        int payable = hand.size() - 1;
        Card best = null;
        int bestAtk = -1;
        for (Card c : hand) {
            if (c.type != CardType.ENTITY) {
                continue;
            }
            if (effectiveCost(c) > payable) {
                continue;
            }
            int atk = parseAtk(c.atk);
            if (atk > bestAtk) {
                bestAtk = atk;
                best = c;
            }
        }
        if (best == null) {
            return false;
        }
        int cost = effectiveCost(best);
        payNpcCost(npc, best, cost);
        int slot = firstEmptySlot(slotsFor(npc));
        statusLabel.setText("NPC: " + best.name + " を顕現");
        placeEntity(npc, best, slot, null);
        return true;
    }

    /** NPCがコスト cost 枚を手札（exclude以外）からレムナントへ払う。 */
    private void payNpcCost(Game.Player npc, Card exclude, int cost) {
        java.util.List<Card> hand = handFor(npc);
        java.util.List<Card> remnant = remnantFor(npc);
        int paid = 0;
        for (java.util.Iterator<Card> it = hand.iterator(); it.hasNext() && paid < cost;) {
            Card c = it.next();
            if (c == exclude) {
                continue;
            }
            it.remove();
            c.zone = Zone.REMNANT;
            remnant.add(c);
            paid++;
        }
    }

    /**
     * NPCのバトル（1回の攻撃を選ぶ）。属性相性(防御-3)・リーサル・脅威の優先除去・自爆回避を考慮する。
     * 優先順位: ①1体で相手コアを削り切れるならリーサルで顔面 → ②倒せて生き残る攻撃で最大の脅威を最小十分な攻撃者で除去
     *  → ③有利な相打ち（相手の価値≧自分の価値） → ④それ以外は自爆を避けて顔面を削る。
     */
    private boolean npcTryAttack() {
        if (!canAttackNow()) {
            return false;
        }
        Game.Player npc = Game.Player.PLAYER2;
        Game.Player foe = Game.Player.PLAYER1;

        List<Card> attackers = new ArrayList<>();
        for (Card c : slotsFor(npc)) {
            if (c != null && !state.attackedEntities.contains(c)
                    && !hasKeyword(c, "拘束") && effectiveAtk(c) > 0) {
                attackers.add(c);
            }
        }
        if (attackers.isEmpty()) {
            return false;
        }
        List<Card> targets = new ArrayList<>();
        for (Card c : slotsFor(foe)) {
            if (c != null && !isUnattackable(c)) {
                targets.add(c);
            }
        }
        int foeCore = game.getCore(foe);

        // ① リーサル: 1体で相手コアを削り切れるなら顔面へ（最小十分な攻撃者で温存）。
        Card lethal = null;
        for (Card a : attackers) {
            if (effectiveAtk(a) >= foeCore
                    && (lethal == null || effectiveAtk(a) < effectiveAtk(lethal))) {
                lethal = a;
            }
        }
        if (lethal != null) {
            return npcDeclareAttack(lethal, null, true, foe);
        }

        // ② 倒せて生き残る攻撃: 最大の脅威(=実効ATK最大)を、倒せる中で最小ATKの攻撃者で除去。
        Card bestTarget = null, bestAttacker = null;
        for (Card t : targets) {
            Card chosen = null;
            for (Card a : attackers) {
                if (effectiveAtk(a) > npcKillThreshold(a, t)
                        && (chosen == null || effectiveAtk(a) < effectiveAtk(chosen))) {
                    chosen = a; // 倒せて生き残る（最小十分な攻撃者）
                }
            }
            if (chosen != null
                    && (bestTarget == null || effectiveAtk(t) > effectiveAtk(bestTarget))) {
                bestTarget = t;
                bestAttacker = chosen;
            }
        }
        if (bestTarget != null) {
            return npcDeclareAttack(bestAttacker, bestTarget, false, foe);
        }

        // ③ 有利な相打ち（相手の価値≧自分の価値のときのみ）。最大の脅威を最小価値の攻撃者で。
        bestTarget = null;
        bestAttacker = null;
        for (Card t : targets) {
            Card chosen = null;
            for (Card a : attackers) {
                if (effectiveAtk(a) == npcKillThreshold(a, t)
                        && effectiveAtk(t) >= effectiveAtk(a)
                        && (chosen == null || effectiveAtk(a) < effectiveAtk(chosen))) {
                    chosen = a;
                }
            }
            if (chosen != null
                    && (bestTarget == null || effectiveAtk(t) > effectiveAtk(bestTarget))) {
                bestTarget = t;
                bestAttacker = chosen;
            }
        }
        if (bestTarget != null) {
            return npcDeclareAttack(bestAttacker, bestTarget, false, foe);
        }

        // ④ 有利な攻撃が無い: 自爆を避け、最大ATKの攻撃者で顔面を削る。
        Card faceAttacker = attackers.get(0);
        for (Card a : attackers) {
            if (effectiveAtk(a) > effectiveAtk(faceAttacker)) {
                faceAttacker = a;
            }
        }
        return npcDeclareAttack(faceAttacker, null, true, foe);
    }

    /**
     * 攻撃側 a が対象 t を破壊するために超える必要のある実効防御値（低いほど倒しやすい）。
     * 属性有利なら -3。ストーム・イーグルの宇宙特効は確定破壊として -1（必ず超える）。
     */
    private int npcKillThreshold(Card a, Card t) {
        if ("ストーム・イーグル".equals(a.name) && t.attribute == Attribute.COSMOS) {
            return -1; // バトルせず確定破壊
        }
        int def = effectiveAtk(t);
        if (isAdvantage(a.attribute, t.attribute)) {
            def -= 3;
        }
        return def;
    }

    /** NPCが攻撃を宣言する（赤/青の発光を見せてから confirmAttack で解決）。 */
    private boolean npcDeclareAttack(Card attackerCard, Card target, boolean element, Game.Player foe) {
        attacker = attackerCard;
        attackTargetCard = element ? null : target;
        attackTargetElement = element;
        attackTargetNode = null;
        attackTargetActive = true;
        // 攻撃エンティティ=赤／攻撃対象=青を先に光らせ、少し見せてから解決する。
        setBattleGlows(attackerCard, element ? null : target, element, foe);
        render();
        scheduleNpc(900, this::confirmAttack); // ガード/スキルチェックは人間(防御側)へ。
        return true;
    }

    /** NPCが防御側のときのガード判断（攻撃者を上回れるガード持ちがいれば受ける）。 */
    private Card npcChooseGuard(Game.Player defP, Card atkCard) {
        Card best = null;
        int bestAtk = -1;
        for (Card c : slotsFor(defP)) {
            if (grantsGuard(c)) {
                int a = effectiveAtk(c);
                if (a > bestAtk) {
                    bestAtk = a;
                    best = c;
                }
            }
        }
        return (best != null && bestAtk >= effectiveAtk(atkCard)) ? best : null;
    }

    /** 現在のフェイズにまだ可能な行動が残っていれば確認メッセージを返す（無ければ null）。 */
    private String remainingActionMessage() {
        Game.Player p = game.getCurrentPlayer();
        switch (game.getCurrentPhase()) {
            case STANDBY:
                if (hasManifestableEntity(p)) {
                    return "まだ顕現可能なエンティティが手札に残っています。";
                }
                break;
            case SKILL:
                if (!skillUsedThisPhase && hasUsableSkill(p)) {
                    return "まだ発動可能なスキルが手札に残っています。";
                }
                break;
            case BATTLE:
                if (hasUnattackedEntity(p)) {
                    return "まだ攻撃していないエンティティが場に残っています。";
                }
                break;
            default:
                break;
        }
        return null;
    }

    /** 「次のフェイズへ進んでよろしいですか？」をはい／いいえで確認する。 */
    private boolean confirmAdvancePhase(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("確認");
        alert.setHeaderText(null);
        alert.setContentText(message + "\n次のフェイズへ進んでよろしいですか？");
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }
        ButtonType yes = new ButtonType("はい", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("いいえ", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yes, no);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    /** 顕現可能なエンティティが手札にあるか（空きスロットあり＆コスト支払い可能）。 */
    private boolean hasManifestableEntity(Game.Player p) {
        if (firstEmptySlot(slotsFor(p)) < 0) {
            return false;
        }
        int payable = handFor(p).size() - 1;
        for (Card c : handFor(p)) {
            if (c.type == CardType.ENTITY && effectiveCost(c) <= payable) {
                return true;
            }
        }
        return false;
    }

    /** 発動可能なスキルが手札にあるか（コスト支払い可能）。 */
    private boolean hasUsableSkill(Game.Player p) {
        for (Card c : handFor(p)) {
            if (skillUsableInSkillPhase(c, p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * スキル skill を今このスキルフェイズで発動できるか（コスト＋効果ごとの対象/条件）。
     * dispatchSkillEffect の各分岐が return（エラー表示）する条件と一致させ、
     * 発動できないスキルは青く光らせない／発動ボタンを出さないために使う。
     */
    private boolean skillUsableInSkillPhase(Card skill, Game.Player p) {
        if (skill == null || skill.type != CardType.SKILL) {
            return false;
        }
        Game.Player foe = p.getOpponent();
        // コスト: aスキルは「x or all」で常に支払える（手札を全封印）。通常スキルは手札枚数で判定。
        if (!isASkill(skill) && effectiveCost(skill) > handFor(p).size() - 1) {
            return false;
        }
        switch (skill.skillEffect) {
            case DESTROY_ENEMY_ENTITY:
                return hasAnyEntity(foe);
            case EQUIP:
            case MODIFY_ATK:
            case GRANT_TRAMPLE:
                return hasAnyEntity(Game.Player.PLAYER1) || hasAnyEntity(Game.Player.PLAYER2);
            case DESTROY_EQUIPMENT:
                return hasAnyEquippedEntity(Game.Player.PLAYER1) || hasAnyEquippedEntity(Game.Player.PLAYER2);
            case GRANT_IMMUNITY:
                return anyAttributeOnField(Attribute.VOLCANO);
            case RECYCLE:
                return !recycleHandCandidates(p).isEmpty();
            case DISABLE_GUARD:
                return true; // 対象を取らない
            case DESTROY_ALL_ENTITIES:
                return hasAnyEntity(Game.Player.PLAYER1) || hasAnyEntity(Game.Player.PLAYER2);
            case REVIVE_EQUIP:
                return hasAnyEntityInRemnant(p) && firstEmptySlot(slotsFor(p)) >= 0;
            case ZERO_ATTACKER_ATK: // コズミック・ノヴァはスキルフェイズ不可（リアクティブ専用）
            default:
                return false;
        }
    }

    /** 攻撃していないエンティティが場にいるか（攻撃可能な状況のみ）。 */
    private boolean hasUnattackedEntity(Game.Player p) {
        if (!canAttackNow()) {
            return false;
        }
        for (Card c : slotsFor(p)) {
            if (c != null && !state.attackedEntities.contains(c) && !hasKeyword(c, "拘束")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 中央パネルを作成（空きエリア）。
     */
    private VBox createCenterPanel() {
        manifestButton.setStyle("-fx-font-size:11; -fx-background-color:#FFD700; -fx-text-fill:#333333; -fx-font-weight:bold; -fx-padding:2 8 2 8;");
        manifestButton.setOnAction(e -> doManifest());
        attackButton.setStyle("-fx-font-size:11; -fx-background-color:#E53935; -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:2 8 2 8;");
        attackButton.setOnAction(e -> startAttack());
        skillButton.setStyle("-fx-font-size:11; -fx-background-color:#FFD700; -fx-text-fill:#333333; -fx-font-weight:bold; -fx-padding:2 8 2 8;");
        skillButton.setOnAction(e -> doSkill());

        playerHandBox = createHandPane();
        opponentHandIndicator = createOpponentHandIndicator();

        // エレメントコア表示（攻撃対象選択中は防御側コアをクリックで対象にできる）
        opponentCoreLabel = createCoreLabel();
        playerCoreLabel = createCoreLabel();
        opponentCoreLabel.setOnMouseClicked(e -> {
            if (attackTargetActive && game.getCurrentPlayer() == Game.Player.PLAYER1) {
                setAttackTarget(null, opponentCoreLabel, true);
            }
        });
        playerCoreLabel.setOnMouseClicked(e -> {
            if (attackTargetActive && game.getCurrentPlayer() == Game.Player.PLAYER2) {
                setAttackTarget(null, playerCoreLabel, true);
            }
        });

        battleFieldBox = new VBox(0, createPlayerArea(true), createPlayerArea(false));
        battleFieldBox.setAlignment(Pos.CENTER);
        battleFieldBox.setPadding(new Insets(0));
        battleFieldBox.setMaxWidth(Double.MAX_VALUE);
        battleFieldBox.setMaxHeight(Double.MAX_VALUE);
        battleFieldBox.setStyle("-fx-border-color:#999999; -fx-border-width:2; -fx-background-color:#f8f8f8; -fx-border-radius:8; -fx-background-radius:8;");
        VBox.setVgrow(battleFieldBox, Priority.ALWAYS);

        // 左カラム: 相手コア（上）＋相手ハンド枚数表示＋レムナント閲覧パネル（下）。
        remnantListPanel = createRemnantListPanel();
        VBox leftColumn = new VBox(8, opponentCoreLabel, opponentHandIndicator, remnantListPanel);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        leftColumn.setMinWidth(150);
        leftColumn.setPrefWidth(150);
        VBox.setVgrow(remnantListPanel, Priority.ALWAYS);

        HBox battleRow = new HBox(8, leftColumn, battleFieldBox);
        battleRow.setAlignment(Pos.TOP_LEFT);
        battleRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(battleFieldBox, Priority.ALWAYS);
        VBox.setVgrow(battleRow, Priority.ALWAYS);

        confirmBar = createConfirmBar();
        choiceBar = createChoiceBar();

        // 下段: 自分コア（左下）＋ハンド
        VBox playerCoreBox = new VBox(playerCoreLabel);
        playerCoreBox.setAlignment(Pos.CENTER_LEFT);
        playerCoreBox.setMinWidth(150);
        playerCoreBox.setPrefWidth(150);
        HBox bottomRow = new HBox(8, playerCoreBox, playerHandBox);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(playerHandBox, Priority.NEVER);

        VBox center = new VBox(8, battleRow, confirmBar, choiceBar, bottomRow);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(6));
        center.setMaxWidth(Double.MAX_VALUE);
        center.setMaxHeight(Double.MAX_VALUE);
        center.setMinSize(0, 0);
        VBox.setVgrow(center, Priority.ALWAYS);
        return center;
    }

    /** エレメントコア表示ラベル（◆：N）を作る。 */
    private Label createCoreLabel() {
        Label label = new Label();
        label.setFont(Font.font(18));
        label.setStyle("-fx-font-weight:bold; -fx-text-fill:#1a3a8a;");
        return label;
    }

    /**
     * 選択操作の確認バー（決定／キャンセル）。通常は非表示で、選択モード中のみ表示する。
     */
    private HBox createConfirmBar() {
        confirmPrompt = new Label();
        confirmPrompt.setFont(Font.font(13));

        confirmOkButton = new Button("決定");
        confirmOkButton.setOnAction(e -> onConfirm());

        Button cancelButton = new Button("キャンセル");
        cancelButton.setOnAction(e -> onCancel());

        HBox bar = new HBox(12, confirmPrompt, confirmOkButton, cancelButton);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(6));
        bar.setStyle("-fx-background-color:#fff8e1; -fx-border-color:#DAA520; -fx-border-width:1; -fx-background-radius:4;");
        bar.setVisible(false);
        bar.setManaged(false);
        return bar;
    }

    /**
     * リアクティブ選択バー（ガードチェック／スキルチェック）。モーダルにせず画面内に選択肢を出すため、
     * バーを出している間も両者のハンド／レムナント／エリアのカードをタップして詳細を確認できる。
     */
    private HBox createChoiceBar() {
        choicePrompt = new Label();
        choicePrompt.setFont(Font.font(13));
        HBox bar = new HBox(10, choicePrompt);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(6));
        bar.setStyle("-fx-background-color:#e3f2fd; -fx-border-color:#1E90FF; -fx-border-width:1; -fx-background-radius:4;");
        bar.setVisible(false);
        bar.setManaged(false);
        return bar;
    }

    /** 非ブロッキングの選択肢バーを表示する（label→処理）。選択でバーを閉じてから処理を実行する。 */
    private void showChoiceBar(String prompt, java.util.LinkedHashMap<String, Runnable> options) {
        choiceActive = true;
        choicePrompt.setText(prompt);
        choiceBar.getChildren().setAll(choicePrompt);
        for (java.util.Map.Entry<String, Runnable> en : options.entrySet()) {
            Button b = new Button(en.getKey());
            Runnable r = en.getValue();
            b.setOnAction(ev -> {
                hideChoiceBar();
                r.run();
            });
            choiceBar.getChildren().add(b);
        }
        choiceBar.setVisible(true);
        choiceBar.setManaged(true);
        statusLabel.setText(prompt);
    }

    private void hideChoiceBar() {
        choiceActive = false;
        choiceBar.getChildren().clear();
        choiceBar.setVisible(false);
        choiceBar.setManaged(false);
    }

    // ===== バトル中の発光（攻撃=赤／対象=青、ガード可能=黄） =====

    /** カードが置かれているエリアスロットのセルを返す（無ければ null）。 */
    private StackPane cellOf(Card card) {
        if (card == null) {
            return null;
        }
        for (Game.Player p : Game.Player.values()) {
            Card[] slots = slotsFor(p);
            StackPane[] cells = slotCellsFor(p);
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == card) {
                    return cells[i];
                }
            }
        }
        return null;
    }

    /** 攻撃エンティティ＝赤、攻撃対象（エンティティ青／エレメントは相手コア青）の発光を設定する。 */
    private void setBattleGlows(Card atkCard, Card tgtCard, boolean tgtElem, Game.Player defP) {
        battleAtkCard = atkCard;
        battleTgtCard = tgtElem ? null : tgtCard;
        battleTgtElem = tgtElem;
        battleDefP = defP;
        applyBattleGlows();
    }

    /** 現在のバトル発光状態をセル／コアに適用する（render の最後に再適用される）。 */
    private void applyBattleGlows() {
        if (battleAtkCard != null) {
            StackPane cell = cellOf(battleAtkCard);
            if (cell != null) {
                cell.setEffect(CardView.attackGlow());
            }
        }
        if (battleTgtElem) {
            Label core = battleDefP == Game.Player.PLAYER1 ? playerCoreLabel : opponentCoreLabel;
            if (core != null) {
                core.setEffect(CardView.targetGlow());
            }
        } else if (battleTgtCard != null) {
            StackPane cell = cellOf(battleTgtCard);
            if (cell != null) {
                cell.setEffect(CardView.targetGlow());
            }
        }
    }

    /** バトル発光を消す。 */
    private void clearBattleGlows() {
        if (battleAtkCard != null) {
            StackPane c = cellOf(battleAtkCard);
            if (c != null) {
                c.setEffect(null);
            }
        }
        if (battleTgtCard != null) {
            StackPane c = cellOf(battleTgtCard);
            if (c != null) {
                c.setEffect(null);
            }
        }
        if (playerCoreLabel != null) {
            playerCoreLabel.setEffect(null);
        }
        if (opponentCoreLabel != null) {
            opponentCoreLabel.setEffect(null);
        }
        battleAtkCard = null;
        battleTgtCard = null;
        battleTgtElem = false;
        battleDefP = null;
    }

    /** ガードチェック中、ガード可能なエンティティを黄色く光らせる。 */
    private void highlightGuardEntities(List<Card> guards) {
        clearGuardHighlights();
        for (Card g : guards) {
            StackPane cell = cellOf(g);
            if (cell != null) {
                cell.setEffect(CardView.selectionGlow());
                guardGlowNodes.add(cell);
            }
        }
    }

    private void clearGuardHighlights() {
        for (Node n : guardGlowNodes) {
            if (n != null) {
                n.setEffect(null);
            }
        }
        guardGlowNodes.clear();
    }

    /**
     * レムナント閲覧パネル（公開情報）。レムナント枠クリックで該当プレイヤーのカード一覧を縦スクロール表示する。
     */
    private VBox createRemnantListPanel() {
        remnantListTitle = new Label();
        remnantListTitle.setFont(Font.font(12));
        remnantListTitle.setStyle("-fx-font-weight:bold;");

        Button closeButton = new Button("閉じる");
        closeButton.setOnAction(e -> hideRemnantList());

        HBox header = new HBox(6, remnantListTitle, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);

        remnantListContent = new VBox(4);
        remnantListContent.setPadding(new Insets(4));

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(remnantListContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox panel = new VBox(4, header, scroll);
        panel.setPadding(new Insets(6));
        panel.setStyle("-fx-border-color:#999999; -fx-border-width:1; -fx-background-color:#ffffff;");
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setVisible(false);
        panel.setManaged(false);
        return panel;
    }

    /** 指定プレイヤーのレムナント一覧を左パネルに表示する。 */
    private void showRemnantList(Game.Player owner) {
        remnantListOwner = owner;
        List<Card> remnant = remnantFor(owner);
        remnantListTitle.setText(owner.getDisplayName() + "のレムナント (" + remnant.size() + ")");
        remnantListContent.getChildren().clear();
        if (remnant.isEmpty()) {
            Label empty = new Label("（空）");
            empty.setFont(Font.font(11));
            remnantListContent.getChildren().add(empty);
        } else {
            for (Card c : remnant) {
                remnantListContent.getChildren().add(createRemnantItem(c));
            }
        }
        remnantListPanel.setVisible(true);
        remnantListPanel.setManaged(true);
    }

    private void hideRemnantList() {
        remnantListOwner = null;
        remnantListPanel.setVisible(false);
        remnantListPanel.setManaged(false);
    }

    /** レムナント一覧の1行。属性色で塗り、クリックで右側に詳細表示する。 */
    private Node createRemnantItem(Card c) {
        StringBuilder sb = new StringBuilder(c.name);
        if (c.atk != null) {
            sb.append("  ATK/").append(c.atk);
        }
        if (c.cost != null) {
            sb.append("  C/").append(c.cost);
        }
        Label item = new Label(sb.toString());
        item.setFont(Font.font(11));
        item.setWrapText(true);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setPadding(new Insets(4, 6, 4, 6));
        Attribute a = c.attribute;
        if (a != null) {
            String border = a.borderColor != null
                    ? "-fx-border-color:" + a.borderColor + "; -fx-border-width:2;"
                    : "-fx-border-color:#999999; -fx-border-width:1;";
            item.setStyle("-fx-background-color:" + a.fillColor + "; -fx-text-fill:" + a.textColor + ";" + border);
        } else {
            item.setStyle("-fx-background-color:#ffffff; -fx-border-color:#999999; -fx-border-width:1;");
        }
        item.setOnMouseClicked(e -> {
            // 再生装備の対象選択中は、自分のレムナントのエンティティをクリックで対象指定。
            if (reviveTargetActive && c.owner == game.getCurrentPlayer() && c.type == CardType.ENTITY) {
                if ("フレイム・キッズ".equals(c.name)) {
                    statusLabel.setText("フレイム・キッズは【再生】できません。");
                } else {
                    setReviveTarget(c, item);
                }
            } else if (manifestReviveActive && c.owner == game.getCurrentPlayer()
                    && isManifestReviveCandidate(c)) {
                setManifestReviveTarget(c, item);
            } else if (skyReturnActive && c.owner == game.getCurrentPlayer()
                    && c.type == CardType.SKILL) {
                setSkyReturnTarget(c, item);
            } else if (recycleToHandActive && isRecycleHandCandidate(c)) {
                setRecycleTarget(c, item);
            } else if (recycleReviveActive && isRecycleReviveCandidate(c)) {
                setRecycleTarget(c, item);
            } else {
                showCardDetail(c);
            }
        });

        // 自己再生できるカード（リビングデッド・ナイト等）には、条件を満たす時のみ「再生」ボタンを出す。
        if (canSelfReviveNow(c)) {
            Button reviveBtn = new Button("再生");
            reviveBtn.setStyle("-fx-font-size:10; -fx-padding:1 6 1 6;");
            reviveBtn.setOnAction(e -> startSelfRevive(c));
            HBox row = new HBox(4, item, reviveBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(item, Priority.ALWAYS);
            return row;
        }
        return item;
    }

    /** カード c をいま自己再生できるか（自分のレムナント・スタンバイ・空きスロット・コスト支払い可能）。 */
    private boolean canSelfReviveNow(Card c) {
        return !isBusy()
                && game.getCurrentPhase() == Game.Phase.STANDBY
                && c.owner == game.getCurrentPlayer()
                && "リビングデッド・ナイト".equals(c.name)
                && firstEmptySlot(slotsFor(c.owner)) >= 0
                && effectiveCost(c) <= handFor(c.owner).size();
    }

    /**
     * 相手ハンドの枚数表示（非公開情報）。カード1枚分のサイズで「相手ハンド ×x」と表示する。
     */
    private VBox createOpponentHandIndicator() {
        Label title = new Label("相手\nハンド");
        title.setFont(Font.font(12));
        title.setTextAlignment(TextAlignment.CENTER);

        opponentHandCountLabel = new Label("×" + state.opponentHand.size());
        opponentHandCountLabel.setFont(Font.font(15));
        opponentHandCountLabel.setStyle("-fx-font-weight:bold;");

        VBox box = new VBox(6, title, opponentHandCountLabel);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-border-color:#999999; -fx-border-width:1; -fx-background-color:#ffffff;");
        box.setMinSize(CARD_PREF_WIDTH, CARD_PREF_HEIGHT);
        box.setPrefSize(CARD_PREF_WIDTH, CARD_PREF_HEIGHT);
        box.setMaxSize(CARD_PREF_WIDTH, CARD_PREF_HEIGHT);
        return box;
    }

    /** 相手ハンド枚数表示を最新の状態へ更新する。 */
    private void updateOpponentHandCount() {
        if (opponentHandCountLabel != null) {
            opponentHandCountLabel.setText("×" + state.opponentHand.size());
        }
    }

    /**
     * 手札カードのセルを属性のカラーコードで塗り分ける。
     * 枠色（aスキルの金枠・空の淡い枠）が指定されていればそれを使用する。
     */
    private static final double CARD_PREF_WIDTH = 80;
    private static final double CARD_PREF_HEIGHT = 112;

    private void selectCard(Card card, Node node) {
        if (selectionActive) {
            // 選択モード中はコスト支払い対象のトグルとして扱う。
            toggleSelectionCard(card, node);
            showCardDetail(card);
            return;
        }
        if (attackTargetActive || skillTargetActive || choiceActive) {
            // 対象選択中・ガード/スキルチェック中は選択カードを変えず、詳細表示のみ。
            showCardDetail(card);
            return;
        }
        if (fairyManifestActive) {
            // 舞踊る妖精の無償顕現: ハンドの候補（コスト1以下の森属性）をクリックで指定。
            if (isFairyManifestCandidate(card)) {
                setFairyManifestTarget(card, node);
            }
            showCardDetail(card);
            return;
        }
        selectedCard = card;
        selectedCardNode = node;
        showCardDetail(card);
        updateManifestButton();
        updateAttackButton();
        updateSkillButton();
    }

    /**
     * 選択中の手札エンティティが顕現可能（スタンバイ中・空きスロットあり・コスト支払い可能）なら、
     * そのカードの上に「顕現」ボタンを表示する。条件を満たさない場合は非表示。
     */
    private void updateManifestButton() {
        if (manifestButtonHost != null) {
            manifestButtonHost.getChildren().remove(manifestButton);
            manifestButtonHost = null;
        }
        if (isBusy()) {
            return;
        }
        Card c = selectedCard;
        if (c == null || c.zone != Zone.HAND || c.type != CardType.ENTITY) {
            return;
        }
        Game.Player current = game.getCurrentPlayer();
        if (c.owner != current) {
            return;
        }
        if (game.getCurrentPhase() != Game.Phase.STANDBY) {
            return;
        }
        if (firstEmptySlot(slotsFor(current)) < 0) {
            return;
        }
        int cost = effectiveCost(c);
        if (cost > handFor(current).size() - 1) {
            return; // コスト支払い不可
        }
        StackPane cell = handCellMap.get(c);
        if (cell == null) {
            return;
        }
        StackPane.setAlignment(manifestButton, Pos.TOP_CENTER);
        cell.getChildren().add(manifestButton);
        manifestButtonHost = cell;
    }

    /**
     * 選択中の手札スキルがスキルフェイズに発動可能なら、そのカードの上に「発動」ボタンを表示する。
     */
    private void updateSkillButton() {
        if (skillButtonHost != null) {
            skillButtonHost.getChildren().remove(skillButton);
            skillButtonHost = null;
        }
        if (isBusy()) {
            return;
        }
        Card c = selectedCard;
        if (c == null || c.zone != Zone.HAND || c.type != CardType.SKILL) {
            return;
        }
        Game.Player current = game.getCurrentPlayer();
        if (c.owner != current) {
            return;
        }
        if (game.getCurrentPhase() != Game.Phase.SKILL || skillUsedThisPhase) {
            return;
        }
        if (!skillUsableInSkillPhase(c, current)) {
            return; // 条件を満たさないスキルは発動ボタンを出さない
        }
        StackPane cell = handCellMap.get(c);
        if (cell == null) {
            return;
        }
        StackPane.setAlignment(skillButton, Pos.TOP_CENTER);
        cell.getChildren().add(skillButton);
        skillButtonHost = cell;
    }

    /** カード詳細を右側パネルに表示する（描画は CardDetailPanel に委譲）。 */
    private void showCardDetail(Card card) {
        detailPanel.show(card, effectiveCost(card), card.owner == Game.Player.PLAYER1);
    }

    private void initializeHands() {
        state.playerHand.clear();
        state.opponentHand.clear();
        state.playerRemnant.clear();
        state.opponentRemnant.clear();
        state.playerSeal.clear();
        state.opponentSeal.clear();
        state.playerLibrary.clear();
        state.opponentLibrary.clear();
        state.attackedEntities.clear();
        state.mistGuardUsed.clear();
        state.effectImmuneThisTurn.clear();
        state.tramplersThisTurn.clear();
        state.guardDisabledThisTurn.clear();
        state.atkZeroedThisTurn.clear();
        defenderSkillCheckUsed = false;
        skillCheckSkillActive = false;
        skillCastPlayer = null;
        gameOver = false;
        npcThinking = false;
        handViewOwner = Game.Player.PLAYER1;
        scAttacker = null;
        scTarget = null;
        scAtkP = null;
        scDefP = null;
        pendingSkillCheckCard = null;
        skillUsedThisPhase = false;
        for (int i = 0; i < 3; i++) {
            state.playerSlots[i] = null;
            state.opponentSlots[i] = null;
        }
        selectedCard = null;
        selectedCardNode = null;

        // PLAYER1: 使用中デッキ（バトル使用可なら）からライブラリを生成。無ければ全36種プール。
        Deck active = deckStore != null ? deckStore.active() : null;
        if (active != null && deckScreens != null && deckScreens.isUsable(active)) {
            state.playerLibrary.addAll(CardLoader.buildDeckLibrary(active, Game.Player.PLAYER1));
        } else {
            state.playerLibrary.addAll(buildLibrary(Game.Player.PLAYER1));
        }
        // PLAYER2(NPC): 指定NPCデッキ（バトル使用可なら）。無ければ全36種プール。
        if (npcDeck != null && deckScreens != null && deckScreens.isUsable(npcDeck)) {
            state.opponentLibrary.addAll(CardLoader.buildDeckLibrary(npcDeck, Game.Player.PLAYER2));
        } else {
            state.opponentLibrary.addAll(buildLibrary(Game.Player.PLAYER2));
        }
        Collections.shuffle(state.playerLibrary);
        Collections.shuffle(state.opponentLibrary);

        // 初期手札はお互い5枚（スタンバイ移行時に2枚引いて7枚になる）。
        drawCards(Game.Player.PLAYER1, 5);
        drawCards(Game.Player.PLAYER2, 5);

        refreshHandPanes();
        refreshSlots();
        render();
        detailPanel.reset();
    }

    private void refreshHandPanes() {
        if (playerHandBox != null) {
            Node container = playerHandBox.getChildren().get(1);
            HBox cards = extractCardsHBox(container);
            if (cards != null) buildHandCards(cards);
        }
        updateOpponentHandCount();
    }

    private HBox extractCardsHBox(Node container) {
        if (container instanceof HBox) {
            return (HBox) container;
        }
        if (container instanceof StackPane) {
            for (Node n : ((StackPane) container).getChildren()) {
                if (n instanceof javafx.scene.control.ScrollPane) {
                    Node content = ((javafx.scene.control.ScrollPane) n).getContent();
                    if (content instanceof HBox) return (HBox) content;
                } else if (n instanceof HBox) {
                    return (HBox) n;
                }
            }
        }
        if (container instanceof javafx.scene.control.ScrollPane) {
            Node content = ((javafx.scene.control.ScrollPane) container).getContent();
            if (content instanceof HBox) return (HBox) content;
        }
        return null;
    }

    /** 指定プレイヤーが count 枚引く。 */
    private void drawCards(Game.Player player, int count) {
        for (int i = 0; i < count; i++) {
            drawOne(player);
        }
        refreshHandPanes();
    }

    /** 1枚ドローする（ライブラリの先頭からハンドへ）。 */
    private void drawOne(Game.Player player) {
        List<Card> lib = player == Game.Player.PLAYER1 ? state.playerLibrary : state.opponentLibrary;
        if (lib.isEmpty()) {
            // ライブラリアウト: ドローできなかったプレイヤーの敗北。
            game.markDeckOut(player);
            return;
        }
        Card drawn = lib.remove(0);
        drawn.zone = Zone.HAND;
        handFor(player).add(drawn);
    }

    /** 第1弾「新生の胎動」全36種をそのプレイヤー所有で生成する。 */
    /** 第1弾「新生の胎動」全36種を cards.json から読み込んで生成する。 */
    private List<Card> buildLibrary(Game.Player o) {
        return CardLoader.buildLibrary(o);
    }

    private void doManifest() {
        if (blockedByGameOver() || npcThinking) {
            return;
        }
        if (isBusy()) {
            statusLabel.setText("選択を完了してください（決定／キャンセル）。");
            return;
        }
        if (game.getCurrentPhase() != Game.Phase.STANDBY) {
            statusLabel.setText("顕現はスタンバイフェイズ中のみ可能です。");
            return;
        }
        Game.Player current = game.getCurrentPlayer();
        if (selectedCard == null || selectedCard.owner != current || selectedCard.zone != Zone.HAND) {
            statusLabel.setText("顕現するエンティティを手札から選択してください。");
            return;
        }
        if (selectedCard.type != CardType.ENTITY) {
            statusLabel.setText("顕現できるのはエンティティのみです。");
            return;
        }
        int slot = firstEmptySlot(slotsFor(current));
        if (slot < 0) {
            statusLabel.setText("エンティティスロットに空きがありません。");
            return;
        }
        int cost = effectiveCost(selectedCard);
        List<Card> hand = handFor(current);
        int payable = hand.size() - 1; // 顕現するカード自身は支払いに使えない
        if (cost > payable) {
            statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚 / 支払い可能" + payable + "枚）。");
            return;
        }

        if (cost == 0) {
            placeEntity(current, selectedCard, slot, selectedCardNode);
        } else {
            beginCostSelection(selectedCard, cost, slot);
        }
    }

    /** コスト支払いのカード選択モードを開始する。 */
    private void beginCostSelection(Card entity, int cost, int slot) {
        selectionActive = true;
        clearActionHighlights(); // 青ハイライトを消す（選択モードは黄グロー）
        selectionRequired = cost;
        selectionChosen.clear();
        selectionNodeMap.clear();
        manifestPendingEntity = entity;
        manifestPendingSlot = slot;
        manifestSourceNode = selectedCardNode;
        updateManifestButton(); // 選択モード中は顕現ボタンを隠す
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        updateSelectionPrompt();
        statusLabel.setText("顕現「" + entity.name + "」のコスト支払い: 手札を選択してください。");
    }

    private void updateSelectionPrompt() {
        String what;
        if (pendingSkillCheckPay) {
            what = "スキルチェックのコストとして払う手札を";
        } else if (pendingPyroDraw) {
            what = "ドローのためコストとして払う手札を";
        } else if (pendingDiscardOnly) {
            what = "ディスカードする手札を";
        } else {
            what = "コストとしてレムナントへ送る手札を";
        }
        confirmPrompt.setText(what + " " + selectionRequired
                + " 枚選択（選択中 " + selectionChosen.size() + " 枚）");
        confirmOkButton.setDisable(selectionChosen.size() != selectionRequired);
    }

    /** 選択モード中の手札クリック。コスト支払い対象をトグルする。 */
    private void toggleSelectionCard(Card card, Node node) {
        if (card == manifestPendingEntity) {
            statusLabel.setText("顕現するカード自身はコストにできません。");
            return;
        }
        if (card == recycleSkill) {
            statusLabel.setText("発動するスキル自身はコストにできません。");
            return;
        }
        if (card == pendingSkill) {
            statusLabel.setText("発動するスキル自身はコストにできません。");
            return;
        }
        if (card == pendingSkillCheckCard) {
            statusLabel.setText("発動するスキル自身はコストにできません。");
            return;
        }
        if (card.owner != selectionOwner() || card.zone != Zone.HAND) {
            return;
        }
        if (selectionChosen.contains(card)) {
            selectionChosen.remove(card);
            node.setEffect(null);
        } else {
            if (selectionChosen.size() >= selectionRequired) {
                statusLabel.setText("すでに " + selectionRequired + " 枚選択済みです。");
                return;
            }
            selectionChosen.add(card);
            selectionNodeMap.put(card, node);
            node.setEffect(CardView.selectionGlow());
        }
        updateSelectionPrompt();
    }

    /** 手札・スロットのセルに付けたグローをすべて消す。 */
    private void clearActionHighlights() {
        for (StackPane cell : handCellMap.values()) {
            if (cell != null) {
                cell.setEffect(null);
            }
        }
        for (StackPane cell : playerSlotCells) {
            if (cell != null) {
                cell.setEffect(null);
            }
        }
        for (StackPane cell : opponentSlotCells) {
            if (cell != null) {
                cell.setEffect(null);
            }
        }
    }

    /**
     * 現フェイズで行動可能なカード（顕現可能エンティティ／発動可能スキル／攻撃可能エンティティ）の
     * 縁を青く光らせる。選択モード中は黄グロー管理に任せるため何もしない。
     */
    private void refreshActionHighlights() {
        if (isBusy()) {
            return;
        }
        clearActionHighlights();
        // 相手（NPC）の手番では行動ハイライト（青）を出さない。攻撃=赤／被攻撃=青／ガード=黄 のみを残す。
        if (npcThinking || isNpc(game.getCurrentPlayer())) {
            return;
        }
        Game.Player p = game.getCurrentPlayer();
        switch (game.getCurrentPhase()) {
            case STANDBY: {
                if (firstEmptySlot(slotsFor(p)) < 0) {
                    break;
                }
                int payable = handFor(p).size() - 1;
                for (Card c : handFor(p)) {
                    if (c.type == CardType.ENTITY && effectiveCost(c) <= payable) {
                        glowCell(handCellMap.get(c));
                    }
                }
                break;
            }
            case SKILL: {
                if (skillUsedThisPhase) {
                    break;
                }
                for (Card c : handFor(p)) {
                    // 発動可能＝コスト支払い可能 かつ 効果の対象/条件を満たす。
                    if (skillUsableInSkillPhase(c, p)) {
                        glowCell(handCellMap.get(c));
                    }
                }
                break;
            }
            case BATTLE: {
                if (!canAttackNow()) {
                    break;
                }
                Card[] slots = slotsFor(p);
                StackPane[] cells = slotCellsFor(p);
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] != null && !state.attackedEntities.contains(slots[i]) && !hasKeyword(slots[i], "拘束")) {
                        glowCell(cells[i]);
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private void glowCell(StackPane cell) {
        if (cell != null) {
            cell.setEffect(CardView.actionGlow());
        }
    }

    /** 決定: 選択した手札をレムナントへ送り、エンティティを場へ出す。 */
    private void confirmSelection() {
        if (!selectionActive || selectionChosen.size() != selectionRequired) {
            return;
        }
        if (pendingSkillCheckPay) {
            resolveSkillCheckPayment();
            return;
        }
        // スキルのコスト支払いは発動者（スキルチェック中は防御側）、それ以外は手番プレイヤー。
        Game.Player player = (pendingSkill != null) ? skillCaster() : game.getCurrentPlayer();
        List<Card> hand = handFor(player);
        List<Card> remnant = remnantFor(player);
        for (Card c : selectionChosen) {
            hand.remove(c);
            c.zone = Zone.REMNANT;
            remnant.add(c);
        }
        for (Node n : selectionNodeMap.values()) {
            n.setEffect(null);
        }
        boolean discardOnly = pendingDiscardOnly;
        boolean pyroDraw = pendingPyroDraw;
        boolean recycleCost = pendingRecycleCost;
        boolean skillResolve = pendingSkill != null;
        boolean reviveResolve = pendingReviveEntity != null;
        Card entity = manifestPendingEntity;
        int slot = manifestPendingSlot;
        Node source = manifestSourceNode;
        Card skill = pendingSkill;
        Card skillTarget = skillTargetCard;
        Card revive = pendingReviveEntity;
        endSelection();
        if (recycleCost) {
            resolveRecycleAfterPayment(player);
        } else if (pyroDraw) {
            // パイロ・インプ: コスト1枚を払った（レムナントへ送済）→ 1ドロー。
            drawOne(player);
            refreshHandPanes();
            render();
            statusLabel.setText("パイロ・インプ: コスト1枚を払って1ドロー。");
        } else if (discardOnly) {
            // ディスカードのみ（顕現/スキル/再生を伴わない）。
            refreshHandPanes();
            render();
            statusLabel.setText("ディスカード完了。");
        } else if (skillResolve) {
            resolveSkill(player, skill, skillTarget);
        } else if (reviveResolve) {
            resolveSelfRevive(player, revive);
        } else {
            placeEntity(player, entity, slot, source);
        }
    }

    /** キャンセル: 選択を破棄して顕現を取りやめる。 */
    private void cancelSelection() {
        for (Node n : selectionNodeMap.values()) {
            n.setEffect(null);
        }
        boolean wasSkill = pendingSkill != null;
        boolean wasRevive = pendingReviveEntity != null;
        boolean wasDiscard = pendingDiscardOnly;
        boolean wasPyro = pendingPyroDraw;
        boolean wasRecycle = pendingRecycleCost;
        boolean wasSkillCheck = pendingSkillCheckPay || skillCheckSkillActive;
        skillCard = null;
        skillTargetCard = null;
        skillTargetNode = null;
        endSelection();
        if (wasSkillCheck) {
            // 防御側のコスト支払いを中断＝割り込みを取りやめ（効果は適用しない）。発動カードは手札/レムナントに残る。
            skillCheckSkillActive = false;
            skillCastPlayer = null;
            handViewOwner = Game.Player.PLAYER1;
            refreshHandPanes();
            render();
            statusLabel.setText("スキルチェックの割り込みを取りやめました。");
            resumeBattleAfterSkillCheck(); // 効果なしでバトルへ
            return;
        }
        if (wasRecycle) {
            clearRecycleState();
        }
        refreshActionHighlights();
        statusLabel.setText(wasRecycle ? "リサイクル・アースをキャンセルしました。"
                : wasPyro ? "パイロ・インプの効果を見送りました。"
                : wasDiscard ? "ディスカードを見送りました。"
                : wasSkill ? "スキルをキャンセルしました。"
                : wasRevive ? "再生をキャンセルしました。" : "顕現をキャンセルしました。");
    }

    private void endSelection() {
        selectionActive = false;
        selectionRequired = 0;
        selectionChosen.clear();
        selectionNodeMap.clear();
        manifestPendingEntity = null;
        manifestPendingSlot = -1;
        manifestSourceNode = null;
        pendingSkill = null;
        pendingReviveEntity = null;
        pendingDiscardOnly = false;
        pendingPyroDraw = false;
        pendingRecycleCost = false;
        pendingSkillCheckPay = false;
        selectionPlayer = null;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
    }

    /** 手札を count 枚ディスカードする選択を開始する（ダンシング・クラウド等）。 */
    private void beginDiscardSelection(int count) {
        selectionActive = true;
        clearActionHighlights();
        selectionRequired = count;
        selectionChosen.clear();
        selectionNodeMap.clear();
        pendingDiscardOnly = true;
        updateManifestButton();
        updateAttackButton();
        updateSkillButton();
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        updateSelectionPrompt();
        statusLabel.setText("ディスカードする手札を " + count + " 枚選択してください。");
    }

    /** パイロ・インプ: コスト1枚を払って1ドローする選択を開始する（不要ならキャンセルで見送り）。 */
    private void beginPyroPayment() {
        selectionActive = true;
        clearActionHighlights();
        selectionRequired = 1;
        selectionChosen.clear();
        selectionNodeMap.clear();
        pendingPyroDraw = true;
        updateManifestButton();
        updateAttackButton();
        updateSkillButton();
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        updateSelectionPrompt();
        statusLabel.setText("パイロ・インプ: コスト1枚を払えば1ドロー。払う手札を選択（不要ならキャンセル）。");
    }

    /** リビングデッド・ナイト等の自己再生を開始する（レムナント一覧の「再生」ボタンから呼ぶ）。 */
    private void startSelfRevive(Card entity) {
        if (isBusy()) {
            statusLabel.setText("選択を完了してください（決定／キャンセル）。");
            return;
        }
        if (game.getCurrentPhase() != Game.Phase.STANDBY) {
            statusLabel.setText("再生はスタンバイフェイズ中のみ可能です。");
            return;
        }
        Game.Player current = game.getCurrentPlayer();
        if (entity.owner != current || !remnantFor(current).contains(entity)) {
            return;
        }
        if (firstEmptySlot(slotsFor(current)) < 0) {
            statusLabel.setText("エンティティスロットに空きがありません。");
            return;
        }
        int cost = effectiveCost(entity);
        if (cost > handFor(current).size()) {
            statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
            return;
        }
        if (cost == 0) {
            resolveSelfRevive(current, entity);
        } else {
            selectionActive = true;
            clearActionHighlights();
            selectionRequired = cost;
            selectionChosen.clear();
            selectionNodeMap.clear();
            pendingReviveEntity = entity;
            confirmBar.setVisible(true);
            confirmBar.setManaged(true);
            updateSelectionPrompt();
            statusLabel.setText("再生「" + entity.name + "」のコスト支払い: 手札を選択してください。");
        }
    }

    /** 自己再生を解決する（レムナントから場へ。【再生】時の効果を適用）。 */
    private void resolveSelfRevive(Game.Player player, Card entity) {
        remnantFor(player).remove(entity);
        entity.equipments.clear();
        entity.addedEffects.clear();
        entity.zone = Zone.BATTLE;
        entity.revealed = true;
        applyReviveTriggers(entity);
        int slot = firstEmptySlot(slotsFor(player));
        slotsFor(player)[slot] = entity;
        selectedCard = null;
        selectedCardNode = null;
        refreshHandPanes();
        refreshSlots();
        render();
        statusLabel.setText("再生: " + entity.name);
    }

    /** 確認バーの「決定」。実行中のモードに応じて処理を振り分ける。 */
    private void onConfirm() {
        if (selectionActive) {
            confirmSelection();
        } else if (attackTargetActive) {
            confirmAttack();
        } else if (skillTargetActive) {
            confirmSkillTarget();
        } else if (reviveTargetActive) {
            confirmReviveTarget();
        } else if (manifestEffectActive) {
            confirmManifestTarget();
        } else if (manifestReviveActive) {
            confirmManifestRevive();
        } else if (skyReturnActive) {
            confirmSkyReturn();
        } else if (fairyManifestActive) {
            confirmFairyManifest();
        } else if (recycleToHandActive) {
            confirmRecycleToHand();
        } else if (recycleReviveActive) {
            confirmRecycleRevive();
        }
    }

    /** 確認バーの「キャンセル」。 */
    private void onCancel() {
        if (selectionActive) {
            cancelSelection();
        } else if (attackTargetActive) {
            cancelAttack();
        } else if (skillTargetActive) {
            cancelSkillTarget();
        } else if (reviveTargetActive) {
            cancelReviveTarget();
        } else if (manifestEffectActive) {
            cancelManifestTarget();
        } else if (manifestReviveActive) {
            cancelManifestRevive();
        } else if (skyReturnActive) {
            cancelSkyReturn();
        } else if (fairyManifestActive) {
            cancelFairyManifest();
        } else if (recycleToHandActive || recycleReviveActive) {
            cancelRecycle();
        }
    }

    private boolean isBusy() {
        return selectionActive || attackTargetActive || skillTargetActive
                || reviveTargetActive || manifestEffectActive || manifestReviveActive
                || skyReturnActive || fairyManifestActive
                || recycleToHandActive || recycleReviveActive || choiceActive;
    }

    // ===== バトルフロー =====

    /** バトルフェイズで攻撃可能か（先攻=P1の1ターン目は攻撃不可）。 */
    private boolean canAttackNow() {
        if (game.getCurrentPhase() != Game.Phase.BATTLE) {
            return false;
        }
        // 先攻プレイヤーの1ターン目（ラウンド1の先攻手番）のみ攻撃不可。後攻は1ターン目から攻撃可。
        return !(game.getTurnNumber() == 1 && game.getCurrentPlayer() == game.getFirstPlayer());
    }

    /** カードがキーワード（固有能力）を持つか。 */
    private boolean hasKeyword(Card c, String keyword) {
        return c != null && c.description != null && c.description.contains("【" + keyword + "】");
    }

    /**
     * 選択中の自エンティティがバトルフェイズに攻撃可能なら、その上に「攻撃」ボタンを表示する。
     */
    private void updateAttackButton() {
        if (attackButtonHost != null) {
            attackButtonHost.getChildren().remove(attackButton);
            attackButtonHost = null;
        }
        if (isBusy() || attacker != null) {
            return;
        }
        Card c = selectedCard;
        if (c == null || c.zone != Zone.BATTLE) {
            return;
        }
        Game.Player current = game.getCurrentPlayer();
        if (c.owner != current || !canAttackNow() || hasKeyword(c, "拘束")) {
            return;
        }
        Card[] slots = slotsFor(current);
        StackPane[] cells = slotCellsFor(current);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == c) {
                StackPane.setAlignment(attackButton, Pos.TOP_CENTER);
                cells[i].getChildren().add(attackButton);
                attackButtonHost = cells[i];
                break;
            }
        }
    }

    /** 攻撃宣言を開始し、対象選択モードに入る。 */
    private void startAttack() {
        if (blockedByGameOver() || npcThinking) {
            return;
        }
        if (selectedCard == null || selectedCard.zone != Zone.BATTLE
                || selectedCard.owner != game.getCurrentPlayer()) {
            return;
        }
        if (!canAttackNow()) {
            statusLabel.setText("先攻1ターン目は攻撃できません。");
            return;
        }
        if (hasKeyword(selectedCard, "拘束")) {
            statusLabel.setText("【拘束】: このエンティティは攻撃できません。");
            return;
        }
        attacker = selectedCard;
        if (attackButtonHost != null) {
            attackButtonHost.getChildren().remove(attackButton);
            attackButtonHost = null;
        }
        clearActionHighlights(); // 青ハイライトを消す

        // 相手エリアにエンティティがいない場合は、確認してそのままエレメントへダイレクトアタック。
        Game.Player defP = game.getCurrentPlayer().getOpponent();
        if (!hasAnyEntity(defP)) {
            if (confirmDirectAttack()) {
                attackTargetActive = true;
                attackTargetElement = true;
                attackTargetCard = null;
                attackTargetNode = null;
                statusLabel.setText("ダイレクトアタック！");
                confirmAttack();
            } else {
                attacker = null;
                refreshActionHighlights();
                statusLabel.setText("攻撃をキャンセルしました。");
            }
            return;
        }

        attackTargetActive = true;
        attackTargetCard = null;
        attackTargetElement = false;
        attackTargetNode = null;
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        confirmPrompt.setText("攻撃対象を選択してください（相手エンティティ／相手エレメント）");
        confirmOkButton.setDisable(true);
        statusLabel.setText("相手を選択して下さい");
    }

    /** 「相手エレメントへダイレクトアタックしますか？」をはい／いいえで確認する。 */
    private boolean confirmDirectAttack() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("確認");
        alert.setHeaderText(null);
        alert.setContentText("相手のエリアにエンティティがいません。\n相手エレメントへダイレクトアタックしますか？");
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }
        ButtonType yes = new ButtonType("はい", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("いいえ", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yes, no);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    /** 攻撃対象を設定する（相手エンティティ or 相手エレメント）。 */
    private void setAttackTarget(Card card, Node node, boolean element) {
        if (attackTargetNode != null) {
            attackTargetNode.setEffect(null);
        }
        attackTargetCard = card;
        attackTargetElement = element;
        attackTargetNode = node;
        if (node != null) {
            node.setEffect(CardView.selectionGlow());
        }
        confirmOkButton.setDisable(false);
        confirmPrompt.setText("攻撃対象: " + (element ? "相手エレメント" : card.name) + " — 決定で攻撃");
    }

    /** 決定: ガードチェック→スキルチェック→バトル解決。 */
    private void confirmAttack() {
        if (!attackTargetActive || (!attackTargetElement && attackTargetCard == null)) {
            statusLabel.setText("攻撃対象を選択してください。");
            return;
        }
        Game.Player atkP = game.getCurrentPlayer();
        Game.Player defP = atkP.getOpponent();
        Card atkCard = attacker;
        Card tgt = attackTargetCard;
        boolean tgtElem = attackTargetElement;

        if (attackTargetNode != null) {
            attackTargetNode.setEffect(null);
        }

        // ギャラクシー・ベヒモス: 火山・海・森に攻撃宣言した時、その対象をターン終了まで-2する。
        if (!tgtElem && tgt != null && "ギャラクシー・ベヒモス".equals(atkCard.name)
                && !isEffectImmune(tgt)
                && (tgt.attribute == Attribute.VOLCANO || tgt.attribute == Attribute.SEA
                    || tgt.attribute == Attribute.FOREST)) {
            tgt.tempAtkMod -= 2;
            refreshSlots();
        }

        // 攻撃宣言（この攻撃者は攻撃済み）。
        state.attackedEntities.add(atkCard);
        endAttack(); // 攻撃対象選択UIを閉じる

        // 攻撃エンティティ=赤、攻撃対象=青の発光を出す（ガード/スキルチェック中も維持される）。
        setBattleGlows(atkCard, tgt, tgtElem, defP);
        render();
        startGuardCheck(atkCard, tgt, tgtElem, atkP, defP);
    }

    /**
     * ガードチェック: 防御側に【ガード】持ちがいれば発動するか選ばせる。
     * クリムゾン・ドレイクの攻撃／アーマー・バインド適用中は発動できない。
     * 人間防御側には非ブロッキングの選択バーを出し、ガード可能エンティティを黄色く光らせる。
     */
    private void startGuardCheck(Card atkCard, Card tgt, boolean tgtElem, Game.Player atkP, Game.Player defP) {
        boolean guardAllowed = !"クリムゾン・ドレイク".equals(atkCard.name)
                && !state.guardDisabledThisTurn.contains(defP);
        if (guardAllowed && hasGuardEntity(defP)) {
            List<Card> guards = new ArrayList<>();
            for (Card c : slotsFor(defP)) {
                if (grantsGuard(c)) {
                    guards.add(c);
                }
            }
            if (isNpc(defP)) {
                afterGuard(npcChooseGuard(defP, atkCard), atkCard, tgt, tgtElem, atkP, defP);
                return;
            }
            // 人間防御側: ガード可能エンティティを黄色く光らせ、選択バーで選ばせる。
            highlightGuardEntities(guards);
            java.util.LinkedHashMap<String, Runnable> opts = new java.util.LinkedHashMap<>();
            for (Card g : guards) {
                Card guard = g;
                opts.put("ガード: " + g.name + "（ATK" + effectiveAtk(g) + "）",
                        () -> { clearGuardHighlights(); afterGuard(guard, atkCard, tgt, tgtElem, atkP, defP); });
            }
            opts.put("ガードしない",
                    () -> { clearGuardHighlights(); afterGuard(null, atkCard, tgt, tgtElem, atkP, defP); });
            showChoiceBar("ガードチェック: " + atkCard.name + " の攻撃。ガードしますか？（黄色のエンティティで受けられます）", opts);
            return;
        }
        afterGuard(null, atkCard, tgt, tgtElem, atkP, defP);
    }

    /** ガード選択の後: 対象を更新し、スキルチェックへ進む（無ければバトル解決）。 */
    private void afterGuard(Card guardEntity, Card atkCard, Card tgt, boolean tgtElem,
                            Game.Player atkP, Game.Player defP) {
        if (guardEntity != null) {
            tgt = guardEntity; // 攻撃対象をガード発動エンティティに変更
            tgtElem = false;
            statusLabel.setText("ガード: 攻撃対象を " + guardEntity.name + " に変更しました。");
            setBattleGlows(atkCard, tgt, false, defP); // 青の対象発光をガードエンティティへ移す
            render();
        }
        // スキルチェック: タイダル・サーペントの攻撃時は不可。NPCが防御側のときは簡易AIはパス。
        if (!isNpc(defP) && !"タイダル・サーペント".equals(atkCard.name) && hasReactiveOption(defP, atkCard)) {
            beginSkillCheck(atkCard, tgt, tgtElem, atkP, defP);
            return; // バトルは割り込み解決後に proceedToBattle で行う
        }
        proceedToBattle(atkCard, tgt, tgtElem, atkP, defP);
    }

    /** スキルチェック後のバトル解決（割り込みが無い／終わった後に呼ぶ）。 */
    private void proceedToBattle(Card atkCard, Card tgt, boolean tgtElem, Game.Player atkP, Game.Player defP) {
        // 巻き戻し: エンティティ攻撃で対象が場を離れていたら不発。
        if (!tgtElem && (tgt == null || !onField(tgt))) {
            statusLabel.setText("攻撃対象がいなくなったため攻撃は不発になりました。");
            finishAttackCleanup();
            return;
        }
        pyroDrawOffered = false;
        resolveBattle(atkCard, tgt, tgtElem, atkP, defP);
        finishAttackCleanup();
        // パイロ・インプの直接ダメージ後: コスト1枚を払って1ドローする選択を提示する（任意）。
        if (pyroDrawOffered) {
            pyroDrawOffered = false;
            // NPC攻撃者のパイロ・インプはコスト支払い選択を出さない（簡易AIは見送り）。
            if (!isNpc(atkP)) {
                beginPyroPayment();
            }
        }
    }

    private void finishAttackCleanup() {
        selectedCard = null;
        selectedCardNode = null;
        clearBattleGlows();     // 攻撃=赤／対象=青の発光を消す
        clearGuardHighlights(); // 念のためガードの黄発光も消す
        refreshSlots();
        render();
        checkGameOver(); // バトルダメージでコアが0になっていれば勝敗を表示
    }

    /** カード c がいずれかのエリアスロットに存在するか。 */
    private boolean onField(Card c) {
        for (Game.Player p : Game.Player.values()) {
            for (Card s : slotsFor(p)) {
                if (s == c) {
                    return true;
                }
            }
        }
        return false;
    }

    private void cancelAttack() {
        if (attackTargetNode != null) {
            attackTargetNode.setEffect(null);
        }
        endAttack();
        clearBattleGlows();
        refreshActionHighlights();
        statusLabel.setText("攻撃をキャンセルしました。");
    }

    private void endAttack() {
        attacker = null;
        attackTargetActive = false;
        attackTargetCard = null;
        attackTargetElement = false;
        attackTargetNode = null;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
    }

    // ===== スキルチェック割り込み（防御側のリアクティブスキル） =====

    private Card findInHand(Game.Player p, String name) {
        for (Card c : handFor(p)) {
            if (name.equals(c.name)) {
                return c;
            }
        }
        return null;
    }

    private Card findInRemnant(Game.Player p, String name) {
        for (Card c : remnantFor(p)) {
            if (name.equals(c.name)) {
                return c;
            }
        }
        return null;
    }

    /** 防御側に、この攻撃宣言へ割り込めるリアクティブな選択肢があるか。 */
    private boolean hasReactiveOption(Game.Player defP, Card atkCard) {
        if (defenderSkillCheckUsed) {
            return false; // 相手バトルフェイズ中に発動できるスキルは合計1枚まで
        }
        if (findInHand(defP, "コズミック・ノヴァ") != null) {
            return true; // 手札のコズミック・ノヴァ（allで常に支払い可）
        }
        if (findInRemnant(defP, "コズミック・ノヴァ") != null) {
            return true; // レムナントのコズミック・ノヴァ（封印＋all）
        }
        // レムナントのブレイク・ブレイク（封印＋コスト3を手札から）
        if (findInRemnant(defP, "ブレイク・ブレイク") != null && handFor(defP).size() >= 3) {
            return true;
        }
        // 手札の一般サポート/妨害スキル（発動可能なもの）。
        for (Card c : handFor(defP)) {
            if (reactiveSkillUsable(c, defP)) {
                return true;
            }
        }
        return false;
    }

    /** カードがサポート/妨害カテゴリか（説明文の先頭で判定。装備は除外）。 */
    private boolean isSupportOrInterference(Card c) {
        return c.description != null
                && (c.description.startsWith("【サポート】") || c.description.startsWith("【妨害】"));
    }

    /** スキルチェックで一般スキルとして発動できる skillEffect か（aスキル・装備・複雑系は除外）。 */
    private boolean isGeneralReactiveEffect(SkillEffect e) {
        switch (e) {
            case MODIFY_ATK:
            case GRANT_IMMUNITY:
            case GRANT_TRAMPLE:
            case DESTROY_EQUIPMENT:
            case DESTROY_ENEMY_ENTITY:
            case DISABLE_GUARD:
                return true;
            default:
                return false; // EQUIP/REVIVE_EQUIP(装備)、RECYCLE、aスキル系は対象外
        }
    }

    /** 防御側 defP が手札のカード c をスキルチェックで発動できるか（カテゴリ・コスト・対象存在）。 */
    private boolean reactiveSkillUsable(Card c, Game.Player defP) {
        if (c.type != CardType.SKILL || c.attribute == Attribute.A_SKILL
                || !isSupportOrInterference(c) || !isGeneralReactiveEffect(c.skillEffect)) {
            return false;
        }
        // 宇宙軽減は自分のターン中のみ＝防御側ターンではないので base コストで判定。
        Integer base = numericCost(c.cost);
        int cost = base != null ? base : 0;
        if (cost > handFor(defP).size() - 1) {
            return false; // コストを払えない
        }
        // 効果ごとの対象存在チェック。
        switch (c.skillEffect) {
            case DESTROY_ENEMY_ENTITY:
                return hasAnyEntity(defP.getOpponent());
            case GRANT_IMMUNITY:
                return anyAttributeOnField(Attribute.VOLCANO);
            case DESTROY_EQUIPMENT:
                return hasAnyEquippedEntity(Game.Player.PLAYER1) || hasAnyEquippedEntity(Game.Player.PLAYER2);
            case MODIFY_ATK:
            case GRANT_TRAMPLE:
                return hasAnyEntity(Game.Player.PLAYER1) || hasAnyEntity(Game.Player.PLAYER2);
            case DISABLE_GUARD:
                return true; // 対象なし
            default:
                return false;
        }
    }

    /** スキルチェック: 防御側に割り込むか選ばせ、選んだリアクティブ効果を解決する。 */
    private void beginSkillCheck(Card atkCard, Card tgt, boolean tgtElem, Game.Player atkP, Game.Player defP) {
        scAttacker = atkCard;
        scTarget = tgt;
        scTargetElem = tgtElem;
        scAtkP = atkP;
        scDefP = defP;

        Card novaHand = findInHand(defP, "コズミック・ノヴァ");
        Card novaRem = findInRemnant(defP, "コズミック・ノヴァ");
        Card breakRem = findInRemnant(defP, "ブレイク・ブレイク");

        // 非ブロッキングの選択バーで提示する（バー表示中もカード詳細をタップで確認できる）。
        java.util.LinkedHashMap<String, Runnable> opts = new java.util.LinkedHashMap<>();
        if (novaHand != null) {
            opts.put("コズミック・ノヴァ（手札）", () -> reactNovaFromHand(novaHand));
        }
        if (novaRem != null) {
            opts.put("コズミック・ノヴァ（封印して発動）", () -> reactNovaFromRemnant(novaRem));
        }
        if (breakRem != null && handFor(defP).size() >= 3) {
            opts.put("ブレイク・ブレイク（封印して発動）", () -> reactBreakFromRemnant(breakRem));
        }
        // 手札の一般サポート/妨害スキル。
        for (Card c : new ArrayList<>(handFor(defP))) {
            if (reactiveSkillUsable(c, defP)) {
                Card cc = c;
                opts.put(cc.name + "（手札）", () -> reactSkill(cc));
            }
        }
        opts.put("割り込まない（パス）", this::resumeBattleAfterSkillCheck);

        showChoiceBar("スキルチェック: " + atkCard.name + " の攻撃。スキルで割り込みますか？", opts);
    }

    /** 手札のコズミック・ノヴァで割り込む（Cx or all、カードはレムナントへ）。 */
    private void reactNovaFromHand(Card nova) {
        int cx = aSkillBaseCx(nova.cost); // 防御側ターンではないため宇宙軽減なし
        int sealCount = 0;
        for (Card c : handFor(scDefP)) {
            if (c != nova && !isASkill(c)) {
                sealCount++;
            }
        }
        boolean cxAffordable = cx <= handFor(scDefP).size() - 1;
        int choice = chooseASkillCost(nova, cx, cxAffordable, sealCount);
        if (choice == 1) {
            // all: 他の手札を封印。ノヴァはレムナントへ。
            payAllToSeal(scDefP, nova);
            disposeReactiveCard(nova, false, scDefP);
            applyReactiveAndResume(false);
        } else if (choice == 0) {
            // Cx: 防御側の手札を公開してコスト選択。
            beginSkillCheckCost(nova, cx, false, false);
        } else {
            resumeBattleAfterSkillCheck(); // キャンセル＝パス
        }
    }

    /** レムナントのコズミック・ノヴァで割り込む（封印＋allコスト）。 */
    private void reactNovaFromRemnant(Card nova) {
        disposeReactiveCard(nova, true, scDefP); // ノヴァを封印
        payAllToSeal(scDefP, nova);              // 他の手札も封印
        applyReactiveAndResume(false);
    }

    /** レムナントのブレイク・ブレイクで割り込む（封印＋コスト3、攻撃者を破壊）。 */
    private void reactBreakFromRemnant(Card brk) {
        beginSkillCheckCost(brk, 3, true, true); // destroy=true, このカードは封印
    }

    /**
     * 手札の一般サポート/妨害スキルでスキルチェックに割り込む。
     * 発動者を防御側にして通常のスキル解決フロー（対象選択→コスト）を流用し、
     * resolveSkill 完了後に proceedToBattle へ戻る（resolveSkill のリアクティブ分岐）。
     */
    private void reactSkill(Card skill) {
        skillCastPlayer = scDefP;
        skillCheckSkillActive = true;
        handViewOwner = scDefP; // 防御側の手札を一時公開（コスト支払い・詳細用）
        refreshHandPanes();
        statusLabel.setText("スキルチェック: " + scDefP.getDisplayName() + " が「" + skill.name + "」を発動。");
        dispatchSkillEffect(skill);
    }

    /** 防御側のコスト支払い選択を開始する（手札を一時公開）。 */
    private void beginSkillCheckCost(Card card, int count, boolean destroy, boolean seal) {
        pendingSkillCheckCard = card;
        pendingSkillCheckDestroy = destroy;
        pendingSkillCheckSeal = seal;
        pendingSkillCheckPay = true;
        selectionPlayer = scDefP;
        handViewOwner = scDefP;     // 防御側の手札を表面で一時公開
        refreshHandPanes();
        selectionActive = true;
        selectionRequired = count;
        selectionChosen.clear();
        selectionNodeMap.clear();
        clearActionHighlights();
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        updateSelectionPrompt();
        statusLabel.setText("スキルチェック: " + card.name + " のコストとして"
                + scDefP.getDisplayName() + "の手札を " + count + " 枚選択してください。");
    }

    /** スキルチェックのコスト支払い確定後の解決（confirmSelection から呼ぶ）。 */
    private void resolveSkillCheckPayment() {
        Game.Player payer = selectionPlayer;
        for (Card c : selectionChosen) {
            handFor(payer).remove(c);
            c.zone = Zone.REMNANT;
            remnantFor(payer).add(c);
        }
        for (Node n : selectionNodeMap.values()) {
            n.setEffect(null);
        }
        Card card = pendingSkillCheckCard;
        boolean destroy = pendingSkillCheckDestroy;
        boolean seal = pendingSkillCheckSeal;
        endSelection();
        disposeReactiveCard(card, seal, payer);
        applyReactiveAndResume(destroy);
    }

    /** 発動したリアクティブカードを所定のゾーン（封印 or レムナント）へ送る。 */
    private void disposeReactiveCard(Card card, boolean seal, Game.Player owner) {
        handFor(owner).remove(card);
        remnantFor(owner).remove(card);
        if (seal) {
            card.zone = Zone.SEAL;
            card.revealed = false;
            sealFor(owner).add(card);
        } else {
            card.zone = Zone.REMNANT;
            remnantFor(owner).add(card);
        }
    }

    /** リアクティブ効果（破壊 or ATK0）を攻撃者へ適用し、手札表示を戻してバトルへ戻る。 */
    private void applyReactiveAndResume(boolean destroy) {
        defenderSkillCheckUsed = true; // この相手バトルフェイズでの防御側スキル発動は1枚まで
        if (destroy) {
            destroyEntity(scAttacker.owner, scAttacker);
            statusLabel.setText("スキルチェック: " + scAttacker.name + " を破壊（攻撃は不発）。");
        } else {
            state.atkZeroedThisTurn.add(scAttacker);
            statusLabel.setText("スキルチェック: " + scAttacker.name + " のATKをターン終了まで0に。");
        }
        handViewOwner = Game.Player.PLAYER1; // 手札表示を手番プレイヤーへ戻す
        refreshHandPanes();
        refreshSlots();
        render();
        resumeBattleAfterSkillCheck();
    }

    /** スキルチェック解決後、攻撃者が残っていればバトルへ、破壊されていれば不発。 */
    private void resumeBattleAfterSkillCheck() {
        handViewOwner = Game.Player.PLAYER1;
        Card a = scAttacker;
        Card t = scTarget;
        boolean te = scTargetElem;
        Game.Player ap = scAtkP;
        Game.Player dp = scDefP;
        clearSkillCheckContext();
        if (a == null || !onField(a)) {
            statusLabel.setText("攻撃者が破壊されたため、攻撃は不発になりました。");
            finishAttackCleanup();
            return;
        }
        proceedToBattle(a, t, te, ap, dp);
    }

    private void clearSkillCheckContext() {
        scAttacker = null;
        scTarget = null;
        scTargetElem = false;
        scAtkP = null;
        scDefP = null;
        pendingSkillCheckCard = null;
        pendingSkillCheckDestroy = false;
        pendingSkillCheckSeal = false;
        skillCheckSkillActive = false;
        skillCastPlayer = null;
    }

    /** バトル解決。属性有利なら防御側ATKを-3して比較し、低い方を破壊（同値は相打ち）。 */
    private void resolveBattle(Card attacker, Card target, boolean targetElement,
                               Game.Player atkP, Game.Player defP) {
        int atk = effectiveAtk(attacker);
        if (targetElement) {
            game.damageCore(defP, atk);
            statusLabel.setText(attacker.name + " が相手エレメントに " + atk + " ダメージ！");
            // パイロ・インプ: 相手エレメントにダメージを与えた時、コスト1枚払って1ドローできる。
            if ("パイロ・インプ".equals(attacker.name) && handFor(atkP).size() >= 1) {
                pyroDrawOffered = true;
            }
            return;
        }
        // ストーム・イーグル: 宇宙属性へ攻撃した時、バトルを行わず破壊する。
        if ("ストーム・イーグル".equals(attacker.name) && target.attribute == Attribute.COSMOS) {
            destroyEntity(target.owner, target);
            statusLabel.setText(attacker.name + " が " + target.name + "（宇宙）をバトルせず破壊！");
            return;
        }
        int defAtk = effectiveAtk(target);
        boolean advantage = isAdvantage(attacker.attribute, target.attribute);
        int effDef = advantage ? defAtk - 3 : defAtk;
        StringBuilder msg = new StringBuilder();
        msg.append("バトル: ").append(attacker.name).append("(ATK").append(atk).append(") vs ")
                .append(target.name).append("(ATK").append(defAtk);
        if (advantage) {
            msg.append("→").append(effDef).append(" 属性有利");
        }
        msg.append(") ");
        if (atk > effDef) {
            destroyInBattle(defP, target, attacker, msg);
        } else if (atk < effDef) {
            destroyInBattle(atkP, attacker, target, msg);
        } else {
            destroyInBattle(defP, target, attacker, msg);
            destroyInBattle(atkP, attacker, target, msg);
            msg.append("（相打ち）");
        }
        statusLabel.setText(msg.toString());
    }

    /**
     * バトルでの破壊。ミスト・ミラージュは「元々のATKが高い相手とのバトル時、1ターン1度破壊されない」。
     * 保護が働いた場合は破壊せず、メッセージに記録する。
     */
    private void destroyInBattle(Game.Player owner, Card victim, Card opponent, StringBuilder msg) {
        if (mistProtect(victim, opponent)) {
            msg.append("→ ").append(victim.name).append(" は破壊されない（ミスト・ミラージュ） ");
            return;
        }
        // ホワイトフェザー: 破壊した側がトランプル効果を持つなら、破壊した相手のATK分を相手エレメントへ。
        if (opponent != null && state.tramplersThisTurn.contains(opponent)) {
            int dmg = effectiveAtk(victim); // 破壊（リセット）前のATK
            game.damageCore(owner, dmg);
            msg.append("→ ").append(opponent.name).append("の効果: 相手エレメントに ").append(dmg).append(" ダメージ ");
        }
        destroyEntity(owner, victim);
        msg.append("→ ").append(victim.name).append(" を破壊 ");
    }

    /** ミスト・ミラージュが今回のバトルで破壊を回避するか（条件成立で1ターン1度フラグを消費）。 */
    private boolean mistProtect(Card victim, Card opponent) {
        if (!"ミスト・ミラージュ".equals(victim.name) || opponent == null) {
            return false;
        }
        if (parseAtk(opponent.atk) <= parseAtk(victim.atk)) {
            return false; // 相手の「元々のATK」が高い時のみ
        }
        if (state.mistGuardUsed.contains(victim)) {
            return false; // このターンは使用済み
        }
        state.mistGuardUsed.add(victim);
        return true;
    }

    /** エンティティを破壊してレムナントへ送る。 */
    private void destroyEntity(Game.Player owner, Card card) {
        Card[] slots = slotsFor(owner);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == card) {
                slots[i] = null;
                break;
            }
        }
        // 装備カードは持ち主のレムナントへ送る。
        for (Card eq : card.equipments) {
            eq.zone = Zone.REMNANT;
            remnantFor(eq.owner).add(eq);
        }
        card.equipments.clear();
        card.addedEffects.clear(); // 追加効果（再生時のガード等）は破壊で消える
        card.tempAtkMod = 0;
        card.permAtkMod = 0;
        card.zone = Zone.REMNANT;
        remnantFor(owner).add(card);
        onDestroy(owner, card); // 破壊された時の効果
    }

    /** 破壊された時の効果を発動する（対象が複数いる場合は先頭を自動選択）。 */
    private void onDestroy(Game.Player owner, Card card) {
        if ("氷河の落とし子".equals(card.name)) {
            // 破壊された時、相手エンティティ1体のATKを-1する（簡易: ターン終了まで）。
            Card t = firstAnyEntity(owner.getOpponent());
            if (t != null && !isEffectImmune(t)) {
                t.tempAtkMod -= 1;
            }
        }
    }

    private Card firstAnyEntity(Game.Player p) {
        for (Card c : slotsFor(p)) {
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    /** 攻撃側 a が防御側 d に対して属性有利か。 */
    private boolean isAdvantage(Attribute a, Attribute d) {
        if (a == null || d == null) {
            return false;
        }
        switch (a) {
            case VOLCANO: return d == Attribute.FOREST || d == Attribute.COSMOS;
            case SEA:     return d == Attribute.VOLCANO || d == Attribute.COSMOS;
            case FOREST:  return d == Attribute.SEA || d == Attribute.COSMOS;
            case COSMOS:  return d == Attribute.VOLCANO || d == Attribute.SEA || d == Attribute.FOREST;
            default:      return false; // 空・スキル系は有利なし
        }
    }

    private int parseAtk(String atk) {
        if (atk == null) {
            return 0;
        }
        try {
            return Integer.parseInt(atk.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * エンティティにATK修正を加える共通処理。
     * 森々の巨人は「ATKが＋される時、その数値を2増やす」ため、正の修正を+2増幅する。
     * perm=true は永続修正（破壊で消える）、false はターン終了までの修正。
     */
    private void buffAtk(Card c, int delta, boolean perm) {
        if (c == null) {
            return;
        }
        if (delta > 0 && "森々の巨人".equals(c.name)) {
            delta += 2;
        }
        if (perm) {
            c.permAtkMod += delta;
        } else {
            c.tempAtkMod += delta;
        }
    }

    /** 装備の補正を含めた実効ATK。 */
    private int effectiveAtk(Card entity) {
        // コズミック・ノヴァ: このターンATKを0にされたエンティティは常に0。
        if (state.atkZeroedThisTurn.contains(entity)) {
            return 0;
        }
        int atk = parseAtk(entity.atk);
        boolean giant = "森々の巨人".equals(entity.name);
        for (Card eq : entity.equipments) {
            if ("スピリット・バーク".equals(eq.name)) {
                int bonus = 2;
                if (entity.attribute == Attribute.FOREST) {
                    bonus += 1;
                }
                if (giant) {
                    bonus += 2; // 森々の巨人: ＋される時その数値を2増やす
                }
                atk += bonus;
            }
        }
        atk += entity.tempAtkMod; // スキル等によるターン終了までの増減
        atk += entity.permAtkMod; // 永続の増減（ヴォイド・ゲート等）
        // 癒しの珊瑚礁: 同じエリアに存在する限り、他の【海属性】は+2される。
        if (entity.attribute == Attribute.SEA) {
            for (Card c : slotsFor(entity.owner)) {
                if (c != null && c != entity && "癒しの珊瑚礁".equals(c.name)) {
                    atk += 2;
                    break;
                }
            }
        }
        // エルダー・トレント: 自分のエリアの他の【森属性】1体につき+2。
        if ("エルダー・トレント".equals(entity.name)) {
            int forests = 0;
            for (Card c : slotsFor(entity.owner)) {
                if (c != null && c != entity && c.attribute == Attribute.FOREST) {
                    forests++;
                }
            }
            atk += forests * 2;
        }
        return atk;
    }

    /** MODIFY_ATK スキルのATK増減量を計算する。 */
    private int atkModDelta(Card skill, Game.Player player) {
        if ("アイシクル・トラップ".equals(skill.name)) {
            // エリアに【海属性】がいるなら-5、いなければ-3
            return anyAttributeOnField(Attribute.SEA) ? -5 : -3;
        }
        if ("アース・パルス".equals(skill.name)) {
            // 自分のレムナントに【宇宙属性】が無いなら+3、あれば+2
            return cosmosReduction(player) == 0 ? 3 : 2;
        }
        return 0;
    }

    /** ターン終了まで持続するATK修正を全エンティティでリセットする。 */
    private void clearTempAtkMods() {
        for (Game.Player p : Game.Player.values()) {
            for (Card c : slotsFor(p)) {
                // 森々の巨人: 自身に対するATKの±効果はターン終了時に失われない。
                if (c != null && !"森々の巨人".equals(c.name)) {
                    c.tempAtkMod = 0;
                }
            }
        }
    }

    /** 場（両エリア）に指定属性のエンティティが存在するか。 */
    private boolean anyAttributeOnField(Attribute attr) {
        for (Game.Player p : Game.Player.values()) {
            for (Card c : slotsFor(p)) {
                if (c != null && c.attribute == attr) {
                    return true;
                }
            }
        }
        return false;
    }

    /** エンティティが【ガード】を持つか（固有 or 装備による付与）。 */
    private boolean grantsGuard(Card entity) {
        if (entity == null) {
            return false;
        }
        if (entity.description != null && entity.description.trim().equals("【ガード】")) {
            return true;
        }
        for (Card eq : entity.equipments) {
            if ("アトモスフィア・アーマー".equals(eq.name)) {
                return true;
            }
        }
        // 追加効果（【再生】時に得る【ガード】など）
        if (entity.addedEffects.contains("【ガード】")) {
            return true;
        }
        return false;
    }

    /** 【再生】した時の効果を適用する（リビングデッド・ナイトは【ガード】を得る）。 */
    private void applyReviveTriggers(Card entity) {
        if ("リビングデッド・ナイト".equals(entity.name) && !entity.addedEffects.contains("【ガード】")) {
            entity.addedEffects.add("【ガード】");
        }
    }

    private boolean hasGuardEntity(Game.Player p) {
        for (Card c : slotsFor(p)) {
            if (grantsGuard(c)) {
                return true;
            }
        }
        return false;
    }

    /** プレイヤー p が所有する「死霊術師の呪縛」装備1枚につき3ダメージ（ターン終了時の自傷）。 */
    private int necroticBindingDamage(Game.Player p) {
        int dmg = 0;
        for (Game.Player owner : Game.Player.values()) {
            for (Card ent : slotsFor(owner)) {
                if (ent == null) {
                    continue;
                }
                for (Card eq : ent.equipments) {
                    if ("死霊術師の呪縛".equals(eq.name) && eq.owner == p) {
                        dmg += 3;
                    }
                }
            }
        }
        return dmg;
    }


    /** このエンティティは攻撃対象にできないか（【不可侵】／ヴォイド・ゲート）。 */
    private boolean isUnattackable(Card c) {
        return hasKeyword(c, "不可侵") || "ヴォイド・ゲート".equals(c.name);
    }

    /** エンティティを手札からスロットへ配置する。 */
    private void placeEntity(Game.Player player, Card entity, int slotIndex, Node source) {
        handFor(player).remove(entity);
        entity.zone = Zone.BATTLE;
        entity.revealed = true;
        slotsFor(player)[slotIndex] = entity;
        selectedCard = null;
        selectedCardNode = null;
        refreshHandPanes();
        refreshSlots();
        render();

        Node target = slotCellsFor(player)[slotIndex];
        Node from = source != null ? source : (player == Game.Player.PLAYER1 ? playerHandBox : opponentHandIndicator);
        animateAction("顕現", from, target, () -> {
            showCardDetail(entity);
            statusLabel.setText("顕現: " + entity.name);
            onManifest(player, entity); // 【顕現】した時の効果（再生では発動しない）
        });
    }

    /**
     * 【顕現】した時の効果を発動する。対象を取る効果はプレイヤーが選択する
     * （候補が1体なら自動、複数なら選択モードに入る）。※【再生】では呼ばれない。
     */
    private void onManifest(Game.Player player, Card entity) {
        // ダンシング・クラウド: ライブラリから1ドロー → ハンド1枚を選択してレムナントへ。
        if ("ダンシング・クラウド".equals(entity.name)) {
            drawOne(player);
            refreshHandPanes();
            render();
            statusLabel.setText("顕現: ダンシング・クラウド → 1ドロー");
            if (!handFor(player).isEmpty()) {
                beginDiscardSelection(1);
            }
            return;
        }
        // 舞踊る妖精: ハンドの元コスト1以下の【森属性】1体を、コストを払わず【顕現】できる。
        if ("舞踊る妖精".equals(entity.name)) {
            if (firstEmptySlot(slotsFor(player)) >= 0 && !fairyManifestCandidates(player).isEmpty()) {
                beginFairyManifest(entity);
            }
            return;
        }
        // スカイ・ファルコン: 自レムナントの「スキルカード」1枚をデッキに戻してシャッフルする。
        if ("スカイ・ファルコン".equals(entity.name)) {
            if (!skyReturnCandidates(player).isEmpty()) {
                beginSkyReturn(entity);
            }
            return;
        }
        // エルダー・トレント: 自レムナントにコスト1以下の【森属性】があるなら、自エリアへ無償で【再生】できる。
        if ("エルダー・トレント".equals(entity.name)) {
            if (firstEmptySlot(slotsFor(player)) >= 0 && !manifestReviveCandidates(player).isEmpty()) {
                beginManifestRevive(entity);
            }
            return;
        }
        // 深海のアビス・レイ: 条件付き（他の海属性が場にいる）→ 相手ハンドをランダム除去→相手エンティティ破壊。
        if ("深海のアビス・レイ".equals(entity.name)) {
            if (!hasOtherSeaInArea(entity)) {
                return; // 他の【海属性】が場にいない
            }
            Game.Player opp = player.getOpponent();
            if (handFor(opp).isEmpty()) {
                return; // 相手ハンドが無いので、その後の破壊も発動しない
            }
            discardRandom(opp);
            refreshHandPanes();
            render();
            statusLabel.setText("顕現: 深海のアビス・レイ → 相手ハンド1枚をレムナントへ");
            // その後、相手エンティティ1体を破壊（候補がいれば）
            List<Card> targets = manifestTargetCandidates(player, entity);
            if (targets.size() == 1) {
                applyManifestEffect(entity, targets.get(0));
            } else if (targets.size() > 1) {
                beginManifestTarget(entity);
            }
            return;
        }
        if (manifestEffectName(entity) != null) {
            List<Card> targets = manifestTargetCandidates(player, entity);
            if (targets.isEmpty()) {
                return; // 対象なし
            }
            if (targets.size() == 1) {
                applyManifestEffect(entity, targets.get(0));
            } else {
                beginManifestTarget(entity);
            }
        }
    }

    /** 自分以外の【海属性】が場（両エリア）に存在するか。 */
    private boolean hasOtherSeaInArea(Card self) {
        for (Game.Player p : Game.Player.values()) {
            for (Card c : slotsFor(p)) {
                if (c != null && c != self && c.attribute == Attribute.SEA) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 指定プレイヤーのハンドからランダムに1枚をレムナントへ送る。 */
    private void discardRandom(Game.Player p) {
        List<Card> hand = handFor(p);
        if (hand.isEmpty()) {
            return;
        }
        Card c = hand.remove((int) (Math.random() * hand.size()));
        c.zone = Zone.REMNANT;
        remnantFor(p).add(c);
    }

    /** 顕現時に対象選択を要する効果名（なければnull）。 */
    private String manifestEffectName(Card entity) {
        if ("マグマ・ハウンド".equals(entity.name) || "芽吹く妖精".equals(entity.name)) {
            return entity.name;
        }
        return null;
    }

    /** 顕現時効果の対象候補。 */
    private List<Card> manifestTargetCandidates(Game.Player player, Card source) {
        List<Card> list = new ArrayList<>();
        if ("マグマ・ハウンド".equals(source.name)) {
            for (Card c : slotsFor(player.getOpponent())) {
                if (c != null && grantsGuard(c) && !isEffectImmune(c)) {
                    list.add(c);
                }
            }
        } else if ("芽吹く妖精".equals(source.name)) {
            for (Card c : slotsFor(player)) {
                if (c != null && c != source && !isEffectImmune(c)) {
                    list.add(c);
                }
            }
        } else if ("深海のアビス・レイ".equals(source.name)) {
            for (Card c : slotsFor(player.getOpponent())) {
                if (c != null && !isEffectImmune(c)) {
                    list.add(c);
                }
            }
        }
        return list;
    }

    /** 顕現時効果を対象に適用する。 */
    private void applyManifestEffect(Card source, Card target) {
        if ("マグマ・ハウンド".equals(source.name)) {
            destroyEntity(target.owner, target);
            refreshSlots();
            render();
            statusLabel.setText("顕現: マグマ・ハウンド → " + target.name + "（ガード持ち）を破壊");
        } else if ("芽吹く妖精".equals(source.name)) {
            buffAtk(target, 1, true);
            refreshSlots();
            render();
            statusLabel.setText("顕現: 芽吹く妖精 → " + target.name + " のATK+1");
        } else if ("深海のアビス・レイ".equals(source.name)) {
            destroyEntity(target.owner, target);
            refreshSlots();
            render();
            statusLabel.setText("顕現: 深海のアビス・レイ → " + target.name + " を破壊");
        }
    }

    private void beginManifestTarget(Card source) {
        manifestEffectActive = true;
        manifestEffectSource = source;
        manifestEffectTarget = null;
        manifestEffectNode = null;
        clearActionHighlights();
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        String prompt;
        if ("マグマ・ハウンド".equals(source.name)) {
            prompt = "破壊する相手の【ガード】持ちを選択してください";
        } else if ("深海のアビス・レイ".equals(source.name)) {
            prompt = "破壊する相手エンティティを選択してください";
        } else {
            prompt = "ATK+1する自分の他エンティティを選択してください";
        }
        confirmPrompt.setText(prompt);
        confirmOkButton.setDisable(true);
        statusLabel.setText("顕現効果「" + source.name + "」: " + prompt);
    }

    private void setManifestTarget(Card card, Node node) {
        if (manifestEffectNode != null) {
            manifestEffectNode.setEffect(null);
        }
        manifestEffectTarget = card;
        manifestEffectNode = node;
        if (node != null) {
            node.setEffect(CardView.selectionGlow());
        }
        confirmOkButton.setDisable(false);
        confirmPrompt.setText("対象: " + card.name + " — 決定で発動");
    }

    private void confirmManifestTarget() {
        if (!manifestEffectActive || manifestEffectTarget == null) {
            statusLabel.setText("対象を選択してください。");
            return;
        }
        if (manifestEffectNode != null) {
            manifestEffectNode.setEffect(null);
        }
        Card source = manifestEffectSource;
        Card target = manifestEffectTarget;
        endManifestTarget();
        applyManifestEffect(source, target);
    }

    private void cancelManifestTarget() {
        if (manifestEffectNode != null) {
            manifestEffectNode.setEffect(null);
        }
        endManifestTarget();
        refreshActionHighlights();
        statusLabel.setText("顕現時効果を見送りました。");
    }

    private void endManifestTarget() {
        manifestEffectActive = false;
        manifestEffectSource = null;
        manifestEffectTarget = null;
        manifestEffectNode = null;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
    }

    // ===== 顕現時【再生】（エルダー・トレント）：自レムナントのコスト1以下の森属性を無償再生 =====

    /** カード c がエルダー・トレントの顕現時【再生】対象になるか（コスト1以下の【森属性】エンティティ）。 */
    private boolean isManifestReviveCandidate(Card c) {
        return c.type == CardType.ENTITY
                && c.attribute == Attribute.FOREST
                && parseCost(c.cost) <= 1
                && !"フレイム・キッズ".equals(c.name);
    }

    /** 指定プレイヤーのレムナントにある、顕現時【再生】の候補一覧。 */
    private List<Card> manifestReviveCandidates(Game.Player player) {
        List<Card> list = new ArrayList<>();
        for (Card c : remnantFor(player)) {
            if (isManifestReviveCandidate(c)) {
                list.add(c);
            }
        }
        return list;
    }

    private void beginManifestRevive(Card source) {
        manifestReviveActive = true;
        manifestReviveTarget = null;
        manifestReviveNode = null;
        clearActionHighlights();
        showRemnantList(game.getCurrentPlayer()); // 自分のレムナント一覧を開く
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        confirmPrompt.setText("再生するコスト1以下の【森属性】を自分のレムナントから選択してください（見送り可）");
        confirmOkButton.setDisable(true);
        statusLabel.setText("顕現: " + source.name + " → 再生する森属性を選択してください（キャンセルで見送り）");
    }

    private void setManifestReviveTarget(Card card, Node node) {
        if (manifestReviveNode != null) {
            manifestReviveNode.setEffect(null);
        }
        manifestReviveTarget = card;
        manifestReviveNode = node;
        if (node != null) {
            node.setEffect(CardView.selectionGlow());
        }
        confirmOkButton.setDisable(false);
        confirmPrompt.setText("再生対象: " + card.name + " — 決定で【再生】");
    }

    private void confirmManifestRevive() {
        if (!manifestReviveActive || manifestReviveTarget == null) {
            statusLabel.setText("再生する対象を選択してください。");
            return;
        }
        if (manifestReviveNode != null) {
            manifestReviveNode.setEffect(null);
        }
        Game.Player player = game.getCurrentPlayer();
        Card target = manifestReviveTarget;
        endManifestRevive();
        if (firstEmptySlot(slotsFor(player)) < 0) {
            refreshActionHighlights();
            statusLabel.setText("エリアに空きが無いため再生できませんでした。");
            return;
        }
        resolveSelfRevive(player, target); // コストを払わず場へ。【再生】時効果も適用される。
    }

    private void cancelManifestRevive() {
        if (manifestReviveNode != null) {
            manifestReviveNode.setEffect(null);
        }
        endManifestRevive();
        refreshActionHighlights();
        statusLabel.setText("顕現時【再生】を見送りました。");
    }

    private void endManifestRevive() {
        manifestReviveActive = false;
        manifestReviveTarget = null;
        manifestReviveNode = null;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
    }

    // ===== 顕現時：スキルをデッキに戻す（スカイ・ファルコン） =====

    /** 指定プレイヤーのレムナントにある、デッキに戻せるスキルカード一覧。 */
    private List<Card> skyReturnCandidates(Game.Player player) {
        List<Card> list = new ArrayList<>();
        for (Card c : remnantFor(player)) {
            if (c.type == CardType.SKILL) {
                list.add(c);
            }
        }
        return list;
    }

    private void beginSkyReturn(Card source) {
        skyReturnActive = true;
        skyReturnTarget = null;
        skyReturnNode = null;
        clearActionHighlights();
        showRemnantList(game.getCurrentPlayer()); // 自分のレムナント一覧を開く
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        confirmPrompt.setText("デッキに戻すスキルカードを自分のレムナントから選択してください（見送り可）");
        confirmOkButton.setDisable(true);
        statusLabel.setText("顕現: " + source.name + " → デッキに戻すスキルを選択してください（キャンセルで見送り）");
    }

    private void setSkyReturnTarget(Card card, Node node) {
        if (skyReturnNode != null) {
            skyReturnNode.setEffect(null);
        }
        skyReturnTarget = card;
        skyReturnNode = node;
        if (node != null) {
            node.setEffect(CardView.selectionGlow());
        }
        confirmOkButton.setDisable(false);
        confirmPrompt.setText("対象: " + card.name + " — 決定でデッキへ戻してシャッフル");
    }

    private void confirmSkyReturn() {
        if (!skyReturnActive || skyReturnTarget == null) {
            statusLabel.setText("デッキに戻すスキルを選択してください。");
            return;
        }
        if (skyReturnNode != null) {
            skyReturnNode.setEffect(null);
        }
        Game.Player player = game.getCurrentPlayer();
        Card target = skyReturnTarget;
        endSkyReturn();
        remnantFor(player).remove(target);
        target.zone = Zone.LIBRARY;
        target.equipments.clear();
        target.addedEffects.clear();
        libraryFor(player).add(target);
        Collections.shuffle(libraryFor(player));
        if (remnantListOwner != null) {
            showRemnantList(remnantListOwner); // レムナント一覧の表示を更新
        }
        render();
        statusLabel.setText("顕現: スカイ・ファルコン → " + target.name + " をデッキに戻してシャッフル");
    }

    private void cancelSkyReturn() {
        if (skyReturnNode != null) {
            skyReturnNode.setEffect(null);
        }
        endSkyReturn();
        refreshActionHighlights();
        statusLabel.setText("顕現時効果（デッキに戻す）を見送りました。");
    }

    private void endSkyReturn() {
        skyReturnActive = false;
        skyReturnTarget = null;
        skyReturnNode = null;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
    }

    // ===== リサイクル・アース（レムナント回収＋任意の再生） =====

    /** ①ハンドに加えられる候補か（自レムナント・エンティティ・宇宙以外・元コスト3以下）。 */
    private boolean isRecycleHandCandidate(Card c) {
        return c.type == CardType.ENTITY
                && c.owner == game.getCurrentPlayer()
                && c.attribute != Attribute.COSMOS
                && parseCost(c.cost) <= 3;
    }

    /** ②再生できる候補か（①に加え、再生不可カードを除く）。 */
    private boolean isRecycleReviveCandidate(Card c) {
        return isRecycleHandCandidate(c) && !"フレイム・キッズ".equals(c.name);
    }

    private List<Card> recycleHandCandidates(Game.Player player) {
        List<Card> list = new ArrayList<>();
        for (Card c : remnantFor(player)) {
            if (c.type == CardType.ENTITY && c.attribute != Attribute.COSMOS && parseCost(c.cost) <= 3) {
                list.add(c);
            }
        }
        return list;
    }

    private void beginRecycleToHand(Card skill) {
        recycleToHandActive = true;
        recycleReviveActive = false;
        recycleSkill = skill;
        recycleHandTarget = null;
        recycleReviveTarget = null;
        recycleNode = null;
        recycleDoRevive = false;
        clearActionHighlights();
        showRemnantList(game.getCurrentPlayer());
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        confirmPrompt.setText("ハンドに加えるエンティティ（宇宙以外・コスト3以下）を自分のレムナントから選択してください");
        confirmOkButton.setDisable(true);
        statusLabel.setText("スキル「" + skill.name + "」: ハンドに加える対象を選択してください");
    }

    /** ①②共通: レムナントの対象を選択ハイライトする。 */
    private void setRecycleTarget(Card card, Node node) {
        if (recycleNode != null) {
            recycleNode.setEffect(null);
        }
        recycleNode = node;
        if (node != null) {
            node.setEffect(CardView.selectionGlow());
        }
        if (recycleToHandActive) {
            recycleHandTarget = card;
        } else {
            recycleReviveTarget = card;
        }
        confirmOkButton.setDisable(false);
        confirmPrompt.setText("対象: " + card.name + " — 決定");
    }

    /** ①決定: 追加再生の可否を尋ね、コスト支払いへ進む。 */
    private void confirmRecycleToHand() {
        if (!recycleToHandActive || recycleHandTarget == null) {
            statusLabel.setText("ハンドに加える対象を選択してください。");
            return;
        }
        if (recycleNode != null) {
            recycleNode.setEffect(null);
            recycleNode = null;
        }
        Game.Player player = game.getCurrentPlayer();
        recycleToHandActive = false;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);

        int baseCost = effectiveCost(recycleSkill);
        // 追加再生②が可能か（別の再生候補・空きスロット・合計コスト支払い可能）。
        boolean anotherReviveExists = false;
        for (Card c : remnantFor(player)) {
            if (c != recycleHandTarget && isRecycleReviveCandidate(c)) {
                anotherReviveExists = true;
                break;
            }
        }
        boolean slotFree = firstEmptySlot(slotsFor(player)) >= 0;
        // スキル自身はコストに使えないため -1。
        boolean canAffordExtra = (baseCost + 2) <= handFor(player).size() - 1;
        recycleDoRevive = false;
        if (anotherReviveExists && slotFree && canAffordExtra) {
            recycleDoRevive = confirmYesNo("更にコストを2払い、もう1体を【再生】しますか？（はい＝合計コスト"
                    + (baseCost + 2) + " / いいえ＝コスト" + baseCost + "）");
        }
        int total = baseCost + (recycleDoRevive ? 2 : 0);
        if (total == 0) {
            resolveRecycleAfterPayment(player); // コスト0（宇宙軽減等）の特殊ケース
        } else {
            beginRecycleCostPayment(total);
        }
    }

    /** リサイクルのコスト支払い選択を開始する。 */
    private void beginRecycleCostPayment(int total) {
        selectionActive = true;
        clearActionHighlights();
        selectionRequired = total;
        selectionChosen.clear();
        selectionNodeMap.clear();
        pendingRecycleCost = true;
        updateManifestButton();
        updateAttackButton();
        updateSkillButton();
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        updateSelectionPrompt();
        statusLabel.setText("リサイクル・アースのコスト支払い: 手札を " + total + " 枚選択してください。");
    }

    /** コスト支払い後の解決（①ハンド回収を確定。②があれば再生対象選択へ）。 */
    private void resolveRecycleAfterPayment(Game.Player player) {
        // ①ハンドに加える。
        if (recycleHandTarget != null) {
            remnantFor(player).remove(recycleHandTarget);
            recycleHandTarget.zone = Zone.HAND;
            recycleHandTarget.revealed = true;
            handFor(player).add(recycleHandTarget);
        }
        // スキルを使用済みにしてレムナントへ。
        handFor(player).remove(recycleSkill);
        recycleSkill.zone = Zone.REMNANT;
        remnantFor(player).add(recycleSkill);
        skillUsedThisPhase = true;
        String handName = recycleHandTarget != null ? recycleHandTarget.name : "";
        recycleHandTarget = null;
        refreshHandPanes();
        refreshSlots();
        render();
        if (recycleDoRevive) {
            beginRecycleRevive(handName);
        } else {
            recycleSkill = null;
            statusLabel.setText("リサイクル・アース: " + handName + " をハンドに加えた。");
        }
    }

    /** ②再生対象の選択を開始する。 */
    private void beginRecycleRevive(String recoveredName) {
        recycleReviveActive = true;
        recycleReviveTarget = null;
        recycleNode = null;
        clearActionHighlights();
        showRemnantList(game.getCurrentPlayer());
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        confirmPrompt.setText("【再生】するエンティティ（宇宙以外・コスト3以下）を自分のレムナントから選択してください");
        confirmOkButton.setDisable(true);
        statusLabel.setText("リサイクル・アース: " + recoveredName + " を回収。次に再生する対象を選択してください。");
    }

    /** ②決定: レムナントから無償で再生する。 */
    private void confirmRecycleRevive() {
        if (!recycleReviveActive || recycleReviveTarget == null) {
            statusLabel.setText("再生する対象を選択してください。");
            return;
        }
        if (recycleNode != null) {
            recycleNode.setEffect(null);
            recycleNode = null;
        }
        Game.Player player = game.getCurrentPlayer();
        Card target = recycleReviveTarget;
        recycleReviveActive = false;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
        clearRecycleState();
        if (firstEmptySlot(slotsFor(player)) < 0) {
            refreshActionHighlights();
            statusLabel.setText("エリアに空きが無いため再生できませんでした。");
            return;
        }
        resolveSelfRevive(player, target); // コストは支払い済みなので無償で再生
    }

    /** リサイクルを中断/完了したときの状態クリア。 */
    private void clearRecycleState() {
        recycleToHandActive = false;
        recycleReviveActive = false;
        recycleDoRevive = false;
        recycleSkill = null;
        recycleHandTarget = null;
        recycleReviveTarget = null;
        if (recycleNode != null) {
            recycleNode.setEffect(null);
            recycleNode = null;
        }
    }

    /** リサイクルの各段階のキャンセル。 */
    private void cancelRecycle() {
        if (recycleNode != null) {
            recycleNode.setEffect(null);
        }
        clearRecycleState();
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
        refreshActionHighlights();
        statusLabel.setText("リサイクル・アースをキャンセルしました。");
    }

    /** はい／いいえの確認ダイアログ。 */
    private boolean confirmYesNo(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("確認");
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType yes = new ButtonType("はい", ButtonBar.ButtonData.YES);
        ButtonType no = new ButtonType("いいえ", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yes, no);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yes;
    }

    // ===== 顕現時：ハンドの低コスト森属性を無償【顕現】（舞踊る妖精） =====

    /** カード c が舞踊る妖精の無償顕現対象になるか（ハンドの元コスト1以下の【森属性】エンティティ）。 */
    private boolean isFairyManifestCandidate(Card c) {
        return c != null
                && c.owner == game.getCurrentPlayer()
                && c.zone == Zone.HAND
                && c.type == CardType.ENTITY
                && c.attribute == Attribute.FOREST
                && parseCost(c.cost) <= 1;
    }

    /** 指定プレイヤーのハンドにある、無償顕現の候補一覧。 */
    private List<Card> fairyManifestCandidates(Game.Player player) {
        List<Card> list = new ArrayList<>();
        for (Card c : handFor(player)) {
            if (isFairyManifestCandidate(c)) {
                list.add(c);
            }
        }
        return list;
    }

    private void beginFairyManifest(Card source) {
        fairyManifestActive = true;
        fairyManifestTarget = null;
        fairyManifestNode = null;
        clearActionHighlights();
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        confirmPrompt.setText("コストを払わず顕現するコスト1以下の【森属性】を手札から選択してください（見送り可）");
        confirmOkButton.setDisable(true);
        statusLabel.setText("顕現: " + source.name + " → 無償顕現する森属性を手札から選択（キャンセルで見送り）");
    }

    private void setFairyManifestTarget(Card card, Node node) {
        if (fairyManifestNode != null) {
            fairyManifestNode.setEffect(null);
        }
        fairyManifestTarget = card;
        fairyManifestNode = node;
        if (node != null) {
            node.setEffect(CardView.selectionGlow());
        }
        confirmOkButton.setDisable(false);
        confirmPrompt.setText("無償顕現: " + card.name + " — 決定で【顕現】");
    }

    private void confirmFairyManifest() {
        if (!fairyManifestActive || fairyManifestTarget == null) {
            statusLabel.setText("顕現するエンティティを選択してください。");
            return;
        }
        if (fairyManifestNode != null) {
            fairyManifestNode.setEffect(null);
        }
        Game.Player player = game.getCurrentPlayer();
        Card target = fairyManifestTarget;
        Node node = fairyManifestNode;
        endFairyManifest();
        int slot = firstEmptySlot(slotsFor(player));
        if (slot < 0) {
            refreshActionHighlights();
            statusLabel.setText("エリアに空きが無いため顕現できませんでした。");
            return;
        }
        // コストを払わず【顕現】する。placeEntity 経由なので顕現先の【顕現】時効果も誘発する。
        placeEntity(player, target, slot, node);
    }

    private void cancelFairyManifest() {
        if (fairyManifestNode != null) {
            fairyManifestNode.setEffect(null);
        }
        endFairyManifest();
        refreshActionHighlights();
        statusLabel.setText("無償顕現を見送りました。");
    }

    private void endFairyManifest() {
        fairyManifestActive = false;
        fairyManifestTarget = null;
        fairyManifestNode = null;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
    }


    /** 場のスロットがクリックされたとき。中身があれば詳細表示する。 */
    private void handleSlotClick(Game.Player owner, int slotIndex) {
        Card c = slotsFor(owner)[slotIndex];
        // NPCの手番（自動攻撃宣言中など）／ガード・スキルチェックの選択バー表示中は、エリアのカードは詳細表示のみ。
        // （リアクティブなコスト支払い=selectionActive や スキル対象選択=skillTargetActive の時は通常処理を通す）
        if ((npcThinking || choiceActive) && !selectionActive && !skillTargetActive) {
            if (c != null) {
                showCardDetail(c);
            }
            return;
        }
        // 攻撃対象選択中: 防御側（相手）のエンティティをクリックで対象指定。（【幻影】は攻撃対象にはなる）
        if (attackTargetActive) {
            if (owner == game.getCurrentPlayer().getOpponent() && c != null) {
                if (isUnattackable(c)) {
                    statusLabel.setText("このエンティティは攻撃対象にできません。");
                } else {
                    setAttackTarget(c, slotCellsFor(owner)[slotIndex], false);
                }
            } else if (c != null) {
                // 自分側のエンティティなどは詳細表示のみ。
                showCardDetail(c);
            }
            return;
        }
        // スキル対象選択中: 装備は両エリア、破壊は相手のみ。
        if (skillTargetActive) {
            if (c != null) {
                boolean equip = skillCard != null && skillCard.skillEffect == SkillEffect.EQUIP;
                boolean destroyEq = skillCard != null && skillCard.skillEffect == SkillEffect.DESTROY_EQUIPMENT;
                boolean immunity = skillCard != null && skillCard.skillEffect == SkillEffect.GRANT_IMMUNITY;
                boolean trample = skillCard != null && skillCard.skillEffect == SkillEffect.GRANT_TRAMPLE;
                boolean anySide = equip || destroyEq || immunity || trample
                        || (skillCard != null && skillCard.skillEffect == SkillEffect.MODIFY_ATK);
                boolean ok = anySide || owner == skillCaster().getOpponent();
                // 幻影: 相手（＝スキル発動者から見た相手）は相手のエンティティを効果の対象にできない。
                if (ok && owner != skillCaster() && hasKeyword(c, "幻影")) {
                    statusLabel.setText("【幻影】: 相手は効果の対象にできません。");
                } else if ("アンノウン・グレイ".equals(c.name)) {
                    statusLabel.setText("アンノウン・グレイはスキルカードの効果を受けません。");
                } else if (isEffectImmune(c)) {
                    statusLabel.setText(c.name + " は他のカードの効果を受けません（ボルカニック・シールド）。");
                } else if (immunity && c.attribute != Attribute.VOLCANO) {
                    statusLabel.setText("ボルカニック・シールドの対象は【火山属性】のみです。");
                } else if (equip && !c.equipments.isEmpty()) {
                    statusLabel.setText("このエンティティには既に装備カードがあります（1枚まで）。");
                } else if (destroyEq && c.equipments.isEmpty()) {
                    statusLabel.setText("このエンティティは装備カードを持っていません。");
                } else if (ok) {
                    setSkillTarget(c, slotCellsFor(owner)[slotIndex]);
                }
            }
            return;
        }
        // 顕現時効果の対象選択中: 候補のエンティティをクリックで指定。
        if (manifestEffectActive) {
            if (c != null && manifestTargetCandidates(game.getCurrentPlayer(), manifestEffectSource).contains(c)) {
                setManifestTarget(c, slotCellsFor(owner)[slotIndex]);
            }
            return;
        }
        if (c != null) {
            selectCard(c, slotCellsFor(owner)[slotIndex]);
        }
    }

    private List<Card> handFor(Game.Player p) {
        return state.handFor(p);
    }

    private List<Card> remnantFor(Game.Player p) {
        return state.remnantFor(p);
    }

    private List<Card> libraryFor(Game.Player p) {
        return state.libraryFor(p);
    }

    private List<Card> sealFor(Game.Player p) {
        return state.sealFor(p);
    }

    /** スキルを発動するプレイヤー（skillCastPlayer 指定があればそれ、無ければ手番）。 */
    private Game.Player skillCaster() {
        return skillCastPlayer != null ? skillCastPlayer : game.getCurrentPlayer();
    }

    /** 現在のコスト支払いを行うプレイヤー（selectionPlayer→skillCaster→手番の優先順）。 */
    private Game.Player selectionOwner() {
        if (selectionPlayer != null) {
            return selectionPlayer;
        }
        return skillCaster();
    }

    private Card[] slotsFor(Game.Player p) {
        return state.slotsFor(p);
    }

    private StackPane[] slotCellsFor(Game.Player p) {
        return p == Game.Player.PLAYER1 ? playerSlotCells : opponentSlotCells;
    }

    private int firstEmptySlot(Card[] slots) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private int parseCost(String cost) {
        Integer n = numericCost(cost);
        return n != null ? n : 0;
    }

    /** コスト文字列を数値で返す。「3 or all」などの非数値は null。 */
    private Integer numericCost(String cost) {
        if (cost == null) {
            return null;
        }
        try {
            return Integer.parseInt(cost.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 指定プレイヤーのレムナントにある宇宙属性の枚数（＝コスト軽減量）。 */
    private int cosmosReduction(Game.Player p) {
        int n = 0;
        for (Card c : remnantFor(p)) {
            if (c.attribute == Attribute.COSMOS) {
                n++;
            }
        }
        return n;
    }

    /**
     * 実際に支払うコスト。自分のターン中・自分の手札のカードのみ、
     * レムナントの宇宙属性の枚数だけ軽減する（下限1）。非数値コストはそのまま0扱い。
     */
    private int effectiveCost(Card card) {
        Integer base = numericCost(card.cost);
        if (base == null) {
            return 0;
        }
        // 手札（顕現・スキル）／レムナント（再生）のコストを、自分のターン中は宇宙軽減する。
        if (base >= 1 && (card.zone == Zone.HAND || card.zone == Zone.REMNANT)
                && card.owner == game.getCurrentPlayer()) {
            return Math.max(1, base - cosmosReduction(card.owner));
        }
        return base;
    }

    /** aスキルか（コスト欄が「x or all」形式）。 */
    private boolean isASkill(Card card) {
        return card != null && card.cost != null && card.cost.contains("or all");
    }

    /** 「x or all」コストの x（基礎値）。aスキルでなければ null。 */
    private Integer aSkillBaseCx(String cost) {
        if (cost == null) {
            return null;
        }
        int idx = cost.indexOf("or all");
        if (idx < 0) {
            return null;
        }
        try {
            return Integer.parseInt(cost.substring(0, idx).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** aスキルの Cx を宇宙軽減込みで返す（1未満にはならない）。 */
    private int aSkillEffectiveCx(Card card) {
        Integer base = aSkillBaseCx(card.cost);
        if (base == null) {
            return 0;
        }
        if (base >= 1 && card.owner == game.getCurrentPlayer()) {
            return Math.max(1, base - cosmosReduction(card.owner));
        }
        return base;
    }

    /** 全エンティティスロットの表示を実体に同期する。 */
    private void refreshSlots() {
        fillSlotRow(Game.Player.PLAYER1);
        fillSlotRow(Game.Player.PLAYER2);
    }

    private void fillSlotRow(Game.Player owner) {
        StackPane[] cells = slotCellsFor(owner);
        Card[] slots = slotsFor(owner);
        for (int i = 0; i < cells.length; i++) {
            StackPane cell = cells[i];
            if (cell == null) {
                continue;
            }
            cell.getChildren().clear();
            Card c = slots[i];
            if (c == null) {
                Label empty = new Label("エンティティ");
                empty.setFont(Font.font(13));
                cell.getChildren().add(empty);
                cell.setStyle("-fx-border-color:#999999; -fx-border-width:1; -fx-background-color:#ffffff;");
                continue;
            }
            // セルは透明な容器とし、装備カードを裏（背面）に、本体を前面に置く。
            cell.setStyle("-fx-background-color:transparent;");
            Attribute a = c.attribute;

            // 装備カード（最大1枚）：縦向きのまま裏に重ね、右下にはみ出させる。
            if (!c.equipments.isEmpty()) {
                cell.getChildren().add(CardView.equipVisual(c.equipments.get(0)));
            }

            // エンティティ本体（前面）
            Label name = new Label(c.name);
            name.setFont(Font.font(12));
            name.setWrapText(true);
            name.setTextAlignment(TextAlignment.CENTER);
            int baseAtk = parseAtk(c.atk);
            int effAtk = effectiveAtk(c);
            Label atk = new Label(c.atk != null ? "ATK/" + effAtk : "");
            atk.setFont(Font.font(11));
            VBox content = new VBox(2, name, atk);
            content.setAlignment(Pos.CENTER);
            StackPane face = new StackPane(content);
            face.setAlignment(Pos.CENTER);
            face.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            if (a != null) {
                String border = a.borderColor != null
                        ? "-fx-border-color:" + a.borderColor + "; -fx-border-width:3;"
                        : "-fx-border-color:#999999; -fx-border-width:1;";
                face.setStyle("-fx-background-color:" + a.fillColor + ";" + border);
                name.setStyle("-fx-text-fill:" + a.textColor + ";");
                // ATK増減はグレー背景のチップ化で、どの属性色の上でも見やすくする。
                // 上昇=赤、下降=青、通常は属性の文字色（チップなし）。
                if (effAtk > baseAtk) {
                    atk.setStyle("-fx-text-fill:#D32F2F; -fx-font-weight:bold;"
                            + " -fx-background-color:#ECECEC; -fx-background-radius:6; -fx-padding:0 6 0 6;");
                } else if (effAtk < baseAtk) {
                    atk.setStyle("-fx-text-fill:#1565C0; -fx-font-weight:bold;"
                            + " -fx-background-color:#ECECEC; -fx-background-radius:6; -fx-padding:0 6 0 6;");
                } else {
                    atk.setStyle("-fx-text-fill:" + a.textColor + ";");
                }
            } else {
                face.setStyle("-fx-border-color:#999999; -fx-border-width:1; -fx-background-color:#ffffff;");
            }
            cell.getChildren().add(face);
        }
    }

    /** 装備カード1枚の表示。縦向きのままエンティティの裏に重ね、右下にはみ出させる。 */
    private void doSkill() {
        if (blockedByGameOver() || npcThinking) {
            return;
        }
        if (isBusy()) {
            statusLabel.setText("選択を完了してください（決定／キャンセル）。");
            return;
        }
        if (game.getCurrentPhase() != Game.Phase.SKILL) {
            statusLabel.setText("スキルはスキルフェイズ中のみ使用できます。");
            return;
        }
        if (skillUsedThisPhase) {
            statusLabel.setText("スキルはこのスキルフェイズで既に使用済みです（1枚まで）。");
            return;
        }
        Game.Player current = game.getCurrentPlayer();
        List<Card> hand = current == Game.Player.PLAYER1 ? state.playerHand : state.opponentHand;
        if (hand.isEmpty()) {
            statusLabel.setText("手札がありません。");
            return;
        }
        if (selectedCard == null || selectedCard.owner != current || selectedCard.zone != Zone.HAND || selectedCard.type != CardType.SKILL) {
            statusLabel.setText("発動するスキルを手札から選択してください。");
            return;
        }
        Card skill = selectedCard;
        dispatchSkillEffect(skill);
    }

    /**
     * スキル効果のディスパッチ（対象選択／コスト支払いへ）。発動者は skillCaster()。
     * doSkill（スキルフェイズ）と reactSkill（スキルチェック）の両方から呼ぶ。
     */
    private void dispatchSkillEffect(Card skill) {
        Game.Player current = skillCaster();
        List<Card> hand = handFor(current);
        int cost = effectiveCost(skill);
        boolean affordable = cost <= hand.size() - 1;
        switch (skill.skillEffect) {
            case DESTROY_ENEMY_ENTITY:
                if (!hasAnyEntity(current.getOpponent())) {
                    statusLabel.setText("破壊できる相手エンティティが場にいません。");
                    return;
                }
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                beginSkillTarget(skill);
                break;
            case EQUIP:
                if (!hasAnyEntity(Game.Player.PLAYER1) && !hasAnyEntity(Game.Player.PLAYER2)) {
                    statusLabel.setText("装備できるエンティティが場にいません。");
                    return;
                }
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                beginSkillTarget(skill);
                break;
            case MODIFY_ATK:
                if (!hasAnyEntity(Game.Player.PLAYER1) && !hasAnyEntity(Game.Player.PLAYER2)) {
                    statusLabel.setText("対象にできるエンティティが場にいません。");
                    return;
                }
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                beginSkillTarget(skill);
                break;
            case DESTROY_EQUIPMENT:
                if (!hasAnyEquippedEntity(Game.Player.PLAYER1) && !hasAnyEquippedEntity(Game.Player.PLAYER2)) {
                    statusLabel.setText("破壊できる装備カードが場にありません。");
                    return;
                }
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                beginSkillTarget(skill);
                break;
            case GRANT_IMMUNITY:
                if (!anyAttributeOnField(Attribute.VOLCANO)) {
                    statusLabel.setText("対象にできる【火山属性】が場にいません。");
                    return;
                }
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                beginSkillTarget(skill);
                break;
            case GRANT_TRAMPLE:
                if (!hasAnyEntity(Game.Player.PLAYER1) && !hasAnyEntity(Game.Player.PLAYER2)) {
                    statusLabel.setText("対象にできるエンティティが場にいません。");
                    return;
                }
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                beginSkillTarget(skill);
                break;
            case RECYCLE:
                if (recycleHandCandidates(current).isEmpty()) {
                    statusLabel.setText("ハンドに加えられるエンティティ（宇宙以外・コスト3以下）が自分のレムナントにいません。");
                    return;
                }
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                beginRecycleToHand(skill);
                break;
            case DISABLE_GUARD:
                // 対象を取らないスキル。コスト支払いへ直接進む（0なら即解決）。
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                skillCard = skill;
                skillTargetCard = null;
                if (cost == 0) {
                    resolveSkill(current, skill, null);
                } else {
                    beginSkillCostSelection(skill, cost);
                }
                break;
            case DESTROY_ALL_ENTITIES:
                // aスキル。対象なし・破壊できるエンティティが場にいる時のみ。
                if (!hasAnyEntity(Game.Player.PLAYER1) && !hasAnyEntity(Game.Player.PLAYER2)) {
                    statusLabel.setText("破壊できるエンティティが場にいません。");
                    return;
                }
                beginASkill(skill);
                break;
            case ZERO_ATTACKER_ATK:
                // コズミック・ノヴァはスキルフェイズでは発動できない（リアクティブ専用）。
                statusLabel.setText("このカードはスキルフェイズでは発動できません（相手の攻撃宣言時に割り込みます）。");
                return;
            case REVIVE_EQUIP:
                if (!hasAnyEntityInRemnant(current)) {
                    statusLabel.setText("再生できるエンティティが自分のレムナントにいません。");
                    return;
                }
                if (firstEmptySlot(slotsFor(current)) < 0) {
                    statusLabel.setText("エンティティスロットに空きがありません。");
                    return;
                }
                if (!affordable) {
                    statusLabel.setText("コストを支払う手札が足りません（必要" + cost + "枚）。");
                    return;
                }
                beginReviveTarget(skill);
                break;
            default:
                statusLabel.setText("このスキルには効果が未実装です: " + skill.name);
                break;
        }
    }

    /** スキル効果の対象（破壊する相手エンティティ）選択を開始する。 */
    private void beginSkillTarget(Card skill) {
        skillTargetActive = true;
        clearActionHighlights(); // 青ハイライトを消す（選択モードは黄グロー）
        skillCard = skill;
        skillTargetCard = null;
        skillTargetNode = null;
        updateSkillButton(); // 対象選択中は発動ボタンを隠す
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        String prompt;
        switch (skill.skillEffect) {
            case EQUIP:             prompt = "装備する対象のエンティティを選択してください"; break;
            case MODIFY_ATK:        prompt = "ATKを変化させる対象のエンティティを選択してください"; break;
            case DESTROY_EQUIPMENT: prompt = "装備カードを破壊する対象のエンティティを選択してください"; break;
            case GRANT_IMMUNITY:    prompt = "効果耐性を与える【火山属性】を選択してください"; break;
            case GRANT_TRAMPLE:     prompt = "効果を与える対象のエンティティを選択してください"; break;
            default:                prompt = "破壊する相手エンティティを選択してください"; break;
        }
        confirmPrompt.setText(prompt);
        confirmOkButton.setDisable(true);
        statusLabel.setText("スキル「" + skill.name + "」: " + prompt);
    }

    private void setSkillTarget(Card card, Node node) {
        if (skillTargetNode != null) {
            skillTargetNode.setEffect(null);
        }
        skillTargetCard = card;
        skillTargetNode = node;
        if (node != null) {
            node.setEffect(CardView.selectionGlow());
        }
        confirmOkButton.setDisable(false);
        confirmPrompt.setText("対象: " + card.name + " — 決定でコスト支払いへ");
    }

    /** スキル対象の決定 → コスト支払いへ（コスト0なら即解決）。 */
    private void confirmSkillTarget() {
        if (!skillTargetActive || skillTargetCard == null) {
            statusLabel.setText("対象を選択してください。");
            return;
        }
        if (skillTargetNode != null) {
            skillTargetNode.setEffect(null);
        }
        Card skill = skillCard;
        skillTargetActive = false;
        int cost = effectiveCost(skill);
        if (cost == 0) {
            confirmBar.setVisible(false);
            confirmBar.setManaged(false);
            resolveSkill(skillCaster(), skill, skillTargetCard);
        } else {
            beginSkillCostSelection(skill, cost);
        }
    }

    private void cancelSkillTarget() {
        if (skillTargetNode != null) {
            skillTargetNode.setEffect(null);
        }
        skillTargetActive = false;
        skillCard = null;
        skillTargetCard = null;
        skillTargetNode = null;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
        if (skillCheckSkillActive) {
            // スキルチェックでの割り込みを取りやめ（効果なし）→ バトルへ戻る。
            skillCheckSkillActive = false;
            skillCastPlayer = null;
            statusLabel.setText("スキルチェックの割り込みを取りやめました。");
            resumeBattleAfterSkillCheck();
            return;
        }
        refreshActionHighlights();
        statusLabel.setText("スキルをキャンセルしました。");
    }

    /** スキル発動コストの支払い選択を開始する。 */
    private void beginSkillCostSelection(Card skill, int cost) {
        selectionActive = true;
        selectionRequired = cost;
        selectionChosen.clear();
        selectionNodeMap.clear();
        pendingSkill = skill;
        updateManifestButton();
        updateAttackButton();
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        updateSelectionPrompt();
        statusLabel.setText("スキル「" + skill.name + "」のコスト支払い: 手札を選択してください。");
    }

    // ===== aスキル（金枠）：Cx or all コスト =====

    /** aスキルの発動。「Cx」か「all（封印）」を選ばせて支払い、効果へ進む。 */
    private void beginASkill(Card skill) {
        Game.Player player = game.getCurrentPlayer();
        int cx = aSkillEffectiveCx(skill);
        int sealCount = 0;
        for (Card c : handFor(player)) {
            if (c != skill && !isASkill(c)) {
                sealCount++;
            }
        }
        boolean cxAffordable = cx <= handFor(player).size() - 1; // スキル自身は支払いに使えない
        int choice = chooseASkillCost(skill, cx, cxAffordable, sealCount);
        if (choice == 0) {
            // Cx: 指定枚数をレムナントへ。
            skillCard = skill;
            skillTargetCard = null;
            if (cx == 0) {
                resolveSkill(player, skill, null);
            } else {
                beginSkillCostSelection(skill, cx);
            }
        } else if (choice == 1) {
            // all: aスキル以外の他の手札をすべて封印エリアへ（裏側）。
            payAllToSeal(player, skill);
            skillCard = skill;
            skillTargetCard = null;
            resolveSkill(player, skill, null);
        }
        // choice == -1: キャンセル（何もしない）
    }

    /** aスキルの「all」コスト: 使用スキルと他のaスキルを除く手札をすべて封印エリアへ送る。 */
    private void payAllToSeal(Game.Player player, Card skill) {
        List<Card> hand = handFor(player);
        List<Card> seal = sealFor(player);
        for (Card c : new ArrayList<>(hand)) {
            if (c == skill || isASkill(c)) {
                continue; // 使用するaスキル・他のaスキルは封印しない
            }
            hand.remove(c);
            c.zone = Zone.SEAL;
            c.revealed = false; // 裏側（非公開）
            seal.add(c);
        }
    }

    /** コスト支払い方法の選択ダイアログ。0=Cx / 1=all / -1=キャンセル。 */
    private int chooseASkillCost(Card skill, int cx, boolean cxAffordable, int sealCount) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("aスキルのコスト支払い");
        alert.setHeaderText(null);
        alert.setContentText("aスキル「" + skill.name + "」のコスト支払い方法を選択してください。");
        ButtonType cxBtn = new ButtonType("C" + cx + "（" + cx + "枚レムナントへ）", ButtonBar.ButtonData.OTHER);
        ButtonType allBtn = new ButtonType("all（他の手札" + sealCount + "枚を封印）", ButtonBar.ButtonData.OTHER);
        ButtonType cancel = new ButtonType("キャンセル", ButtonBar.ButtonData.CANCEL_CLOSE);
        if (cxAffordable) {
            alert.getButtonTypes().setAll(cxBtn, allBtn, cancel);
        } else {
            alert.getButtonTypes().setAll(allBtn, cancel);
        }
        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent()) {
            return -1;
        }
        ButtonType r = result.get();
        if (r == cxBtn) {
            return 0;
        }
        if (r == allBtn) {
            return 1;
        }
        return -1;
    }

    /** スキル効果を解決する（破壊／装備／再生装備）。 */
    private void resolveSkill(Game.Player player, Card skill, Card target) {
        handFor(player).remove(skill);
        String msg;
        if (skill.skillEffect == SkillEffect.EQUIP) {
            skill.zone = Zone.BATTLE;
            target.equipments.add(skill);
            msg = "装備: " + skill.name + " → " + target.name;
        } else if (skill.skillEffect == SkillEffect.REVIVE_EQUIP) {
            // target は自分のレムナントのエンティティ。再生して装備する。
            remnantFor(player).remove(target);
            target.equipments.clear();
            target.addedEffects.clear();
            target.zone = Zone.BATTLE;
            target.revealed = true;
            applyReviveTriggers(target); // 【再生】時の効果（リビングデッドは【ガード】を得る）
            int slot = firstEmptySlot(slotsFor(player));
            slotsFor(player)[slot] = target;
            skill.zone = Zone.BATTLE;
            target.equipments.add(skill);
            msg = "再生＆装備: " + target.name + " に " + skill.name;
        } else if (skill.skillEffect == SkillEffect.MODIFY_ATK) {
            int delta = atkModDelta(skill, player);
            buffAtk(target, delta, false);
            skill.zone = Zone.REMNANT;
            remnantFor(player).add(skill);
            msg = "スキル「" + skill.name + "」: " + target.name + " のATKを "
                    + (delta >= 0 ? "+" : "") + delta + "（ターン終了まで）";
        } else if (skill.skillEffect == SkillEffect.DESTROY_EQUIPMENT) {
            // target は装備カードを持つエンティティ。その装備カードを破壊（持ち主のレムナントへ）。
            StringBuilder destroyed = new StringBuilder();
            for (Card eq : new ArrayList<>(target.equipments)) {
                eq.zone = Zone.REMNANT;
                remnantFor(eq.owner).add(eq);
                if (destroyed.length() > 0) {
                    destroyed.append("・");
                }
                destroyed.append(eq.name);
            }
            target.equipments.clear();
            skill.zone = Zone.REMNANT;
            remnantFor(player).add(skill);
            msg = "スキル「" + skill.name + "」: " + target.name + " の装備「" + destroyed + "」を破壊";
        } else if (skill.skillEffect == SkillEffect.GRANT_IMMUNITY) {
            state.effectImmuneThisTurn.add(target);
            skill.zone = Zone.REMNANT;
            remnantFor(player).add(skill);
            msg = "スキル「" + skill.name + "」: " + target.name + " はターン終了まで他のカードの効果を受けない";
        } else if (skill.skillEffect == SkillEffect.GRANT_TRAMPLE) {
            state.tramplersThisTurn.add(target);
            String extra = "";
            if (target.attribute == Attribute.SKY) {
                buffAtk(target, 2, false); // 空属性ならターン終了までATK+2
                extra = "（空属性: ATK+2）";
            }
            skill.zone = Zone.REMNANT;
            remnantFor(player).add(skill);
            msg = "スキル「" + skill.name + "」: " + target.name
                    + " はバトルで破壊した相手のATK分を相手エレメントに与える" + extra;
        } else if (skill.skillEffect == SkillEffect.DISABLE_GUARD) {
            state.guardDisabledThisTurn.add(player.getOpponent());
            skill.zone = Zone.REMNANT;
            remnantFor(player).add(skill);
            msg = "スキル「" + skill.name + "」: ターン終了まで相手は【ガード】を発動できない";
        } else if (skill.skillEffect == SkillEffect.DESTROY_ALL_ENTITIES) {
            // エリアのエンティティを全て破壊する（両プレイヤー）。
            List<Card> toDestroy = new ArrayList<>();
            for (Game.Player p : Game.Player.values()) {
                for (Card c : slotsFor(p)) {
                    if (c != null) {
                        toDestroy.add(c);
                    }
                }
            }
            for (Card c : toDestroy) {
                destroyEntity(c.owner, c);
            }
            skill.zone = Zone.REMNANT;
            remnantFor(player).add(skill);
            msg = "aスキル「" + skill.name + "」: エリアのエンティティを全て破壊（" + toDestroy.size() + "体）";
        } else {
            // DESTROY_ENEMY_ENTITY
            destroyEntity(target.owner, target);
            skill.zone = Zone.REMNANT;
            remnantFor(player).add(skill);
            msg = "スキル「" + skill.name + "」: " + target.name + " を破壊";
        }
        boolean reactive = skillCheckSkillActive;
        skillCard = null;
        skillTargetCard = null;
        skillTargetNode = null;
        pendingSkill = null;
        selectedCard = null;
        selectedCardNode = null;
        if (reactive) {
            // スキルチェックでの防御側スキル発動。スキルフェイズの使用回数は消費しない。
            skillCheckSkillActive = false;
            skillCastPlayer = null;
            defenderSkillCheckUsed = true;
            statusLabel.setText(msg);
            resumeBattleAfterSkillCheck(); // 効果適用後にバトルへ戻る（handViewOwner も戻る）
            return;
        }
        skillUsedThisPhase = true;
        refreshHandPanes();
        refreshSlots();
        render();
        statusLabel.setText(msg);
    }

    private boolean hasAnyEntity(Game.Player p) {
        for (Card c : slotsFor(p)) {
            if (c != null) {
                return true;
            }
        }
        return false;
    }

    /** 指定プレイヤーの場に、装備カードを持つエンティティがいるか（ピュリファイ・ブリーズ用）。 */
    private boolean hasAnyEquippedEntity(Game.Player p) {
        for (Card c : slotsFor(p)) {
            if (c != null && !c.equipments.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** ボルカニック・シールドで「他のカードの効果を受けない」状態か。 */
    private boolean isEffectImmune(Card c) {
        return c != null && state.effectImmuneThisTurn.contains(c);
    }

    private boolean hasAnyEntityInRemnant(Game.Player p) {
        for (Card c : remnantFor(p)) {
            if (c.type == CardType.ENTITY) {
                return true;
            }
        }
        return false;
    }

    // ===== 再生装備フロー（死霊術師の呪縛） =====

    /** 自分のレムナントから再生するエンティティの選択を開始する。 */
    private void beginReviveTarget(Card skill) {
        reviveTargetActive = true;
        clearActionHighlights();
        reviveSkill = skill;
        reviveTarget = null;
        reviveTargetNode = null;
        updateSkillButton();
        showRemnantList(game.getCurrentPlayer()); // 自分のレムナント一覧を開く
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
        confirmPrompt.setText("再生して装備するエンティティを自分のレムナントから選択してください");
        confirmOkButton.setDisable(true);
        statusLabel.setText("スキル「" + skill.name + "」: 自分のレムナントのエンティティを選択して下さい");
    }

    private void setReviveTarget(Card card, Node node) {
        if (reviveTargetNode != null) {
            reviveTargetNode.setEffect(null);
        }
        reviveTarget = card;
        reviveTargetNode = node;
        if (node != null) {
            node.setEffect(CardView.selectionGlow());
        }
        confirmOkButton.setDisable(false);
        confirmPrompt.setText("再生対象: " + card.name + " — 決定でコスト支払いへ");
    }

    private void confirmReviveTarget() {
        if (!reviveTargetActive || reviveTarget == null) {
            statusLabel.setText("再生する対象を選択してください。");
            return;
        }
        if (reviveTargetNode != null) {
            reviveTargetNode.setEffect(null);
        }
        Card skill = reviveSkill;
        Card target = reviveTarget;
        reviveTargetActive = false;
        reviveSkill = null;
        reviveTarget = null;
        reviveTargetNode = null;
        // コスト支払いへ橋渡し（resolveSkill が REVIVE_EQUIP として処理）。
        skillCard = skill;
        skillTargetCard = target;
        int cost = effectiveCost(skill);
        if (cost == 0) {
            confirmBar.setVisible(false);
            confirmBar.setManaged(false);
            resolveSkill(game.getCurrentPlayer(), skill, target);
        } else {
            beginSkillCostSelection(skill, cost);
        }
    }

    private void cancelReviveTarget() {
        if (reviveTargetNode != null) {
            reviveTargetNode.setEffect(null);
        }
        reviveTargetActive = false;
        reviveSkill = null;
        reviveTarget = null;
        reviveTargetNode = null;
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
        refreshActionHighlights();
        statusLabel.setText("スキルをキャンセルしました。");
    }

    private void animateAction(String label, Node source, Node target, Runnable finished) {
        if (source == null || target == null) {
            if (finished != null) finished.run();
            return;
        }
        Label movingCard = new Label(label);
        movingCard.setStyle("-fx-background-color:#ffffff; -fx-border-color:#222222; -fx-border-width:2; -fx-padding:12; -fx-font-size:13; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 0);");
        animationLayer.getChildren().add(movingCard);

        Point2D startScene = source.localToScene(source.getBoundsInLocal().getMinX(), source.getBoundsInLocal().getMinY());
        Point2D endScene = target.localToScene(target.getBoundsInLocal().getMinX(), target.getBoundsInLocal().getMinY());
        Point2D start = animationLayer.sceneToLocal(startScene);
        Point2D end = animationLayer.sceneToLocal(endScene);

        movingCard.relocate(start.getX(), start.getY());
        TranslateTransition transition = new TranslateTransition(Duration.millis(700), movingCard);
        transition.setToX(end.getX() - start.getX());
        transition.setToY(end.getY() - start.getY());
        transition.setOnFinished(evt -> {
            animationLayer.getChildren().remove(movingCard);
            if (finished != null) finished.run();
        });
        transition.play();
    }

    /**
     * 自分の手札表示を作成する（相手ハンドは枚数表示のみのため公開されるのは自分の手札だけ）。
     */
    private HBox createHandPane() {
        Label handLabel = new Label("ハンド");
        handLabel.setFont(Font.font(12));

        HBox cards = new HBox(8);
        cards.setMinHeight(CARD_PREF_HEIGHT);
        cards.setId("playerCards");
        buildHandCards(cards);

        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(cards);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(false);
        scroll.setFitToHeight(true);
        // 7枚ぶんの幅（カード7枚＋6ギャップ＋スクロールバー余白）に固定する。
        double viewport = 7 * CARD_PREF_WIDTH + 6 * 8 + 18;
        scroll.setPrefViewportWidth(viewport);
        scroll.setMinViewportHeight(CARD_PREF_HEIGHT + 16);

        // left overflow indicator
        Label leftDots = new Label("...");
        leftDots.setVisible(false);
        leftDots.setStyle("-fx-font-size:18; -fx-background-color:transparent; -fx-padding:4 6 4 6;");

        // show leftDots when scrolled right
        scroll.hvalueProperty().addListener((obs, oldV, newV) -> {
            leftDots.setVisible(newV.doubleValue() > 0.01);
        });

        StackPane scrollWithDots = new StackPane(scroll);
        StackPane.setAlignment(leftDots, Pos.CENTER_LEFT);
        scrollWithDots.getChildren().add(leftDots);

        HBox box = new HBox(10, handLabel, scrollWithDots);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6));
        box.setMaxWidth(Region.USE_PREF_SIZE);
        return box;
    }

    private void buildHandCards(HBox cards) {
        cards.getChildren().clear();
        handCellMap.clear();
        // 再構築で古いセルに付いた顕現／発動ボタンを切り離す。
        if (manifestButtonHost != null) {
            manifestButtonHost.getChildren().remove(manifestButton);
            manifestButtonHost = null;
        }
        if (skillButtonHost != null) {
            skillButtonHost.getChildren().remove(skillButton);
            skillButtonHost = null;
        }
        // 所持しているカードのみ表示する（空き枠は出さない）。handViewOwner の手札を表面で表示。
        for (Card card : handFor(handViewOwner)) {
            Label label = new Label(card.getDisplayName(true));
            label.setFont(Font.font(12));
            label.setWrapText(true);
            label.setTextAlignment(TextAlignment.CENTER);
            // Button はクリックを消費して親に伝えないため、セルと同じ StackPane+Label で構成する。
            StackPane cell = new StackPane(label);
            cell.setAlignment(Pos.CENTER);
            CardView.applyCardColor(cell, label, card);
            AspectRatioPane cardWrapper = new AspectRatioPane(cell, 88.0 / 63.0);
            cardWrapper.setMinSize(63, 88);
            cardWrapper.setPrefSize(CARD_PREF_WIDTH, CARD_PREF_HEIGHT);
            // fixed width to prevent layout stretching
            cardWrapper.setMaxWidth(CARD_PREF_WIDTH);
            HBox.setHgrow(cardWrapper, Priority.NEVER);
            handCellMap.put(card, cell);
            cardWrapper.setOnMouseClicked(e -> selectCard(card, cardWrapper));
            cards.getChildren().add(cardWrapper);
        }
    }

    /**
     * プレイヤー/相手エリア（3×2）を作成する。
     * エンティティ行は実体スロットに連動し、封印/レムナント/ライブラリ行は固定枠。
     * inverted が true（相手側）の場合は列を左右反転して向かい合わせに配置する。
     */
    private VBox createPlayerArea(boolean inverted) {
        Game.Player owner = inverted ? Game.Player.PLAYER2 : Game.Player.PLAYER1;
        StackPane[] slotCells = inverted ? opponentSlotCells : playerSlotCells;

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setMaxHeight(Double.MAX_VALUE);

        // 自分: 上=エンティティ / 下=ゾーン、相手: 上=ゾーン / 下=エンティティ
        int entityRow = inverted ? 1 : 0;
        int zoneRow = inverted ? 0 : 1;
        String[] zoneTexts = {"封印", "レムナント", "ライブラリ"};

        // エンティティスロット行
        for (int col = 0; col < 3; col++) {
            int actualCol = inverted ? 2 - col : col;
            final int slotIndex = col;
            StackPane cell = new StackPane();
            cell.setAlignment(Pos.CENTER);
            slotCells[slotIndex] = cell;
            AspectRatioPane cellWrapper = new AspectRatioPane(cell, 88.0 / 63.0);
            cellWrapper.setMinSize(63, 88);
            cellWrapper.setPrefSize(80, 112);
            cellWrapper.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(cellWrapper, Priority.ALWAYS);
            GridPane.setVgrow(cellWrapper, Priority.ALWAYS);
            cellWrapper.setOnMouseClicked(e -> handleSlotClick(owner, slotIndex));
            grid.add(cellWrapper, actualCol, entityRow);
        }

        // 封印 / レムナント / ライブラリ行（固定枠）
        for (int col = 0; col < 3; col++) {
            int actualCol = inverted ? 2 - col : col;
            String cardName = zoneTexts[col];
            Label label = new Label(cardName);
            label.setFont(Font.font(13));
            label.setTextAlignment(TextAlignment.CENTER);
            StackPane cell = new StackPane(label);
            cell.setAlignment(Pos.CENTER);
            cell.setStyle("-fx-border-color:#999999; -fx-border-width:1; -fx-background-color:#ffffff;");
            AspectRatioPane cellWrapper = new AspectRatioPane(cell, 88.0 / 63.0);
            cellWrapper.setMinSize(63, 88);
            cellWrapper.setPrefSize(80, 112);
            cellWrapper.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(cellWrapper, Priority.ALWAYS);
            GridPane.setVgrow(cellWrapper, Priority.ALWAYS);
            // レムナントは公開情報。クリックで左側に一覧を表示する。
            if (cardName.equals("レムナント")) {
                if (inverted) {
                    opponentRemnantCellLabel = label;
                } else {
                    playerRemnantCellLabel = label;
                }
                cellWrapper.setOnMouseClicked(e -> showRemnantList(owner));
            } else if (cardName.equals("封印")) {
                // 封印エリアは非公開（裏側）。枚数のみ表示する。
                if (inverted) {
                    opponentSealCellLabel = label;
                } else {
                    playerSealCellLabel = label;
                }
            } else if (cardName.equals("ライブラリ")) {
                // ライブラリは枚数のみ表示する（残り枚数）。
                if (inverted) {
                    opponentLibraryCellLabel = label;
                } else {
                    playerLibraryCellLabel = label;
                }
            }
            grid.add(cellWrapper, actualCol, zoneRow);
        }

        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.3333);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < 2; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(50);
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);
        }

        VBox container = new VBox(grid);
        container.setPadding(new Insets(6));
        container.setStyle("-fx-border-color:#dddddd; -fx-border-width:1; -fx-background-color:#f8f8f8;");
        container.setAlignment(Pos.CENTER);
        container.setMinSize(240, 150);
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMaxHeight(Double.MAX_VALUE);
        return container;
    }


    /**
     * ゲーム状態をUIに反映する。
     */
    private void render() {
        phaseLabel.setText("ターン：" + game.getTurnNumber()
                + "　フェイズ：" + game.getCurrentPhaseName()
                + "（" + game.getCurrentPlayer().getDisplayName() + "）");

        if (playerCoreLabel != null) {
            playerCoreLabel.setText("◆：" + game.getCore(Game.Player.PLAYER1));
        }
        if (opponentCoreLabel != null) {
            opponentCoreLabel.setText("◆：" + game.getCore(Game.Player.PLAYER2));
        }

        if (playerRemnantCellLabel != null) {
            playerRemnantCellLabel.setText("レムナント\n(" + state.playerRemnant.size() + ")");
        }
        if (opponentRemnantCellLabel != null) {
            opponentRemnantCellLabel.setText("レムナント\n(" + state.opponentRemnant.size() + ")");
        }
        if (playerSealCellLabel != null) {
            playerSealCellLabel.setText("封印\n(" + state.playerSeal.size() + ")");
        }
        if (opponentSealCellLabel != null) {
            opponentSealCellLabel.setText("封印\n(" + state.opponentSeal.size() + ")");
        }
        if (playerLibraryCellLabel != null) {
            playerLibraryCellLabel.setText("ライブラリ\n(" + state.playerLibrary.size() + ")");
        }
        if (opponentLibraryCellLabel != null) {
            opponentLibraryCellLabel.setText("ライブラリ\n(" + state.opponentLibrary.size() + ")");
        }
        // 開いているレムナント一覧があれば内容を更新する。
        if (remnantListPanel != null && remnantListPanel.isVisible() && remnantListOwner != null) {
            showRemnantList(remnantListOwner);
        }
        // 行動可能カードの青ハイライトを更新する。
        refreshActionHighlights();
        // バトル中の発光（攻撃=赤／対象=青）は最後に再適用して、再描画で消えないようにする。
        applyBattleGlows();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
