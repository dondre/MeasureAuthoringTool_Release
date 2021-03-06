package mat.dao.impl.clause;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;

import mat.client.measure.MeasureSearchFilterPanel;
import mat.dao.clause.CQLLibraryAssociationDAO;
import mat.dao.search.GenericDAO;
import mat.model.LockedUserInfo;
import mat.model.User;
import mat.model.clause.CQLLibrary;
import mat.model.clause.ShareLevel;
import mat.model.cql.CQLLibraryAssociation;
import mat.model.cql.CQLLibraryShare;
import mat.model.cql.CQLLibraryShareDTO;
import mat.server.LoggedInUserUtil;
import mat.server.util.MATPropertiesService;
import mat.shared.StringUtility;

public class CQLLibraryDAO extends GenericDAO<CQLLibrary, String> implements mat.dao.clause.CQLLibraryDAO {
	
	/** The Constant logger. */
	private static final Log logger = LogFactory.getLog(CQLLibraryDAO.class);
	
	/** The cql library association DAO. */
	@Autowired
	private CQLLibraryAssociationDAO cqlLibraryAssociationDAO;
		
class CQLLibraryComparator implements Comparator<CQLLibrary> {
		
		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(CQLLibrary o1, CQLLibrary o2) {
			// 1 if either isDraft
			// 2 version
			int ret = o1.isDraft() ? -1 : o2.isDraft() ? 1
					: compareDoubleStrings(o1.getVersion(), o2.getVersion());
			return ret;
		}
		
		/**
		 * Compare double strings.
		 * 
		 * @param s1
		 *            the s1
		 * @param s2
		 *            the s2
		 * @return the int
		 */
		private int compareDoubleStrings(String s1, String s2) {
			Double d1 = Double.parseDouble(s1);
			Double d2 = Double.parseDouble(s2);
			return d2.compareTo(d1);
		}
	}
	
	/*
	 * assumption: each CQL in this list is part of the same measure set
	 */
	/**
	 * The Class CQLLibraryListComparator.
	 */
	class CQLLibraryListComparator implements Comparator<List<CQLLibrary>> {
		
		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(List<CQLLibrary> o1, List<CQLLibrary> o2) {
			String v1 = o1.get(0).getName();
			String v2 = o2.get(0).getName();
			return v1.compareToIgnoreCase(v2);
		}
	}
	
	/** The lock threshold. */
	private final long lockThreshold = 3 * 60 * 1000; // 3 minutes

	@Override
	public List<CQLLibrary> searchForIncludes(String setId, String searchText){
		String searchString = searchText.toLowerCase().trim();
		Criteria cCriteria = getSessionFactory().getCurrentSession()
				.createCriteria(CQLLibrary.class);
		cCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		cCriteria.add(Restrictions.eq("draft", false));
		cCriteria.add(Restrictions.eq("qdmVersion", MATPropertiesService.get().getQmdVersion()));
		cCriteria.addOrder(Order.desc("set_id"))
		.addOrder(Order.desc("version"));
		cCriteria.setFirstResult(0);
		
		List<CQLLibrary> libraryResultList = cCriteria.list();
		
		List<CQLLibrary> orderedCQlLibList = null;
		if(libraryResultList != null){
			orderedCQlLibList = sortLibraryList(libraryResultList);
		} else {
			orderedCQlLibList = new ArrayList<CQLLibrary>();
		}
		
		StringUtility su = new StringUtility();
		List<CQLLibrary> orderedList = new ArrayList<CQLLibrary>();
		
		Iterator<CQLLibrary> orderedCQlLibListIte = orderedCQlLibList.iterator();
		
		//Adding logic to get rid of libraries with second level of child and also those with cyclic dependency.
		 List<CQLLibraryAssociation> totalAssociations = new ArrayList<CQLLibraryAssociation>();
		 while(orderedCQlLibListIte.hasNext()){
			 CQLLibrary cqlLibrary = orderedCQlLibListIte.next();
	               String asociationId = (cqlLibrary.getMeasureId() != null) ? cqlLibrary.getMeasureId():cqlLibrary.getId();
	               
	               if(cqlLibraryAssociationDAO.findAssociationCount(asociationId) == 0){
	            	   if(setId != null && asociationId != null){
	            		   if(setId.equalsIgnoreCase(getSetIdForCQLLibrary(asociationId))){
	            			   orderedCQlLibListIte.remove();
	            		   }
	            	   }
	               } else {
	            	   totalAssociations = cqlLibraryAssociationDAO.getAssociations(asociationId);
	            	   if(hasChildLibraries(totalAssociations)){
	            		   orderedCQlLibListIte.remove();
	            	   }
	            	   else if(hasCyclicDependency(setId, asociationId)){
	            		   orderedCQlLibListIte.remove();
	            	   }
	               }
	        }
		
	        for (CQLLibrary cqlLibrary : orderedCQlLibList) {
				
				boolean matchesSearch = searchResultsForCQLLibrary(searchString, su,
						cqlLibrary);
				if (matchesSearch) {
					orderedList.add(cqlLibrary);
				}
			}
		return orderedList;
		
	}
	
