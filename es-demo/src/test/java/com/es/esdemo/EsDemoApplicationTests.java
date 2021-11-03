package com.es.esdemo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.es.esdemo.mapper.GoodsMapper;
import com.es.esdemo.pojo.Goods;
import com.es.esdemo.pojo.Person;
import org.apache.http.HttpHost;
import org.assertj.core.data.Index;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.query.functionscore.WeightBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.util.*;

@SpringBootTest
class EsDemoApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private GoodsMapper goodsMapper;

    @Test
    void contextLoads() {
//        //1.创建客户端对象
//        client = new RestHighLevelClient(RestClient.builder(
//                new HttpHost(
//                        "192.168.154.129",
//                        9200,
//                        "http"
//                )
//        ));
        System.out.println(client);
    }

    @Test
    public void addindex() throws IOException {
        //1.使用client对象操作索引；
        IndicesClient indices = client.indices();
        //2.具体操作获取返回值
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("heima");

        String mapping = "{\n" +
                "      \"properties\" : {\n" +
                "        \"address\" : {\n" +
                "          \"type\" : \"text\",\n" +
                "          \"analyzer\" : \"ik_max_word\"\n" +
                "        },\n" +
                "        \"name\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    }";
        createIndexRequest.mapping("_doc",mapping, XContentType.JSON);
        CreateIndexResponse response = indices.create(createIndexRequest, RequestOptions.DEFAULT);
        System.out.println(response.isAcknowledged());
    }

    @Test
    public void getindex() throws IOException {
        IndicesClient indices = client.indices();
        GetIndexRequest index = new GetIndexRequest("heima");
        boolean exists = indices.exists(index, RequestOptions.DEFAULT);
        GetIndexResponse response = indices.get(index, RequestOptions.DEFAULT);

        Map<String, MappingMetadata> mappings = response.getMappings();
        for (String s : mappings.keySet()) {
            System.out.println("key:"+s+mappings.get(s).getSourceAsMap());
            System.out.println(mappings.get(s));
        }
    }

    @Test
    public void deleteIndex() throws IOException {
        IndicesClient indices = client.indices();
        DeleteIndexRequest request = new DeleteIndexRequest("heima");
        AcknowledgedResponse delete = indices.delete(request, RequestOptions.DEFAULT);
        System.out.println(delete.isAcknowledged());
    }

    /**
     * 添加文档，map格式
     * @throws IOException
     */
    @Test
    public void addDoc() throws IOException {
        Map map = new HashMap();
        map.put("address","上海浦东新区");
        map.put("name","小红");
        IndexRequest indexRequest = new IndexRequest("heima").id("4").source(map);
        IndexResponse index = client.index(indexRequest, RequestOptions.DEFAULT);
    }

    /**
     * 添加文档，JSON格式
     */
    @Test
    public void addDocByJson() throws IOException {
        Person person = new Person();
        person.setId(1);
        person.setAddress("来自外天空");
        person.setName("卫星一号");
        String s = JSON.toJSONString(person);
        IndexRequest indexRequest = new IndexRequest("heima").id("4").source(s,XContentType.JSON);
        IndexResponse index = client.index(indexRequest, RequestOptions.DEFAULT);
    }

    /**
     * 批量操作
     */
    @Test
    public void testBulk() throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        //删除3号
        DeleteRequest dr = new DeleteRequest("person","3");
        bulkRequest.add(dr);

        //添加4号
        Map map = new HashMap();
        map.put("name","蟠桃");
        map.put("address","为名录");
        IndexRequest ir = new IndexRequest("person").id("4").source(map);
        bulkRequest.add(ir);

        //修改1号
        Map m = new HashMap();
        m.put("name","王二麻子");
        UpdateRequest ur = new UpdateRequest("person","1").doc(m);
        bulkRequest.add(ur);
        BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    /**
     * 批量导入
     */
    @Test
    public void importBulk() throws IOException {
        List<Goods> list = goodsMapper.selectgoods();
        BulkRequest bulkRequest = new BulkRequest();
        for (Goods good: list
             ) {
            good.setSpec(JSON.parseObject(good.getSpecStr(),Map.class));
            IndexRequest ir = new IndexRequest("goods");
            ir.id(good.getId()+"").source(JSON.toJSONString(good),XContentType.JSON);
            bulkRequest.add(ir);
        }
        client.bulk(bulkRequest,RequestOptions.DEFAULT);
        System.out.println(list);
    }

    /**
     * match_all查询
     *
     */
    @Test
    public void matchAll() throws IOException {
        //构建查询请求对象
        SearchRequest searchRequest = new SearchRequest("goods");
        //创建查询条件构造器
        SearchSourceBuilder builder = new SearchSourceBuilder();
        //设置查询条件
        MatchAllQueryBuilder match = QueryBuilders.matchAllQuery();
        builder.query(match);
        builder.from(0);
        builder.size(2);
        //添加查询条件构造器
            searchRequest.source(builder);
        //构建查询请求
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);

        List<Goods> list = new ArrayList<>();
        SearchHits hits = search.getHits();
        for (int i = 0; i < hits.getHits().length; i++) {
            String sourceAsString = hits.getHits()[i].getSourceAsString();
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            list.add(goods);
        }

        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
    }

    /**
     * term 查询
     */
    @Test
    public void tremQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("brandName", "汽车");
        searchSourceBuilder.query(termQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value+"条");
        SearchHit[] hits1 = search.getHits().getHits();
        for (int i = 0; i < hits1.length; i++) {
            SearchHit documentFields = hits1[i];
            String source = documentFields.getSourceAsString();
            Goods goods = JSON.parseObject(source, Goods.class);
            System.out.println(goods.toString());
        }
    }

    /**
     * match 查询
     * 会对查询条件进行分词
     * 将分词后的查询条件进行查询
     * 默认取并集
     * @throws IOException
     */
    @Test
    public void matchQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("title", "小米手机");
        searchSourceBuilder.query(matchQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);

        SearchHit[] hits = search.getHits().getHits();
        for (int i = 0; i < hits.length; i++) {
            String source = hits[i].getSourceAsString();
            Goods goods = JSON.parseObject(source, Goods.class);
            System.out.println(goods.toString());
        }
    }

    /**
     * 模糊查询
     * @throws IOException
     */
    @Test
    public void likeQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //*代表向后匹配多为
