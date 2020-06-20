package xyz.ymtao.gd.service;

import xyz.ymtao.gd.entity.ImageCodeInfo;
import xyz.ymtao.gd.entity.User;
import xyz.ymtao.gd.entity.UserComment;
import xyz.ymtao.gd.entity.UserReceiveAddress;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface UserService {
    List<User> getAllUser();
    public User getUserById(Long id);

    User getUserByPhone(String phone);

    User login(User user);

    Map<String,String> register(User user,String sms);

    ImageCodeInfo getImageCode(String ip) throws IOException;

    String getMessageCode(String phone);

    String checkImageCode(String imageCode,String ip);

    int addUserToken(String token, String userId);

    User checkOauthUser(User userCheck);

    User addOauthUser(User user);

    List<UserReceiveAddress> getReceiveAddressByUserId(String userId);

    UserReceiveAddress getReceiveAddressById(String receiveAddressId);
    int updateUserByUserId(User user);

    int addReceiveAddress(UserReceiveAddress receiveAddress);
    
    int addComment(UserComment userComment);
    int deleteComment(UserComment userComment);

    List<UserComment> getUserCommentByUserId(String userId);
}
