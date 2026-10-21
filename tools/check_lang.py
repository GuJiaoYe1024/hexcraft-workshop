import json
zh = json.load(open(r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json", encoding="utf-8"))
en = json.load(open(r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\en_us.json", encoding="utf-8"))
names = [k for k in zh if k.startswith("hexcraft.hextech.") and k.endswith(".name")]
effects = [k for k in zh if k.startswith("hexcraft.effect.")]
print("zh hex_name_keys=%d effect_keys=%d" % (len(names), len(effects)))
print("en hex_name_keys=%d effect_keys=%d" % (len([k for k in en if k.startswith('hexcraft.hextech.') and k.endswith('.name')]), len([k for k in en if k.startswith('hexcraft.effect.')])))
