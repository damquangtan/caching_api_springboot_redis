# Spring Boot Redis Cache
Làm thế nào để thêm Redis Cache vào ứng dụng Spring Boot của bạn (ví dụ CRUD) sử dụng spring-boot-starter-data-redis và Lettuce Connector.

## Tại sao sử dụng Redis Cache?

- Khi một services có trải nghiệm số lượng request lớn và dữ liệu được yêu cầu vẫn ở trạng thái tĩnh, giải pháp bộ nhớ đệm (caching) có thể giảm đáng kể thời gian phản hồi và giảm tải cho service.

- Bộ nhớ đệm (Cache) là một loại bộ nhớ phụ (auxiliary memory) được thiết kế đặc biệt để chứa dữ liệu hoặc cấu trúc thường xuyên được các ứng dụng hoặc trang web truy cập. Khả năng truy cập nhanh chóng và dễ dàng khiến nó trở thành một công cụ có giá trị trong việc giúp các ứng dụng và trang web này hoạt động nhanh hơn.

- Có nhiều kiểu caching, bao gồm:
    + Memory cache: Primary Cache L1, Secondary Cache L2, Main Memory L3 Cache.
    + Web Cache: Site Cache, Browser Cache, Micro Cache, Server Cache.
    + Application/ Software Cache.
    + Data Caching.
    + Application/Output Cache.
    + Distributed Caching.

Chúng ta sẽ xây dựng Spring Boot Redis Cache với mục đích Data Caching.

Giả sử chúng ta có một Spring Boot Application mà phơi ra REST API cho một ứng dụng Tutorial.

| Methods   | Url                           |   Actions                                    |       
| --------  | -------                       | -------                                      |
| POST      | /api/tutorials                | Tạo mới một Tutorial                         |
| GET       | /api/tutorials                | Lấy thông tin tất cả tutorials               |
| GET       | /api/tutorials/:id            | Lấy thông tin một Tutorial theo id           |
| PUT       | /api/tutorials/:id            | Cập nhật thông tin một Tutorial theo id      |
| DELETE    | /api/tutorials/:id            | Xoá thông tin một Tutorial theo id           |
| DELETE    | /api/tutorials                | Xoá tất cả Tutorials                         |
| GET       | /api/tutorials/publised       | Tìm tất cả Tutorials đã được phát hành       |
| GET       | /api/tutorials?title=[keyword]| Tìm tất cả Tutorials có tiêu đề chứa keyword |


Chúng ta sẽ thêm Redis Cache cho ứng dụng Spring Boot CRUD trên bằng việc lưu trữ kết quả truy vấn (GET requests) với Redis.

Điều đó có nghĩa là kết quả của các truy vấn dưới đây sẽ được cache:

| Methods   | Url                           |   Actions                                    |       
| --------  | -------                       | -------                                      |
| GET       | /api/tutorials                | Lấy thông tin tất cả tutorials               |
| GET       | /api/tutorials/:id            | Lấy thông tin một Tutorial theo id           |
| GET       | /api/tutorials/publised       | Tìm tất cả Tutorials đã được phát hành       |
| GET       | /api/tutorials?title=[keyword]| Tìm tất cả Tutorials có tiêu đề chứa keyword |

Những gì chúng ta cần là thêm package **config** và một package **service** để caching abstraction.

![Alt text](image.png)

## Bước 1: Cài đặt Redis
-> TODO: cần hoàn thành

## Import Redis vào Spring Boot

Để sử dụng Redis Cache trong ứng dụng maven, bạn cần thêm *spring-boot-starter-data-redis* dependency vào file pom dự án của bạn.

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

Thư viện này sẽ hỗ trợ Java Redis client tương tác với Redis server. Trong hướng dẫn này, chúng ta sẻ dụng Lettuce cùng với Spring Data.

## Cấu hình kết nối Redis

Chúng ta cấu hình thông tin xác thực để kết nối với thể hiện của Redis từ ứng dụng Spring Boot trong file *application.properties*.

    redis.host=localhost
    redis.port=6379

Sau đó, chúng ta tiếp tục cấu hình Spring Boot Redis Lettuce bằng cách tạo một factory kết nối Lettuce mới (new Lettuce connection factory)

**config/RedisConfig.java**
```java
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class RedisConfig {
  @Value("${redis.host}")
  private String redisHost;

  @Value("${redis.port}")
  private int redisPort;

  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisHost, redisPort);

    return new LettuceConnectionFactory(configuration);
  }
}
```

## RedisCacheManager trong Spring Boot
Với package *org.springframework.data.redis.cache*, Spring Redis cung cấp một triển khai tích hợp có sẵn (build-in implementation) để trừu tượng hoá bộ đệm(caching abstraction) Spring. Cái hay của trừu tượng hoá bộ đệm là nó cho phép sử dụng nhiều giải pháp bộ nhớ đệm với những thay đổi tối thiểu đối với codebase của bạn.

