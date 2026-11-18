package org.example.aispingboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.command.EmotionDiaryCreateDTO;
import org.example.aispingboot.entity.EmotionDiary;
import org.example.aispingboot.mapper.EmotionDiaryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class EmotionDiaryService {

    @Resource
    private EmotionDiaryMapper emotionDiaryMapper;

    public EmotionDiary createDiary(Long userId, EmotionDiaryCreateDTO dto) {
        LocalDate diaryDate = StrUtil.isNotBlank(dto.getDiaryDate())
                ? LocalDate.parse(dto.getDiaryDate())
                : LocalDate.now();

        // 同一天已有记录 → 更新
        EmotionDiary existing = emotionDiaryMapper.selectOne(
                new LambdaQueryWrapper<EmotionDiary>()
                        .eq(EmotionDiary::getUserId, userId)
                        .eq(EmotionDiary::getDiaryDate, diaryDate)
        );

        if (existing != null) {
            existing.setMoodScore(dto.getMoodScore());
            existing.setDominantEmotion(dto.getDominantEmotion());
            existing.setEmotionTriggers(dto.getEmotionTriggers());
            existing.setDiaryContent(dto.getDiaryContent());
            existing.setSleepQuality(dto.getSleepQuality());
            existing.setStressLevel(dto.getStressLevel());
            emotionDiaryMapper.updateById(existing);
            return existing;
        }

        // 没有 → 插入
        EmotionDiary diary = EmotionDiary.builder()
                .userId(userId)
                .diaryDate(diaryDate)
                .moodScore(dto.getMoodScore())
                .dominantEmotion(dto.getDominantEmotion())
                .emotionTriggers(dto.getEmotionTriggers())
                .diaryContent(dto.getDiaryContent())
                .sleepQuality(dto.getSleepQuality())
                .stressLevel(dto.getStressLevel())
                .build();
        emotionDiaryMapper.insert(diary);
        return diary;
    }
}
