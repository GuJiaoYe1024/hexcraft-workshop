package com.hexcraft.dlc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.hexcraft.Hexcraft;
import com.hexcraft.data.EquipmentDataLoader;
import com.hexcraft.data.HexDataLoader;
import com.hexcraft.data.WorldHexDataLoader;
import com.hexcraft.hextech.HextechRegistry;
import com.hexcraft.hextech.WorldHexRegistry;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * v0.6.2 DLC 外接内容包系统。
 * 目录：游戏版本根目录 hexworkshop_dlc/packs（与 mods / config / tacz 平级）。
 * 支持【解压文件夹】与【zip 压缩包】两种格式；zip 采用内存流读取 dlc_meta.json 与 data JSON，
 * 不解压写入磁盘。dlc_index.json 记录启用状态（zip 以完整文件名作为 ID，文件夹以目录名作为 ID）。
 * isCodeDlc=true 的包仅展示（提示放入 mods 文件夹重启），不加载任何 Java 类代码。
 * 本体完全不依赖该目录：目录缺失自动创建，删除不影响本体游玩。
 */
public final class DlcManager {

    private DlcManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACKS_REL = "hexworkshop_dlc/packs";
    private static final String INDEX_REL = "hexworkshop_dlc/dlc_index.json";

    private static Path packsDir() {
        return FMLPaths.GAMEDIR.get().resolve(PACKS_REL);
    }

    private static Path indexFile() {
        return FMLPaths.GAMEDIR.get().resolve(INDEX_REL);
    }

    /** 单个 DLC 包信息（UI 展示 + 加载依据） */
    public static class PackInfo {
        public String id;            // zip=完整文件名 / 文件夹=目录名
        public String name;
        public String description;
        public String version;
        public List<String> dependencies = new ArrayList<>();
        public boolean isCodeDlc;
        public boolean enabled;
        public String error = "";    // 无效包提示（如 zip 根缺 dlc_meta.json）
    }

    private static final Map<String, PackInfo> PACKS = new LinkedHashMap<>();

    /** v0.9.3 修正6：内容归属——内容 id（海克斯/装备/世界海克斯）→ 所属 DLC 包 id（抽取启停按包控制） */
    private static final Map<String, String> CONTENT_OWNER = new java.util.HashMap<>();

    public static String packOf(String contentId) {
        return CONTENT_OWNER.get(contentId);
    }

    /** 外接包内容启停：返回 Boolean（true=所属包已启用；false=关闭/无效）；非外接包内容返回 null（走 source 判定） */
    public static Boolean packEnabledOf(String contentId) {
        String pid = CONTENT_OWNER.get(contentId);
        if (pid == null) return null;
        PackInfo p = PACKS.get(pid);
        return p != null && p.enabled && p.error.isEmpty();
    }

    /** 是否为外接包内容（用于抽取分类：DLC 按钮下归入 DLC） */
    public static boolean isPackContent(String contentId) {
        return CONTENT_OWNER.containsKey(contentId);
    }