Điều quan trọng cần lưu ý là dịch vụ bộ nhớ đệm (caching service) chỉ là một bản tóm tắt chứ không phải triển khai bộ nhớ đệm, nghĩa là nó vẫn cần một giải pháp lưu trữ thực tế dữ liệu được lưu trữ trong bộ nhớ đệm. Khái niệm này được thực hiện hoá thông qua việc sử dụng các interfaces *org.springframework.cache.Cache* và *org.springframework.cache.CacheManager*

Thêm RedisCacheManager vào file cấu hình trên để sử dụng Redis làm phương pháp triển khai sao lưu.

**config/RedisConfig.java**
```java
import org.springframework.data.redis.cache.RedisCacheManager;

@Configuration
public class RedisConfig {

  // ...

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    return RedisCacheManager.create(connectionFactory);
  }
}
```

## Tạo service cho Redis Cache abstraction

Để bật bộ nhớ đệm cho các methods cụ thể, chúng ta sử dụng annotation @Cacheable, chỉ định các phương thức có thể lưu vào bộ nhớ đệm. Điều này có nghĩa là kết quả của method được lưu trữ trong bộ nhớ đệm, do đó các lệnh gọi tiếp theo có cùng đối số có thể trả về giá trị được lưu trữ trong bộ nhớ đệm mà không cần gọi chính phương thức đó. Annotation chỉ yêu cầu tên của bộ đệm được liên kết với method, như trong ví dụ sau:

**service**/TutorialService.java
```java
@Service
public class TutorialService {
  @Autowired
  TutorialRepository tutorialRepository;

  @Cacheable("tutorials")
  public List<Tutorial> findAll() {
    doLongRunningTask();

    return tutorialRepository.findAll();
  }

  @Cacheable("tutorials")
  public List<Tutorial> findByTitleContaining(String title) {
    doLongRunningTask();

    return tutorialRepository.findByTitleContaining(title);
  }

  @Cacheable("tutorial")
  public Optional<Tutorial> findById(long id) {
    doLongRunningTask();

    return tutorialRepository.findById(id);
  }

  @Cacheable("published_tutorials")
  public List<Tutorial> findByPublished(boolean isPublished) {
    doLongRunningTask();

    return tutorialRepository.findByPublished(isPublished);
  }

  // other methods...

  private void doLongRunningTask() {
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
```

Phương thức được chú thích (annotated) cùng với @Cacheable("tutorials") hoặc @Cacheable(value="tutorials"), sẽ yêu cầu Spring lưu kết quả của phương thức này vào bộ nhớ đệm có tên là *"tutorials"*. Giá trị (bí danh (alias) cho cacheNames) là tên của bộ đệm được sử dụng để lưu trữ kết quả lệnh gọi method.

Khi phương thức findAll() được gọi, nó được liên kết với tên bộ đệm "tutorials" và bộ đệm được kiểm tra để xem liệu method đó đã được gọi chưa, ngăn không cho nó được thực thi lại. Mặc dù thông thường chỉ có một bộ đệm duy nhất được chỉ định nhưng annotation cho phép cung cấp nhiều tên, cho phép sử dụng nhiều bộ đệm. Trong trường hợp này, mỗi bộ đệm được đánh giá trước khi method được thực thi và nếu bất kỳ bộ đệm nào chứa giá trị mong muốn thì nó sẽ được trả về.


## Enable Caching Annotations

Để bật chú thích bộ nhớ đệm, chúng ta sử dụng annotation @EnableCaching

**service**/TutorialService.java
```java
@Service
@EnableCaching
public class TutorialService {
  
}
```

## Tuỳ chỉnh cấu hình bộ nhớ đệm Redis (Customize Redis Cache Configuration)

Hành vi của RedisCacheManager có thể được tuỳ chỉnh bằng RedisCacheManagerBuilder.

RedisCache được tạo thông qua RedisCacheManager chịu sự điều chỉnh của RedisCacheConfiguration, cho phép bạn chỉ định thời gian hết hạn khoá(TTL), triển khai RedisSerializer để chuyển đổi giữa định dạng lưu trữ nhị phân và các định dạng khác. Ví dụ dưới đây minh hoạ cách đạt được điều này:
**config**/RedisConfig.java
```java
@Configuration
public class RedisConfig {

  // ...

  @Bean
  public RedisCacheManager cacheManager() {
    RedisCacheConfiguration cacheConfig = myDefaultCacheConfig(Duration.ofMinutes(10)).disableCachingNullValues();

    return RedisCacheManager.builder(redisConnectionFactory())
        .cacheDefaults(cacheConfig)
        .withCacheConfiguration("tutorials", myDefaultCacheConfig(Duration.ofMinutes(5)))
        .withCacheConfiguration("tutorial", myDefaultCacheConfig(Duration.ofMinutes(1)))
        .build();
  }

  private RedisCacheConfiguration myDefaultCacheConfig(Duration duration) {
    return RedisCacheConfiguration
        .defaultCacheConfig()
        .entryTtl(duration)
        .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
  }
}
```

## Chạy và kiểm tra (run and check)
- Chạy Redis trước (run docker-compose up -d)
- Chạy ứng dụng Spring Boot với maven bằng câu lệnh mvn spring-boot:run
- Tạo một vài tutorials với - POST: http://localhost:8080/api/tutorials.
