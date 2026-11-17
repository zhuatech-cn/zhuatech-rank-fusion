package cn.zhuatech.rankfusion;

import static cn.zhuatech.rankfusion.Checks.*;

import java.util.*;

/**
 * 按查询把融合 Top-K 与原始词法 Top-K 并排比较，列出新增、退出和保留的文档数。词法权重为零时，仍以输入的词法列表作为对照。 上海如静知华信息科技有限公司 ·
 * https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Inspection {
  /**
   * 有序结果，可表达无法估计的数值。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  private static Map<String, Object> fields(Object... pairs) {
    var row = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]);
    return row;
  }

  /**
   * 对已校验的输入生成复核证据。上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ ·
   * 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
   */
  @SuppressWarnings("unchecked")
  static Map<String, Object> evaluate(
      Map<String, Object> cfg, List<Map<String, Object>> rows, Map<String, Object> base) {
    var details = new ArrayList<Map<String, Object>>();

    var output = (List<Map<String, Object>>) base.get("rows");
    int k = ((Number) cfg.get("topK")).intValue(), changes = 0;
    var fused = new HashMap<String, List<String>>();
    for (var v : output)
      fused
          .computeIfAbsent((String) v.get("id"), x -> new ArrayList<>())
          .add((String) v.get("doc"));
    for (var r : rows) {
      var a = strings(r, "lexical");
      var old = new LinkedHashSet<>(a.subList(0, Math.min(k, a.size())));
      var cur = new LinkedHashSet<>(fused.get((String) r.get("id")));
      var plus = new ArrayList<>(cur);
      plus.removeAll(old);
      var minus = new ArrayList<>(old);
      minus.removeAll(cur);
      boolean changed = !new ArrayList<>(cur).equals(new ArrayList<>(old));
      if (changed) changes++;
      details.add(
          fields(
              "id",
              r.get("id"),
              "gained",
              plus,
              "displaced",
              minus,
              "retained",
              cur.size() - plus.size(),
              "orderChanged",
              changed));
    }
    return result(fields("changedQueries", changes), details);
  }
}
