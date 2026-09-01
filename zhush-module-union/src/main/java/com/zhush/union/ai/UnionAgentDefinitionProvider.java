package com.zhush.union.ai;

import com.zhush.agentscope.agent.AgentDefinition;
import com.zhush.agentscope.agent.AgentDefinitionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 电商联盟推广系统业务智能体定义。
 *
 * <p>五个场景智能体：选品推荐 / 政策咨询 / 文案生成 / 合规审核 / 经营洞察，
 * 复用 {@link UnionAgentTools} 提供的业务工具，供「AI 智能助手」选择器选用。
 *
 * @author zhush
 */
@Component
public class UnionAgentDefinitionProvider implements AgentDefinitionProvider {

    @Override
    public List<AgentDefinition> getDefinitions() {
        return List.of(
                AgentDefinition.builder()
                        .name("union-selector")
                        .description("电商选品推荐助手：按预算、类目、佣金率推荐高佣商品")
                        .systemPrompt("你是电商联盟选品专家。根据用户对价格上限、商品类目、佣金率的要求，" +
                                "调用工具查询候选商品，并给出推荐理由与佣金测算，回答简洁、给出明确建议。")
                        .model("qwen-max")
                        .toolNames(List.of("queryHotGoods", "calcCommission"))
                        .streaming(true)
                        .maxSteps(6)
                        .groupCode("biz")
                        .build(),
                AgentDefinition.builder()
                        .name("union-policy")
                        .description("电商联盟政策咨询助手：解答推广政策、结算规则、合规要求")
                        .systemPrompt("你是电商联盟政策顾问。基于平台推广政策与结算规则，" +
                                "针对用户的推广、结算、合规问题给出准确解答，用语专业且易于理解。")
                        .model("qwen-max")
                        .toolNames(List.of("queryPolicyRule"))
                        .streaming(true)
                        .maxSteps(6)
                        .groupCode("biz")
                        .build(),
                AgentDefinition.builder()
                        .name("union-copywriter")
                        .description("电商推广文案生成助手：根据商品生成种草文案与推广话术")
                        .systemPrompt("你是电商推广文案专家。根据商品信息与目标受众，生成有吸引力、" +
                                "符合平台规范的种草文案与推广话术，语气自然口语化。")
                        .model("qwen-max")
                        .toolNames(List.of("queryGoodsInfo", "generateCopywriting"))
                        .streaming(true)
                        .maxSteps(6)
                        .groupCode("biz")
                        .build(),
                AgentDefinition.builder()
                        .name("union-compliance")
                        .description("电商推广合规审核助手：审核文案/链接是否违反平台规则")
                        .systemPrompt("你是电商推广合规审核员。依据平台推广合规规范，对文案、商品、" +
                                "推广链接进行合规性判断，指出违规点并给出整改建议，态度严谨。")
                        .model("qwen-max")
                        .toolNames(List.of("complianceCheck"))
                        .streaming(true)
                        .maxSteps(6)
                        .groupCode("biz")
                        .build(),
                AgentDefinition.builder()
                        .name("union-insight")
                        .description("电商经营数据洞察助手：分析推广数据与经营指标")
                        .systemPrompt("你是电商经营数据分析师。根据推广订单、点击、佣金等数据，" +
                                "洞察经营趋势并给出优化建议，结论有数据支撑。")
                        .model("qwen-max")
                        .toolNames(List.of("queryInsightStats"))
                        .streaming(true)
                        .maxSteps(6)
                        .groupCode("biz")
                        .build()
        );
    }
}
