package cn.zhuatech.rankfusion;

import static cn.zhuatech.rankfusion.Checks.*;

import java.util.*;

/**
 * 为每个查询汇合词法和向量排行，按加权倒数排名融合打分，保留两个输入名次以解释最终排序。 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
 * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Algorithm {
  /**
   * 不连接外部向量库。输入须为已去重的文档标识列表；同分按文档标识稳定排序。 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  static Map<String, Object> compute(Map<String, Object> cfg, List<Map<String, Object>> rows) {
    var base = calculate(cfg, rows);
    var enriched = new LinkedHashMap<String, Object>(base);
    enriched.put("inspection", Inspection.evaluate(cfg, rows, base));
    enriched.put("diagnostics", Diagnostics.evaluate(cfg, rows, base));
    return enriched;
  }

  /**
   * 保留原始计算口径。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  private static Map<String, Object> calculate(
      Map<String, Object> cfg, List<Map<String, Object>> rows) {
    must(rows != null && !rows.isEmpty() && rows.size() <= 200, "需要 1–200 行");
    unique(rows);
    var evidence = new ArrayList<Map<String, Object>>();
    int k = integer(cfg, "constant", 1, 1000), top = integer(cfg, "topK", 1, 100);
    double w = num(cfg, "lexWeight", 0, 1);
    for (var r : rows) {
      var a = strings(r, "lexical");
      var b = strings(r, "semantic");
      var d = new TreeMap<String, int[]>();
      for (int i = 0; w > 0 && i < a.size(); i++)
        d.computeIfAbsent(a.get(i), v -> new int[2])[0] = i + 1;
      for (int i = 0; w < 1 && i < b.size(); i++)
        d.computeIfAbsent(b.get(i), v -> new int[2])[1] = i + 1;
      must(!d.isEmpty(), "启用的排行为空");
      var scores = new TreeMap<String, Double>();
      for (var e : d.entrySet()) {
        int[] v = e.getValue();
        scores.put(
            e.getKey(), (v[0] == 0 ? 0 : w / (k + v[0])) + (v[1] == 0 ? 0 : (1 - w) / (k + v[1])));
      }
      var docs = new ArrayList<>(scores.keySet());
      docs.sort(
          Comparator.<String>comparingDouble(scores::get)
              .reversed()
              .thenComparing(Comparator.naturalOrder()));
      for (int i = 0; i < Math.min(top, docs.size()); i++) {
        String key = docs.get(i);
        evidence.add(
            Map.of(
                "id",
                r.get("id"),
                "doc",
                key,
                "rank",
                i + 1,
                "score",
                scores.get(key),
                "lexRank",
                d.get(key)[0],
                "semRank",
                d.get(key)[1]));
      }
    }
    return result(Map.of("queries", rows.size(), "returned", evidence.size()), evidence);
  }
}
