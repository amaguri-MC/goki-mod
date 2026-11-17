# W1: サーバー側（段階進行・強化・とりもち・コマンド）

## 構成
| ファイル | 役割 |
|---|---|
| `GokiServer` | `init()` 入口。ブロック登録、`GokiHooks.SERVER_STAGE` 設定、各イベント・コマンド登録 |
| `GokiState` | target(UUID/無し)+stage をワールド直下 `goki_state.json` に保存（変更のたび即保存）。変更時・参加時に `StageSyncPayload` を全員へ送信 |
| `StageLogic` / `StageText` | MC 非依存の純ロジック（段階計算、罠条件、強化量、文言） |
| `Progress` | 1秒おき。誰か1人でも `ADVANCEMENTS[n]` 達成済みなら stage を n まで上げる（下げない・飛ばしOK） |
| `LevelUp` | title「LEVEL UP!」+共通subtitle+`player.levelup`。ターゲットだけに裏強化メッセージ（チャット） |
| `Enhancements` | 全員の強化（属性 `goki:speed/armor/attack/max_health`、火炎耐性・暗視は隠しエフェクトを付け直し） |
| `Rustle` | S6+ ターゲットが歩くと、歩いた距離ベースでたまに silverfish の足音（高ピッチ・小音量、ターゲット自身には鳴らさない） |
| `SprayClouds` | 殺虫スプレー本体。ターゲット設定中だけ有効。雲の変換/生成、毎tickのターゲット即死判定、竜の狙い寄せ判定 |
| `GokiCommands` | `/goki`（権限2） |
| `mixin/DragonFireballMixin` | `DragonFireball.onHit` … 着弾で殺虫スプレーの雲（効果なし）。ターゲット未設定なら何もしない |
| `mixin/DragonFlamingPhaseMixin` | `DragonSittingFlamingPhase.doServerTick` … 止まり木ブレスの雲をスプレー化。終盤は雲をターゲットの位置へ |
| `mixin/DragonStrafeFocusMixin` | `DragonStrafePlayerPhase.doServerTick` … 終盤は火の玉の狙い(`attackTarget`)をターゲットに差し替え |
| `mixin/PlayerClimbMixin` | `Player.onClimbable` … 壁よじ登り（S4+、共通コード） |
| `mixin/FoodDataMixin` | `FoodData.tick` … 満腹度・隠し満腹度を減らさない（S2+、サーバー） |

## 設計メモ
- 強化は「欲しい状態を stage から毎回計算し、持っている状態と比べて差分だけ直す」方式。固定 Identifier なので二重付与しない。
  参加時・リスポーン直後（`ServerPlayerEvents.AFTER_RESPAWN`）・毎秒に実行。死亡・ログアウト・次元移動・牛乳で消えても1秒以内に戻る。
  属性は permanent（プレイヤーデータに保存。ログイン時に最大体力+4が先に載るので体力が切られない）。
  S4/S6 のエフェクトは ambient・パーティクル無し・アイコン無し、残り600tick未満で1200tickに付け直し（暗視のチカチカ閾値200より上）。
- `player.tickCount` は使っていない（間隔は `server.getTickCount()` と歩行距離）。
- 壁登り: `horizontalCollision` かつ前進キー（クライアント=`zza>0`、サーバー=`getLastClientInput().forward()`）で `onClimbable()=true`。
  あとは vanilla の梯子処理（衝突中 y=+0.2、スニークで静止、落下は最大0.15）。ターゲット判定は `GokiHooks.stageOf`。