    /** 启动 / reload dlc 时扫描 packs 目录（文件夹 + *.zip），读取元数据与启用状态 */
    public static void scan() {
        com.hexcraft.hextech.HextechRegistry.invalidatePoolCache(); // v0.9.5：内容变更清空抽取池缓存（下次抽取重建）
        PACKS.clear();
        CONTENT_OWNER.clear();
        Path dir = packsDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Hexcraft.LOGGER.error("Hexcraft-DLC: 无法创建 packs 目录", e);
            return;
        }
        ensureExamplePack(dir); // v0.9.3：目录为空时写入内置示例包（模板，玩家可复制参考）
        Map<String, Boolean> index = readIndex();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    loadFolder(p, index);
                } else if (p.getFileName().toString().endsWith(".zip")) {
                    loadZip(p, index);
                }
            }
        } catch (IOException e) {
            Hexcraft.LOGGER.error("Hexcraft-DLC: 扫描 packs 失败", e);
        }
        Hexcraft.LOGGER.info("Hexcraft-DLC: 扫描完成，共 {} 个外接包", PACKS.size());
    }

    /** v0.9.3 内置示例包：packs 目录为空时写入两个 zip 模板（原版/逆原分开，命名 [中文]英文），
     *  玩家可直接查看/复制了解 DLC 包结构；放入任意自己的包后不再生成。
     *  v0.9.3 修正2：原文件夹形式 example_pack 废弃，若存在则先删除再按 zip 双包生成。 */
    private static void ensureExamplePack(Path dir) {
        // 清理上一版文件夹模板（递归删除，防止残留空目录被误判为无效包）
        Path legacy = dir.resolve("example_pack");
        if (Files.isDirectory(legacy)) {
            try (java.util.stream.Stream<Path> walk = Files.walk(legacy)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
        }
        // 目录里只有示例 zip（固定两名）或空目录时：写入/重写两个示例包并默认启用；
        // 存在玩家自己的包（其他文件/非空文件夹）则不干预
        String[] exZips = {"[内置DLC·原版回归]VanillaRevival.zip", "[内置DLC·逆原]NiyuanHunt.zip"};
        boolean onlySamples = true;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    boolean empty = true;
                    try (DirectoryStream<Path> sub = Files.newDirectoryStream(p)) {
                        empty = !sub.iterator().hasNext();
                    } catch (IOException e) { continue; }
                    if (!empty) { onlySamples = false; break; }
                } else {
                    String fn = p.getFileName().toString();
                    if (!fn.equals(exZips[0]) && !fn.equals(exZips[1])) { onlySamples = false; break; }
                }
            }
        } catch (IOException e) {
            return;
        }
        if (!onlySamples) return;
        try {
            Files.createDirectories(dir);
            for (String z : exZips) Files.deleteIfExists(dir.resolve(z));
            writeExampleZip(dir, exZips[0], EXAMPLE_META_VANILLA, "data/hex/example_vanilla_demo.json", EXAMPLE_HEX_VANILLA);
            writeExampleZip(dir, exZips[1], EXAMPLE_META_NIYUAN, "data/hex/example_niyuan_demo.json", EXAMPLE_HEX_NIYUAN);
            // v0.9.3 修正5：示例包默认启用（用户要求 DLC 默认开启），写入启用索引
            Map<String, Boolean> idx = readIndex();
            idx.put(exZips[0], true);
            idx.put(exZips[1], true);
            writeIndex0(idx);
            Hexcraft.LOGGER.info("Hexcraft-DLC: 已写入示例包 zip（原版+逆原，模板，默认启用，可删除）");
        } catch (IOException e) {
            Hexcraft.LOGGER.warn("Hexcraft-DLC: 示例包写入失败", e);
        }
    }

    private static void writeExampleZip(Path dir, String fileName, String meta, String hexPath, String hex) throws IOException {
        Path zip = dir.resolve(fileName);
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("dlc_meta.json"));
            zos.write(meta.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new java.util.zip.ZipEntry(hexPath));
            zos.write(hex.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /** 示例包 dlc_meta.json（真实可被扫描加载，v0.9.3 起默认启用，玩家在 DLC 页可关闭） */
    private static final String EXAMPLE_META_VANILLA = """
            {
              "name": "内置示例包·原版向",
              "description": "内置示例包，默认启用，可直接删除；复制本包并替换 data/hex 内海克斯即可自制内容包",
              "version": "1.0.0"
            }
            """;

    private static final String EXAMPLE_META_NIYUAN = """
            {
              "name": "内置DLC·逆原 示例",
              "description": "内置示例包，默认启用，可直接删除；自制方式与原版示例包相同",
              "version": "1.0.0"
            }
            """;

    /** 示例海克斯 1（原版向）：展示基础结构（稀有度/权重/来源/效果键事件，事件用合法键 melee_power=近战增伤） */
    private static final String EXAMPLE_HEX_VANILLA = """
            {
              "rarity": "SILVER",
              "weight": 10,
              "source": "example_vanilla",
              "event": "melee_power:0.2"
            }
            """;

    /** 示例海克斯 2（逆原向）：展示黄金档与吸血效果键（lifesteal=吸血） */
    private static final String EXAMPLE_HEX_NIYUAN = """
            {
              "rarity": "GOLD",
              "weight": 6,
              "source": "example_niyuan",
              "event": "lifesteal:0.05"
            }
            """;

    private static void loadFolder(Path dir, Map<String, Boolean> index) {
        String id = dir.getFileName().toString();
        PackInfo info = new PackInfo();
        info.id = id;
        info.enabled = index.getOrDefault(id, false);
        Path meta = dir.resolve("dlc_meta.json");
        if (!Files.exists(meta)) {
            info.error = "无效DLC，缺少 dlc_meta.json";
            PACKS.put(id, info);
            return;
        }
        try {
            JsonObject m = GSON.fromJson(Files.readString(meta, StandardCharsets.UTF_8), JsonObject.class);
            fillMeta(info, m);
        } catch (Exception e) {
            info.error = "dlc_meta.json 解析失败";
        }
        PACKS.put(id, info);
    }

    private static void loadZip(Path zip, Map<String, Boolean> index) {
        String id = zip.getFileName().toString(); // zip 以完整文件名作为唯一 ID
        PackInfo info = new PackInfo();
        info.id = id;
        info.enabled = index.getOrDefault(id, false);
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            ZipEntry metaEntry = zf.getEntry("dlc_meta.json");
            if (metaEntry == null || metaEntry.isDirectory()) {
                info.error = "无效DLC，压缩包内部结构错误"; // 根缺 dlc_meta.json → 判定无效
                PACKS.put(id, info);
                return;
            }
            JsonObject m = GSON.fromJson(readZipEntry(zf, metaEntry), JsonObject.class);
            fillMeta(info, m);
        } catch (IOException | RuntimeException e) {
            info.error = "压缩包无法读取";
        }
        PACKS.put(id, info);
    }

    private static void fillMeta(PackInfo info, JsonObject m) {
        info.name = m.has("name") ? m.get("name").getAsString() : info.id;
        info.description = m.has("description") ? m.get("description").getAsString() : "";
        info.version = m.has("version") ? m.get("version").getAsString() : "1.0.0";
        if (m.has("dependencies")) {
            m.getAsJsonArray("dependencies").forEach(e -> info.dependencies.add(e.getAsString()));
        }
        info.isCodeDlc = m.has("isCodeDlc") && m.get("isCodeDlc").getAsBoolean();
    }

    private static String readZipEntry(ZipFile zf, ZipEntry entry) throws IOException {
        try (InputStream in = zf.getInputStream(entry);
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) >= 0) sb.append(buf, 0, n);
            return sb.toString();
        }
    }

    // ================= 启用状态（dlc_index.json）=================

    private static Map<String, Boolean> readIndex() {
        Map<String, Boolean> index = new LinkedHashMap<>();
        try {
            Path f = indexFile();
            if (Files.exists(f)) {
                JsonObject root = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class);
                root.keySet().forEach(k -> index.put(k, root.get(k).getAsBoolean()));
            }
        } catch (Exception e) {
            Hexcraft.LOGGER.warn("Hexcraft-DLC: dlc_index.json 读取失败，按默认关闭处理");
        }
        return index;
    }

    private static void writeIndex() {
        Map<String, Boolean> index = new LinkedHashMap<>();
        PACKS.values().forEach(p -> index.put(p.id, p.enabled));
        writeIndex0(index);
    }

    /** v0.9.3 修正5：按给定索引写文件（示例包默认启用用：scan 早期 PACKS 未加载，直接合并写） */
    private static void writeIndex0(Map<String, Boolean> index) {
        JsonObject root = new JsonObject();
        index.forEach(root::addProperty);
        try {
            Path f = indexFile();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Hexcraft.LOGGER.error("Hexcraft-DLC: dlc_index.json 写入失败", e);
        }
    }

    /** UI 开关：修改启用状态并持久化（zip 以文件名、文件夹以目录名为 ID） */
    public static void setEnabled(String id, boolean enabled) {
        PackInfo p = PACKS.get(id);
        if (p == null) return;
        p.enabled = enabled;
        writeIndex();
        com.hexcraft.hextech.HextechRegistry.invalidatePoolCache(); // v0.9.5：包启停即时影响抽取池
    }

    /** 全部包（UI 列表用） */
    public static List<PackInfo> packs() {
        return new ArrayList<>(PACKS.values());
    }

    /** 启用且无错误的纯数据包（zip 根有 dlc_meta.json、非 code DLC） */
    private static List<PackInfo> activeDataPacks() {
        List<PackInfo> out = new ArrayList<>();
        for (PackInfo p : PACKS.values()) {
            if (p.enabled && !p.isCodeDlc && p.error.isEmpty()) out.add(p);
        }
        return out;
    }

    // ================= 数据合并（服务端加载 / reload dlc）=================

    /** DLC 数据目录结构：data/hex、data/equipment、data/world_hex（zip 内存流读取，不解压落盘） */
    public static int applyHexPacks() {
        int n = 0;
        for (PackInfo p : activeDataPacks()) {
            for (Map.Entry<String, JsonObject> e : readPackJson(p, "data/hex").entrySet()) {
                try {
                    HextechRegistry.put(e.getKey(), HexDataLoader.parse(e.getKey(), e.getValue()));
                    CONTENT_OWNER.put(e.getKey(), p.id);
                    n++;
                } catch (Exception ex) {
                    Hexcraft.LOGGER.error("Hexcraft-DLC: 包 {} 海克斯 {} 加载失败", p.id, e.getKey(), ex);
                }
            }
        }
        return n;
    }

    public static int applyEquipPacks() {
        int n = 0;
        for (PackInfo p : activeDataPacks()) {
            for (Map.Entry<String, JsonObject> e : readPackJson(p, "data/equipment").entrySet()) {
                try {
                    HextechRegistry.putEquip(e.getKey(), EquipmentDataLoader.parse(e.getKey(), e.getValue()));
                    CONTENT_OWNER.put(e.getKey(), p.id);
                    n++;
                } catch (Exception ex) {
                    Hexcraft.LOGGER.error("Hexcraft-DLC: 包 {} 装备 {} 加载失败", p.id, e.getKey(), ex);
                }
            }
        }
        return n;
    }

    public static int applyWorldHexPacks() {
        int n = 0;
        for (PackInfo p : activeDataPacks()) {
            for (Map.Entry<String, JsonObject> e : readPackJson(p, "data/world_hex").entrySet()) {
                try {
                    WorldHexRegistry.put(e.getKey(), WorldHexDataLoader.parse(e.getKey(), e.getValue()));
                    CONTENT_OWNER.put(e.getKey(), p.id);
                    n++;
                } catch (Exception ex) {
                    Hexcraft.LOGGER.error("Hexcraft-DLC: 包 {} 世界海克斯 {} 加载失败", p.id, e.getKey(), ex);
                }
            }
        }
        return n;
    }

    /** 读取启用包的 data/<sub>/*.json（zip 内存流 / 文件夹直读），返回 id → JsonObject */
    private static Map<String, JsonObject> readPackJson(PackInfo p, String sub) {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        Path packs = packsDir();
        Path target = packs.resolve(p.id);
        try {
            if (Files.isDirectory(target)) {
                Path dataDir = target.resolve(sub);
                if (!Files.isDirectory(dataDir)) return out;
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dataDir, "*.json")) {
                    for (Path jf : ds) {
                        String id = jf.getFileName().toString().replace(".json", "");
                        out.put(id, GSON.fromJson(Files.readString(jf, StandardCharsets.UTF_8), JsonObject.class));
                    }
                }
            } else if (p.id.endsWith(".zip")) {
                try (ZipFile zf = new ZipFile(target.toFile())) {
                    java.util.Enumeration<? extends ZipEntry> en = zf.entries();
                    String prefix = sub + "/";
                    while (en.hasMoreElements()) {
                        ZipEntry ze = en.nextElement();
                        if (ze.isDirectory() || !ze.getName().startsWith(prefix) || !ze.getName().endsWith(".json")) continue;
                        String id = ze.getName().substring(prefix.length()).replace(".json", "");
                        if (id.contains("/")) continue; // 只读子目录第一层 JSON
                        out.put(id, GSON.fromJson(readZipEntry(zf, ze), JsonObject.class));
                    }
                }
            }
        } catch (Exception e) {
            Hexcraft.LOGGER.error("Hexcraft-DLC: 读取包 {} 的 {} 失败", p.id, sub, e);
        }
        return out;
    }

    // ================= reload dlc 入口 =================

    /** /hexcraft reload dlc：重新扫描 + 重新应用全部模块（服务端即时生效，客户端界面由 S2C 刷新） */
    public static void reloadDlc() {
        scan();
        applyHexPacks();
        applyEquipPacks();
        applyWorldHexPacks();
        Hexcraft.LOGGER.info("Hexcraft-DLC: reload dlc 完成，海克斯={} 装备={} 世界海克斯={}",
                HextechRegistry.all().size(), HextechRegistry.allEquips().size(), WorldHexRegistry.all().size());
    }
}
