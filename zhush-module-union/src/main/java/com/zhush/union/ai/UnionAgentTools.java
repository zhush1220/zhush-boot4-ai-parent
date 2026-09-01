package com.zhush.union.ai;

import tools.jackson.databind.ObjectMapper;
import com.zhush.agentscope.tool.AgentTool;
import com.zhush.agentscope.tool.ToolParam;
import com.zhush.union.query.UnionProductQuery;
import com.zhush.union.service.UnionCommissionRuleService;
import com.zhush.union.service.UnionProductService;
import com.zhush.union.vo.UnionProductVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 电商联盟业务工具：供五个业务智能体调用。
 *
 * <p>每个方法为一项原子能力，入参使用 {@link ToolParam} 说明，返回 JSON 文本给 LLM。
 * 敏感方法（@{@code sensitive=true}）触发后走人工审批。
 *
 * @author zhush
 */
@Component
public class UnionAgentTools {

    private static final Logger log = LoggerFactory.getLogger(UnionAgentTools.class);

    private final UnionProductService productService;
    private final UnionCommissionRuleService commissionRuleService;
    private final ObjectMapper objectMapper;

    public UnionAgentTools(UnionProductService productService,
                           UnionCommissionRuleService commissionRuleService,
                           ObjectMapper objectMapper) {
        this.productService = productService;
        this.commissionRuleService = commissionRuleService;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询高佣商品（按预算/类目过滤）。
     */
    @AgentTool(name = "queryHotGoods", description = "按价格预算与商品类目查询高佣候选商品")
    public String queryHotGoods(
            @ToolParam(name = "priceMax", description = "价格上限（元）") BigDecimal priceMax,
            @ToolParam(name = "category", description = "商品类目名称") String category) {
        UnionProductQuery query = new UnionProductQuery();
        query.setTitle(category);
        List<UnionProductVO> products = productService.getProductPage(query).getList();
        StringBuilder sb = new StringBuilder();
        for (UnionProductVO p : products) {
            if (p.getSalePrice() != null && priceMax != null && p.getSalePrice().compareTo(priceMax) > 0) {
                continue;
            }
            sb.append(formatProduct(p)).append("\n");
        }
        return sb.length() == 0 ? "未找到匹配商品" : sb.toString();
    }

    /**
     * 查询商品信息（用于文案生成）。
     */
    @AgentTool(name = "queryGoodsInfo", description = "按商品ID查询商品详情")
    public String queryGoodsInfo(
            @ToolParam(name = "productId", description = "联盟商品ID") Long productId) {
        UnionProductVO p = productService.getProduct(productId);
        return p == null ? "商品不存在" : formatProduct(p);
    }

    /**
     * 计算佣金（按售价与佣金率）。
     */
    @AgentTool(name = "calcCommission", description = "根据售价与佣金率估算佣金金额")
    public String calcCommission(
            @ToolParam(name = "salePrice", description = "售价（元）") BigDecimal salePrice,
            @ToolParam(name = "commissionRate", description = "佣金率（百分比，如 15 表示 15%）") BigDecimal commissionRate) {
        if (salePrice == null || commissionRate == null) {
            return "参数缺失";
        }
        BigDecimal amount = salePrice.multiply(commissionRate).divide(
                new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        return "预估佣金：" + amount + " 元";
    }

    /**
     * 查询推广政策/结算规则。
     */
    @AgentTool(name = "queryPolicyRule", description = "查询平台推广政策与结算规则")
    public String queryPolicyRule(
            @ToolParam(name = "keyword", description = "政策关键词（如 结算/返佣/合规）") String keyword) {
        String base = "常规推广政策：单次推广佣金按实际成交金额结算；收益次月 20 日可提现；"
                + "严禁虚假推广、刷单，违规将冻结佣金。";
        return keyword == null ? base : base + "\n（关键词：" + keyword + " 相关规则见结算说明）";
    }

    /**
     * 生成推广种草文案。
     */
    @AgentTool(name = "generateCopywriting", description = "根据商品卖点生成贴合平台的种草文案")
    public String generateCopywriting(
            @ToolParam(name = "productName", description = "商品名称") String productName,
            @ToolParam(name = "keywords", description = "核心卖点关键词（逗号分隔）") String keywords) {
        String name = productName == null ? "该商品" : productName;
        String kw = keywords == null ? "高性价比" : keywords;
        return "🎉 " + name + " 今日惊喜价！\n" + kw + "，你值得拥有。点击下单立享优惠～";
    }

    /**
     * 合规审核（敏感工具，触发人工审批）。
     */
    @AgentTool(name = "complianceCheck", sensitive = true,
            description = "审核文案/链接是否违反平台合规规范")
    public String complianceCheck(
            @ToolParam(name = "content", description = "待审核的文案或推广链接") String content) {
        if (content == null || content.isEmpty()) {
            return "审核内容为空";
        }
        boolean risky = content.contains("最") || content.contains("第一") || content.contains("绝对");
        return risky ? "检测到绝对化用语，存在违规风险，建议整改。" : "未发现明显违规项，可正常推广。";
    }

    /**
     * 查询经营洞察数据。
     */
    @AgentTool(name = "queryInsightStats", description = "查询推广订单、点击、佣金等经营数据")
    public String queryInsightStats(
            @ToolParam(name = "range", description = "统计周期：day/week/month") String range) {
        String period = range == null ? "近30天" : range;
        return period + " 推广概览：曝光 128,900 次，点击 3,421 次，成交 96 单，佣金合计 ¥1,238.50"
                + "。建议加大高佣金类目引流。";
    }

    // ================ 内部 ================

    private String formatProduct(UnionProductVO p) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(p.getId()).append("】").append(p.getTitle());
        if (p.getSalePrice() != null) {
            sb.append(" 售价¥").append(p.getSalePrice());
        }
        if (p.getCommissionAmount() != null) {
            sb.append(" 佣金¥").append(p.getCommissionAmount());
        }
        if (p.getShopName() != null) {
            sb.append(" 店铺：").append(p.getShopName());
        }
        return sb.toString();
    }
}
