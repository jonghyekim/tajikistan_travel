package egovframework.example.board.mapper;

import org.apache.ibatis.annotations.Mapper;
import egovframework.example.board.vo.BoardVO;

@Mapper
public interface BoardMapper {

    BoardVO selectBoardById(Integer boardId);

    void insertBoard(BoardVO board);
}