	private boolean hasChildLibraries(List<CQLLibraryAssociation> totalAssociations) {
		for(CQLLibraryAssociation result : totalAssociations){
			String associatedMeasureId = getAssociatedMeasureId(result.getCqlLibraryId());
			String searchId = (associatedMeasureId!=null) ? associatedMeasureId:result.getCqlLibraryId();
				if(cqlLibraryAssociationDAO.findAssociationCount(searchId) != 0){
					return true;
				}
		}
		return false;
	}
	
	private boolean hasCyclicDependency(String setId, String asociationId){
		String associateSetId = getSetIdForCQLLibrary(asociationId);
		if(setId != null && associateSetId != null){
			if(!setId.equalsIgnoreCase(associateSetId)){
				List<CQLLibraryAssociation> primaryAssociations = cqlLibraryAssociationDAO.getAssociations(asociationId);
				for(CQLLibraryAssociation parent : primaryAssociations){
					String associatedMeasureId = getAssociatedMeasureId(parent.getCqlLibraryId());
					String searchId = (associatedMeasureId!=null) ? associatedMeasureId:parent.getCqlLibraryId();
					if(cqlLibraryAssociationDAO.findAssociationCount(searchId) != 0){
							hasCyclicDependency(setId,searchId);
					}
				}
			}else{
				System.out.println("The CqlLibrary with Id : "+asociationId+" is removed from list due to cyclic dependency with existing libraries");
				logger.info("The CqlLibrary with Id : "+asociationId+" is removed from list due to cyclic dependency with existing libraries");
				return true;
			}
		}
		return false;
	}

