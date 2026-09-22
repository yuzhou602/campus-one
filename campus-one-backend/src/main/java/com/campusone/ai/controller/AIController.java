package com.campusone.ai.controller;

import com.campusone.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import org.springframework.util.StringUtils;

@Tag(name = "AI校园助手")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final ThreadPoolTaskExecutor aiExecutor;

    @Data
    static class ChatRequest {
        @Size(max = 100, message = "会话ID不能超过100个字符")
        private String conversationId;
        @NotBlank(message = "消息不能为空")
        @Size(max = 2000, message = "消息不能超过2000个字符")
        private String message;
    }

    @Data
    static class ChatResponse {
        private String conversationId;
        private String content;
        private List<String> sources;
        private Map<String, Object> actionCard;
    }

    @Operation(summary = "AI对话")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);
        String conversationId = StringUtils.hasText(request.getConversationId())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        aiExecutor.submit(() -> {
            try {
                String message = request.getMessage().toLowerCase(Locale.ROOT);
                String response;

                if (message.contains("课表") || message.contains("课程") || message.contains("上课")) {
                    response = "📅 **今日课程安排**\n\n" +
                        "| 时间 | 课程 | 地点 | 教师 |\n" +
                        "|------|------|------|------|\n" +
                        "| 08:00-09:40 | 软件工程 | 信息楼301 | 张教授 |\n" +
                        "| 10:00-11:40 | Java EE | 实验楼205 | 李老师 |\n" +
                        "| 14:00-15:40 | 数据库原理 | 教学楼A201 | 王教授 |\n\n" +
                        "💡 提示：你可以在「我的 → 课表」中查看完整学期课表，支持按周查看。\n" +
                        "如需调课或查看成绩，请告诉我。";

                } else if (message.contains("请假") || message.contains("假条")) {
                    response = "📋 **请假管理规定**\n\n" +
                        "**审批流程：**\n" +
                        "| 时长 | 审批人 |\n" +
                        "|------|--------|\n" +
                        "| 1天以内 | 班主任 |\n" +
                        "| 1-3天 | 班主任 → 辅导员 |\n" +
                        "| 3天以上 | 班主任 → 辅导员 → 学院负责人 |\n\n" +
                        "**所需材料：**\n" +
                        "• 请假事由说明\n" +
                        "• 起止时间\n" +
                        "• 紧急联系人信息\n\n" +
                        "📌 请通过「校园事务 → 请假申请」提交，审批结果会在24小时内通知你。";

                } else if (message.contains("预约") || message.contains("教室") || message.contains("实验室") || message.contains("场地")) {
                    response = "🏫 **场地预约信息**\n\n" +
                        "**当前可用场地：**\n" +
                        "| 场地 | 容量 | 可预约时段 |\n" +
                        "|------|------|------------|\n" +
                        "| 软件实验室305 | 45人 | 14:00-16:00 |\n" +
                        "| AI实验室402 | 30人 | 15:00-17:00 |\n" +
                        "| 自习室B102 | 60人 | 全天 |\n" +
                        "| 多功能报告厅 | 200人 | 需提前3天预约 |\n\n" +
                        "**预约规则：**\n" +
                        "• 每人每天最多预约2个场地\n" +
                        "• 预约后30分钟内未签到将自动取消\n" +
                        "• 会议/活动需提交审批\n\n" +
                        "需要帮你预约哪个场地？";

                } else if (message.contains("报修") || message.contains("维修") || message.contains("坏了")) {
                    response = "🔧 **校园报修指南**\n\n" +
                        "**支持的报修类型：**\n" +
                        "• 宿舍维修（门窗、家具、卫浴）\n" +
                        "• 教室设备（投影仪、空调、灯光）\n" +
                        "• 水电维修（水管、电路、开关）\n" +
                        "• 网络故障（校园网、WiFi、网口）\n" +
                        "• 其他（请描述具体问题）\n\n" +
                        "**报修流程：**\n" +
                        "1️⃣ 提交报修工单（描述问题+上传照片）\n" +
                        "2️⃣ 系统自动分类并分配维修人员\n" +
                        "3️⃣ 维修人员联系你确认时间\n" +
                        "4️⃣ 完成维修后你确认并评价\n\n" +
                        "⏱️ 一般响应时间：工作日2小时内，节假日24小时内。\n\n" +
                        "你可以直接描述问题，我来帮你创建工单。";

                } else if (message.contains("成绩") || message.contains("分数") || message.contains("绩点") || message.contains("gpa")) {
                    response = "📊 **成绩查询**\n\n" +
                        "**查询方式：**\n" +
                        "• 「我的 → 成绩查询」可查看所有学期成绩\n" +
                        "• 支持按学期、按课程类型筛选\n" +
                        "• 可导出成绩单PDF\n\n" +
                        "**绩点计算规则：**\n" +
                        "| 等级 | 分数区间 | 绩点 |\n" +
                        "|------|----------|------|\n" +
                        "| A | 90-100 | 4.0 |\n" +
                        "| B+ | 85-89 | 3.5 |\n" +
                        "| B | 80-84 | 3.0 |\n" +
                        "| C+ | 75-79 | 2.5 |\n" +
                        "| C | 60-74 | 2.0 |\n" +
                        "| D | <60 | 0.0 |\n\n" +
                        "💡 本学期成绩将在考试结束后2周内发布。\n" +
                        "如需成绩复核，请联系教务处。";

                } else if (message.contains("活动") || message.contains("社团") || message.contains("比赛")) {
                    response = "🎉 **近期校园活动**\n\n" +
                        "| 活动 | 时间 | 地点 | 报名 |\n" +
                        "|------|------|------|------|\n" +
                        "| 编程马拉松 | 3月15日 | 信息楼大厅 | ✅ 可报名 |\n" +
                        "| 校园歌手大赛 | 3月20日 | 大礼堂 | ✅ 可报名 |\n" +
                        "| AI技术讲座 | 3月22日 | 学术报告厅 | ✅ 可报名 |\n" +
                        "| 春季运动会 | 4月1日 | 体育场 | 即将开放 |\n\n" +
                        "📌 报名后请准时参加，累计3次缺席将影响信用分。\n" +
                        "如需了解详情或报名，请告诉我活动名称。";

                } else if (message.contains("费用") || message.contains("缴费") || message.contains("学费") || message.contains("充值")) {
                    response = "💰 **费用缴纳说明**\n\n" +
                        "**可缴纳费用类型：**\n" +
                        "• 学费（每学期开学前缴纳）\n" +
                        "• 住宿费（按学年缴纳）\n" +
                        "• 水电费（每月10日出账，15日前缴纳）\n" +
                        "• 饼卡充值（实时到账）\n" +
                        "• 考试报名费\n\n" +
                        "**缴费方式：**\n" +
                        "• 支付宝/微信扫码支付\n" +
                        "• 银行卡代扣（需提前绑定）\n" +
                        "• 线下缴费（财务处窗口）\n\n" +
                        "📌 缴费成功后电子发票会发送到你的邮箱。\n" +
                        "如需查询具体账单，请告诉我费用类型。";

                } else if (message.contains("宿舍") || message.contains("寝室") || message.contains("宿管")) {
                    response = "🏠 **宿舍信息**\n\n" +
                        "**宿舍管理规定：**\n" +
                        "• 门禁时间：23:00（周五、周六延至23:30）\n" +
                        "• 来访登记：外来人员需在门卫处登记\n" +
                        "• 用电安全：禁止使用大功率电器\n" +
                        "• 卫生检查：每周三下午例行检查\n\n" +
                        "**报修渠道：**\n" +
                        "• 宿舍内设施故障 → 校园报修\n" +
                        "• 紧急情况（漏水、断电）→ 宿管热线 138xxxx\n\n" +
                        "💡 你可以在「我的 → 宿舍」查看宿舍详情和室友信息。\n" +
                        "如需调换宿舍，请提交「宿舍调整申请」。";

                } else if (message.contains("图书馆") || message.contains("借书") || message.contains("还书")) {
                    response = "📚 **图书馆服务指南**\n\n" +
                        "**开放时间：**\n" +
                        "| 区域 | 时间 |\n" +
                        "|------|------|\n" +
                        "| 阅览区 | 8:00-22:00 |\n" +
                        "| 自习区 | 6:30-23:00 |\n" +
                        "| 电子阅览室 | 8:00-21:30 |\n" +
                        "| 借还书处 | 9:00-17:00 |\n\n" +
                        "**借阅规则：**\n" +
                        "• 本科生：最多借10本，借期30天\n" +
                        "• 研究生：最多借20本，借期60天\n" +
                        "• 续借：可线上续借1次，延长15天\n" +
                        "• 逾期：每天0.1元/本\n\n" +
                        "💡 你可以通过「图书馆」模块在线查询馆藏、预约座位。\n" +
                        "需要帮你查某本书的馆藏状态吗？";

                } else if (message.contains("食堂") || message.contains("餐厅") || message.contains("吃饭")) {
                    response = "🍽️ **食堂信息**\n\n" +
                        "**各食堂营业时间：**\n" +
                        "| 食堂 | 位置 | 营业时间 | 特色 |\n" +
                        "|------|------|----------|------|\n" +
                        "| 第一食堂 | 生活区A | 6:30-20:00 | 早餐、面食 |\n" +
                        "| 第二食堂 | 生活区B | 7:00-21:00 | 中式快餐 |\n" +
                        "| 教工食堂 | 行政楼旁 | 11:00-13:00 | 精品套餐 |\n" +
                        "| 美食广场 | 综合楼B1 | 10:00-22:00 | 各类小吃 |\n\n" +
                        "**校园卡充值：**\n" +
                        "• 自助充值机（各食堂入口）\n" +
                        "• 线上充值（校园APP → 生活缴费）\n\n" +
                        "💡 今日推荐：第二食堂推出新品「酸菜鱼米饭」，欢迎品尝！";

                } else if (message.contains("你好") || message.contains("你是谁") || message.contains("hi") || message.contains("hello")) {
                    response = "👋 你好！我是 **Campus Copilot**，你的 AI 校园助手。\n\n" +
                        "我可以帮你处理校园生活中的各种事务：\n\n" +
                        "📚 **学习相关** — 查课表、查成绩、查图书馆\n" +
                        "🏫 **校园服务** — 场地预约、校园报修、请假申请\n" +
                        "🎉 **校园生活** — 活动报名、食堂推荐、宿舍管理\n" +
                        "💰 **费用管理** — 学费缴纳、水电费查询\n\n" +
                        "请直接用自然语言告诉我你的需求，我会尽力帮助你！";

                } else {
                    response = "🤔 我理解你在问：「" + request.getMessage() + "」\n\n" +
                        "虽然我暂时无法完全理解你的问题，但我可以帮你处理以下事务：\n\n" +
                        "• 📅 **查课表** — 说「课表」或「课程」\n" +
                        "• 🏫 **预约场地** — 说「预约教室」或「实验室」\n" +
                        "• 📋 **请假** — 说「请假」或「假条」\n" +
                        "• 🔧 **报修** — 说「报修」或「维修」\n" +
                        "• 📊 **成绩** — 说「成绩」或「GPA」\n" +
                        "• 🎉 **活动** — 说「活动」或「比赛」\n" +
                        "• 💰 **缴费** — 说「缴费」或「学费」\n" +
                        "• 🏠 **宿舍** — 说「宿舍」或「寝室」\n" +
                        "• 📚 **图书馆** — 说「图书馆」或「借书」\n" +
                        "• 🍽️ **食堂** — 说「食堂」或「吃饭」\n\n" +
                        "请告诉我你想了解什么？";
                }

                ChatResponse chatResponse = new ChatResponse();
                chatResponse.setConversationId(conversationId);
                chatResponse.setContent(response);
                chatResponse.setSources(List.of("校园知识库"));

                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(chatResponse));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Operation(summary = "知识库列表")
    @GetMapping("/knowledge")
    public ApiResponse<?> getKnowledgeList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // TODO: Implement knowledge base listing
        return ApiResponse.success(Map.of("records", List.of(), "total", 0));
    }

    @Operation(summary = "快捷操作")
    @GetMapping("/quick-actions")
    public ApiResponse<List<Map<String, String>>> quickActions() {
        List<Map<String, String>> actions = List.of(
            Map.of("label", "查课表", "icon", "📅"),
            Map.of("label", "找教室", "icon", "🏫"),
            Map.of("label", "查申请", "icon", "📋"),
            Map.of("label", "校园规定", "icon", "📖"),
            Map.of("label", "场地预约", "icon", "🗓️"),
            Map.of("label", "报修进度", "icon", "🔧"),
            Map.of("label", "校园活动", "icon", "🎉")
        );
        return ApiResponse.success(actions);
    }
}
