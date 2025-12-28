package egovframework.example.boardtext.mapper;

import org.apache.ibatis.annotations.Mapper;
import egovframework.example.boardtext.vo.BoardTextVO;

@Mapper
public interface BoardTextMapper {

    BoardTextVO selectByBoardIdAndLang(Integer boardId, String langCode);

    void insertBoardText(BoardTextVO boardText);
}