	@Override
	public List<CQLLibraryShareDTO> search(String searchText, int pageSize, User user, int filter ) {
		
		String searchString = searchText.toLowerCase().trim();
		Criteria cCriteria = getSessionFactory().getCurrentSession()
				.createCriteria(CQLLibrary.class);
		cCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			if (filter == MeasureSearchFilterPanel.MY_MEASURES) {
				cCriteria.add(Restrictions.or(
						Restrictions.eq("ownerId.id", user.getId()),
						Restrictions.eq("share.shareUser.id", user.getId())));
				cCriteria.createAlias("shares", "share", Criteria.LEFT_JOIN);
			}
			cCriteria.add(Restrictions.isNull("measureId"))
			.addOrder(Order.desc("set_id"))
			.addOrder(Order.desc("draft"))
			.addOrder(Order.desc("version"));
		cCriteria.setFirstResult(0);
		
		List<CQLLibrary> libraryResultList = cCriteria.list();
		if (!user.getSecurityRole().getId().equals("2")) {
			libraryResultList = getAllLibrariesInSet(libraryResultList);
		}
			
		List<CQLLibrary> orderedCQlLibList = null;
		if(libraryResultList != null){
			orderedCQlLibList = sortLibraryList(libraryResultList);
		} else {
			orderedCQlLibList = new ArrayList<CQLLibrary>();
		}
		
		StringUtility su = new StringUtility();
		List<CQLLibraryShareDTO> orderedList = new ArrayList<CQLLibraryShareDTO>();
		Map<String, CQLLibraryShareDTO> cqlLibIdDTOMap = new HashMap<String, CQLLibraryShareDTO>();
		boolean isNormalUserAndAllCQLLib = user.getSecurityRole().getId()
				.equals("3")
				&& (filter == MeasureSearchFilterPanel.ALL_MEASURES);
		for (CQLLibrary cqlLibrary : orderedCQlLibList) {
			
			boolean matchesSearch = searchResultsForCQLLibrary(searchString, su,
					cqlLibrary);
			if (matchesSearch) {
				CQLLibraryShareDTO dto = extractDTOFromCQLLibrary(cqlLibrary);
				cqlLibIdDTOMap.put(cqlLibrary.getId(), dto);
				orderedList.add(dto);
			}
		}
		
		if (orderedList.size() > 0) {
			Criteria shareCriteria = getSessionFactory().getCurrentSession()
					.createCriteria(CQLLibraryShare.class);
			shareCriteria.add(Restrictions.eq("shareUser.id", user.getId()));
			shareCriteria.add(Restrictions.in("cqlLibrary.id",
					cqlLibIdDTOMap.keySet()));
			List<CQLLibraryShare> shareList = shareCriteria.list();
			// get share level for each measure set and set it on each dto
			HashMap<String, String> measureSetIdToShareLevel = new HashMap<String, String>();
			for (CQLLibraryShare share : shareList) {
				String msid = share.getCqlLibrary().getSet_id();
				String shareLevel = share.getShareLevel().getId();
				
				String existingShareLevel = measureSetIdToShareLevel.get(msid);
				if ((existingShareLevel == null)
						|| ShareLevel.VIEW_ONLY_ID.equals(existingShareLevel)) {
					measureSetIdToShareLevel.put(msid, shareLevel);
				}
			}
			
			for (Iterator<CQLLibraryShareDTO> iterator = orderedList.iterator(); iterator
					.hasNext();) {
				CQLLibraryShareDTO dto = iterator.next();
				String msid = dto.getCqlLibrarySetId();
				String shareLevel = measureSetIdToShareLevel.get(msid);
				if (isNormalUserAndAllCQLLib
						&& dto.isPrivateCQLLibrary()
						&& !dto.getOwnerUserId().equals(user.getId())
						&& ((shareLevel == null) || !shareLevel
								.equals(ShareLevel.MODIFY_ID))) {
					iterator.remove();
					continue;
				}
				if (shareLevel != null) {
					dto.setShareLevel(shareLevel);
				}
			}
		}
		
		
		if (pageSize < orderedList.size()) {
			return orderedList.subList(0, pageSize);
		} else {
			return orderedList;
		}
		
	}
	
	public List<CQLLibrary> getAllLibrariesInSet(List<CQLLibrary> libraries) {
		if (!libraries.isEmpty()) {
			Set<String> cqlLibSetIds = new HashSet<String>();
			for (CQLLibrary m : libraries) {
				cqlLibSetIds.add(m.getSet_id());
			}
			
			Criteria setCriteria = getSessionFactory().getCurrentSession()
					.createCriteria(CQLLibrary.class);
			setCriteria.add(Restrictions.in("set_id", cqlLibSetIds));
			libraries = setCriteria.list();
		}
		return sortLibraryList(libraries);
	}
	
	
	
	private List<CQLLibrary> sortLibraryList(List<CQLLibrary> libraryResultList) {
		// generate sortable lists
		List<List<CQLLibrary>> libraryList = new ArrayList<List<CQLLibrary>>();
		for (CQLLibrary cqlLib : libraryResultList) {
			boolean hasList = false;
			for (List<CQLLibrary> list : libraryList) {
				String cqlsetId = list.get(0).getSet_id();
				if (cqlLib.getSet_id().equalsIgnoreCase(cqlsetId)) {
					list.add(cqlLib);
					hasList = true;
					break;
				}
			}
			// }
			if (!hasList) {
				List<CQLLibrary> cqllist = new ArrayList<CQLLibrary>();
				// Check if Measure is softDeleted then dont include that into
				// list.
				// if(m.getDeleted()==null){
				cqllist.add(cqlLib);
				libraryList.add(cqllist);
				// }
			}
		}
		// sort
		for (List<CQLLibrary> list : libraryList) {
			Collections.sort(list, new CQLLibraryComparator());
		}
		Collections.sort(libraryList, new CQLLibraryListComparator());
		// compile list
		List<CQLLibrary> retList = new ArrayList<CQLLibrary>();
		for (List<CQLLibrary> mlist : libraryList) {
			for (CQLLibrary m : mlist) {
				retList.add(m);
			}
		}
		return retList;
	}
	
	
	/**
	 * Search method to search cql lib by owner name(First/last) or by cql library name.
	 * @param searchTextLC
	 * @param stringUtility
	 * @param cqlLibrary
	 * @return boolean
	 */
	private boolean searchResultsForCQLLibrary(String searchTextLC,
			StringUtility stringUtility, CQLLibrary cqlLibrary) {
		
		boolean matchesSearch = stringUtility.isEmptyOrNull(searchTextLC) ? true :
		// CQL name
				cqlLibrary.getName().toLowerCase().contains(searchTextLC) ? true : // CQL
																					// owner
																					// first
																					// name
						cqlLibrary.getOwnerId().getFirstName().toLowerCase().contains(searchTextLC) ? true :
						// CQL owner last name
								cqlLibrary.getOwnerId().getLastName().toLowerCase().contains(searchTextLC) ? true:
									// Owner email address
									//cqlLibrary.getOwnerId().getEmailAddress().contains(searchTextLC) ? true :
									false;
		return matchesSearch;
	}
	
