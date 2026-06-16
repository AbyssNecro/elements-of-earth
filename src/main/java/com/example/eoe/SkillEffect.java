package com.example.eoe;

/** スキルの効果種別。 */
public enum SkillEffect {
    NONE,
    DESTROY_ENEMY_ENTITY, // 相手エンティティ1体を選択し破壊
    EQUIP,                // 場のエンティティ1体に装備する
    REVIVE_EQUIP,         // 自レムナントのエンティティを再生し、これを装備する
    MODIFY_ATK,           // 場のエンティティ1体のATKをターン終了まで増減する
    DESTROY_EQUIPMENT,    // 装備されている装備カード1枚を破壊する（ピュリファイ・ブリーズ）
    GRANT_IMMUNITY,       // 火山属性1体に、ターン終了まで他のカードの効果を受けない耐性を付与（ボルカニック・シールド）
    GRANT_TRAMPLE,        // エンティティ1体に、バトル破壊時に破壊したエンティティのATK分コアダメージを付与（ホワイトフェザー）
    RECYCLE,              // 自レムナントの宇宙以外コスト3以下を1体ハンドへ＋追加コストでもう1体を再生（リサイクル・アース）
    DISABLE_GUARD,        // ターン終了まで、相手は【ガード】を発動できない（アーマー・バインド）。対象なし
    DESTROY_ALL_ENTITIES, // エリアのエンティティ全てを破壊する（カタストロフ・レイ／aスキル）。対象なし
    ZERO_ATTACKER_ATK     // 攻撃宣言したエンティティのATKをターン終了まで0にする（コズミック・ノヴァ／リアクティブ専用aスキル）
}
