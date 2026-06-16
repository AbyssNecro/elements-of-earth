package com.example.eoe;

/**
 * Elements of Earth（EoE）のゲーム状態を管理するモデルクラス。
 *
 * ゲームの進行状態、プレイヤー情報、カード管理などを保持する。
 * UIからは独立して動作する。
 */
public class Game {

    /**
     * ゲームのフェーズ。
     */
    public enum Phase {
        DRAW("ドロー"),
        STANDBY("スタンバイ"),
        SKILL("スキル"),
        BATTLE("バトル"),
        END("エンド");

        private final String displayName;

        Phase(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * プレイヤーの識別子。
     */
    public enum Player {
        PLAYER1("プレイヤー1"),
        PLAYER2("プレイヤー2");

        private final String displayName;

        Player(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Player getOpponent() {
            return this == PLAYER1 ? PLAYER2 : PLAYER1;
        }
    }

    /** 初期エレメントコア。 */
    private static final int INITIAL_CORE = 30;

    /** 現在のターン数。 */
    private int turnNumber = 1;

    /** 現在のフェーズ。 */
    private Phase currentPhase = Phase.DRAW;

    /** 現在の手番プレイヤー。 */
    private Player currentPlayer = Player.PLAYER1;

    /** 先攻プレイヤー（コイントスで決定）。 */
    private Player firstPlayer = Player.PLAYER1;

    /** プレイヤー1のエレメントコア。 */
    private int player1Core = INITIAL_CORE;

    /** プレイヤー2のエレメントコア。 */
    private int player2Core = INITIAL_CORE;

    /** プレイヤー1の手札枚数（簡易版）。 */
    private int player1HandSize = 0;

    /** プレイヤー2の手札枚数（簡易版）。 */
    private int player2HandSize = 0;

    /** デッキアウト（ライブラリ切れでドローできなかった）した敗者。いなければ null。 */
    private Player deckedOutLoser = null;

    /**
     * ゲームを初期化する。
     */
    public Game() {
        reset();
    }

    /**
     * ゲームをリセットする。
     */
    public void reset() {
        turnNumber = 1;
        currentPhase = Phase.DRAW;
        currentPlayer = firstPlayer;
        player1Core = INITIAL_CORE;
        player2Core = INITIAL_CORE;
        player1HandSize = 0;
        player2HandSize = 0;
        deckedOutLoser = null;
    }

    /**
     * 先攻プレイヤーを設定し、その手番（ドローフェイズ・ターン1）から開始する。
     * コイントスの結果が出た直後に呼ぶ。
     */
    public void setFirstPlayer(Player first) {
        this.firstPlayer = first;
        this.currentPlayer = first;
        this.currentPhase = Phase.DRAW;
        this.turnNumber = 1;
    }

    /**
     * 先攻プレイヤーを返す。
     */
    public Player getFirstPlayer() {
        return firstPlayer;
    }

    /**
     * 現在のフェーズを返す。
     */
    public Phase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * 現在のフェーズ名（日本語）を返す。
     */
    public String getCurrentPhaseName() {
        return currentPhase.getDisplayName();
    }

    /**
     * ターン数を返す。
     */
    public int getTurnNumber() {
        return turnNumber;
    }

    /**
     * 現在の手番プレイヤーを返す。
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * 指定プレイヤーのエレメントコアを返す。
     */
    public int getCore(Player player) {
        return player == Player.PLAYER1 ? player1Core : player2Core;
    }

    /**
     * 指定プレイヤーのエレメントコアにダメージを与える。
     */
    public void damageCore(Player player, int damage) {
        if (player == Player.PLAYER1) {
            player1Core = Math.max(0, player1Core - damage);
        } else {
            player2Core = Math.max(0, player2Core - damage);
        }
    }

    /**
     * 指定プレイヤーの手札枚数を返す。
     */
    public int getHandSize(Player player) {
        return player == Player.PLAYER1 ? player1HandSize : player2HandSize;
    }

    /**
     * フェーズを次へ進める。
     */
    public void nextPhase() {
        Phase[] phases = Phase.values();
        int nextOrdinal = (currentPhase.ordinal() + 1) % phases.length;
        currentPhase = phases[nextOrdinal];

        // エンドフェーズの後は手番を切り替える
        if (currentPhase == Phase.DRAW) {
            currentPlayer = currentPlayer.getOpponent();
            // 先攻プレイヤーに手番が戻ったら新しいターン（ラウンド）。
            if (currentPlayer == firstPlayer) {
                turnNumber++;
            }
        }
    }

    /**
     * デッキアウト（ライブラリ切れでドロー不可）した敗者を記録する。
     * ドローしようとして引けなかったプレイヤーの敗北＝相手の勝利。
     */
    public void markDeckOut(Player loser) {
        if (deckedOutLoser == null) {
            deckedOutLoser = loser;
        }
    }

    /** デッキアウトした敗者（いなければ null）。 */
    public Player getDeckedOutLoser() {
        return deckedOutLoser;
    }

    /**
     * ゲームが終了しているかどうかを判定する（コア0 または デッキアウト）。
     */
    public boolean isGameOver() {
        return player1Core == 0 || player2Core == 0 || deckedOutLoser != null;
    }

    /**
     * ゲームの勝者を返す（ゲーム終了時）。
     */
    public Player getWinner() {
        if (player1Core == 0) return Player.PLAYER2;
        if (player2Core == 0) return Player.PLAYER1;
        if (deckedOutLoser != null) return deckedOutLoser.getOpponent();
        return null;
    }
}
