package cacheMgmt;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static java.util.Arrays.asList;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    ConcurrentMapCacheManager mgr = new ConcurrentMapCacheManager();
    mgr.setCacheNames(asList("prodStructureCache","locaStructureCache","ratingsCache","priceRangeCache","compClassesCache","resourcesCache"));
    return mgr;
  }
}