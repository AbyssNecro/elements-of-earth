# Elements of Earth (EoE)

**Elements of Earth（EoE）** は、考案者 Necro によるオリジナルのデジタルカードゲームです。
エンティティ（モンスター）とスキルを駆使し、相手の **エレメントコア** を 0 にすることを目指します。
JavaFX 製のデスクトップアプリとして実装されています。

> 第1弾「新生の胎動」全 36 種のカードを収録。シングルプレイ（対 NPC）に対応しています。

---

## 特徴

- **5 属性の駆け引き** — 火山 / 海 / 森 / 空 / 宇宙。属性相性で有利を取ると戦闘で優位に。
- **エンティティとスキル** — 顕現（召喚）・装備・サポート・妨害・aスキル（金枠）・封印エリアなど多彩なギミック。
- **デッキ編成・保存** — 5 スロットにデッキを保存。1 枚ずつ増減できる編成画面＋カード詳細表示。
  - 検証ルール：ちょうど 40 枚 ／ 宇宙属性 ≤ 2 ／ aスキル ≤ 2（同名は通常 3 枚・宇宙/aスキルは 1 枚まで）。
- **NPC 対戦（簡易 AI）** — プリセット 4 デッキ（火山速攻・海コントロール・森ミッドレンジ・混成）＋ランダム選択。
  - 属性相性・リーサル・脅威の優先除去・自爆回避を考慮した戦闘判断。
- **コイントスで先攻決定** — ゲーム開始時に表＝先攻／裏＝後攻。
- **対話的なバトル UI** — 攻撃（赤）／被攻撃（青）／ガード可能（黄）の発光、非モーダルなガード／スキルチェックのバー（その間もカード詳細を確認可能）。
- **勝利条件** — 相手のエレメントコアを 0 にする、または相手をライブラリアウト（山札切れ）させる。

---

## 動作環境

- **JDK 20**（Gradle の toolchain で指定。未導入でも Gradle が自動取得を試みます）
- JavaFX 20 / Gson は Gradle が自動で取得します。

## 実行方法

リポジトリのルートで：

```bash
# macOS / Linux
./gradlew run

# Windows
gradlew.bat run
```

Windows ではルートの **`run.bat`** をダブルクリックしても起動できます。

### 遊び方の流れ

1. タイトル画面で **シングル** を選択 → 対戦相手の NPC デッキ（4 種＋ランダム）を選ぶ。
2. コイントスで先攻 / 後攻が決まる。
3. 各ターン：ドロー → スタンバイ（顕現）→ スキル → バトル（攻撃宣言・ガード・スキルチェック）→ エンド。
4. 相手のエレメントコアを 0、または相手をライブラリアウトさせれば勝利。

> **デッキ** メニューから自分のデッキを編成・保存できます。保存した使用中デッキはシングル対戦の自分側ライブラリに使われます。

---

## 技術スタック

- **言語 / UI**: Java 20, JavaFX 20（controls / fxml / swing）
- **ビルド**: Gradle（`application` + `org.openjfx.javafxplugin`）
- **データ**: Gson（`cards.json` 読み込み、`decks.json` 永続化）
- **アーキテクチャ**: Model / View / Controller の段階的な層分け
  - Model: `Game`（ターン・フェイズ・勝敗）、`MatchState`（場の状態）、`Card` / `Deck` / `DeckStore` / `CardLoader`、enum 群
  - View: `CardView`、`CardDetailPanel`、`AspectRatioPane`
  - Controller / UI: `App`（バトル画面）、`DeckScreens`（タイトル／デッキ画面）

### ディレクトリ構成

```
src/main/java/com/example/eoe/
  App.java            バトル画面の View/Controller（JavaFX エントリ）
  Game.java           ターン/フェイズ/コア/勝敗のモデル
  MatchState.java     場の状態（各ゾーン・スロット）
  Card.java / Deck.java / DeckStore.java / CardLoader.java
  DeckScreens.java    タイトル・デッキ一覧・編成・NPC 選択の画面
  CardView.java / CardDetailPanel.java / AspectRatioPane.java
  NpcDecks.java       NPC 用プリセットデッキ
  Attribute / CardType / SkillEffect / Zone（enum）
src/main/resources/
  cards.json          全 36 種のカードデータ
```

---

## 実装状況

- ✅ 全 36 種のカード効果、aスキル／封印エリア、ガード／スキルチェック
- ✅ デッキ編成・保存、NPC 対戦（簡易 AI＋戦闘判断強化）
- ✅ コイントス先攻決定、勝利条件（コア 0／ライブラリアウト）
- 🚧 オンライン対戦（未実装）、NPC のスキル使用・防御割り込みなど AI のさらなる高度化

---

## クレジット / ライセンス

- ゲームデザイン・原案: **Necro**
- 本リポジトリは個人制作のオリジナル作品です。ライセンスは未定（All rights reserved）。
  二次利用をご希望の場合は作者へご連絡ください。
