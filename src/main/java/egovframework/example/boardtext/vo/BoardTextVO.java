package egovframework.example.boardtext.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BoardTextVO {
    private Integer boardTextId;
    private Integer boardId;
    private String langCode;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
