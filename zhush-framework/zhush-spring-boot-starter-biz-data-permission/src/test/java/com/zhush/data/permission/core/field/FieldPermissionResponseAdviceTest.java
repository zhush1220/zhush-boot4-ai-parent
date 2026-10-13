package com.zhush.data.permission.core.field;

import com.zhush.common.response.PageResult;
import com.zhush.common.response.Result;
import com.zhush.data.permission.annotation.FieldPermission;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 字段级数据权限过滤器测试
 * <p>
 * 覆盖置空/脱敏策略、Result 与 PageResult 拆包、嵌套 VO 递归深度、
 * 基本类型跳过、以及权限判定异常时的降级行为（不阻断响应）。
 * 测试 VO 采用公有字段，避免依赖 setter。
 *
 * @author zhush
 */
class FieldPermissionResponseAdviceTest {

    private static final String MOBILE_PERMISSION = "demo:secrecy:mobile";

    private static final String ID_CARD_PERMISSION = "demo:secrecy:id-card";

    private static final String SALARY_PERMISSION = "demo:secrecy:salary";

    private static final String AGE_PERMISSION = "demo:secrecy:age";

    private static final String COUNT_PERMISSION = "demo:secrecy:count";

    private static final String CHAIN_PERMISSION = "demo:chain:secret";

    /**
     * 以「拥有权限集合」构造过滤器
     */
    private static FieldPermissionResponseAdvice adviceOf(String... ownedPermissions) {
        Set<String> owned = Set.of(ownedPermissions);
        return new FieldPermissionResponseAdvice(owned::contains);
    }

    private static Object write(FieldPermissionResponseAdvice advice, Object body) {
        return advice.beforeBodyWrite(body, null, null, null, null, null);
    }

    private static SecrecyVO secrecyVo() {
        SecrecyVO vo = new SecrecyVO();
        vo.nickname = "张三";
        vo.mobile = "13800001111";
        vo.idCard = "330101199001010011";
        vo.salaryRemark = "月薪 2 万";
        vo.age = 30;
        vo.count = 5;
        return vo;
    }

    @Test
    void clearAndMaskWhenNoPermission() {
        SecrecyVO vo = secrecyVo();

        write(adviceOf(), Result.success(vo));

        // 无权限列被处理，非受控列保持原值
        assertThat(vo.nickname).isEqualTo("张三");
        assertThat(vo.mobile).isNull();
        assertThat(vo.idCard).isNull();
        assertThat(vo.salaryRemark).isEqualTo("保密");
        assertThat(vo.age).isNull();
    }

    @Test
    void keepValuesWhenPermissionGranted() {
        SecrecyVO vo = secrecyVo();

        write(adviceOf(MOBILE_PERMISSION, ID_CARD_PERMISSION, SALARY_PERMISSION, AGE_PERMISSION, COUNT_PERMISSION),
                Result.success(vo));

        assertThat(vo.mobile).isEqualTo("13800001111");
        assertThat(vo.idCard).isEqualTo("330101199001010011");
        assertThat(vo.salaryRemark).isEqualTo("月薪 2 万");
        assertThat(vo.age).isEqualTo(30);
        assertThat(vo.count).isEqualTo(5);
    }

    @Test
    void processEveryItemInPageResult() {
        SecrecyVO first = secrecyVo();
        SecrecyVO second = secrecyVo();

        write(adviceOf(MOBILE_PERMISSION), PageResult.of(List.of(first, second), 2L));

        // 手机号有权限保持原值，身份证无权限逐条置空
        assertThat(first.mobile).isEqualTo("13800001111");
        assertThat(second.mobile).isEqualTo("13800001111");
        assertThat(first.idCard).isNull();
        assertThat(second.idCard).isNull();
    }

    @Test
    void processRawCollectionBody() {
        SecrecyVO vo = secrecyVo();

        write(adviceOf(), List.of(vo));

        assertThat(vo.mobile).isNull();
    }

    @Test
    void maskOnNonStringFieldFallsBackToClear() {
        SecrecyVO vo = secrecyVo();

        write(adviceOf(), vo);

        // Integer 为包装类型可置空；MASK 策略对非 String 字段等同置空
        assertThat(vo.age).isNull();
    }

    @Test
    void skipPrimitiveField() {
        SecrecyVO vo = secrecyVo();

        write(adviceOf(), vo);

        // 基本类型无法置空，保持原值并打印告警（规范：受控字段用包装类型）
        assertThat(vo.count).isEqualTo(5);
    }

    @Test
    void processNestedVoAndCollectionFields() {
        SecrecyVO inner = secrecyVo();
        SecrecyVO item = secrecyVo();
        WrapperVO wrapper = new WrapperVO();
        wrapper.title = "详情";
        wrapper.detail = inner;
        wrapper.history = new ArrayList<>(List.of(item));

        write(adviceOf(), Result.success(wrapper));

        assertThat(wrapper.title).isEqualTo("详情");
        assertThat(inner.mobile).isNull();
        assertThat(item.idCard).isNull();
    }

    @Test
    void stopRecursionAtMaxDepth() {
        // 过滤器递归深度上限为 4，超出层级不再处理
        ChainNode root = new ChainNode();
        ChainNode current = root;
        List<ChainNode> chain = new ArrayList<>();
        chain.add(root);
        for (int i = 1; i <= 5; i++) {
            current.child = new ChainNode();
            current = current.child;
            chain.add(current);
        }
        chain.forEach(node -> node.secret = "sensitive");

        write(adviceOf(), Result.success(root));

        assertThat(chain.subList(0, 5)).allSatisfy(node -> assertThat(node.secret).isNull());
        assertThat(chain.get(5).secret).isEqualTo("sensitive");
    }

    @Test
    void ignorePlainAndMapBody() {
        FieldPermissionResponseAdvice advice = adviceOf();

        assertThat(write(advice, Result.success("pure-string"))).isInstanceOf(Result.class);
        Map<String, Object> body = Map.of("mobile", "13800001111");
        write(advice, body);
        assertThat(body).containsEntry("mobile", "13800001111");
    }

    @Test
    void swallowPermissionCheckerException() {
        SecrecyVO vo = secrecyVo();
        FieldPermissionResponseAdvice advice = new FieldPermissionResponseAdvice(permission -> {
            throw new IllegalStateException("会话解析失败");
        });

        Object body = Result.success(vo);
        assertThatCode(() -> write(advice, body)).doesNotThrowAnyException();

        // 判定异常时不改动响应内容，交由日志告警
        assertThat(vo.mobile).isEqualTo("13800001111");
    }

    /**
     * 受控字段样例 VO：覆盖 CLEAR / MASK（String 与非 String）/ 基本类型
     */
    static class SecrecyVO {

        @FieldPermission(MOBILE_PERMISSION)
        public String mobile;

        @FieldPermission(ID_CARD_PERMISSION)
        public String idCard;

        @FieldPermission(value = SALARY_PERMISSION, strategy = FieldPermission.Strategy.MASK, maskValue = "保密")
        public String salaryRemark;

        @FieldPermission(value = AGE_PERMISSION, strategy = FieldPermission.Strategy.MASK)
        public Integer age;

        @FieldPermission(COUNT_PERMISSION)
        public int count;

        public String nickname;
    }

    /**
     * 嵌套结构样例 VO
     */
    static class WrapperVO {

        public String title;

        public SecrecyVO detail;

        public List<SecrecyVO> history;
    }

    /**
     * 链式结构样例 VO：用于校验递归深度上限
     */
    static class ChainNode {

        @FieldPermission(CHAIN_PERMISSION)
        public String secret;

        public ChainNode child;
    }
}
