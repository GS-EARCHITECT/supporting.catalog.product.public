package compclasses_cache.model.repo;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import compclasses_cache.model.master.ResourceCatalogCompClassesCache;

@Repository("resourceCatalogCompClassesRepo")
public class ResourceCatalogCompClassesCache_Repo implements IResourceCatalogCompClassesCache_Repo {
	// private static final Logger logger =
	// LoggerFactory.getLogger(ResourceCatalogReadMasterRepo.class);

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	private Executor asyncExecutor;

	public CopyOnWriteArrayList<ResourceCatalogCompClassesCache> findResourceCatalogCompClasses(
			Long resCatSeqNo) {

		CompletableFuture<CopyOnWriteArrayList<ResourceCatalogCompClassesCache>> future = CompletableFuture.supplyAsync(() -> 
		{
			MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
			mapSqlParameterSource.addValue("resCatSeqNo", resCatSeqNo);
			CopyOnWriteArrayList<ResourceCatalogCompClassesCache> cList = null; 
			String qryString = "select * from resource_catalog_compclasses a where (a.resource_catalog_seq_no = :resCatSeqNo)";
			ArrayList<ResourceCatalogCompClassesCache> resourceCatalogCompClasses = (ArrayList<ResourceCatalogCompClassesCache>) namedParameterJdbcTemplate
					.query(qryString, mapSqlParameterSource,
							(rs, rowNum) -> new ResourceCatalogCompClassesCache
									(
									rs.getLong("company_class_seq_no"),
									rs.getLong("resource_catalog_seq_no")
									)
									);
			cList = new CopyOnWriteArrayList<ResourceCatalogCompClassesCache>();
			cList.addAll(resourceCatalogCompClasses);
			return cList;
		}, asyncExecutor);

		CopyOnWriteArrayList<ResourceCatalogCompClassesCache> cList = null; 
		try {
			cList = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return cList;
	}

}
