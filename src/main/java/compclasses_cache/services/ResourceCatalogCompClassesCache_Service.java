package compclasses_cache.services;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import compclasses_cache.model.master.ResourceCatalogCompClassesCache;
import compclasses_cache.model.repo.IResourceCatalogCompClassesCache_Repo;

@Service("resourceCatalogCompClassesCacheServ")
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
public class ResourceCatalogCompClassesCache_Service implements IResourceCatalogCompClassesCache_Service {
	// private static final Logger logger =
	// LoggerFactory.getLogger(ResourceCatalogMasterService.class);

	@Autowired
	private IResourceCatalogCompClassesCache_Repo resourceCatalogCompClassesCacheRepo;

	@Autowired
	private Executor asyncExecutor;

	// abstract public ArrayList<Long> getAllResourcesForCompClassess();

	@Override	
	@Cacheable(value="compClassesCache",key = "new org.springframework.cache.interceptor.SimpleKey(#resCatSeqNo)")
	public CopyOnWriteArrayList<ResourceCatalogCompClassesCache> getAllResourceCatalogCompClasses(Long resCatSeqNo)
			throws InterruptedException, ExecutionException 
	{
		CompletableFuture<CopyOnWriteArrayList<ResourceCatalogCompClassesCache>> future = CompletableFuture.supplyAsync(() -> 
		{			
			CopyOnWriteArrayList<ResourceCatalogCompClassesCache> cresCatList = resourceCatalogCompClassesCacheRepo.findResourceCatalogCompClasses(resCatSeqNo);
			return cresCatList;
		},asyncExecutor);

		return future.get();
	}
}