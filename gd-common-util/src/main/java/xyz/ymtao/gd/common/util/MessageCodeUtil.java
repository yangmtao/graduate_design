package xyz.ymtao.gd.common.util;

import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;
import org.json.JSONException;

import java.io.IOException;

public class MessageCodeUtil {
    // 短信应用 SDK AppID
    private static int appid = 1400256622; // SDK AppID 以1400开头
    // 短信应用 SDK AppKey
    private static String appkey = "6b3b0d8d2a1193f87bc03d5938c88e00";
    // 需要发送短信的手机号码
    //String[] phoneNumbers= {"13883300323"};
    // 短信模板 ID，需要在短信应用中申请
    private static int templateId = 420561; // NOTE: 这里的模板 ID`7839`只是示例，真实的模板 ID 需要在短信控制台中申请
    // 签名
    private static String smsSign = "ymtaoxyz"; // NOTE: 签名参数使用的是`签名内容`，而不是`签名ID`。真实的签名需要在短信控制台申请

    public static String sendSms(String phone) {
        try {
            //获取短信随机验证码
            Integer mobile_code = (int) ((Math.random() * 9 + 1) * 100000);
            String sms = mobile_code.toString();
            String[] params = {sms};
            SmsSingleSender ssender = new SmsSingleSender(appid, appkey);
            SmsSingleSenderResult result = ssender.sendWithParam("86", phone,
                    templateId, params, smsSign, "", "");
            String res = result.errMsg;
            System.out.println(res);
            //如果成功发送短信验证码
            if (res != null && res.equals("OK")) {
                return sms;
            }
        } catch (HTTPException e) {
            System.out.println("HTTP 响应码错误");
            e.printStackTrace();
        } catch (JSONException e) {
            System.out.println("JSON 解析错误");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(" 网络 IO 错误");
            e.printStackTrace();
        }
        return null;
    }
}
