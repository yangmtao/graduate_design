package xyz.ymtao.gd.commodity.service;

import com.alibaba.dubbo.config.annotation.Service;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import xyz.ymtao.gd.entity.CommoditySkuInfo;
import xyz.ymtao.gd.entity.SearchParam;
import xyz.ymtao.gd.entity.SearchSkuInfo;
import xyz.ymtao.gd.service.SearchService;
import xyz.ymtao.gd.service.SkuService;


import java.io.IOException;
import java.util.*;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    JestClient jestClient;

    //执行搜索并返回搜索结果
    @Override
    public List<SearchSkuInfo> list(SearchParam pmsSearchParam) {
        //根据搜索请求制定搜索条件字符串
        String dslStr = getSearchDsl(pmsSearchParam);
        System.err.println(dslStr);
        // 用api执行复杂查询
        List<SearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();
        //搜索哪一index下的哪一个类型的数据
        Search search = new Search.Builder(dslStr).addIndex("gd2020").addType("SearchSkuInfo").build();
        SearchResult execute = null;
        List<SearchResult.Hit<SearchSkuInfo, Void>> hits=null;
        try {
            //执行搜索
            execute = jestClient.execute(search);
            //从搜索结果中获取命中的数据
             hits = execute.getHits(SearchSkuInfo.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //如果存在命中数据
        if(hits!=null){
            for (SearchResult.Hit<SearchSkuInfo, Void> hit : hits) {
                SearchSkuInfo source = hit.source;
                    //高亮处理
                    Map<String, List<String>> highlight = hit.highlight;
                    if(highlight!=null){
                        String skuName = highlight.get("skuName").get(0);
                        source.setSkuName(skuName);
                    };
                    pmsSearchSkuInfos.add(source);
            }
        }


        System.out.println("搜索结果数量："+pmsSearchSkuInfos.size());
        return pmsSearchSkuInfos;
    }

    //将数据库中的sku商品信息保存到elasticsearch
    @Override
    public int saveSkuToEleastic(List<CommoditySkuInfo> skuList) {

        //转换为在elasticsearch中保存的数据结构
        List<SearchSkuInfo> searchSkuInfos=new ArrayList<>();
        for(CommoditySkuInfo skuInfo:skuList){
            SearchSkuInfo searchSkuInfo=new SearchSkuInfo();
            BeanUtils.copyProperties(skuInfo,searchSkuInfo);
            searchSkuInfo.setId(Long.parseLong(skuInfo.getId()));
            searchSkuInfos.add(searchSkuInfo);
        }
        //导入es
        try{
            for (SearchSkuInfo searhSku:searchSkuInfos) {
                Index index=new Index.Builder(searhSku).index("gd2020").type("SearchSkuInfo").id(searhSku.getCommodityId()+"").build();
                jestClient.execute(index);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 1;
    }

    //根据搜索条件获取搜索定义语句
    private String getSearchDsl(SearchParam pmsSearchParam) {

         String[] skuAttrValueList = pmsSearchParam.getValueId();
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String sort=pmsSearchParam.getSort();

        // jest的dsl工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        // filter
        if(StringUtils.isNotBlank(catalog3Id)){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }
        if(skuAttrValueList!=null){
            for (String pmsSkuAttrValue : skuAttrValueList) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",pmsSkuAttrValue);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        // must
        if(StringUtils.isNotBlank(keyword)){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }
        // query
        searchSourceBuilder.query(boolQueryBuilder);

        // highlight
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        //searchSourceBuilder.highlight(highlightBuilder);
        searchSourceBuilder.highlighter(highlightBuilder);
        // sort
        if(sort.equals("mostNew")){
            searchSourceBuilder.sort("id",SortOrder.DESC);
        }
        else if(sort.equals("soldNumber")){
            searchSourceBuilder.sort("soldNumber",SortOrder.DESC);
        }
        else if(sort.equals("price")){
            searchSourceBuilder.sort("price",SortOrder.ASC);
        }
        else {
            System.out.println("综合默认排序");
        }

        // from
        searchSourceBuilder.from(0);
        // size
        searchSourceBuilder.size(20);

        return searchSourceBuilder.toString();

    }
}
