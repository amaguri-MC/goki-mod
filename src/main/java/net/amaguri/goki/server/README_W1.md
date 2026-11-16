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
| `TorimochiBlock` / `GokiBlocks` | `goki:torimochi`（ブロック+BlockItem） |
| `TrapController` | S7 でターゲットがエンドかつ竜HP比 ≤ `TRAP_DRAGON_HEALTH_RATIO` → 足元に設置、stage=8、ターゲットにだけ subtitle |
| `GokiCommands` | `/goki`（権限2） |
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
- とりもち: 当たり判定 1/16、選択枠 2/16。`entityInside` で `makeStuckInBlock(0.02,0.02,0.02)`（クモの巣は 0.25,0.05,0.25）＋
  `speedFactor 0.1`＋`jumpFactor 0`。硬さ2.0 = 素手で60tick(3秒)。ターゲットは `getDestroyProgress` が 0（サーバー=UUID一致、クライアント=`stageOf>0`）。
  loot table JSON は無いので `playerDestroy` でアイテム自身を落とす（クリエでは落とさない）。
- stage 8 でターゲットが抜けても stage は 8 のまま（何もしない）。
- 段階を飛ばした時: title/音は最後の1回。ターゲットのチャットだけは通過した段階の分を全部出す（取りこぼし防止）。

## コマンド（結果は実行者にだけ。`sendSuccess(..., false)`）
`/goki target <player>` / `/goki target clear` / `/goki stage <0..8>`（上げる時は演出あり、8は罠を置かず値だけ）/
`/goki trap`（ターゲット在席なら足元に設置+stage8+subtitle、不在なら実行者の足元に置くだけ）/
`/goki free`（実行者とターゲットの周囲半径8のとりもち撤去）/ `/goki status`

## API 確認
スクリプト `verify_api.py`（同じフォルダ）を実行すると、apidump（common/client/fabric）に対して以下を機械チェックする:
1. 自分のコードの `net.minecraft.*` / `net.fabricmc.*` import が全て存在
2. 使った全メソッド・フィールド・コンストラクタ・イベントが「その型自身の宣言」にあること（正規表現で署名ごと）
3. mixin の `@Inject`/`@Shadow` 先が、ターゲットクラス自身の宣言であること

```
cd /tmp && python3 -I /home/claude/goki-mod/src/main/java/net/amaguri/goki/server/verify_api.py
-> 201 項目 ALL OK
```

mixin ターゲットの grep 結果（`/home/claude/apidump/mc/common.txt`）:
```
Player      (public abstract class net.minecraft.world.entity.player.Player)  7900:  public boolean onClimbable();
              ※ LivingEntity にも同名があるが Player が上書き宣言しているので Player を狙う（LivingEntity を狙うと Player の実装に届かない）
FoodData    (public class net.minecraft.world.food.FoodData)
              804:  private float exhaustionLevel;     <- @Shadow
              810:  public void tick(net.minecraft.server.level.ServerPlayer);   <- @Inject HEAD
```
LivingEntity.zza（`public float zza`）、Entity.horizontalCollision（`public boolean`）は mixin ではなく通常のフィールド参照。

## 未検証（ここではビルド不可）
- コンパイル・実機は未実施。シグネチャは上記スクリプトで apidump と突き合わせ済みだが、挙動（壁登りの感触、とりもちの固さ、音量）は実機調整が要る。
- 調整値: `TorimochiBlock.STUCK` / `GokiBlocks`(strength, speedFactor) / `StageLogic.RUSTLE_*` / `Enhancements.EFFECT_TICKS`。
