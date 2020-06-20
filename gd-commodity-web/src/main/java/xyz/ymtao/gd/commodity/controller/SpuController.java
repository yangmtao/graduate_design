package xyz.ymtao.gd.commodity.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.ymtao.gd.entity.CommoditySpuImage;
import xyz.ymtao.gd.entity.CommoditySpuInfo;
import xyz.ymtao.gd.entity.CommoditySpuSaleAttribute;
import xyz.ymtao.gd.service.SpuService;

import java.io.*;
import java.util.List;

@Controller
@CrossOrigin
public class SpuController {
    @Autowired
    FastFileStorageClient fastFileStorageClient;
    @Reference
    SpuService spuService;

    @RequestMapping("/fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        String imgHeadUrl="https://ymtao.xyz/";
        String originalFileName=multipartFile.getOriginalFilename();
        String imgName= StringUtils.getFilename(originalFileName);
        String extName=StringUtils.getFilenameExtension(originalFileName);

        //按比例缩
        Thumbnails.of(multipartFile.getInputStream()).scale(0.5f).toFile("/img/brokerImg.jpg");
        File file=new File("/img/brokerImg.jpg");
        InputStream inputStream=new FileInputStream(file);
        StorePath storePath=fastFileStorageClient.uploadFile(inputStream,file.length(),extName,null);
        return imgHeadUrl + storePath.getFullPath();
    }
    @RequestMapping("/spuImageList")
    @ResponseBody
    public List<CommoditySpuImage> spuImageList(String spuId){
        return spuService.getSpuImageList(spuId);
    }

    @RequestMapping("/spuSaleAttrList")
    @ResponseBody
    public List<CommoditySpuSaleAttribute> spuSaleAttributeList(String spuId){
        return spuService.getSpuSaleAttrList(spuId);
    }
    @RequestMapping("/spuList")
    @ResponseBody
    public List<CommoditySpuInfo> getSpuList(String catalog3Id){
        return spuService.getSpuList(catalog3Id);
    }

    @RequestMapping("/saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo(@RequestBody CommoditySpuInfo spuInfo){
        String msg=spuService.saveSpuInfo(spuInfo);
        if(msg!=null&&msg.equals("success")){
            return msg;
        }
        System.out.println("保存失败"+spuInfo==null);
        return "保存失败";
    }

}