//        WildcardQueryBuilder title = QueryBuilders.wildcardQuery("title", "华*");
        //？号代表着后面匹配多为
        WildcardQueryBuilder title1 = QueryBuilders.wildcardQuery("title", "小?");
        searchSourceBuilder.query(title1);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);
        SearchHit[] hits = search.getHits().getHits();
        for (int i = 0; i < hits.length; i++) {
            String source = hits[i].getSourceAsString();
            Goods goods = JSON.parseObject(source, Goods.class);
            System.out.println(goods.toString());
        }
    }


    private static void functionScore(RestHighLevelClient client) throws IOException {
        SearchRequest searchRequest = new SearchRequest("blogs");

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //function1
        RandomScoreFunctionBuilder randomScoreFunctionBuilder = ScoreFunctionBuilders.randomFunction().seed(10).setField("_seq_no").setWeight(23);
        TermQueryBuilder termQueryBuilder1 = new TermQueryBuilder("post_date", "2020-01-01");
        FunctionScoreQueryBuilder.FilterFunctionBuilder filterFunctionBuilder = new FunctionScoreQueryBuilder.FilterFunctionBuilder(termQueryBuilder1, randomScoreFunctionBuilder);

        //function2
        TermQueryBuilder termQueryBuilder2 = new TermQueryBuilder("author_id", 11402);
        WeightBuilder weightBuilder = ScoreFunctionBuilders.weightFactorFunction(42);
        FunctionScoreQueryBuilder.FilterFunctionBuilder filterFunctionBuilder2 = new FunctionScoreQueryBuilder.FilterFunctionBuilder(termQueryBuilder2, weightBuilder);

        //query and functions
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("content", "rabbits");
        FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders = new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{filterFunctionBuilder, filterFunctionBuilder2};

        FunctionScoreQueryBuilder functionScoreQueryBuilder = new FunctionScoreQueryBuilder(matchQueryBuilder, filterFunctionBuilders);
        functionScoreQueryBuilder.boost(5);
        functionScoreQueryBuilder.scoreMode(org.elasticsearch.common.lucene.search.function.FunctionScoreQuery.ScoreMode.MULTIPLY);
        functionScoreQueryBuilder.boostMode(CombineFunction.MULTIPLY);
        functionScoreQueryBuilder.setMinScore(42);
        functionScoreQueryBuilder.maxBoost(42);

        searchSourceBuilder.query(functionScoreQueryBuilder);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            System.out.println(hit.getSourceAsString());
        }
    }


    //多条件排序
    @Test
    public void sortQuery() throws IOException {

        FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders = new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
              new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchQuery("identity", "time_doptimal").boost(5), ScoreFunctionBuilders.weightFactorFunction(70)),
              new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchQuery("identity", "APP").boost(3), ScoreFunctionBuilders.weightFactorFunction(4)),
              new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchQuery("identity", "new_product").boost(4), ScoreFunctionBuilders.weightFactorFunction(16)),
              new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchQuery("identity", "concessional_rate").boost(2), ScoreFunctionBuilders.weightFactorFunction(2)),
                  new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchQuery("identity", "EXTENDED SIZE").boost(2), ScoreFunctionBuilders.weightFactorFunction(2)),
                  new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchQuery("identity", "revision").boost(2), ScoreFunctionBuilders.weightFactorFunction(2)),
                  new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchQuery("identity", "pickUp"), ScoreFunctionBuilders.weightFactorFunction(1))
        };
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termQuery("num.keyword", "true"));
        FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(boolQuery, filterFunctionBuilders);
        searchSourceBuilder.query(functionScoreQueryBuilder)
                  .sort("_score",SortOrder.DESC);
