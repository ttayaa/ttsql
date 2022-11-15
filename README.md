# ttsql



一款 零配置 基于mybatis的 框架 

`<dependency>`

    `<groupId>io.github.ttayaa.ttsql</groupId>`
    
    `<artifactId>ttsql-spring-boot-starter</artifactId>`
    
    `<version>1.0.3</version>`
    
`</dependency>`


service层 不用继承  
mapper层 不用继承 甚至不用谢mapper层   


代码 比现在市面的框架  更接近 sql 


框架基于 根据数据表生成的 实体类 

内置 模型转换     

用法  :

 支持 动态sql
   并且支持lambda 

1 单表操作

1.1, 查询 的 用法 




  //根据 主键 查询 

  PmsCategoryVo one = TTSQL.selectById(PmsCategory.class,id).executeQueryOne(PmsCategoryVo.class);
  
  //普通查询 
  List<UmsUserAddressVo> umsUserAddressVos = TTSQL.select(UmsUserAddress.class)
        .where(UmsUserAddress::getUserId, userId)
        .executeQueryList(UmsUserAddressVo.class);

 
 等等用法 .......
  
  
 // 分页查询 
  
   TTPage ttPage = new TTPage(pageReqVo.getPageNo(), pageReqVo.getPageSize());
   
      List<PmsCategoryVo> PmsCategoryVos
                    = TTSQL.select(PmsCategory.class)
                  //  .like(PmsCategory::getXXX,"%" + pageReqVo.getKeyword() + "%",pageReqVo.getKeyword()!=null)
                    .executeQueryPage(ttPage, PmsCategoryVo.class);
  


1.2, 新增 的用法  支持 主键回显

    PmsCategory po = new PmsCategory();
    BeanUtils.copyProperties(insertReqVo,po);
       
    int i = TTSQL.insertInto(po)
                .executeUpdate();
                
1.3, 更新 的用法 

    PmsCategory po = new PmsCategory();
    BeanUtils.copyProperties(updateReqVo,po);
    
    int i = TTSQL.updateById(po)
                .executeUpdate();
                
                
1.4, 删除 的用法

 int i = TTSQL.deleteById(PmsCategory.class,reqVo.getId())
                .executeUpdate();



2 多表操作 

        String[] ids = reqVo.getId().split(",");

            List<UmsCartVo> umsCartVos = TTSQL.start()
                    .select("c.*,p.product_name,p_i.url,s.stock,s.sku_name")
                    .from("ums_cart c")
                    .innerJoin("pms_spu p")
                    .innerJoin("pms_spu_img p_i")
                    .innerJoin("pms_sku s")
                    .on("c.product_id = p.product_id")
                    .and("p.product_id = p_i.item_id")
                    .and("c.sku_id = s.sku_id")
                    .where("c.user_id", userId)
                    .andIn("c.cart_id",ids)
                    .andEqual("p_i.is_main", 1)
                    .executeQueryList(UmsCartVo.class);
    
    3 多数据源 
    
    我们可以在 一个方法中 调用 N个不同的数据库   颗粒度是一条 sql语句  现在市面上的都是一个方法 只能绑定一个数据源 
    
   配置方法 在 
   项目创建一个配置类 
   
   注意 当你使用配置类配置数据源后 您的yml中的配置 就被忽略了 
   
   
   
@Configuration
public class TTSQLCfg {

    @Bean
    @Primary
    public DynamicDataSource dataSource() {
  
        //可以在方法中 自己定制 自己的 数据源 连接池等

        DruidDataSource dataSource1 = DruidDataSourceBuilder.create().build();
        dataSource1.setUrl("jdbc:mysql://localhost:3306/mall1?serverTimezone=GMT%2B8");
        dataSource1.setUsername("root");
        dataSource1.setPassword("root");


        DruidDataSource dataSource2 = DruidDataSourceBuilder.create().build();
        dataSource2.setUrl("jdbc:mysql://localhost:3306/mall2?serverTimezone=GMT%2B8");
        dataSource2.setUsername("root");
        dataSource2.setPassword("root");

        DruidDataSource dataSource3 = DruidDataSourceBuilder.create().build();
        dataSource3.setUrl("jdbc:mysql://localhost:3306/mall3?serverTimezone=GMT%2B8");
        dataSource3.setUsername("root");
        dataSource3.setPassword("root");

        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put("db1", dataSource1);
        dataSources.put("db2", dataSource2);
        dataSources.put("db3", dataSource3);

        return new DynamicDataSource(dataSource1, dataSources);

    }



}

    
    
    在项目中就可以 
    
           //不写db 默认第一个数据源
        List<HashMap> hashMaps1 = TTSQL.start().select()
                .from("pms_spu")
                .executeQueryList();

        //数据库1
        List<HashMap> hashMaps2 = TTSQL.start().select()
                .from("pms_brand")
                .executeQueryList("db2");

        //数据库2
        List<HashMap> hashMaps3 = TTSQL.start().select()
                .from("edu_teacher")
                .executeQueryList("db3");

        return BackUtil.ok()
                .data("pms_spu",hashMaps1)
                .data("pms_brand",hashMaps2)
                .data("pms_category",hashMaps3);
    
    
    
    4 实体类中的 一些注解 
    
    4.1 主键的注解
    
    @TTAutoPrimaryKey   // 使用数据库自增 数据库主键必须加上 auto_increment
    
    
    //雪花算法就是  用于 保证每个表 的 主键 都不一样  并且 有递增顺序 
    
    @TTSnowFlakePrimaryKey  // 在使用新增语句时 会 根据 雪花算法 生成 id  注意 数据库表的id 最好为 varchar
    
    //redis + 雪花  保证 高并发情况 下 每个表的主键不一样
    
    @TTRedisPrimaryKey  // 基于redis的雪花算法 只要 正常配置了redis 并加上这个注解 就可以
    
    @TTSnowFlakePrimaryKey  // 在使用新增语句时 会 根据 雪花算法 生成 id  注意 数据库表的id 最好为 varchar 
    定义为主键   这样 你的TTSQL 语句中的 xxxxbyid 才能生效
    
    
    
    @TTLogicField  逻辑删除字段  在使用delete语句时  会根据实体属性上 的这个注解 将数据更新为 1 0
    
    
    @TTCreateField 在新增的时候 会自定填充 当前时间
    
    
    @TTUpdateField 在更新的时候 会自动填充 当前时间
    
    
    注解配置比较简单  本人精力有限   如果需要的朋友 可以提需求  我新增功能
    
    

    还有很多用法  这里不一一说明 
                
