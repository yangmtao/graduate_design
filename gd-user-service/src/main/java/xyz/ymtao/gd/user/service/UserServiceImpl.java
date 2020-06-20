package xyz.ymtao.gd.user.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import xyz.ymtao.gd.common.pojo.ImageCode;
import xyz.ymtao.gd.common.util.ImageCodeUtil;
import xyz.ymtao.gd.common.util.MD5Utils;
import xyz.ymtao.gd.common.util.MessageCodeUtil;
import xyz.ymtao.gd.entity.ImageCodeInfo;
import xyz.ymtao.gd.entity.User;
import xyz.ymtao.gd.entity.UserComment;
import xyz.ymtao.gd.entity.UserReceiveAddress;
import xyz.ymtao.gd.service.UserService;
import xyz.ymtao.gd.service.util.RedisUtil;
import xyz.ymtao.gd.user.mapper.UserCommentMapper;
import xyz.ymtao.gd.user.mapper.UserMapper;
import xyz.ymtao.gd.user.mapper.UserReceiveAddressMapper;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;
    @Autowired
    UserCommentMapper userCommentMapper;
    @Autowired
    UserReceiveAddressMapper userReceiveAddressMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<User> getAllUser() {
        List<User> users=userMapper.selectAll();
        return users;
    }

    @Override
    public User getUserById(Long id) {
        User user=userMapper.selectByPrimaryKey(id);
        return user;
    }

    @Override
    public User getUserByPhone(String phone) {
        User user=new User();
        user.setPhone(phone);
        User resultUser=null;
        resultUser=userMapper.selectOne(user);
        return resultUser;
    }

    @Override
    public User login(User user) {
        Jedis jedis=null;
        try{
            jedis=redisUtil.getJedis();
            if(jedis!=null){
                String userStr=jedis.get("user:"+user.getPhone()+user.getPassword()+":info");
                if(StringUtils.isNotBlank(userStr)){
                    //密码正确，且获取到用户信息缓存
                    User userFromCache= JSON.parseObject(userStr,User.class);
                    return userFromCache;
                }
            }
            //连接redis失败或没有数据，从数据库获取
            User userFromDb=getUserFromDb(user);
            if(userFromDb!=null){
                jedis.setex("user:"+user.getPhone()+user.getPassword()+":info",60*60*24,JSON.toJSONString(userFromDb));
            }
            return userFromDb;
        }finally {
            if(jedis!=null){
                jedis.close();
            }
        }

    }
    private User getUserFromDb(User user) {
        List<User> users = userMapper.select(user);
        System.out.println("从数据库获取用户");
        if(users!=null&&users.size()==1){
            System.out.println("从数据库获取到用户"+users.get(0).getPhone());
            return users.get(0);
        }
        return null;
    }

    //用户注册
    @Override
    public Map<String,String> register(User user,String sms){
        Jedis jedis=null;
        Map<String,String> returnMap=new HashMap<>();
       try{
            jedis=redisUtil.getJedis();
            if(jedis!=null){
                String checkSms=jedis.get("messagecode:"+user.getPhone());
                if(!sms.equals(checkSms)){
                    returnMap.put("status","400");
                    returnMap.put("details","短信验证码错误");
                    return returnMap;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
           jedis.close();
       }

        User checkUser=getUserFromDb(user);
        if(checkUser!=null){
            returnMap.put("status","400");
            returnMap.put("details","该手机号已注册");
            return returnMap;
        }
        //加密密码
        String md5Password=MD5Utils.md5(user.getPassword());
        System.out.println(" register md5Password is: "+md5Password);
        user.setPassword(md5Password);
        //设置注册时间为当前
        user.setCreateTime(new Date());
        user.setUsername("用户"+user.getPhone());
        user.setGender("1");
        int i=userMapper.insertSelective(user);
        if(i==1){
            returnMap.put("status","200");
            returnMap.put("details","注册成功！");
            return returnMap;
        }else{
            returnMap.put("status","400");
            returnMap.put("details","服务器错误，请稍后再试！");
            return returnMap;
        }

    }

    //获取图形验证码
    @Override
    public ImageCodeInfo getImageCode(String ip) throws IOException {
        Jedis jedis=null;
        ImageCode imageCode= ImageCodeUtil.getCode();
        String codeStr=imageCode.getCodeStr();
        try{
            jedis=redisUtil.getJedis();
            //如果成功获取图形验证码
            if(jedis!=null&&(StringUtils.isNotBlank(codeStr))){
               String jedisResult= jedis.setex("imagecode:"+ip,600,codeStr);
                if(jedisResult!=null){
                    System.out.println("jedisResult is: "+jedisResult);
                    ImageCodeInfo imageCodeInfo=new ImageCodeInfo();
                    imageCodeInfo.setCodeStr(imageCode.getCodeStr());
                    ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
                    ImageIO.write(imageCode.getBufferedImage(),"JPEG", byteArrayOutputStream);
                    byte[] bytes=byteArrayOutputStream.toByteArray();
                    imageCodeInfo.setImageBytes(bytes);
                    return imageCodeInfo;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            jedis.close();
        }
        return null;
    }

    //获取短信验证码
    @Override
    public String getMessageCode(String phone) {
       String sms= MessageCodeUtil.sendSms(phone);
       Jedis jedis=null;
       try{
           jedis=redisUtil.getJedis();
           if(jedis!=null && sms!=null){
              String jedisResult=jedis.setex("messagecode:"+phone,60,sms);
              if(jedisResult!=null){
                  System.out.println("短信验证码处理成功");
                  return sms;
              }
           }
       }catch (Exception e){
           e.printStackTrace();
       }finally {
           jedis.close();
       }
        return null;
    }

    @Override
    public String checkImageCode(String imageCode, String ip) {
        Jedis jedis=null;
        try{
            jedis=redisUtil.getJedis();
            if(jedis!=null){
               String code= jedis.get("imagecode:"+ip);
                if(code!=null && code.equals(imageCode)){
                    return "success";
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            jedis.close();
        }
        return "图形验证码错误或已过期";
    }

    //将用户添加到缓存
    @Override
    public int addUserToken(String token, String userId) {
        Jedis jedis=redisUtil.getJedis();
        String setex = jedis.setex("user:" + userId + ":token", 60 * 60 , token);
        jedis.close();
        //用户信息缓存设置成功
        if(setex!=null){
            return 1;
        }
        //用户信息缓存设置失败
        return 0;
    }

    //检查第三方登录用户是否登录过
    @Override
    public User checkOauthUser(User userCheck) {
        User user=userMapper.selectOne(userCheck);
        return userCheck;
    }

    //添加第三方登录用户
    @Override
    public User addOauthUser(User user) {
        userMapper.insertSelective(user);
        return user;
    }
    //获取用户所有收货地址信息
    @Override
    public List<UserReceiveAddress> getReceiveAddressByUserId(String userId) {
        UserReceiveAddress userReceiveAddress=new UserReceiveAddress();
        userReceiveAddress.setUserId(userId);
        List<UserReceiveAddress> userReceiveAddressList=userReceiveAddressMapper.select(userReceiveAddress);
        return userReceiveAddressList;
    }

    @Override
    public UserReceiveAddress getReceiveAddressById(String receiveAddressId) {
        return userReceiveAddressMapper.selectByPrimaryKey(receiveAddressId);
    }

    @Override
    public int updateUserByUserId(User user) {
        return userMapper.updateByPrimaryKeySelective(user);
    }

    @Override
    public int addReceiveAddress(UserReceiveAddress receiveAddress) {
        return userReceiveAddressMapper.insertSelective(receiveAddress);
    }

    @Override
    public int addComment(UserComment userComment) {
        int result=0;
        result=userCommentMapper.insertSelective(userComment);
        return result;
    }

    @Override
    public int deleteComment(UserComment userComment) {
        int result=0;
        result=userCommentMapper.delete(userComment);
        return result;
    }

    @Override
    public List<UserComment> getUserCommentByUserId(String userId) {
        UserComment userComment=new UserComment();
        userComment.setUserId(userId);
        List<UserComment> userComments=userCommentMapper.select(userComment);
        return userComments;
    }
}
