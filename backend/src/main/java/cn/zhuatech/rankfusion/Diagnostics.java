package cn.zhuatech.rankfusion;

import java.util.*;

/**
 * 逐查询比较词法与语义输入的 Top-K，报告交集、各自独有文档及 Jaccard 比例。任一路无输入时单独标记，不将缺失的排行算成零一致性。此指标不衡量召回准确性。 上海如静知华信息科技有限公司
 * · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。
 */
final class Diagnostics {
  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static Map<String, Object> map(Object... pairs) {
    var m = new LinkedHashMap<String, Object>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static double num(Map<String, Object> m, String k) {
    return ((Number) m.get(k)).doubleValue();
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  private static Double ratio(double a, double b) {
    return b == 0 ? null : a / b;
  }

  /** 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业授权或定制开发请微信添加微信号zhuatech或zhuatech2进行咨询。 */
  @SuppressWarnings("unchecked")
  static Map<String, Object> evaluate(
      Map<String, Object> cfg, List<Map<String, Object>> rows, Map<String, Object> base) {
    var metrics = (Map<String, Object>) base.get("metrics");
    var evidence = (List<Map<String, Object>>) base.get("rows");
    var details = new ArrayList<Map<String, Object>>();
    int k = (int) num(cfg, "topK"), available = 0;
    double total = 0;
    for (var r : rows) {
      var x = (List<String>) r.get("lexical");
      var y = (List<String>) r.get("semantic");
      var a = new LinkedHashSet<>(x.subList(0, Math.min(k, x.size())));
      var b = new LinkedHashSet<>(y.subList(0, Math.min(k, y.size())));
      var common = new LinkedHashSet<>(a);
      common.retainAll(b);
      var lex = new LinkedHashSet<>(a);
      lex.removeAll(b);
      var sem = new LinkedHashSet<>(b);
      sem.removeAll(a);
      boolean both = !a.isEmpty() && !b.isEmpty();
      Double j = both ? (double) common.size() / (common.size() + lex.size() + sem.size()) : null;
      if (both) {
        available++;
        total += j;
      }
      details.add(
          map(
              "id",
              r.get("id"),
              "hasBothRankers",
              both,
              "agreementAtK",
              j,
              "sharedDocs",
              List.copyOf(common),
              "lexOnlyDocs",
              List.copyOf(lex),
              "semOnlyDocs",
              List.copyOf(sem)));
    }
    return Checks.result(
        map(
            "meanAgreementAtK",
            ratio(total, available),
            "comparableQueries",
            available,
            "oneSidedQueries",
            rows.size() - available),
        details);
  }
}
