package com.example.eoe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1試合の「場」の状態（モデル層）。各ゾーンのカード群と、ターン中だけ有効な効果フラグを保持する。
 * 描画や操作ロジックは持たず、データとゾーンの取得のみを担当する。
 */
final class MatchState {

    // ----- ゾーン -----
    final List<Card> playerHand = new ArrayList<>();
    final List<Card> opponentHand = new ArrayList<>();
    final List<Card> playerRemnant = new ArrayList<>();
    final List<Card> opponentRemnant = new ArrayList<>();
    /** 封印エリア（aスキルの「all」コストで裏側に置かれたカード。非公開）。 */
    final List<Card> playerSeal = new ArrayList<>();
    final List<Card> opponentSeal = new ArrayList<>();
    /** ライブラリ（山札）。 */
    final List<Card> playerLibrary = new ArrayList<>();
    final List<Card> opponentLibrary = new ArrayList<>();
    /** エンティティスロット（各プレイヤー3枠）。null は空きスロット。 */
    final Card[] playerSlots = new Card[3];
    final Card[] opponentSlots = new Card[3];

    // ----- ターン中だけ有効な効果トラッキング -----
    /** このバトルフェイズで攻撃済みのエンティティ。 */
    final Set<Card> attackedEntities = new HashSet<>();
    /** ミスト・ミラージュの「破壊されない」を既に使用したエンティティ（1ターン1度）。 */
    final Set<Card> mistGuardUsed = new HashSet<>();
    /** ボルカニック・シールドで「他のカードの効果を受けない」状態のエンティティ（ターン終了まで）。 */
    final Set<Card> effectImmuneThisTurn = new HashSet<>();
    /** ホワイトフェザーで「バトル破壊時に破壊した相手のATK分コアダメージ」を得たエンティティ（ターン終了まで）。 */
    final Set<Card> tramplersThisTurn = new HashSet<>();
    /** アーマー・バインドで【ガード】を発動できない状態のプレイヤー（ターン終了まで）。 */
    final Set<Game.Player> guardDisabledThisTurn = new HashSet<>();
    /** コズミック・ノヴァでATKがターン終了まで0にされたエンティティ。 */
    final Set<Card> atkZeroedThisTurn = new HashSet<>();

    // ----- ゾーン取得 -----
    List<Card> handFor(Game.Player p) {
        return p == Game.Player.PLAYER1 ? playerHand : opponentHand;
    }

    List<Card> remnantFor(Game.Player p) {
        return p == Game.Player.PLAYER1 ? playerRemnant : opponentRemnant;
    }

    List<Card> sealFor(Game.Player p) {
        return p == Game.Player.PLAYER1 ? playerSeal : opponentSeal;
    }

    List<Card> libraryFor(Game.Player p) {
        return p == Game.Player.PLAYER1 ? playerLibrary : opponentLibrary;
    }

    Card[] slotsFor(Game.Player p) {
        return p == Game.Player.PLAYER1 ? playerSlots : opponentSlots;
    }
}
