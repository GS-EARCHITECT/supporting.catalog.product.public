package compclasses_cache.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import compclasses_cache.model.master.ResourceCatalogCompClassesCache;
import compclasses_cache.services.IResourceCatalogCompClassesCache_Service;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/resourceCatalogCompClassesCacheManagement")
public class ResourceCatalogCompClassesCachingController 
{

	private static final Logger logger = LoggerFactory.getLogger(ResourceCatalogCompClassesCachingController.class);

	@Autowired
	private IResourceCatalogCompClassesCache_Service resourceCatalogCompClassesCacheServ;

	@GetMapping(value = "/getAllResourceCatalogCompClassesFromCache/{resCatSeqNo}", produces = { MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<Flux<ResourceCatalogCompClassesCache>> getAllResourceCatalogCompClassesFromCache(@PathVariable Long resCatSeqNo) 
	{
		Flux<ResourceCatalogCompClassesCache> resourceCatalogDTOs=Flux.create(emitter -> {
			CompletableFuture<CopyOnWriteArrayList<ResourceCatalogCompClassesCache>> future = CompletableFuture.supplyAsync(() -> 
			{
			CopyOnWriteArrayList<ResourceCatalogCompClassesCache> resourceCatalogDTOs2=null;	
			try 
			{
				resourceCatalogDTOs2 = resourceCatalogCompClassesCacheServ.getAllResourceCatalogCompClasses(resCatSeqNo);
				logger.info("comp class  size :"+resourceCatalogDTOs2.size());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return resourceCatalogDTOs2;
			});
			future.whenComplete((resCatDtoList2, exception) -> {
				if (exception == null) {
					resCatDtoList2.forEach(emitter::next);
					emitter.complete();
				} else {
					emitter.complete();
				}
			});
		}); 
		
	return new ResponseEntity<>(resourceCatalogDTOs, HttpStatus.OK);

		
	}

}