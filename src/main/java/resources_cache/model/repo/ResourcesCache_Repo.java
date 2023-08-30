package resources_cache.model.repo;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import resources_cache.model.dto.ResourceCatalogProdStructure_DTO;
import resources_cache.model.master.ResourceCacheMaster;
import resources_cache.model.dto.ResourceClassDetail_DTO;
import resources_cache.model.dto.ResourceCatalogLocStructure_DTO;
import resources_cache.model.dto.ResourceLocationMaster_DTO;
import resources_cache.model.dto.PlaceClassDetail_DTO;;

@Repository("resourcesCacheRepo")
public class ResourcesCache_Repo implements IResourcesCache_Repo {
//	private static final Logger logger = LoggerFactory.getLogger(ResourcesCache_Repo.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Executor asyncExecutor;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public CompletableFuture<CopyOnWriteArrayList<ResourceCacheMaster>> findAllResourcesForConditions(
			Integer parmLength, CopyOnWriteArrayList<Long> cList, CopyOnWriteArrayList<Long> rList, String catalog) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		String qryString = null;
		boolean cflag = false;
		boolean rflag = false;
		boolean caflag = false;

		if (parmLength == 1) {
			if (cList != null) {
				qryString = "select * from resource_class_details where resource_class_seq_no IN (:cList)";
				mapSqlParameterSource.addValue("cList", cList);
			}

			if (rList != null) {
				qryString = "select * from resource_catalog_master where resource_catalog_seq_no IN (:rList)";
				mapSqlParameterSource.addValue("rList", rList);
			}
			if (catalog != null) {
				qryString = "select * from resource_catalog_master where upper(trim(catalog)) = upper(trim(:catalog))";
				mapSqlParameterSource.addValue("catalog", catalog);
			}
		}

		if (parmLength > 1) {
			if (cList != null) {
				qryString = "select * from resource_catalog_master where company_seq_no IN (:cList)";
				mapSqlParameterSource.addValue("cList", cList);
				cflag = true;
			} else if (rList != null) {
				qryString = "select * from resource_catalog_master where resource_catalog_seq_no IN (:rList)";
				mapSqlParameterSource.addValue("rList", rList);
				rflag = true;
			} else if (catalog != null) {
				qryString = "select * from resource_catalog_master where upper(trim(catalog)) = upper(trim(:catalog))";
				mapSqlParameterSource.addValue("catalog", catalog);
				caflag = true;
			}

			for (int i = 1; i < parmLength; i++) {
				if (cList != null && !cflag) {
					qryString = qryString + " or company_seq_no IN (:cList)";
					mapSqlParameterSource.addValue("cList", cList);
					cflag = true;
				} else if (rList != null && !rflag) {
					qryString = qryString + " or resource_catalog_seq_no IN (:rList)";
					mapSqlParameterSource.addValue("rList", rList);
					rflag = true;
				} else if (catalog != null && !caflag) {
					qryString = qryString + " or upper(trim(catalog)) = upper(trim(:catalog))";
					mapSqlParameterSource.addValue("catalog", catalog);
					caflag = true;
				}

			}

		}

		CopyOnWriteArrayList<ResourceCacheMaster> lCatalogReadMasters = (CopyOnWriteArrayList<ResourceCacheMaster>) namedParameterJdbcTemplate
				.query(qryString, mapSqlParameterSource,
						(rs, rowNum) -> new ResourceCacheMaster(rs.getLong("resource_seq_no")));

		return CompletableFuture.completedFuture(lCatalogReadMasters);
	}

	// dto basis - get resourceclassList from resource_catalog_prodstructure
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findResourceClassesForCatalog(Long resCatSeqNo) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("resCatSeqNo", resCatSeqNo);
		String qryString = "select * from resource_catalog_prodstructure where resource_catalog_seq_no = :resCatSeqNo";
		CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();

		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			List<ResourceCatalogProdStructure_DTO> lResourceCatalogProdStructure_DTOs = namedParameterJdbcTemplate
					.query(qryString, mapSqlParameterSource,
							(rs, rowNum) -> new ResourceCatalogProdStructure_DTO(rs.getLong("resource_class_seq_no"),
									rs.getLong("par_resource_class_seq_no"), rs.getLong("resource_catalog_seq_no")));

			if (lResourceCatalogProdStructure_DTOs != null && lResourceCatalogProdStructure_DTOs.size() > 0) {
				for (int i = 0; i < lResourceCatalogProdStructure_DTOs.size(); i++) {
					if (lResourceCatalogProdStructure_DTOs.get(i).getParResourceClassSeqNo() != null) {
						cList.add(lResourceCatalogProdStructure_DTOs.get(i).getParResourceClassSeqNo());
					}
				}

				for (int i = 0; i < lResourceCatalogProdStructure_DTOs.size(); i++) {
					if (lResourceCatalogProdStructure_DTOs.get(i).getResourceClassSeqNo() != null) {
						cList.add(lResourceCatalogProdStructure_DTOs.get(i).getParResourceClassSeqNo());
					}
				}
			}

			return cList;
		}, asyncExecutor);

		CopyOnWriteArrayList<Long> resourceclassList2 = null;
		;
		try {
			resourceclassList2 = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resourceclassList2;
	}

	// noDTo basis - get resources for resource classes in resourceclassList from
	// resource_class_details
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findResourcesForResourceClassesnoDTO(CopyOnWriteArrayList<Long> resClassList) {
		CompletableFuture<CopyOnWriteArrayList<Long>> futurexx = CompletableFuture.supplyAsync(() -> {
			List<Long> lResSeqNos = jdbcTemplate.queryForList(
					"select resource_seq_no from resource_class_details where resource_class_seq_no in (:resClassList)",
					Long.class, resClassList);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(lResSeqNos);
			return cList;
		});

		CopyOnWriteArrayList<Long> cAList = null;
		try {
			cAList = futurexx.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cAList;
	}

	// DTO basis - get resources for resource classes in resourceclassList from
	// resource_class_details
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findResourcesForResourceClasses(CopyOnWriteArrayList<Long> resClassList) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("resClassList", resClassList);
		String qryString = "select * from resource_class_details where resource_class_seq_no in (:resClassList)";
		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			CopyOnWriteArrayList<Long> resourceList = null;
			List<ResourceClassDetail_DTO> lResourceClassDetails_DTOs = namedParameterJdbcTemplate.query(qryString,
					mapSqlParameterSource,
					(rs, rowNum) -> new ResourceClassDetail_DTO(rs.getLong("resource_class_seq_no"),
							rs.getLong("resource_seq_no"), rs.getLong("party_seq_no")));

			if (lResourceClassDetails_DTOs != null && lResourceClassDetails_DTOs.size() > 0) {
				resourceList = new CopyOnWriteArrayList<Long>();

				for (int i = 0; i < lResourceClassDetails_DTOs.size(); i++) {
					resourceList.add(lResourceClassDetails_DTOs.get(i).getResourceSeqNo());
				}
			}
			return resourceList;
		}, asyncExecutor);

		CopyOnWriteArrayList<Long> resourceList2 = null;
		try {
			resourceList2 = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resourceList2;
	}

	// noDTo basis - get locationClassList from resource_catalog_locstructure
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findLocationClassesForCatalognoDTO(Long resCatSeqNo) {
		String qryString = "select place_class_seq_no from resource_catalog_locstructure where resource_catalog_seq_no = :resCatSeqNo";

		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			List<Long> locclassList = jdbcTemplate.queryForList(qryString, Long.class, resCatSeqNo);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(locclassList);
			return cList;
		});

		CopyOnWriteArrayList<Long> cArrayList = null;

		try {
			cArrayList = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cArrayList;
	}

	// DTo basis - get locationClassList from resource_catalog_locstructure
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findLocationClassesForCatalog(Long resCatSeqNo) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("resCatSeqNo", resCatSeqNo);
		String qryString = "select * from resource_catalog_locstructure where resource_catalog_seq_no = :resCatSeqNo";
		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			CopyOnWriteArrayList<Long> locclassList = null;
			List<ResourceCatalogLocStructure_DTO> lResourceCatalogLocStructure_DTOs = namedParameterJdbcTemplate.query(
					qryString, mapSqlParameterSource,
					(rs, rowNum) -> new ResourceCatalogLocStructure_DTO(rs.getLong("place_class_seq_no"),
							rs.getLong("par_place_class_seq_no"), rs.getLong("resource_catalog_seq_no")));

			if (lResourceCatalogLocStructure_DTOs != null && lResourceCatalogLocStructure_DTOs.size() > 0) {
				locclassList = new CopyOnWriteArrayList<Long>();
				for (int i = 0; i < lResourceCatalogLocStructure_DTOs.size(); i++) {
					if (lResourceCatalogLocStructure_DTOs.get(i).getParLocationClassSeqNo() != null) {
						locclassList.add(lResourceCatalogLocStructure_DTOs.get(i).getParLocationClassSeqNo());
					}

					if (lResourceCatalogLocStructure_DTOs.get(i).getLocationClassSeqNo() != null) {
						locclassList.add(lResourceCatalogLocStructure_DTOs.get(i).getLocationClassSeqNo());
					}
				}
			}

			return locclassList;
		});

		CopyOnWriteArrayList<Long> cList = null;
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

	// noDTo basis - get locationsList for locations in locationClassList from
	// place_class_details
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findLocationsForLocationsInLocationClassesnoDTO(
			CopyOnWriteArrayList<Long> locClassList) {
		String qryString = "select place_seq_no from place_class_details where place_class_seq_no in (:locClassList)";
		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			List<Long> locList = jdbcTemplate.queryForList(qryString, Long.class, locClassList);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(locList);
			return cList;
		});

		CopyOnWriteArrayList<Long> cArrayList = null;

		try {
			cArrayList = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cArrayList;
	}

	// DTO basis - get locationsList for locations in locationClassList from
	// place_class_details
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findLocationsForLocationsInLocationClasses(
			CopyOnWriteArrayList<Long> locClassList) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("locClassList", locClassList);
		String qryString = "select * from place_class_details where place_class_seq_no in (:locClassList)";
		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			CopyOnWriteArrayList<Long> locList = null;
			List<PlaceClassDetail_DTO> lpPlaceClassDetail_DTOs = namedParameterJdbcTemplate.query(qryString,
					mapSqlParameterSource, (rs, rowNum) -> new PlaceClassDetail_DTO(rs.getLong("party_seq_no"),
							rs.getLong("place_class_seq_no"), rs.getLong("place_seq_no")));

			if (lpPlaceClassDetail_DTOs != null && lpPlaceClassDetail_DTOs.size() > 0) {
				locList = new CopyOnWriteArrayList<Long>();

				for (int i = 0; i < lpPlaceClassDetail_DTOs.size(); i++) {
					locList.add(lpPlaceClassDetail_DTOs.get(i).getPlaceSeqNo());
				}
			}
			return locList;
		});

		CopyOnWriteArrayList<Long> cArrayList = null;

		try {
			cArrayList = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cArrayList;

	}

	// noDTO basis - get resources for locationsList from resource_location_master
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findResourcesForLocationsnoDTO(CopyOnWriteArrayList<Long> locList) {
		String qryString = "select resource_seq_no from resource_location_master where location_seq_no in (:locList)";
		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			List<Long> resList = jdbcTemplate.queryForList(qryString, Long.class, locList);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(resList);
			return cList;
		});
		CopyOnWriteArrayList<Long> cArrayList = null;

		try {
			cArrayList = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cArrayList;
	}

	// DTO basis - get resources for locationsList from resource_location_master
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findResourcesForLocations(CopyOnWriteArrayList<Long> locList) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("locList", locList);
		String qryString = "select * from resource_location_master where location_seq_no in (:locList)";
		CompletableFuture<CopyOnWriteArrayList<Long>> future = (CompletableFuture<CopyOnWriteArrayList<Long>>) CompletableFuture
				.supplyAsync(() -> {
					CopyOnWriteArrayList<Long> resList = null;
					List<ResourceLocationMaster_DTO> lResourcelLocationMaster_DTOs = namedParameterJdbcTemplate.query(
							qryString, mapSqlParameterSource,
							(rs, rowNum) -> new ResourceLocationMaster_DTO(rs.getLong("resource_seq_no"),
									rs.getLong("location_seq_no"), rs.getLong("company_seq_no"), rs.getFloat("qoh"),
									rs.getLong("qty_seq_no")));

					if (lResourcelLocationMaster_DTOs != null && lResourcelLocationMaster_DTOs.size() > 0) {
						resList = new CopyOnWriteArrayList<Long>();

						for (int i = 0; i < lResourcelLocationMaster_DTOs.size(); i++) {
							resList.add(lResourcelLocationMaster_DTOs.get(i).getResourceSeqNo());
						}
					}
					return (CopyOnWriteArrayList<Long>) resList;
				}, asyncExecutor);

		CopyOnWriteArrayList<Long> cArrayList = null;

		try {
			cArrayList = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return cArrayList;
	}

	// no DTO basis - get partyclassList from resource_catalog_compclasses
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findSuppliersForResourceCatalog(Long resCatSeqNo) {
		String qryString = "select company_class_seq_no from resource_catalog_compclasses where resource_catalog_seq_no = :resCatSeqNo";
		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			List<Long> compClassList = jdbcTemplate.queryForList(qryString, Long.class, resCatSeqNo);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(compClassList);
			return cList;
		}, asyncExecutor);

		CopyOnWriteArrayList<Long> compClassList2 = null;
		try {
			compClassList2 = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return compClassList2;
	}

	// noDTO - get partysList for partys in partyClassList from
	// party_class_details
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findSupplierListForSupplierClasses(CopyOnWriteArrayList<Long> partyClassList) {
		String qryString = "select party_seq_no from party_class_details where party_class_seq_no in (:partyClassList)";
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("partyClassList", partyClassList);
		CompletableFuture<CopyOnWriteArrayList<Long>> futurejj = CompletableFuture.supplyAsync(() -> {
			List<Long> suppList = namedParameterJdbcTemplate.queryForList(qryString, mapSqlParameterSource, Long.class);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(suppList);
			return cList;
		});
		CopyOnWriteArrayList<Long> sList = null;

		try {
			sList = futurejj.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sList;
	}

	// noDTO - get resources for partysList from parrtyLIER_PRODSERV_details
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findResourcesForSuppliers(CopyOnWriteArrayList<Long> partyList) {
		String qryString = "select resource_seq_no from party_prodserv_details where party_seq_no in (:partyList)";
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("partyList", partyList);
		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			List<Long> resList = namedParameterJdbcTemplate.queryForList(qryString, mapSqlParameterSource, Long.class);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(resList);
			return cList;
		});

		CopyOnWriteArrayList<Long> rList = null;
		try {
			rList = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rList;
	}

	// noDTO - get ratingsList from resource_catalog_ratings
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Float> findRatingsForResourceCatalog(Long resCatSeqNo) {
		String qryString = "select rating from resource_catalog_ratings where resource_catalog_seq_no = :resCatSeqNo";
		CopyOnWriteArrayList<Float> rateList2 = null;
		CompletableFuture<CopyOnWriteArrayList<Float>> future = CompletableFuture.supplyAsync(() -> {
			List<Float> rateList = jdbcTemplate.queryForList(qryString, Float.class, resCatSeqNo);
			CopyOnWriteArrayList<Float> cList = new CopyOnWriteArrayList<Float>();
			cList.addAll(rateList);
			return cList;
		}, asyncExecutor);

		try {
			rateList2 = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rateList2;
	}

	// noDTO - resources for matching prodservseqnos in parrtyLIER_PRODSERV_details &
	// parrtyLIER_PRODSERV_ratings & ratingsList
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findResourcesForRatings(CopyOnWriteArrayList<Float> ratingsList) {
		String qryString = "select a.resource_seq_no from party_prodserv_details a, party_prodserv_ratings b where (a.parrty_PRODSERV_SEQ_NO = b.parrty_PRODSERV_SEQ_NO and b.rating in (:ratingsList))";
		CopyOnWriteArrayList<Long> resList2 = null;
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("ratingsList", ratingsList);
		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			List<Long> resList = namedParameterJdbcTemplate.queryForList(qryString, mapSqlParameterSource, Long.class);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(resList);
			return cList;
		}, asyncExecutor);

		try {
			resList2 = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resList2;
	}

	// noDTO - get priceRange from resource_catalog_pricerange
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public Float findPriceRangeLowForResourceCatalog(Long resCatSeqNo) {
		String qryString = "select price_fr from resource_catalog_pricerange where rownum=1 and resource_catalog_seq_no = :resCatSeqNo";
		Float lowPrice = null;
		CompletableFuture<Float> future = CompletableFuture.supplyAsync(() -> {
			Float rate = jdbcTemplate.queryForObject(qryString, Float.class, resCatSeqNo);
			return rate;
		}, asyncExecutor);

		try {
			lowPrice = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lowPrice;
	}

	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public Float findPriceRangeHighForResourceCatalog(Long resCatSeqNo) {
		String qryString = "select price_to from resource_catalog_pricerange where rownum=1 and resource_catalog_seq_no = :resCatSeqNo";
		Float hiPrice = null;
		CompletableFuture<Float> future = CompletableFuture.supplyAsync(() -> {
			Float rate = jdbcTemplate.queryForObject(qryString, Float.class, resCatSeqNo);
			return rate;
		}, asyncExecutor);

		try {
			hiPrice = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hiPrice;
	}

	// noDTO - get resources for matching prodservseqnos in
	// parrtyLIER_PRODSERV_details & parrtyLIER_PRODSERV_prices & priceRange
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findResourcesForPriceRange(Float lPrice, Float hPrice) {
		String qryString = "select a.resource_seq_no from party_prodserv_details a, party_prodserv_prices b where a.party_PRODSERV_SEQ_NO = b.party_PRODSERV_SEQ_NO and b.amount >= :lPrice and b.amount <= :hPrice";
		CopyOnWriteArrayList<Long> resList2 = null;

		CompletableFuture<CopyOnWriteArrayList<Long>> future = CompletableFuture.supplyAsync(() -> {
			List<Long> resList = jdbcTemplate.queryForList(qryString, Long.class, lPrice, hPrice);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(resList);
			return cList;
		});

		try {
			resList2 = future.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resList2;
	}

	// Fallback - get all resources
	@Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public CopyOnWriteArrayList<Long> findAllResources() 
	{
		CompletableFuture<CopyOnWriteArrayList<Long>> futurexx = CompletableFuture.supplyAsync(() -> {
			List<Long> lResSeqNos = jdbcTemplate.queryForList("select resource_seq_no from resource_master",
					Long.class);
			CopyOnWriteArrayList<Long> cList = new CopyOnWriteArrayList<Long>();
			cList.addAll(lResSeqNos);
			return cList;
		});

		CopyOnWriteArrayList<Long> cAList = null;
		try {
			cAList = futurexx.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return cAList;
	}

}
