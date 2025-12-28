package egovframework.example.board.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BoardVO {
    private Integer boardId;
    private Integer writerId;
    private String useYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}