	@Override
	public String getAssociatedMeasureId(String Id) {
		Session session = getSessionFactory().getCurrentSession();
		Criteria criteria = session.createCriteria(CQLLibrary.class);
		criteria.setProjection(Projections.property("measureId"));
		criteria.add(Restrictions.eq("id", Id));
		String measureId = (String) criteria.list().get(0);
		return measureId;
	}
	
	@Override
	public boolean isLibraryLocked(String cqlLibraryId) {
		Session session = getSessionFactory().getCurrentSession();
		Criteria libCriteria = session.createCriteria(CQLLibrary.class);
		libCriteria.setProjection(Projections.property("lockedOutDate"));
		libCriteria.add(Restrictions.eq("id", cqlLibraryId));
		CQLLibrary libResults = (CQLLibrary) libCriteria.uniqueResult();
		Timestamp lockedOutDate = null;
		if (libResults != null) {
			lockedOutDate = libResults.getLockedOutDate();
		}
		boolean locked = isLocked(lockedOutDate);
		session.close();
		return locked;
	}
	
	/**
	 * Checks if is locked.
	 * 
	 * @param lockedOutDate
	 *            the locked out date
	 * @return false if current time - lockedOutDate < the lock threshold
	 */
	private boolean isLocked(Date lockedOutDate) {
		
		boolean locked = false;
		if (lockedOutDate == null) {
			return locked;
		}
		
		long currentTime = System.currentTimeMillis();
		long lockedOutTime = lockedOutDate.getTime();
		long timeDiff = currentTime - lockedOutTime;
		locked = timeDiff < lockThreshold;
		
		return locked;
	}
	
	@Override
	public void updateLockedOutDate(CQLLibrary existingLibrary) {

		Session session = getSessionFactory().openSession();
		org.hibernate.Transaction tx = session.beginTransaction();
		try {
			CQLLibrary cqlLibrary = (CQLLibrary) session.load(CQLLibrary.class, existingLibrary.getId());
			cqlLibrary.setId(existingLibrary.getId());
			cqlLibrary.setLockedOutDate(null);
			cqlLibrary.setLockedUserId(null);
			session.update(cqlLibrary);
			tx.commit();
			session.close();
		} finally {
			rollbackUncommitted(tx);
			closeSession(session);
		}
	}
	
	
	@Override
	public String findMaxVersion(String setId) {
		Criteria mCriteria = getSessionFactory().getCurrentSession()
				.createCriteria(CQLLibrary.class);
		mCriteria.add(Restrictions.eq("set_id", setId));
		// add check to filter Draft's version number when finding max version
		// number.
		mCriteria.add(Restrictions.ne("draft", true));
		mCriteria.setProjection(Projections.max("version"));
		String maxVersion = (String) mCriteria.list().get(0);
		return maxVersion;
	}
	
