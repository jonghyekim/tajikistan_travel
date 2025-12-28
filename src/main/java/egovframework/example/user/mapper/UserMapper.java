package egovframework.example.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import egovframework.example.user.vo.UserVO;

@Mapper
public interface UserMapper {

    UserVO selectUserByLoginId(String loginId);

    void insertUser(UserVO user);

    void updateUser(UserVO user);
}
