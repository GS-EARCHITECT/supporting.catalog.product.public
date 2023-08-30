package resources_cache.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import compclasses_cache.services.IResourceCatalogCompClassesCache_Service;
import location_classes_cache.model.master.ResourceCatalogLocaStructureCache;
import location_classes_cache.services.I_ResourceCatalogLocaStructureCache_Service;
import pricerange_cache.services.IResourceCatalogPriceRangeCache_Service;
import ratings_cache.services.IResourceCatalogRatingsCache_Service;
import compclasses_cache.model.master.ResourceCatalogCompClassesCache;
import resource_classes_cache.model.master.ResourceCatalogProdStructureCache;
import ratings_cache.model.master.ResourceCatalogRatingsCache;
import pricerange_cache.model.master.ResourceCatalogPriceRangeCache;
import resource_classes_cache.services.*;
import resources_cache.model.repo.*;

@Service("resourcesCacheServ")  
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
public class ResourcesCache_Service implements IResourcesCache_Service 
{
	private static final Logger logger = LoggerFactory.getLogger(ResourcesCache_Service.class);

	@Autowired
	private IResourcesCache_Repo resourcesCacheRepo;
	
	@Autowired
	private Executor asyncExecutor;
		
	@Autowired
	private	I_ResourceCatalogProdStructureCache_Service resourceCatalogProdStructureCacheServ;
	
	@Autowired
	private	I_ResourceCatalogLocaStructureCache_Service resourceCatalogLocaStructureCacheServ;
	
	@Autowired
	private IResourceCatalogCompClassesCache_Service resourceCatalogCompClassesCacheServ;
	
	@Autowired
	private IResourceCatalogRatingsCache_Service resourceCatalogRatingsCacheServ;
	
	@Autowired
	private IResourceCatalogPriceRangeCache_Service resourceCatalogPriceRangeCacheServ; 

