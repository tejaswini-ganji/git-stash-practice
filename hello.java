public ResponseEntity<GeneralResponse> getPalletizationReportData(List<String> ouCodes, Logger log,int pageNo, int pageSize) {
		ResponseEntity<GeneralResponse> response = null;
		try {
			if (ouCodes != null && !ouCodes.isEmpty()) {
				Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
				JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
				QPalletContentsEntity palletContentsEntity = QPalletContentsEntity.palletContentsEntity;
                BooleanBuilder predicate = new BooleanBuilder();
                predicate.and(palletContentsEntity.activeFlag.eq(Constants.ACTIVE_FLAG_ACTIVE));
				predicate.and(palletContentsEntity.branch.in(ouCodes));

				List<PalletContentsEntity> results = queryFactory
						.selectFrom(palletContentsEntity)
						.where(predicate)
						.offset(pageable.getOffset())       // OFFSET
						.limit(pageable.getPageSize())      // LIMIT
						.fetch();
		        long total = queryFactory
							.select(palletContentsEntity.count())
							.from(palletContentsEntity)
							.where(predicate)
							.fetchOne();

			Page<PalletContentsEntity> palletContentsPage = new PageImpl<>(results, pageable, total);
			List<PalletContentsEntity> palletContents = palletContentsPage.getContent();

				List<DktEventsEntity> dktEventsMappings = (List<DktEventsEntity>) dktEventsJpaRepository.findAll();
				Map<String, String> eventNameMap = CommonUtility.isValidList(dktEventsMappings)
						? dktEventsMappings.stream()
								.collect(Collectors.toMap(DktEventsEntity::getEventCode, DktEventsEntity::getEventName))
						: null;
				// System.out.println("DATE1 - " + new Date());
			
				// System.out.println("DATE2 - " + new Date());
				if (palletContents != null && !palletContents.isEmpty()) {
					Map<BigInteger, PalletizationReportModel> resMap = new HashMap<>();
					int batchSize = 100;
					List<BigInteger> docketList = palletContents.stream().map(PalletContentsEntity::getDktNo).distinct()
							.collect(Collectors.toList());
					// System.out.println("DATE3 - " + new Date());
					Map<BigInteger, BookingDataEntity> bookMap = new HashMap<>();
					List<BookingDataEntity> bookingDataList = new ArrayList<>();
					Map<BigInteger, BookingLbhEntity> bookingPktLbhMap = new HashMap<>();
					Map<BigInteger, PacketWeightDataEntity> packetWeightDataLbhMap = new HashMap<>();
					for (int i = 0; i < docketList.size(); i += batchSize) {
						int endIndex = Math.min(i + batchSize, docketList.size());
						List<BigInteger> subList = docketList.subList(i, endIndex);
						bookingDataList.addAll(bookingDataJpaRepository.findBydocketNoIn(subList));
						List<BookingLbhEntity> bookingLbhList = bookingLbhJpaRepository.findByDocketNoIn(subList);
						Map<BigInteger, BookingLbhEntity> bookingPktLbhMiniMap = passportService
								.convertToPktWiseLbhMap(bookingLbhList);
						if (bookingPktLbhMiniMap != null && !bookingPktLbhMiniMap.isEmpty()) {
							bookingPktLbhMap.putAll(bookingPktLbhMiniMap);
						}
						List<PacketWeightDataEntity> pktWiseLbh = packetWeightDataJpaRepository
								.findByDocketNoIn(subList);
						Map<BigInteger, PacketWeightDataEntity> packetWeightDataLbhMiniMap = pktWiseLbh != null
								&& !pktWiseLbh.isEmpty()
										? pktWiseLbh.stream().collect(
												Collectors.toMap(PacketWeightDataEntity::getPacketNo,
														Function.identity(),
														(a, b) -> b))
										: null;
						if (packetWeightDataLbhMiniMap != null && !packetWeightDataLbhMiniMap.isEmpty()) {
							packetWeightDataLbhMap.putAll(packetWeightDataLbhMiniMap);
						}
					}
					bookMap = bookingDataList != null && !bookingDataList.isEmpty() ? bookingDataList.stream()
							.collect(Collectors.toMap(BookingDataEntity::getDocketNo, b -> b)) : null;
					bookingDataList.clear();
					List<FirstOuUnloadingPktsEntity> ouunloadingData = new ArrayList<>();
					for (int i = 0; i < docketList.size(); i += batchSize) {
						int endIndex = Math.min(i + batchSize, docketList.size());
						List<BigInteger> subList = docketList.subList(i, endIndex);
						ouunloadingData.addAll(firstOuUnloadingPktsJpaRepository.findByDktNoIn(subList));
					}
					Map<String, FirstOuUnloadingPktsEntity> unloadMap = new HashMap<>();
					if (ouunloadingData != null && !ouunloadingData.isEmpty()) {
						for (FirstOuUnloadingPktsEntity un : ouunloadingData) {
							if (un.getPktWidth() != null && un.getPktHeight() != null && un.getPktLen() != null)
								unloadMap.put(un.getDktNo() + "-" + un.getPktNo(), un);
						}
					}
					ouunloadingData.clear();
					// System.out.println("DATE6 - " + new Date());
					List<PalletPositionMapEntity> palletList = palletPositionMapJpaRepository
							.findByBranchInAndIsActive(ouCodes, 1);
					Map<String, PalletPositionMapEntity> palletMap = new HashMap<>();
					if (palletList != null && !palletList.isEmpty()) {
						for (PalletPositionMapEntity p : palletList) {
							if (CommonUtility.check(p.getBranch(), p.getPalletNo())) {
								palletMap.put(p.getBranch() + "-" + p.getPalletNo(), p);
							}
						}
					}
					palletList.clear();
					// System.out.println("DATE7 - " + new Date());
					for (PalletContentsEntity p : palletContents) {
						PalletizationReportModel model = resMap.get(p.getPkt());
						if (model == null) {
							model = new PalletizationReportModel();
							model.setPacketNo(p.getPkt());
							model.setDocketNo(p.getDktNo());
							model.setPalletHub(p.getBranch());
							model.setPalletNo(p.getPalletNo());
							model.setPalletTimestamp(Constants.longDateFormat.format(p.getCreatedTimestamp()));
							BookingDataEntity book = bookMap != null ? bookMap.getOrDefault(p.getDktNo(), null) : null;
							if (book != null) {
								model.setBookingBranch(book.getBookingBranch());
								model.setDestBranch(book.getDeliveryBranch());
								model.setCustCode(book.getCustCode());
								model.setBookingAgent(book.getBookingAgentId());
								model.setCreatedAt(book.getBookingBranch());
								model.setReAttemptCount(book.getDeliveryAttempts());
								model.setVehicleNo(book.getVehicleNo());
								// model.setStatusCode(docketStatusMap.get(book.getDocketStatus()));
								DktDetailsEntity dktDetailsEn = dktDetailsJpaRepository
										.findTopByDktNoOrderByIdDesc(book.getDocketNo());
								if (dktDetailsEn != null) {
									model.setUpdatedAt(dktDetailsEn.getOucode());
									String statusCode = dktDetailsEn.getEventCode();
									if (CommonUtility.check(statusCode) && eventNameMap != null
											&& eventNameMap.containsKey(statusCode)) {
										model.setStatusCode(eventNameMap.get(statusCode));
									}
								}
								PalletPositionMapEntity palletPosition = palletMap
										.get(p.getBranch() + "-" + p.getPalletNo());
								if (palletPosition != null) {
									model.setGridLocation(palletPosition.getGrid());
									model.setPalletLocation(palletPosition.getPosition());
								}
								BookingLbhEntity bookingLbh = bookingPktLbhMap != null
										? bookingPktLbhMap.getOrDefault(p.getPkt(), null)
										: null;
								if (bookingLbh != null) {
									model.setBoxWt(0.0);
									model.setBoxLength(bookingLbh.getPktLen());
									model.setBoxBreadth(bookingLbh.getPktWidth());
									model.setBoxHeight(bookingLbh.getPktHeight());
								}
								PacketWeightDataEntity lbh = packetWeightDataLbhMap != null
										? packetWeightDataLbhMap.get(p.getPkt())
										: null;
								if (lbh != null) {
									if (lbh.getWeight() != null) {
										model.setBoxWt(lbh.getWeight().doubleValue());
									}
									if (lbh.getLength() != null && lbh.getLength().compareTo(BigDecimal.ZERO) > 0) {
										model.setBoxLength(lbh.getLength().doubleValue());
									}
									if (lbh.getBreadth() != null && lbh.getBreadth().compareTo(BigDecimal.ZERO) > 0) {
										model.setBoxBreadth(lbh.getBreadth().doubleValue());
									}
									if (lbh.getHeight() != null && lbh.getHeight().compareTo(BigDecimal.ZERO) > 0) {
										model.setBoxHeight(lbh.getHeight().doubleValue());
									}
								}
							}
						}
						resMap.put(p.getPkt(), model);
					}
					// System.out.println("DATE8 - " + new Date());
					response = new ResponseEntity<>(
							new GeneralResponse(Constants.TRUE, Constants.SUCCESSFUL, new ArrayList<>(resMap.values())),
							HttpStatus.OK);
				} else {
					response = new ResponseEntity<>(
							new GeneralResponse(Constants.FALSE, "Pallet details not found for given data", null), HttpStatus.OK);
				}
			} else {
				response = new ResponseEntity<>(
						new GeneralResponse(Constants.FALSE, "Please enter OuCode", null), HttpStatus.OK);
			}
		} catch (Exception e) {
			e.printStackTrace();
			response = new ResponseEntity<>(
					new GeneralResponse(Constants.FALSE, Constants.INTERNAL_SERVER_ERROR, null), HttpStatus.OK);
		}
		return response;
	}