- 殺虫スプレー（エンドラ戦のオチ）: **ターゲット設定中だけ**有効（`SprayClouds.active()`）。未設定なら全 mixin が何もせず完全バニラ。
  - 火の玉: `DragonFireball.onHit` を差し替え。バニラの「ダメージ雲＋ドラゴンブレスの破裂パーティクル」を出さず、効果ゼロのスプレー雲（`GokiParticles.SPRAY`）を作って火の玉を消す（火の玉自体は元々ダメージなし）。
  - ブレス: 止まり木のブレス雲を `convert`（パーティクルを SPRAY、`PotionContents.EMPTY` で効果を全消去）。
  - よって雲は誰にも効果・ダメージを与えない。唯一の反応は `SprayClouds.tick`（毎tick）で、ターゲットが雲（バニラ同様に水平距離≦半径＋AABB交差、待機中は除く）に触れたら即死。
  - 死因 `goki:insecticide`（`data/goki/damage_type/insecticide.json`）。`data/minecraft/tags/damage_type/` の bypasses_armor / bypasses_effects / bypasses_enchantments / bypasses_resistance / bypasses_shield / bypasses_invulnerability（トーテム・クリエイティブ無敵も貫通）/ no_knockback に追加。`hurtServer(1e6)` で殺し、まだ生きていれば `kill()`。
  - 終盤の必中: 竜HP比 ≦ `SPRAY_FOCUS_HEALTH_RATIO` かつターゲットがエンドにいる間だけ、ストラフ（火の玉）フェーズの `attackTarget` をターゲットに差し替え、ブレス雲は生成直後にターゲットの位置へ移す（竜の向きは回さない）。フェーズ選択（ストラフに入る確率など）はバニラのまま。
- 段階を飛ばした時: title/音は最後の1回。ターゲットのチャットだけは通過した段階の分を全部出す（取りこぼし防止）。

## コマンド（結果は実行者にだけ。`sendSuccess(..., false)`）
`/goki target <player>` / `/goki target clear` / `/goki stage <0..MAX(8)>`（上げる時は演出あり）/
`/goki spray`（ターゲットの位置にスプレーの雲＝触れたら即死。撮り直し用）/ `/goki status`（target・stage・スプレーON/OFF・竜HPと狙い撃ち中か）

## API 確認
スクリプト `/home/claude/agent/tasks/w1b/verify_api.py`（goki-mod の外）を実行すると、apidump（common/client/fabric）に対して以下を機械チェックする:
1. 自分のコードの `net.minecraft.*` / `net.fabricmc.*` import が全て存在
2. 使った全メソッド・フィールド・コンストラクタ・イベントが「その型自身の宣言」にあること（署名ごと）
3. 全 mixin の `@Inject`/`@Shadow` 先が、ターゲットクラス自身の宣言であること
4. `TRAPPED`/`torimochi` 等の参照が server・mixin に残っていないこと、goki-mod/src に .py が無いこと

```
cd /tmp && python3 -I /home/claude/agent/tasks/w1b/verify_api.py   -> 204 項目 ALL OK
```

mixin ターゲット: Player.onClimbable / FoodData.tick・exhaustionLevel / DragonFireball.onHit / DragonSittingFlamingPhase.doServerTick・flame / DragonStrafePlayerPhase.doServerTick・attackTarget。全て各クラス自身が宣言（フェーズ基底クラスの `dragon` フィールドは触らず、`level.getDragons()` から `getPhaseManager().getCurrentPhase()==this` で竜を特定）。

## データ形式
`damage_type` JSON は fabric-ref（26.2）のテスト用 `damage_type/*.json` と同じキー（`message_id`, `exhaustion`, `scaling`）。vanilla の damage_type 本体は apidump/data に無いので、タグ（`tags/damage_type/*.json`）のみ apidump と照合した。
lang は `/home/claude/agent/lang/w1.ja_jp.json` / `w1.en_us.json`（`death.attack.insecticide` と `.player`）。

## 未検証
- ここではビルド・実機不可。竜のフェーズ内部（ブレス雲が `flame` 経由で作られる／所有者が竜）は javap の宣言からの推定。ブレス雲を `flame` フィールドで掴めない場合はスプレー化されず vanilla のまま（その時は要調整）。
- 調整値: `SprayClouds.spawn`（半径3・持続）、`StageLogic.RUSTLE_*`、`Enhancements.EFFECT_TICKS`。

## 複数ターゲット（追記）
- `GokiState` は target を複数持つ（全員が同じ stage を共有）。`goki_state.json` は `targets:[{uuid,name,elytraGiven}]`。旧形式（単数 `target`）は読み込み時に移行。
- `/goki target add|remove <players>`（セレクタ可）/ `target clear` / `target list` / `target <player>`（= add）。`spray` は全オンライン生存ターゲット、`status` は全員表示。
- 強化・満腹・壁登り・カサカサ・エリトラ（付与済みフラグはターゲットごと）・チャット・スプレー即死は全ターゲットに適用。終盤の狙いは「エンドにいるターゲットのうち竜に最も近い者」。