	@Override
	public String findMaxOfMinVersion(String setId, String version) {
		logger.info("In CQLLibraryDAO.findMaxOfMinVersion()");
		String maxOfMinVersion = version;
		double minVal = 0;
		double maxVal = 0;
		if (StringUtils.isNotBlank(version)) {
			int decimalIndex = version.indexOf('.');
			minVal = Integer.valueOf(version.substring(0, decimalIndex))
					.intValue();
			logger.info("Min value: " + minVal);
			maxVal = minVal + 1;
			logger.info("Max value: " + maxVal);
		}
		Criteria mCriteria = getSessionFactory().getCurrentSession()
				.createCriteria(CQLLibrary.class);
		
		mCriteria.add(Restrictions.eq("set_id", setId));
		mCriteria.add(Restrictions.ne("draft", true));
		mCriteria.addOrder(Order.asc("version"));
		List<CQLLibrary> cqlList = mCriteria.list();
		double tempVersion = 0;
		if ((cqlList != null) && (cqlList.size() > 0)) {
			logger.info("Finding max of min version from the Library List. Size:"
					+ cqlList.size());
			for (CQLLibrary library : cqlList) {
				logger.info("Looping through Lib Id: " + library.getId()
					+ " Version: " + library.getVersion());
				if ((library.getVersionNumber() > minVal)
						&& (library.getVersionNumber() < maxVal)) {
					if (tempVersion < library.getVersionNumber()) {
						logger.info(tempVersion + "<"
								+ library.getVersionNumber() + "="
								+ (tempVersion < library.getVersionNumber()));
						maxOfMinVersion = library.getVersion();
						logger.info("maxOfMinVersion: " + maxOfMinVersion);
					}
					tempVersion = library.getVersionNumber();
					logger.info("tempVersion: " + tempVersion);
				}
			}
		}
		logger.info("Returned maxOfMinVersion: " + maxOfMinVersion);
		return maxOfMinVersion;
	}
	
	
	
