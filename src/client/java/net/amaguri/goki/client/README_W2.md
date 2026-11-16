# W2 クライアント描画: 使用API・Mixin 確認結果

確認元: `/home/claude/apidump/mc/client.txt`, `common.txt`, `/home/claude/apidump/fabric/fabric.txt`, `/home/claude/fabric-ref`（26.2）。
ビルド不可環境のため未コンパイル（構文は javac のパースのみ確認、シンボル解決は未実施）。

## Mixin（goki.client.mixins.json）
| Mixin | ターゲット | 宣言クラス | 確認 |
|---|---|---|---|
| AvatarRendererMixin | `AvatarRenderer.extractRenderState(Avatar, AvatarRenderState, F)V` TAIL | AvatarRenderer（宣言あり。ディスクリプタは Fabric testmod と同形） | OK |
| AvatarRendererMixin | `AvatarRenderer.shouldRenderLayers(AvatarRenderState)Z` HEAD | AvatarRenderer（protected 宣言あり） | OK |
| AvatarRendererMixin | `AvatarRenderer.renderHand(PoseStack, SubmitNodeCollector, I, Identifier, ModelPart, Z)V` TAIL（private） | AvatarRenderer（renderRightHand/LeftHand が呼ぶ） | OK |
| LivingEntityRendererMixin | `LivingEntityRenderer.submit(LivingEntityRenderState, PoseStack, SubmitNodeCollector, CameraRenderState)V` HEAD | **LivingEntityRenderer**（AvatarRenderer は submit を宣言していない） | OK |
| LivingEntityRendererMixin | `LivingEntityRenderer.getRenderType(LivingEntityRenderState, Z, Z, Z)RenderType` HEAD cancel(null) | **LivingEntityRenderer**（AvatarRenderer は宣言していない） | OK |

## Fabric API
- `LivingEntityRenderLayerRegistrationCallback.EVENT` + `RegistrationHelper.register(RenderLayer)`（fabric-rendering-v1、AvatarRenderer 向けの使用例が testmod にある）
- `RenderStateDataKey` / `FabricRenderState.getData/setData`（EntityRenderState に注入。testmod の PigRendererMixin/AvatarRendererMixin と同じ使い方）
- `HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, id, HudElement)`（バニラ要素の描画条件 `Hud#isHidden` を継承 = F1 で消える）
- `ClientPlayNetworking.registerGlobalReceiver(TYPE, PlayPayloadHandler)`, `ClientPlayConnectionEvents.DISCONNECT`

## Minecraft 26.2 API（dump で存在確認済み）
- `RenderLayer<S,M>.submit(PoseStack, SubmitNodeCollector, int, S, float, float)`, `getParentModel()`
- `OrderedSubmitNodeCollector.submitModel(Model, S, PoseStack, RenderType, light, overlay, outlineColor, CrumblingOverlay)` / `submitModelPart(ModelPart, PoseStack, RenderType, light, overlay, TextureAtlasSprite)`
- `RenderTypes.entityTranslucent(Identifier)`, `entityCutout(Identifier)`, `LivingEntityRenderer.getOverlayCoords(state, f)`, `OverlayTexture.NO_OVERLAY`, `EntityRenderState.NO_OUTLINE`
- `LayerDefinition.create/bakeRoot`, `MeshDefinition`, `PartDefinition.addOrReplaceChild`, `CubeListBuilder.texOffs/addBox`, `PartPose.offsetAndRotation`, `Model.resetPose/setupAnim`, `ModelPart.translateAndRotate`, `PlayerModel.head`
- `GuiGraphicsExtractor.blit(RenderPipeline, Identifier, x, y, u, v, w, h, texW, texH)`, `pose()`(Matrix3x2fStack), `guiWidth()`, `RenderPipelines.GUI_TEXTURED`, `DeltaTracker.getRealtimeDeltaTicks()`
- `Options.getCameraType().isFirstPerson()`, `LivingEntity.walkAnimation`, `ResourceManager.getResource(Identifier)`

## 設計メモ
- ターゲット判定は `AvatarRenderer.extractRenderState` で `ClientGokiState.stageOf(uuid)` を render state（`GokiRender.STAGE`）に入れ、以降は全てそれを参照。ターゲット以外は 0 なので何も変わらない。
- ゴキブリ化: `LivingEntityRenderer.submit` の先頭でゴキブリを描き、バニラ本体は `getRenderType`=null（透明エンティティと同じ経路）と `shouldRenderLayers`=false で抑止。名前タグはバニラの流れでそのまま出る。
- 一人称の腕: `renderHand` の末尾で同じ ModelPart にオーバーレイを重ね描き（ローカルプレイヤー＝ターゲットの時のみ）。
- W3 のテクスチャ（OVERLAY / ANTENNA_HUD）は `GokiRender.textureExists`（リソース有無を2秒キャッシュ）で確認し、無ければ描かない。
- モデルは `LayerDefinition.bakeRoot()` を自前で呼ぶ（モデルレイヤー登録不要）。形状・UV は `agent/tasks/w2/tools/spec.py` から生成（CockroachModel/AntennaModel の createLayer）。
- 調整用定数: `AntennaHud.PEEK`（画面上端から見える px）/`SPREAD`、`CockroachRenderer.SCALE`。
