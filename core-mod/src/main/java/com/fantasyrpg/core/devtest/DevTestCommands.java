package com.fantasyrpg.core.devtest;

import com.fantasyrpg.core.network.RpgNetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * 개발자 전용 명령 {@code /fantasyrpgtest armor ...} (권한 2, 테스트 장비가 켜져 있을 때만 등록).
 * <ul>
 *   <li>{@code list} — 등록된 테스트 장비</li>
 *   <li>{@code give <직업> <등급> [부위]} — 지급 (부위 생략 = 그 직업·등급의 첫 항목)</li>
 *   <li>{@code equip <직업> <등급> [부위]} — 그 칸의 기존 장비를 가방으로(가득 차면 발밑) 옮기고 착용</li>
 *   <li>{@code clear} — 가방·갑옷 칸·보조 손의 테스트 장비 모두 지움</li>
 *   <li>{@code reload} — 명령을 친 플레이어의 클라이언트가 리소스를 다시 읽음 (F3+T 와 같음)</li>
 * </ul>
 */
public final class DevTestCommands {
    private DevTestCommands() {
    }

    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fantasyrpgtest").requires(s -> s.hasPermission(2))
                .then(Commands.literal("armor")
                        .then(Commands.literal("list").executes(DevTestCommands::list))
                        .then(Commands.literal("give").then(pick(false)))
                        .then(Commands.literal("equip").then(pick(true)))
                        .then(Commands.literal("clear").executes(DevTestCommands::clear))
                        .then(Commands.literal("reload").executes(DevTestCommands::reload))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> pick(boolean equip) {
        return Commands.argument("class", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(DevTest.manifest().classes(), b))
                .then(Commands.argument("grade", IntegerArgumentType.integer(1, 10))
                        .executes(c -> give(c, null, equip))
                        .then(Commands.argument("slot", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(DevTestManifest.SLOTS, b))
                                .executes(c -> give(c, StringArgumentType.getString(c, "slot"), equip))));
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        DevTestManifest m = DevTest.manifest();
        c.getSource().sendSuccess(() -> Component.literal("[개발 테스트] 테스트 장비 " + m.entries().size() + "개 (목록: "
                + DevTest.source() + ", 오류 " + m.errors().size() + "건)"), false);
        for (DevTestManifest.Entry e : m.entries()) {
            c.getSource().sendSuccess(() -> Component.literal(" " + m.namespace() + ":" + e.id() + " — " + e.cls() + " " + e.grade()
                    + "급 " + e.slot() + ", 리비전 " + (e.revision().isEmpty() ? "-" : e.revision()) + ", 모델 " + e.model()), false);
        }
        for (String err : m.errors()) c.getSource().sendFailure(Component.literal(" 목록 오류: " + err));
        return m.entries().size();
    }

    private static int give(CommandContext<CommandSourceStack> c, String slot, boolean equip) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String cls = StringArgumentType.getString(c, "class");
        int grade = IntegerArgumentType.getInteger(c, "grade");
        Optional<DevTestManifest.Entry> found = DevTest.manifest().find(cls, grade, slot);
        if (found.isEmpty()) {
            c.getSource().sendFailure(Component.literal("[개발 테스트] " + cls + " " + grade + "급" + (slot == null ? "" : " " + slot)
                    + " 테스트 장비가 목록에 없음 — /fantasyrpgtest armor list"));
            return 0;
        }
        var holder = DevTestRegistry.items().get(found.get().id());
        if (holder == null) {
            c.getSource().sendFailure(Component.literal("[개발 테스트] " + found.get().id() + " 는 이번 실행에 등록되지 않음 — 목록을 바꿨다면 게임을 다시 켜기"));
            return 0;
        }
        ItemStack stack = new ItemStack(holder.get());
        if (equip) {
            EquipmentSlot es = holder.get().getEquipmentSlot();
            ItemStack old = p.getItemBySlot(es);
            if (!old.isEmpty() && !p.getInventory().add(old.copy())) p.drop(old.copy(), false);
            p.setItemSlot(es, stack);
            c.getSource().sendSuccess(() -> Component.literal("[개발 테스트] " + found.get().id() + " 착용 ("
                    + (old.isEmpty() ? "빈 칸" : "기존 장비는 가방으로") + ")"), false);
        } else {
            if (!p.getInventory().add(stack)) p.drop(stack, false);
            c.getSource().sendSuccess(() -> Component.literal("[개발 테스트] " + found.get().id() + " 지급"), false);
        }
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Inventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof DevTestArmorItem) {
                inv.setItem(i, ItemStack.EMPTY);
                n++;
            }
        }
        int removed = n;
        c.getSource().sendSuccess(() -> Component.literal("[개발 테스트] 테스트 장비 " + removed + "개 지움"), false);
        return removed;
    }

    private static int reload(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        PacketDistributor.sendToPlayer(p, new RpgNetwork.DevReload());
        c.getSource().sendSuccess(() -> Component.literal("[개발 테스트] 클라이언트 리소스 다시 읽기 요청 (모델·텍스처·아이콘·애니메이션). "
                + "목록(아이템 추가)·Java 는 게임을 다시 켜야 함"), false);
        return 1;
    }
}
