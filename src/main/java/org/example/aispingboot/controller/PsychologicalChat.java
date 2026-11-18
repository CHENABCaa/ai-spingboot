package org.example.aispingboot.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.example.aispingboot.AiService.PsychologicalSupportService;
import org.example.aispingboot.AiService.StructOutPut;
import org.example.aispingboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispingboot.DTO.command.ConsultationStreamDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.entity.ConsultationMessage;
import org.example.aispingboot.entity.ConsultationSession;
import org.example.aispingboot.mapper.ConsultationMessageMapper;
import org.example.aispingboot.mapper.ConsultationSessionMapper;
import org.example.aispingboot.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChat {
    @Autowired
    private PsychologicalSupportService psychologicalSupportService;

    @Autowired
    private ConsultationSessionMapper consultationSessionMapper;

    @Autowired
    private ConsultationMessageMapper consultationMessageMapper;

    @PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
        return Result.ok(session);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        if (userId == null) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg(), "用户未登录")))
                    .build());
        }

        return psychologicalSupportService.streamPsychologicalChat(streamDTO.getSessionId(), streamDTO.getUserMessage())
                .map(chunk -> {
                    return ServerSentEvent.<String>builder()
                            .event("message")
                            .data(JSONUtil.toJsonStr(Result.ok(Map.of("content", chunk, "type", "normal"))))
                            .build();
                })
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("{}")
                        .build()
                ))
                .delayElements(Duration.ofMillis(50));
    }

    @GetMapping("/sessions")
    public Result<Map<String, Object>> listSessions(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        LambdaQueryWrapper<ConsultationSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationSession::getUserId, userId)
               .orderByDesc(ConsultationSession::getStartedAt);

        Page<ConsultationSession> page = consultationSessionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 为每个 session 组装前端需要的字段
        List<Map<String, Object>> records = page.getRecords().stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("sessionTitle", s.getSessionTitle());
            item.put("startedAt", s.getStartedAt() != null ? DateUtil.format(s.getStartedAt(), "yyyy-MM-dd HH:mm") : "");
            item.put("sessionId", "session_" + s.getId());
            // 最后一条消息预览
            ConsultationMessage lastMsg = consultationMessageMapper.selectOne(
                new LambdaQueryWrapper<ConsultationMessage>()
                    .eq(ConsultationMessage::getSessionId, s.getId())
                    .orderByDesc(ConsultationMessage::getCreatedAt)
                    .last("LIMIT 1")
            );
            item.put("lastMessageContent", lastMsg != null ? lastMsg.getContent() : "开始新的对话...");
            item.put("messageCount", consultationMessageMapper.selectCount(
                new LambdaQueryWrapper<ConsultationMessage>().eq(ConsultationMessage::getSessionId, s.getId())
            ));
            return item;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", page.getTotal());
        data.put("pageNum", page.getCurrent());
        data.put("pageSize", page.getSize());
        return Result.ok(data);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessage>> getSessionMessages(@PathVariable Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsultationMessage::getSessionId, sessionId)
               .orderByAsc(ConsultationMessage::getCreatedAt);
        List<ConsultationMessage> messages = consultationMessageMapper.selectList(wrapper);
        return Result.ok(messages);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        consultationSessionMapper.deleteById(sessionId);
        // 级联删除消息
        consultationMessageMapper.delete(
            new LambdaQueryWrapper<ConsultationMessage>().eq(ConsultationMessage::getSessionId, sessionId)
        );
        return Result.ok();
    }

    @GetMapping("/session/{sessionId}/emotion")
    public Result<Map<String, Object>> getSessionEmotion(@PathVariable String sessionId) {
        // sessionId 前端可能传 "session_123" 格式，提取数字
        Long id;
        try {
            id = Long.parseLong(sessionId.replace("session_", ""));
        } catch (NumberFormatException e) {
            return Result.error("会话ID格式错误");
        }

        ConsultationSession session = consultationSessionMapper.selectById(id);
        Map<String, Object> emotion = new HashMap<>();
        if (session != null && session.getLastEmotionAnalysis() != null) {
            try {
                emotion = JSONUtil.toBean(session.getLastEmotionAnalysis(), Map.class);
            } catch (Exception e) {
                // ignore
            }
        }
        // 默认值兜底
        emotion.putIfAbsent("primaryEmotion", "中性");
        emotion.putIfAbsent("emotionScore", 50);
        emotion.putIfAbsent("isNegative", false);
        emotion.putIfAbsent("riskLevel", 0);
        emotion.putIfAbsent("suggestion", "情绪状态平稳");
        emotion.putIfAbsent("improvementSuggestions", List.of());
        return Result.ok(emotion);
    }
}
