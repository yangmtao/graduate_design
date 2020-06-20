package xyz.ymtao.gd.common.util;

import xyz.ymtao.gd.common.pojo.ImageCode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

public class ImageCodeUtil {
    //图形验证码生成工具
    public static ImageCode getCode() throws IOException {
        ImageCode imageCodeInfo=new ImageCode();
        //定义图片大小
        int width=80;
        int height=30;
        BufferedImage bufferedImage=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics graphics=bufferedImage.getGraphics();
        graphics.setColor(new Color(200,200,200));
        graphics.fillRect(0, 0, width, height);
        Random random=new Random();
        int randnum=random.nextInt(8999)+1000;
        //随机验证码
        String ranString=String.valueOf(randnum);
        imageCodeInfo.setCodeStr(ranString);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("微软雅黑",Font.PLAIN,20));
        //绘制图像，参数分别为内容字符串，内容的x和y坐标
        graphics.drawString(ranString, 14, 22);
        //绘制干扰点
        for(int i=0;i<100;i++){
            int x=random.nextInt(width);
            int y=random.nextInt(height);
            graphics.drawOval(x, y, 1, 1);
        }
        //画干扰线
        for (int i = 0; i < 5; i++) {
            // 设置随机颜色
            graphics.setColor(getRandomColor());
            // 随机画线
            graphics.drawLine(random.nextInt(), random.nextInt(height),random.nextInt(width), random.nextInt(height));
        }
        imageCodeInfo.setBufferedImage(bufferedImage);
        return imageCodeInfo;
    }
    //获取随机颜色
    public static Color getRandomColor() {
        Random ran = new Random();
        Color color = new Color(ran.nextInt(256),
                ran.nextInt(256), ran.nextInt(256));
        return color;
    }
}
