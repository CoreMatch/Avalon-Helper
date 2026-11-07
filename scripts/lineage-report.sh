#!/bin/bash
# lineage-report.sh —— 本仓对上游 RW-HPS 的谱系比对统计重建 (只读)
#
# 基准: upstream/master (RW-HPS 组织仓 master @ 239a7773).
# 产出: 文件级三分类 (与上游 blob 相同 / 有上游对应但被修改 / 无上游对应)
#       + 逐文件 blame 行归属 (上游作者行 / SamuelCheston 行) 汇总.
#
# 用法: 在本仓根目录执行  bash scripts/lineage-report.sh
# 输出: 打印汇总, 明细写入 /tmp/lineage-*.txt 与 /tmp/lineage-blame.csv
set -e

if ! git rev-parse --verify upstream/master >/dev/null 2>&1; then
    echo "错误: 缺少 upstream/master 引用 (upstream = https://github.com/RW-HPS/RW-HPS.git)" >&2
    exit 1
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

git ls-tree -r --name-only master | grep -E '\.(kt|java)$' > "$TMP/cur.txt"

# --- 1. 文件级三分类 (包名逆映射到上游路径) ---
: > "$TMP/identical.txt"; : > "$TMP/modified.txt"; : > "$TMP/new.txt"
while read -r f; do
    up=$(echo "$f" | sed 's|io/github/corematch/avalonhelper/asm|net/rwhps/asm|; t; s|io/github/corematch/avalonhelper|net/rwhps/server|')
    if [ "$up" = "$f" ] && ! git cat-file -e "upstream/master:$f" 2>/dev/null; then
        echo "$f" >> "$TMP/new.txt"; continue
    fi
    if ! git cat-file -e "upstream/master:$up" 2>/dev/null; then
        echo "$f" >> "$TMP/new.txt"; continue
    fi
    if [ "$(git rev-parse "master:$f")" = "$(git rev-parse "upstream/master:$up")" ]; then
        echo "$f" >> "$TMP/identical.txt"
    else
        echo "$f" >> "$TMP/modified.txt"
    fi
done < "$TMP/cur.txt"

echo "== 文件级三分类 =="
echo "identical(与上游 blob 相同): $(wc -l < "$TMP/identical.txt")"
echo "modified(有上游对应, 已修改): $(wc -l < "$TMP/modified.txt")"
echo "new(无上游对应): $(wc -l < "$TMP/new.txt")"

# --- 2. 逐文件 blame 行归属 ---
CSV=/tmp/lineage-blame.csv
echo "file,total,upstream,samuel,other" > "$CSV"
while read -r f; do
    git blame --line-porcelain master -- "$f" 2>/dev/null | grep '^author-mail' > "$TMP/mails.txt" || true
    total=$(wc -l < "$TMP/mails.txt")
    samuel=$(grep -c 'samuelcheston' "$TMP/mails.txt" || true)
    upstream=$(grep -cE 'dr@der\.kim|deng-rui|493505110|users\.noreply\.github|zerodegress|foxmail' "$TMP/mails.txt" || true)
    other=$((total - samuel - upstream)); [ $other -lt 0 ] && other=0
    echo "$f,$total,$upstream,$samuel,$other" >> "$CSV"
done < "$TMP/cur.txt"

echo "== 行归属汇总 =="
awk -F, 'NR>1 {t+=$2; u+=$3; s+=$4} END {
    printf "总行 %d, 上游作者行 %d (%.1f%%), SamuelCheston 行 %d (%.1f%%)\n", t, u, u*100/t, s, s*100/t}' "$CSV"
awk -F, 'NR>1 && $2>0 {r=$3/$2
    if (r>=0.9) a++; else if (r>=0.5) b++; else if (r>0) c++; else d++}
    END {printf "上游>=90%%: %d 文件; 50-90%%: %d; 1-50%%: %d; 0%%: %d\n", a, b, c, d}' "$CSV"
echo "== 模块级 =="
awk -F, 'NR>1 && $2>0 {split($1,p,"/"); m=p[1]; t[m]+=$2; u[m]+=$3; s[m]+=$4}
    END {for (k in t) printf "%s: 总行 %d, 上游 %.1f%%, Samuel %.1f%%\n", k, t[k], u[k]*100/t[k], s[k]*100/t[k]}' "$CSV"
echo "== 上游 0% 文件 (自有) =="
awk -F, 'NR>1 && $2>0 && $3==0 {print $1" ("$2"行)"}' "$CSV"
echo "明细: $CSV"
