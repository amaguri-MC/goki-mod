#!/usr/bin/env python3
"""W1 API check: every MC/Fabric import exists in the apidump, every used member is declared where we use it,
and every @Inject/@Shadow target is declared by the mixin target class itself. Run: python3 -I verify_api.py"""
import re, sys, glob

SRC = "/home/claude/goki-mod/src/main/java/net/amaguri/goki"
DUMPS = ["/home/claude/apidump/mc/common.txt", "/home/claude/apidump/mc/client.txt", "/home/claude/apidump/fabric/fabric.txt"]

# class -> body text of its declaration block
classes = {}
for path in DUMPS:
    cur, lines = None, []
    for line in open(path, encoding="utf-8", errors="replace"):
        m = re.match(r"^(?:public |final |abstract |protected |static |sealed |non-sealed )*(?:class|interface|enum|record) ([^ <]+)", line)
        if m and not line.startswith(" "):
            cur, lines = m.group(1), [line]
            classes.setdefault(cur, lines)
            continue
        if cur is not None:
            lines.append(line)
            if line.startswith("}"):
                cur = None

def body(c):
    return "".join(classes[c]) if c in classes else None

fails = 0
def check(ok, msg):
    global fails
    print(("OK   " if ok else "FAIL ") + msg)
    if not ok:
        fails += 1

# 1. imports
imports = set()
for f in glob.glob(SRC + "/server/*.java") + glob.glob(SRC + "/mixin/*.java"):
    for m in re.finditer(r"^import ((?:net\.minecraft|net\.fabricmc|com\.mojang\.(?:brigadier|serialization))[\w.]*);", open(f, encoding="utf-8").read(), re.M):
        imports.add(m.group(1))
for imp in sorted(imports):
    if imp.startswith("com.mojang"):
        continue  # libraries are not in the dump
    if imp.startswith("net.amaguri"):
        continue
    # nested classes are imported with '.', dump uses '$'
    cands = [imp] + [imp[:i] + "$" + imp[i+1:] for i in range(len(imp)) if imp[i] == "."][-3:]
    check(any(c in classes for c in cands), "import " + imp)