	/**
	 * This method returns a List of CQLLibraryShareDTO objects which have
	 * userId,firstname,lastname and sharelevel for the given measureId.
	 * 
	 * @param measureId
	 *            the measure id
	 * @param startIndex
	 *            the start index
	 * @param pageSize
	 *            the page size
	 * @return the measure share info for measure
	 */
	@Override
	public List<CQLLibraryShareDTO> getLibraryShareInfoForLibrary(
			String cqlId, String searchText) {
		String searchString = searchText.toLowerCase().trim();
		Criteria userCriteria = getSessionFactory().getCurrentSession()
				.createCriteria(User.class);
		int pageSize = Integer.MAX_VALUE;
		userCriteria.add(Restrictions.eq("securityRole.id", "3"));
		//Added restriction for Active user's for User story MAT:2900.
		userCriteria.add(Restrictions.eq("status.id", "1"));
		userCriteria.add(Restrictions.ne("id",
				LoggedInUserUtil.getLoggedInUser()));
		userCriteria.setFirstResult(0);
		// userCriteria.setMaxResults(pageSize);
		userCriteria.addOrder(Order.asc("lastName"));
		
		List<User> userResults = userCriteria.list();
		HashMap<String, CQLLibraryShareDTO> userIdDTOMap = new HashMap<String, CQLLibraryShareDTO>();
		ArrayList<CQLLibraryShareDTO> orderedDTOList = new ArrayList<CQLLibraryShareDTO>();
		List<CQLLibraryShareDTO> dtoList = new ArrayList<CQLLibraryShareDTO>();
		StringUtility stringUtility = new StringUtility();
		for (User user : userResults) {
			if(searchResultsForSharedUsers(searchString, stringUtility, user)){
				CQLLibraryShareDTO dto = new CQLLibraryShareDTO();
				dtoList.add(dto);
				dto.setUserId(user.getId());
				dto.setFirstName(user.getFirstName());
				dto.setLastName(user.getLastName());
				dto.setOrganizationName(user.getOrganizationName());
				userIdDTOMap.put(user.getId(), dto);
				orderedDTOList.add(dto);
			}
		}
		
		if (dtoList.size() > 0) {
			Criteria shareCriteria = getSessionFactory().getCurrentSession()
					.createCriteria(CQLLibraryShare.class);
			shareCriteria.add(Restrictions.in("shareUser.id",
					userIdDTOMap.keySet()));
			shareCriteria.add(Restrictions.eq("cqlLibrary.id", cqlId));
			List<CQLLibraryShare> shareList = shareCriteria.list();
			for (CQLLibraryShare share : shareList) {
				User shareUser = share.getShareUser();
				CQLLibraryShareDTO dto = userIdDTOMap.get(shareUser.getId());
				dto.setShareLevel(share.getShareLevel().getId());
			}
		}
		if (pageSize < orderedDTOList.size()) {
			return orderedDTOList.subList(0, pageSize);
		} else {
			return orderedDTOList;
		}
	}
	
	
	/* (non-Javadoc)
	 * @see mat.dao.clause.MeasureDAO#findShareLevelForUser(java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	 public ShareLevel findShareLevelForUser(String cqlLibraryId, String userID, 
			 String cqlLibrarySetId){
	  
	  ShareLevel shareLevel = null;
	  List<String> cqlLibIds = getCQLLibrarySetForSharedLibrary(cqlLibraryId, cqlLibrarySetId);
	  Criteria shareCriteria = getSessionFactory().getCurrentSession()
	    .createCriteria(CQLLibraryShare.class);
	  shareCriteria.add(Restrictions.eq("shareUser.id",userID));
	  shareCriteria.add(Restrictions.in("cqlLibrary.id", cqlLibIds));
	  List<CQLLibraryShare> shareList = shareCriteria.list();
	  if(!shareList.isEmpty()){
	   shareLevel = shareList.get(0).getShareLevel();
	  }
	  
	  return shareLevel;
	 }
	
	@SuppressWarnings("unchecked")
	private List<String> getCQLLibrarySetForSharedLibrary(String cqlLibraryId, 
			String cqlLibrarySetId){
		
		Criteria mCriteria = getSessionFactory().getCurrentSession()
				.createCriteria(CQLLibrary.class);
		mCriteria.add(Restrictions.eq("set_id", cqlLibrarySetId));
		List<String> cqlLibIds = new ArrayList<String>();
		List<CQLLibrary> cqlLibList = mCriteria.list();
 		for(CQLLibrary cqlLibrary : cqlLibList){
 			cqlLibIds.add(cqlLibrary.getId());
		}
		
		return cqlLibIds;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public String getSetIdForCQLLibrary(String cqlLibraryId){
		
		Criteria mCriteria = getSessionFactory().getCurrentSession()
				.createCriteria(CQLLibrary.class);
		mCriteria.setProjection(Projections.property("set_id"));
		mCriteria.add(Restrictions.or(Restrictions.eq("id", cqlLibraryId),Restrictions.eq("measureId", cqlLibraryId)));
		if(mCriteria.list().size() >0){
		String setId = (String) mCriteria.list().get(0);
		return setId;
		}else{
			return null;
		}
	}
	
	@Override
	public CQLLibraryShareDTO extractDTOFromCQLLibrary(CQLLibrary cqlLibrary) {
		CQLLibraryShareDTO dto = new CQLLibraryShareDTO();
		
		dto.setCqlLibraryId(cqlLibrary.getId());
		dto.setCqlLibraryName(cqlLibrary.getName());
		dto.setOwnerUserId(cqlLibrary.getOwnerId().getId());
		dto.setDraft(cqlLibrary.isDraft());
		dto.setVersion(cqlLibrary.getVersion());
		dto.setFinalizedDate(cqlLibrary.getFinalizedDate());
		dto.setCqlLibrarySetId(cqlLibrary.getSet_id());
		/*dto.setPrivateMeasure(cqlLibrary.getIsPrivate());*/
		dto.setRevisionNumber(cqlLibrary.getRevisionNumber());
		boolean isLocked = isLocked(cqlLibrary.getLockedOutDate());
		dto.setLocked(isLocked);
		if (isLocked && (cqlLibrary.getLockedUserId() != null)) {
			LockedUserInfo lockedUserInfo = new LockedUserInfo();
			lockedUserInfo.setUserId(cqlLibrary.getLockedUserId().getUserId());
			lockedUserInfo.setEmailAddress(cqlLibrary.getLockedUserId()
					.getEmailAddress());
			lockedUserInfo.setFirstName(cqlLibrary.getLockedUserId().getFirstName());
			lockedUserInfo.setLastName(cqlLibrary.getLockedUserId().getLastName());
			dto.setLockedUserInfo(lockedUserInfo);
		}
		return dto;
	}
	
	
	private boolean searchResultsForSharedUsers(String searchTextLC,
			StringUtility stringUtility, User user) {
		
		boolean matchesSearch = stringUtility.isEmptyOrNull(searchTextLC) ? true :
		// User First Name
			user.getFirstName().toLowerCase().contains(searchTextLC) ? true : 
				// User Last Name
						user.getLastName().toLowerCase().contains(searchTextLC) ? true :
							/*// Owner email address
								user.getEmailAddress().toLowerCase().contains(searchTextLC) ? true:*/
									false;
		return matchesSearch;
	}
	
}
