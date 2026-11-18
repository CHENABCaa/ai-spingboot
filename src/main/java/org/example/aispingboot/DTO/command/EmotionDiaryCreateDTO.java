package org.example.aispingboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmotionDiaryCreateDTO {

    @Size(max = 10, message = "日期格式不正确")
    private String diaryDate;

    @NotNull(message = "情绪评分不能为空")
    @Min(value = 1, message = "情绪评分最低1分")
    @Max(value = 10, message = "情绪评分最高10分")
    private Integer moodScore;

    @Size(max = 50, message = "主要情绪最多50个字符")
    private String dominantEmotion;

    @Size(max = 1000, message = "触发因素最多1000个字符")
    private String emotionTriggers;

    @Size(max = 2000, message = "详细记录最多2000个字符")
    private String diaryContent;

    @Min(value = 1)
    @Max(value = 5)
    private Integer sleepQuality;

    @Min(value = 1)
    @Max(value = 5)
    private Integer stressLevel;
}