	@Override
	@Cacheable(value="resourcesCache",key = "new org.springframework.cache.interceptor.SimpleKey(#resCatSeqNo)")
	@HystrixCommand(fallbackMethod = "getAllResources")    
	public List<Long> getAllResourcesForCatalog(Long resCatSeqNo) throws InterruptedException, ExecutionException 
	{
		CompletableFuture<List<Long>> futureC = CompletableFuture.supplyAsync(() -> 
		{
		// get resourceclassList from resource_catalog_prodstructure
		CopyOnWriteArrayList<Long> resList = new CopyOnWriteArrayList<Long>();
		List<Long> gg = null;
		CompletableFuture<ArrayList<Long>> cmpFuture=null;
		CompletableFuture<ArrayList<Long>> resFuture=null;
		CompletableFuture<ArrayList<Long>> locFuture=null;	
		CompletableFuture<ArrayList<Long>> rateFuture=null;
		CompletableFuture<ArrayList<Long>> prnFuture=null;
						
		try {
			
			cmpFuture=getResourcesForSuppliers(resCatSeqNo);
			resFuture = this.getResourcesForResourceClassList(resCatSeqNo);
			locFuture=this.getResourcesForLocationClassList(resCatSeqNo);			
			rateFuture=getResourcesForRatings(resCatSeqNo);
			prnFuture=getResourcesForPriceRange(resCatSeqNo);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		       
		       //CompletableFuture<Void> futureResult = CompletableFuture.allOf(resFuture,locFuture, cmpFuture, rateFuture, prnFuture);
		       CompletableFuture<Void> futureResult = CompletableFuture.allOf(cmpFuture,resFuture,locFuture,rateFuture,prnFuture);
		       ArrayList<Long> allList = (ArrayList<Long>) Stream.of(cmpFuture,resFuture,locFuture,rateFuture,prnFuture).map(CompletableFuture::join).flatMap(List::stream).collect(Collectors.toList());
		       
		return allList;
		},asyncExecutor);
			
		ArrayList<Long> cList = (ArrayList<Long>) futureC.get();
		ArrayList<Long> cNewList = new ArrayList<Long>(); 
		
		synchronized (cList) 
		{		
		HashSet<Long> hset = new HashSet<Long>(cList);
		cNewList.addAll(hset);			
		}
		
		return cNewList;
	}
	
	private CompletableFuture<ArrayList<Long>> getResourcesForResourceClassList(Long resCatSeqNo) throws InterruptedException, ExecutionException 
	{
		// get resourceclassList from resource_catalog_prodstructure
		CompletableFuture<CopyOnWriteArrayList<Long>> future1 = CompletableFuture.supplyAsync(() -> 
		{
		CopyOnWriteArrayList<Long> resCatList=new CopyOnWriteArrayList<Long>();
		
		try {
			CopyOnWriteArrayList<ResourceCatalogProdStructureCache> lResourceCatalogProdStructure_DTOs = resourceCatalogProdStructureCacheServ.getAllResourceCatalogProdStructures(resCatSeqNo);
		
		if (lResourceCatalogProdStructure_DTOs != null && lResourceCatalogProdStructure_DTOs.size() > 0) 
		{
			for (int i = 0; i < lResourceCatalogProdStructure_DTOs.size(); i++) 
			{
				if (lResourceCatalogProdStructure_DTOs.get(i).getParResourceClassSeqNo() != null) {
					resCatList.add(lResourceCatalogProdStructure_DTOs.get(i).getParResourceClassSeqNo());
				}
			}

			for (int i = 0; i < lResourceCatalogProdStructure_DTOs.size(); i++) {
				if (lResourceCatalogProdStructure_DTOs.get(i).getResourceClassSeqNo() != null) {
					resCatList.add(lResourceCatalogProdStructure_DTOs.get(i).getResourceClassSeqNo());
				}
			}
		}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

		return resCatList;
		},asyncExecutor);
		
		// get resources for resource classes in resourceclassList from resource_class_details
		CompletableFuture<ArrayList<Long>> cc = future1.thenApplyAsync((input) -> 
		{
			CopyOnWriteArrayList<Long> resList = null;
			ArrayList<Long> resLst = new ArrayList<Long>();
		    
			if (input!= null) 
            {
		   	    resList = resourcesCacheRepo.findResourcesForResourceClasses(input);
            }
		    
			synchronized (resLst) 
			{
			for (int i = 0; i < resList.size(); i++) 
			{
			resLst.add(resList.get(i));	
			}	
			}
		    
		    return resLst;
		},asyncExecutor);
		
						
		return cc;
}
	
	
	// get locationClassList from resource_catalog_locstructure
	private CompletableFuture<ArrayList<Long>> getResourcesForLocationClassList(Long resCatSeqNo) throws InterruptedException, ExecutionException 
	{	
	CompletableFuture<CopyOnWriteArrayList<Long>> future3 = CompletableFuture.supplyAsync(() -> 
	{
		CopyOnWriteArrayList<Long> resLocList =null;
		try {
			CopyOnWriteArrayList<ResourceCatalogLocaStructureCache> resLocComp = resourceCatalogLocaStructureCacheServ.getAllResourceCatalogLocaStructures(resCatSeqNo);
			resLocList =new CopyOnWriteArrayList<Long>();;
			
			if (resLocComp != null && resLocComp.size() > 0) 
			{
				for (int i = 0; i < resLocComp.size(); i++) 
				{
					if (resLocComp.get(i).getParLocationClassSeqNo() != null) {
						resLocList.add(resLocComp.get(i).getParLocationClassSeqNo());
					}
				}
				for (int i = 0; i < resLocComp.size(); i++) 
				{
					if (resLocComp.get(i).getLocationClassSeqNo() != null) {
						resLocList.add(resLocComp.get(i).getLocationClassSeqNo());
					}
				}
			}	
			
		}
		catch (InterruptedException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return resLocList;
		},asyncExecutor);
	

	
		// get locationsList for locations in locationClassList from place_class_details
	CompletableFuture<CopyOnWriteArrayList<Long>> future4 = future3.thenApplyAsync((input) -> 
	{
		
		CopyOnWriteArrayList<Long> resList = null;
	    if (input!= null) 
        {
	   	    resList = resourcesCacheRepo.findLocationsForLocationsInLocationClasses(input);
    }
	    return resList;
	},asyncExecutor);
					
			// get resources for locationsList from resource_location_master
	CompletableFuture<ArrayList<Long>> future5 = future4.thenApplyAsync((input) -> 
	{
		CopyOnWriteArrayList<Long> resList = null;
		ArrayList<Long> resLst = new ArrayList<Long>(); 
		
		if (input!= null) 
        {
	    	resList = resourcesCacheRepo.findResourcesForLocations(input);
        }
	    
		synchronized (resLst) 
		{
		for (int i = 0; i < resList.size(); i++) 
		{
		resLst.add(resList.get(i));	
		}	
		}
	    return resLst;
	},asyncExecutor);
	
	return future5;
}
	// get resources for suppliers
	private CompletableFuture<ArrayList<Long>> getResourcesForSuppliers(Long resCatSeqNo) throws InterruptedException, ExecutionException 
	{
		// get supplierclassList from resource_catalog_compclasses
		CompletableFuture<CopyOnWriteArrayList<Long>> future6 = CompletableFuture.supplyAsync(() -> 
		{
			CopyOnWriteArrayList<ResourceCatalogCompClassesCache> supClassComps=null;
			try {
				supClassComps = resourceCatalogCompClassesCacheServ.getAllResourceCatalogCompClasses(resCatSeqNo);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			CopyOnWriteArrayList<Long> supClassList =new CopyOnWriteArrayList<Long>();
			
			if (supClassComps != null && supClassComps.size() > 0) 
			{
				for (int i = 0; i < supClassComps.size(); i++) 
				{
					if (supClassComps.get(i).getCompClassSeqNo() != null) {
						supClassList.add(supClassComps.get(i).getCompClassSeqNo());
					}
				}

				}						
				return supClassList;
			},asyncExecutor);
		
		// get suppliersList for suppliers in supplierClassList from supplier_class_details
		CompletableFuture<CopyOnWriteArrayList<Long>> future7 = future6.thenApplyAsync((input) -> 
		{
			CopyOnWriteArrayList<Long> supComp = null;
            if (input != null) 
            {
            		supComp = resourcesCacheRepo.findSupplierListForSupplierClasses(input); 
            }
    	    return supComp;
    	},asyncExecutor);
		
		// get resources for suppliersList from SUPPLIER_PRODSERV_details	
		CompletableFuture<ArrayList<Long>> future8 = future7.thenApplyAsync((input) -> 
		{
			CopyOnWriteArrayList<Long> resList = null;
            ArrayList<Long> resLst = new ArrayList<Long>(); 
			
            if (input != null) 
            {
            	resList = resourcesCacheRepo.findResourcesForSuppliers(input); 
            }
            
    		synchronized (resLst) 
    		{
    		for (int i = 0; i < resList.size(); i++) 
    		{
    		resLst.add(resList.get(i));	
    		}	
    		}
    	    return resLst;

    	},asyncExecutor);

		return future8;		
				
	}
	
	// get resources for ratings
	private CompletableFuture<ArrayList<Long>> getResourcesForRatings(Long resCatSeqNo) throws InterruptedException, ExecutionException 
	{
		CompletableFuture<CopyOnWriteArrayList<Float>> future9 = CompletableFuture.supplyAsync(() -> 
		{
			CopyOnWriteArrayList<ResourceCatalogRatingsCache> supRateList=null;
			try {
				supRateList = resourceCatalogRatingsCacheServ.getAllResourceCatalogRatings(resCatSeqNo);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			CopyOnWriteArrayList<Float>  cList = new CopyOnWriteArrayList<Float>(); 
			if(supRateList!=null)
			{	
			for (int i = 0; i < supRateList.size(); i++) 
			{
			cList.add(supRateList.get(i).getRating());	
			}	
			}
			return cList;
		},asyncExecutor);
		
		CompletableFuture<ArrayList<Long>> future10 = future9.thenApplyAsync((input) -> 
		{
			CopyOnWriteArrayList<Long> resList = null;
			ArrayList<Long> resLst = new ArrayList<Long>();
			
			if (input != null) 
            {
            resList= resourcesCacheRepo.findResourcesForRatings(input); 
            }
			
			synchronized (resLst) 
    		{
    		for (int i = 0; i < resList.size(); i++) 
    		{
    		resLst.add(resList.get(i));	
    		}	
    		}
    	    return resLst;    	    
    	},asyncExecutor);

		return future10;
	}
	

	// get resources for price range
	private CompletableFuture<ArrayList<Long>> getResourcesForPriceRange(Long resCatSeqNo) throws InterruptedException, ExecutionException 
	{
        CompletableFuture<ResourceCatalogPriceRangeCache> future11 = CompletableFuture.supplyAsync(() -> 
		{
			ResourceCatalogPriceRangeCache supPriceRange=null;
			
			try {
				supPriceRange = resourceCatalogPriceRangeCacheServ.getAllResourceCatalogPriceRange(resCatSeqNo);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
			return supPriceRange;
		},asyncExecutor);
		
		CompletableFuture<ArrayList<Long>> future12 = future11.thenApplyAsync((input) -> 
		{
			CopyOnWriteArrayList<Long> resList = null;
			ArrayList<Long> resLst = new ArrayList<Long>();
            
			if (input != null) 
            {
			resList= resourcesCacheRepo.findResourcesForPriceRange(input.getFrPrice(),input.getToPrice()); 
            }
			synchronized (resLst) 
    		{
    		for (int i = 0; i < resList.size(); i++) 
    		{
    		resLst.add(resList.get(i));	
    		}	
    		}
    	    return resLst;    	    

    	},asyncExecutor);

		return future12;
	}

	@Cacheable("resourcesCache")
	public ArrayList<Long> getAllResources() throws InterruptedException, ExecutionException 
	{
        CompletableFuture<ArrayList<Long>> future13 = CompletableFuture.supplyAsync(() -> 
		{
			ArrayList<Long> resList=null;
			
			CopyOnWriteArrayList<Long>	cpresList = resourcesCacheRepo.findAllResources();
			if(cpresList!=null)
			{
			resList = new ArrayList<Long>();
			
			for (int i = 0; i < cpresList.size(); i++) 
			{
			resList.add(cpresList.get(i));	
			}
			}			
			
			
			return resList;
		},asyncExecutor);

	return future13.get();
	}
}