# 2. members used (class, regex that must match inside that class's own declaration)
members = [
 ("net.minecraft.world.level.block.state.BlockBehaviour", r"protected .*VoxelShape getShape\(.*BlockState, .*BlockGetter, .*BlockPos, .*CollisionContext\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour", r"protected .*VoxelShape getCollisionShape\("),
 ("net.minecraft.world.level.block.state.BlockBehaviour", r"protected void entityInside\(.*BlockState, .*Level, .*BlockPos, .*Entity, .*InsideBlockEffectApplier, boolean\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour", r"protected float getDestroyProgress\(.*BlockState, .*Player, .*BlockGetter, .*BlockPos\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour", r"static <B extends .*> .*MapCodec<B> simpleCodec"),
 ("net.minecraft.world.level.block.Block", r"public void playerDestroy\(.*Level, .*Player, .*BlockPos, .*BlockState, .*BlockEntity, .*ItemStack\)"),
 ("net.minecraft.world.level.block.Block", r"protected .*MapCodec<\? extends .*Block> codec\(\)"),
 ("net.minecraft.world.level.block.Block", r"public static .*VoxelShape box\(double, double, double, double, double, double\)"),
 ("net.minecraft.world.level.block.Block", r"public static void popResource\(.*Level, .*BlockPos, .*ItemStack\)"),
 ("net.minecraft.world.entity.Entity", r"public void makeStuckInBlock\(.*BlockState, .*Vec3\)"),
 ("net.minecraft.world.entity.Entity", r"public boolean horizontalCollision|public boolean horizontalCollision;"),
 ("net.minecraft.world.entity.Entity", r"public boolean isShiftKeyDown\(\)"),
 ("net.minecraft.world.entity.Entity", r"public .*RandomSource getRandom\(\)"),
 ("net.minecraft.world.entity.Entity", r"public .*BlockPos blockPosition\(\)"),
 ("net.minecraft.world.entity.LivingEntity", r"public float zza;"),
 ("net.minecraft.world.entity.LivingEntity", r"public .*MobEffectInstance getEffect\("),
 ("net.minecraft.world.entity.LivingEntity", r"public boolean addEffect\(|public final boolean addEffect\("),
 ("net.minecraft.world.entity.LivingEntity", r"public boolean removeEffect\("),
 ("net.minecraft.world.entity.LivingEntity", r"public .*AttributeInstance getAttribute\("),
 ("net.minecraft.world.entity.LivingEntity", r"public boolean isDeadOrDying\(\)"),
 ("net.minecraft.world.entity.LivingEntity", r"public float getHealth\(\)"),
 ("net.minecraft.world.entity.LivingEntity", r"public final float getMaxHealth\(\)"),
 ("net.minecraft.world.entity.player.Player", r"public boolean onClimbable\(\)"),
 ("net.minecraft.world.entity.player.Player", r"public .*String getPlainTextName\(\)"),
 ("net.minecraft.world.entity.player.Player", r"public boolean isCreative\(\)"),
 ("net.minecraft.world.food.FoodData", r"private float exhaustionLevel;"),
 ("net.minecraft.world.food.FoodData", r"public void tick\(.*ServerPlayer\)"),
 ("net.minecraft.server.level.ServerPlayer", r"public .*Input getLastClientInput\(\)"),
 ("net.minecraft.server.level.ServerPlayer", r"public .*ServerLevel level\(\)"),
 ("net.minecraft.server.level.ServerPlayer", r"public .*ServerGamePacketListenerImpl connection;"),
 ("net.minecraft.server.level.ServerPlayer", r"public .*PlayerAdvancements getAdvancements\(\)"),
 ("net.minecraft.server.level.ServerPlayer", r"public void sendSystemMessage\(.*Component\)"),
 ("net.minecraft.world.entity.player.Input", r"public boolean forward\(\)"),
 ("net.minecraft.server.network.ServerCommonPacketListenerImpl", r"public void send\(.*Packet<\?>\)"),
 ("net.minecraft.server.network.ServerGamePacketListenerImpl", r"public .*ServerPlayer getPlayer\(\)"),
 ("net.minecraft.server.level.ServerLevel", r"public java.util.List<\? extends .*EnderDragon> getDragons\(\)"),
 ("net.minecraft.world.level.Level", r"public void playSound\(.*Entity, double, double, double, .*SoundEvent, .*SoundSource, float, float\)"),
 ("net.minecraft.world.level.Level", r"public boolean setBlockAndUpdate\("),
 ("net.minecraft.world.level.Level", r"public boolean removeBlock\(.*BlockPos, boolean\)"),
 ("net.minecraft.world.level.Level", r"public static final .*ResourceKey<.*Level> END;"),
 ("net.minecraft.world.level.Level", r"public .*ResourceKey<.*Level> dimension\(\)"),
 ("net.minecraft.world.level.Level", r"public boolean isClientSide\(\)"),
 ("net.minecraft.resources.ResourceKey", r"public .*Identifier identifier\(\)"),
 ("net.minecraft.resources.ResourceKey", r"public static <T> .*ResourceKey<T> create\(.*ResourceKey<\? extends .*Registry<T>>, .*Identifier\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase", r"public boolean isAir\(\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase", r"public boolean canBeReplaced\(\)"),
 ("net.minecraft.core.TypedInstance", r"public default boolean is\(T\)"),
 ("net.minecraft.core.BlockPos", r"public .*BlockPos immutable\(\)"),
 ("net.minecraft.core.BlockPos", r"public static .*Iterable<.*BlockPos> betweenClosed\(.*BlockPos, .*BlockPos\)"),
 ("net.minecraft.core.BlockPos", r"public static .*BlockPos containing\(.*Position\)"),
 ("net.minecraft.core.Registry", r"public abstract .*Holder<T> wrapAsHolder\(T\)"),
 ("net.minecraft.core.Registry", r"static <V, T extends V> T register\(.*Registry<V>, .*ResourceKey<V>, T\)"),
 ("net.minecraft.core.registries.BuiltInRegistries", r"Registry<.*SoundEvent> SOUND_EVENT;"),
 ("net.minecraft.core.registries.Registries", r"ResourceKey<.*Registry<.*Block>> BLOCK;"),
 ("net.minecraft.core.registries.Registries", r"ResourceKey<.*Registry<.*Item>> ITEM;"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties setId\("),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties noLootTable\(\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties speedFactor\(float\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties jumpFactor\(float\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties strength\(float\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties noOcclusion\(\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties pushReaction\("),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties mapColor\(.*MapColor\)"),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"Properties sound\("),
 ("net.minecraft.world.level.block.state.BlockBehaviour$Properties", r"static .*Properties of\(\)"),
 ("net.minecraft.world.item.Item$Properties", r"Properties setId\(.*ResourceKey<.*Item>\)"),
 ("net.minecraft.world.item.Item$Properties", r"Properties useBlockDescriptionPrefix\(\)"),
 ("net.minecraft.world.item.BlockItem", r"public .*BlockItem\(.*Block, .*Item\$Properties\)"),
 ("net.minecraft.world.entity.ai.attributes.AttributeInstance", r"public .*AttributeModifier getModifier\(.*Identifier\)"),
 ("net.minecraft.world.entity.ai.attributes.AttributeInstance", r"public boolean removeModifier\(.*Identifier\)"),
 ("net.minecraft.world.entity.ai.attributes.AttributeInstance", r"public void addOrReplacePermanentModifier\("),
 ("net.minecraft.world.entity.ai.attributes.AttributeModifier", r"public .*AttributeModifier\(.*Identifier, double, .*Operation\)"),
 ("net.minecraft.world.entity.ai.attributes.AttributeModifier", r"public double amount\(\)"),
 ("net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation", r"ADD_MULTIPLIED_BASE;"),
 ("net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation", r"ADD_VALUE;"),
 ("net.minecraft.world.entity.ai.attributes.Attributes", r"Attribute> MOVEMENT_SPEED;"),
 ("net.minecraft.world.entity.ai.attributes.Attributes", r"Attribute> ARMOR;"),
 ("net.minecraft.world.entity.ai.attributes.Attributes", r"Attribute> ATTACK_DAMAGE;"),
 ("net.minecraft.world.entity.ai.attributes.Attributes", r"Attribute> MAX_HEALTH;"),
 ("net.minecraft.world.effect.MobEffects", r"FIRE_RESISTANCE;"),
 ("net.minecraft.world.effect.MobEffects", r"NIGHT_VISION;"),
 ("net.minecraft.world.effect.MobEffectInstance", r"public .*MobEffectInstance\(.*Holder<.*MobEffect>, int, int, boolean, boolean, boolean\)"),
 ("net.minecraft.world.effect.MobEffectInstance", r"public boolean isInfiniteDuration\(\)"),
 ("net.minecraft.world.effect.MobEffectInstance", r"public int getDuration\(\)"),
 ("net.minecraft.world.effect.MobEffectInstance", r"public boolean isAmbient\(\)"),
 ("net.minecraft.world.effect.MobEffectInstance", r"public boolean isVisible\(\)"),
 ("net.minecraft.world.effect.MobEffectInstance", r"public boolean showIcon\(\)"),
 ("net.minecraft.network.protocol.game.ClientboundSoundPacket", r"public .*ClientboundSoundPacket\(.*Holder<.*SoundEvent>, .*SoundSource, double, double, double, float, float, long\)"),
 ("net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket", r"public .*ClientboundSetTitleTextPacket\(.*Component\)"),
 ("net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket", r"public .*ClientboundSetSubtitleTextPacket\(.*Component\)"),
 ("net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket", r"public .*ClientboundSetTitlesAnimationPacket\(int, int, int\)"),
 ("net.minecraft.sounds.SoundEvents", r"SoundEvent PLAYER_LEVELUP;"),
 ("net.minecraft.sounds.SoundEvents", r"SoundEvent SILVERFISH_STEP;"),
 ("net.minecraft.sounds.SoundSource", r"SoundSource PLAYERS;"),
 ("net.minecraft.network.chat.Component", r"static .*MutableComponent literal\(.*String\)"),
 ("net.minecraft.network.chat.Component", r"static .*MutableComponent empty\(\)"),
 ("net.minecraft.network.chat.MutableComponent", r"public .*MutableComponent withStyle\(.*ChatFormatting\.\.\.\)"),
 ("net.minecraft.ChatFormatting", r"ChatFormatting GOLD;"),
 ("net.minecraft.ChatFormatting", r"ChatFormatting BOLD;"),
 ("net.minecraft.ChatFormatting", r"ChatFormatting YELLOW;"),
 ("net.minecraft.ChatFormatting", r"ChatFormatting GREEN;"),
 ("net.minecraft.world.level.material.MapColor", r"MapColor COLOR_BROWN;"),
 ("net.minecraft.world.level.block.SoundType", r"SoundType SLIME_BLOCK;"),
 ("net.minecraft.world.level.material.PushReaction", r"PushReaction DESTROY;"),
 ("net.minecraft.world.level.storage.LevelResource", r"LevelResource ROOT;"),
 ("net.minecraft.server.MinecraftServer", r"public .*Path getWorldPath\(.*LevelResource\)"),
 ("net.minecraft.server.MinecraftServer", r"public int getTickCount\(\)"),
 ("net.minecraft.server.MinecraftServer", r"public .*PlayerList getPlayerList\(\)"),
 ("net.minecraft.server.MinecraftServer", r"public .*ServerAdvancementManager getAdvancements\(\)"),
 ("net.minecraft.server.players.PlayerList", r"public .*ServerPlayer getPlayer\(java.util.UUID\)"),
 ("net.minecraft.server.players.PlayerList", r"public java.util.List<.*ServerPlayer> getPlayers\(\)"),
 ("net.minecraft.server.ServerAdvancementManager", r"public .*AdvancementHolder get\(.*Identifier\)"),
 ("net.minecraft.server.PlayerAdvancements", r"public .*AdvancementProgress getOrStartProgress\(.*AdvancementHolder\)"),
 ("net.minecraft.advancements.AdvancementProgress", r"public boolean isDone\(\)"),
 ("net.minecraft.resources.Identifier", r"public static .*Identifier tryParse\(.*String\)"),
 ("net.minecraft.world.phys.Vec3", r"public .*Vec3\(double, double, double\)"),
 ("net.minecraft.world.item.ItemStack", r"public .*ItemStack\(.*ItemLike\)"),
 ("net.minecraft.commands.Commands", r"public static .*literal\(.*String\)"),
 ("net.minecraft.commands.Commands", r"public static .*argument\(.*String, .*ArgumentType<T>\)"),
 ("net.minecraft.commands.Commands", r"PermissionCheck LEVEL_GAMEMASTERS;"),
 ("net.minecraft.commands.Commands", r"public static .*PermissionProviderCheck<T> hasPermission\(.*PermissionCheck\)"),
 ("net.minecraft.commands.CommandSourceStack", r"public void sendSuccess\(.*Supplier<.*Component>, boolean\)"),
 ("net.minecraft.commands.CommandSourceStack", r"public void sendFailure\(.*Component\)"),
 ("net.minecraft.commands.CommandSourceStack", r"public .*ServerPlayer getPlayer\(\)"),
 ("net.minecraft.commands.CommandSourceStack", r"public .*ServerLevel getLevel\(\)"),
 ("net.minecraft.commands.CommandSourceStack", r"public .*Vec3 getPosition\(\)"),
 ("net.minecraft.commands.CommandSourceStack", r"public .*MinecraftServer getServer\(\)"),
 ("net.minecraft.commands.arguments.EntityArgument", r"public static .*EntityArgument player\(\)"),
 ("net.minecraft.commands.arguments.EntityArgument", r"public static .*ServerPlayer getPlayer\(.*CommandContext<.*CommandSourceStack>, .*String\)"),
 ("net.minecraft.world.level.block.entity.BlockEntity", None),
 ("net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking", r"public static void send\(.*ServerPlayer, .*CustomPacketPayload\)"),
 ("net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents", r"Event<.*ServerPlayConnectionEvents\$Join> JOIN;"),
 ("net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents$Join", r"onPlayReady\(.*ServerGamePacketListenerImpl, .*PacketSender, .*MinecraftServer\)"),
 ("net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents", r"Event<.*ServerPlayerEvents\$AfterRespawn> AFTER_RESPAWN;"),
 ("net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents$AfterRespawn", r"afterRespawn\(.*ServerPlayer, .*ServerPlayer, boolean\)"),
 ("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", r"ServerStarting> SERVER_STARTING;"),
 ("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", r"ServerStopped> SERVER_STOPPED;"),
 ("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents", r"EndTick> END_SERVER_TICK;"),
 ("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents$EndTick", r"onEndTick\(.*MinecraftServer\)"),
 ("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback", r"register\(.*CommandDispatcher<.*CommandSourceStack>, .*CommandBuildContext, .*Commands\$CommandSelection\)"),
]
for cls, rx in members:
    b = body(cls)
    if b is None:
        check(False, f"class {cls} not found")
    elif rx is None:
        check(True, f"class {cls}")
    else:
        check(re.search(rx, b) is not None, f"{cls.split('.')[-1]}: {rx[:70]}")

# 3. mixin targets must be declared by the target class itself
src = open(SRC + "/mixin/PlayerClimbMixin.java", encoding="utf-8").read()
check(re.search(r'@Mixin\(Player\.class\)', src) and "onClimbable" in src, "PlayerClimbMixin targets Player.onClimbable")
check(re.search(r"public boolean onClimbable\(\)", body("net.minecraft.world.entity.player.Player")) is not None, "  -> Player declares onClimbable itself")
src = open(SRC + "/mixin/FoodDataMixin.java", encoding="utf-8").read()
check("@Mixin(FoodData.class)" in src, "FoodDataMixin targets FoodData")
check(re.search(r"public void tick\(.*ServerPlayer\)", body("net.minecraft.world.food.FoodData")) is not None, "  -> FoodData declares tick(ServerPlayer) itself")
check(re.search(r"private float exhaustionLevel;", body("net.minecraft.world.food.FoodData")) is not None, "  -> FoodData declares exhaustionLevel itself")

print("\nFAILED: %d" % fails if fails else "\nALL OK")
sys.exit(1 if fails else 0)
