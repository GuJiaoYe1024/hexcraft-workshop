package com.hexcraft.gametest;

import com.hexcraft.capability.PlayerData;
import com.hexcraft.event.HextechEvents;
import com.hexcraft.hextech.ComboRegistry;
import com.hexcraft.link.StorageLink;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** v0.9.27：第一组 GameTest。
 *
 *  <p>为什么从存储顺序开始：`StorageLink.order` 是纯 `int[]` 运算，不碰实体、不碰网络、
 *  不碰存档，是最适合做回归基线的一条。它同时也是 20-测试与验证流程里点名的
 *  「存储顺序」那一项。
 *
 *  <p>跑法：`gradlew runGameTestServer`（见 build.gradle 的 gameTestServer 段），
 *  或开发环境 `runServer` 起来后执行 `/test runall hexworkshop`。
 *  绿色信标=通过，红色=失败。
 *
 *  <p>约定：新增测试方法必须是 `static`，参数固定为 `GameTestHelper`。Forge 的
 *  `GameTestRegistry` 补丁对静态方法不做实例化，非静态才 `newInstance()`。
 */
@GameTestHolder("hexworkshop")
@PrefixGameTestTemplate(false)
public final class HexGameTests {

    private HexGameTests() {}

    /** 各项优先级互不相同时必须严格按数值升序：
     *  BD=3 SB=1 RS=2 BAG=0 → 玩家背包(0) 精妙背包(1) 精致存储(2) 超越维度(3) */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void storageOrderSortsByPriority(GameTestHelper helper) {
        int[] got = StorageLink.order(new int[]{3, 1, 2, 0});
        int[] want = {3, 1, 2, 0};
        for (int i = 0; i < want.length; i++) {
            if (got[i] != want[i]) {
                helper.fail("order mismatch at " + i + ": got " + got[i] + " want " + want[i]
                        + " (full=" + java.util.Arrays.toString(got) + ")");
                return;
            }
        }
        helper.succeed();
    }

    /** 四项优先级全相同时必须保持默认先后 0/1/2/3，不能因为排序把玩家背包提到前面 */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void storageOrderKeepsDefaultOnTies(GameTestHelper helper) {
        int[] got = StorageLink.order(new int[]{4, 4, 4, 4});
        for (int i = 0; i < 4; i++) {
            if (got[i] != i) {
                helper.fail("tie-break mismatch at " + i + ": got " + got[i]
                        + " (full=" + java.util.Arrays.toString(got) + ")");
                return;
            }
        }
        helper.succeed();
    }

    /** 默认配置 1/2/3/4 必须原样返回，玩家背包始终最后兜底 */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void storageOrderDefaultsUnchanged(GameTestHelper helper) {
        int[] got = StorageLink.order(new int[]{1, 2, 3, 4});
        for (int i = 0; i < 4; i++) {
            if (got[i] != i) {
                helper.fail("default order mismatch at " + i + ": got " + got[i]
                        + " (full=" + java.util.Arrays.toString(got) + ")");
                return;
            }
        }
        helper.succeed();
    }

    /** 把玩家背包调到最高优先级时，它必须排到第一位；其余三项仍按数值升序 */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void storageOrderHonoursBagFirst(GameTestHelper helper) {
        int[] got = StorageLink.order(new int[]{2, 3, 4, 0});
        int[] want = {3, 0, 1, 2};
        for (int i = 0; i < want.length; i++) {
            if (got[i] != want[i]) {
                helper.fail("bag-first mismatch at " + i + ": got " + got[i] + " want " + want[i]
                        + " (full=" + java.util.Arrays.toString(got) + ")");
                return;
            }
        }
        helper.succeed();
    }

    // ================= v0.9.27 叠层上限（规范 20 点名的第二项） =================

    /** 没有任何「叠层之源」成员时，个人叠层上限就是基础 200 */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void stackCapBaseIs200(GameTestHelper helper) {
        PlayerData empty = new PlayerData();
        int cap = HextechEvents.stackCapFor(empty);
        if (cap != 200) {
            helper.fail("empty loadout cap should be 200, got " + cap);
            return;
        }
        if (HextechEvents.stackCapFor(null) != 200) {
            helper.fail("null data should fall back to 200");
            return;
        }
        helper.succeed();
    }

    /** 叠层之源 2 件 = 档 1，上限应为 400 */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void stackCapTier1Is400(GameTestHelper helper) {
        PlayerData d = new PlayerData();
        String[] members = ComboRegistry.STACK_SOURCE.members();
        d.getHexes().add(new PlayerData.HexEntry(members[0], 1));
        d.getHexes().add(new PlayerData.HexEntry(members[1], 1));
        int cap = HextechEvents.stackCapFor(d);
        if (cap != 400) {
            helper.fail("tier1 (2 pieces) cap should be 400, got " + cap);
            return;
        }
        helper.succeed();
    }

    /** 叠层之源 6 件 = 档 3，上限应为 800 */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void stackCapTier3Is800(GameTestHelper helper) {
        PlayerData d = new PlayerData();
        String[] members = ComboRegistry.STACK_SOURCE.members();
        for (int i = 0; i < 6; i++) d.getHexes().add(new PlayerData.HexEntry(members[i], 1));
        int cap = HextechEvents.stackCapFor(d);
        if (cap != 800) {
            helper.fail("tier3 (6 pieces) cap should be 800, got " + cap);
            return;
        }
        helper.succeed();
    }

    /** 叠层之源全员 10 件 = 档 5，上限应为「无上限」（用 Integer.MAX_VALUE 表示） */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void stackCapTier5IsUnlimited(GameTestHelper helper) {
        PlayerData d = new PlayerData();
        for (String id : ComboRegistry.STACK_SOURCE.members()) {
            d.getHexes().add(new PlayerData.HexEntry(id, 1));
        }
        int cap = HextechEvents.stackCapFor(d);
        if (cap != Integer.MAX_VALUE) {
            helper.fail("tier5 (all pieces) cap should be unlimited, got " + cap);
            return;
        }
        helper.succeed();
    }

    // ================= v0.9.27 护盾钳制（规范 20 点名的第二项） =================

    /** `SHIELD_CAP` 默认开启时，护盾上限等于当前最大生命 */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void shieldCapEqualsMaxHealth(GameTestHelper helper) {
        Player mock = helper.makeMockPlayer();
        float max = mock.getMaxHealth();
        float cap = HextechEvents.shieldCap(mock);
        if (Math.abs(cap - max) > 0.001f) {
            helper.fail("shieldCap should equal maxHealth " + max + ", got " + cap);
            return;
        }
        helper.succeed();
    }

    /** 最大生命被抬高后，护盾上限必须跟着抬高（不能仍停在 20） */
    @GameTest(template = "platform", timeoutTicks = 10)
    public static void shieldCapTracksMaxHealthBoost(GameTestHelper helper) {
        Player mock = helper.makeMockPlayer();
        mock.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40.0);
        float cap = HextechEvents.shieldCap(mock);
        if (Math.abs(cap - 40.0f) > 0.001f) {
            helper.fail("after MAX_HEALTH=40 shieldCap should be 40, got " + cap);
            return;
        }
        helper.succeed();
    }
}