//                  .sort("identity.keyword",SortOrder.DESC);
        searchSourceBuilder.explain(true);
        SearchRequest searchRequest = new SearchRequest("my_test");
        searchRequest.source(searchSourceBuilder);
        searchSourceBuilder.from(1);
        searchSourceBuilder.size(30);

        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = search.getHits().getHits();
        for (int i = 0; i < hits.length; i++) {
            String source = hits[i].getSourceAsString();
            System.out.println(source.toString()+":_score:"+hits[i].getScore());
        }

        System.out.println(functionScoreQueryBuilder.toString());


//        SearchRequest searchRequest = new SearchRequest("my_test");
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        TermQueryBuilder termQuery = QueryBuilders.termQuery("num", "true");
//        searchSourceBuilder.query(termQuery);
//        searchSourceBuilder.sort("identity");
//        searchRequest.source(searchSourceBuilder);
//        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
//        System.out.println(search.getHits().getTotalHits().value);
//        SearchHit[] hits = search.getHits().getHits();
//        for (int i = 0; i < hits.length; i++) {
//            String source = hits[i].getSourceAsString();
//            System.out.println(source.toString());
//        }
    }


    //范围查询
    @Test
    public void rangeQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        RangeQueryBuilder range = QueryBuilders.rangeQuery("price");
        //指定下限
        range.gte(2000);
        //指定上限
        range.lte(40000);
        searchSourceBuilder.query(range);
        //设置排序
        searchSourceBuilder.sort("price", SortOrder.ASC);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);
        SearchHit[] hits = search.getHits().getHits();
        for (int i = 0; i < hits.length; i++) {
            String source = hits[i].getSourceAsString();
            Goods goods = JSON.parseObject(source, Goods.class);
            System.out.println(goods.toString());
        }
    }

    //queryString

    /**
     * 多字段查询
     * @throws IOException
     */
    @Test
    public void queryString() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryStringQueryBuilder queryString = QueryBuilders.queryStringQuery("华为手机").field("title").field("categoryName").field("brandName").defaultOperator(Operator.AND);
        searchSourceBuilder.query(queryString);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);
        SearchHit[] hits = search.getHits().getHits();
        for (int i = 0; i < hits.length; i++) {
            String source = hits[i].getSourceAsString();
            Goods goods = JSON.parseObject(source, Goods.class);
            System.out.println(goods.toString());
        }
    }

    /**
     * booleanQuery
     * 多条件联合查询
     * @throws IOException
     */
    @Test
    public void BooleanQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        //构建各个查询条件
        //1:查询品牌华为
        boolQuery.must(QueryBuilders.termQuery("brandName","华为"));

        //2.查询标题包含华为的
        boolQuery.filter(QueryBuilders.matchQuery("title","手机"));

        //3.查询价格在2000-40000
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        rangeQueryBuilder.gte(2000);
        rangeQueryBuilder.lte(40000);
        boolQuery.filter(rangeQueryBuilder);

        searchSourceBuilder.query(boolQuery);
        searchRequest.source(searchSourceBuilder);
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);
        SearchHit[] hits = search.getHits().getHits();
        for (int i = 0; i < hits.length; i++) {
            String source = hits[i].getSourceAsString();
            Goods goods = JSON.parseObject(source, Goods.class);
            System.out.println(goods.toString());
        }
    }

    /**
     * 聚合查询
     * 查询title中包含手机的品牌
     * 价格最贵的手机
     * @throws IOException
     */
    @Test
    public void testaggs() throws IOException {
        SearchRequest searchRequest = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //添加条件
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("title", "手机");
        searchSourceBuilder.query(matchQueryBuilder);
        //添加聚合
        AggregationBuilder aggregation = AggregationBuilders.terms("goods_Brand").field("brandName").size(4);
        searchSourceBuilder.aggregation(aggregation);

        searchRequest.source(searchSourceBuilder);
        SearchResponse search = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);
        SearchHit[] hits = search.getHits().getHits();
        for (int i = 0; i < hits.length; i++) {
            String source = hits[i].getSourceAsString();
            Goods goods = JSON.parseObject(source, Goods.class);
            System.out.println(goods.toString());
        }
        Aggregations aggregations = search.getAggregations();
        Map<String, Aggregation> map = aggregations.asMap();
        Terms term = (Terms) map.get("goods_Brand");
        List<? extends Terms.Bucket> buckets = term.getBuckets();
        List brandName = new ArrayList<>();
        for (Terms.Bucket bucket : buckets) {
            brandName.add(bucket.getKey());
        }
        for (Object name:brandName
             ) {
            System.out.println(
                    name.toString()
            );
        }
    }

}
