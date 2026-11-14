package com.hexcraft.util;

/** v0.9.27 P3：tick 开销测量（**纯内存累计，不写盘、不上传**）。
 *
 *  <p>测两个数：
 *  <ul>
 *    <li>`serverTick` —— 整个服务端 tick 的时长（含原版与其它 mod），反映「整体有没有超 50ms」；</li>
 *    <li>`playerTick` —— 所有 PlayerTick 处理器的总时长（本 mod 与其它 mod 共用同一条事件总线），
 *        按「单玩家单次」折算，反映「玩家一多会不会拖慢」。</li>
 *  </ul>
 *
 *  <p>实现方式零侵入：在事件总线的最早优先级（HIGHEST）取时间戳，最晚优先级（LOWEST）结算，
 *  两者之差就是「这条事件上所有处理器」的耗时，不需要逐个类改代码。
 *  代价是没法把耗时归到某一个事件类，但足以回答清单里那一问——「tick 有没有尖峰」。
 *  若真出现尖峰，再加细粒度埋点定位。
 */
public final class TickProfiler {

    private static long serverStartNs;
    private static long serverTotalNs;
    private static long serverMaxNs;
    private static long serverTicks;

    private static long playerStartNs;
    private static long playerTotalNs;
    private static long playerMaxNs;
    private static long playerTicks;

    private TickProfiler() {}

    public static void serverStart() {
        serverStartNs = System.nanoTime();
    }

    public static void serverEnd() {
        long s = serverStartNs;
        if (s == 0) return;
        serverStartNs = 0;
        long d = System.nanoTime() - s;
        serverTicks++;
        serverTotalNs += d;
        if (d > serverMaxNs) serverMaxNs = d;
    }

    public static void playerStart() {
        playerStartNs = System.nanoTime();
    }

    public static void playerEnd() {
        long s = playerStartNs;
        if (s == 0) return;
        playerStartNs = 0;
        long d = System.nanoTime() - s;
        playerTicks++;
        playerTotalNs += d;
        if (d > playerMaxNs) playerMaxNs = d;
    }

    private static double ms(long ns) { return ns / 1_000_000.0; }

    /** 一行总览：平均 / 最大 / 样本数 */
    public static synchronized String summary() {
        double sAvg = serverTicks == 0 ? 0.0 : ms(serverTotalNs) / serverTicks;
        double pAvg = playerTicks == 0 ? 0.0 : ms(playerTotalNs) / playerTicks;
        return String.format(
                "serverTick avg=%.2fms max=%.2fms n=%d | playerTick avg=%.3fms max=%.3fms n=%d",
                sAvg, ms(serverMaxNs), serverTicks,
                pAvg, ms(playerMaxNs), playerTicks);
    }

    /** 单次数值是否已经超过给定毫秒阈值（用于快速判断有没有尖峰） */
    public static synchronized boolean serverSpiked(double thresholdMs) {
        return ms(serverMaxNs) > thresholdMs;
    }

    public static synchronized boolean playerSpiked(double thresholdMs) {
        return ms(playerMaxNs) > thresholdMs;
    }

    public static synchronized void reset() {
        serverStartNs = 0;
        serverTotalNs = 0;
        serverMaxNs = 0;
        serverTicks = 0;
        playerStartNs = 0;
        playerTotalNs = 0;
        playerMaxNs = 0;
        playerTicks = 0;
    }
}
