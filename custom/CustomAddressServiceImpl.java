package net.intermedia.uav.postal.custom;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.intermedia.uav.cache.UavCacheService;
import net.intermedia.uav.cache.UavPostalCachedObject;
import net.intermedia.uav.exceptions.NotFoundException;
import net.intermedia.uav.pojo.postal.CustomAddressRequest;
import net.intermedia.uav.pojo.postal.DeleteAddressResult;
import net.intermedia.uav.pojo.postal.EditShippingValidResult;
import net.intermedia.uav.postal.UavPostalValidationResult;
import net.intermedia.uav.postal.custom.handlers.CustomAddressHandler;
import net.intermedia.uav.postal.custom.handlers.CustomAddressHandlerFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class CustomAddressServiceImpl implements CustomAddressService {


    private final CustomAddressRequestToUavPostalValidationResultMapper mapper;

    private final CustomAddressHandlerFactory customAddressHandlerFactory;

    private final UavCacheService uavCacheService;

    @Override
    public UavPostalValidationResult createCustomAddress(CustomAddressRequest customAddressRequest) {
        return createValidationResultWithCustomAddress(customAddressRequest);
    }

    private UavPostalValidationResult createValidationResultWithCustomAddress(CustomAddressRequest customAddressRequest) {
        CustomAddressHandler customAddressHandler = customAddressHandlerFactory.getHandler(customAddressRequest.getCountry());
        UavPostalValidationResult uavPostalValidationResult = mapper.mapCustomAddressRequestToUavPostalValidationResult(customAddressRequest);
        customAddressHandler.populateOtherFields(uavPostalValidationResult);
        customAddressHandler.fillStateName(uavPostalValidationResult);
        customAddressHandler.fillAddressLine1InUavPostalValidationResult(uavPostalValidationResult);
        customAddressHandler.fillAddressLine2InUavPostalValidationResult(uavPostalValidationResult);
        customAddressHandler.checkAddressLines1and2(uavPostalValidationResult);
        customAddressHandler.fillFullAddress(uavPostalValidationResult);
        return uavPostalValidationResult;
    }

    @Override
    public CustomAddressRequest enrichCustomAddressRequestWithAddressLinesAndStateCode(CustomAddressRequest customAddressRequest) {
        UavPostalValidationResult validationResultWithCustomAddress = createValidationResultWithCustomAddress(customAddressRequest);
        customAddressRequest.setAddressLine1(validationResultWithCustomAddress.getAddressLine1());
        customAddressRequest.setAddressLine2(validationResultWithCustomAddress.getAddressLine2());
        customAddressRequest.setState(validationResultWithCustomAddress.getState());
        return customAddressRequest;
    }

    @Override
    public UavPostalValidationResult clearCustomAddressValidation(CustomAddressRequest customAddressRequest) {
        UavPostalCachedObject uavPostalCachedObject = uavCacheService.getUavPostalCachedObject(customAddressRequest);
        UavPostalValidationResult postalValidationResult;
        if (uavPostalCachedObject != null && uavPostalCachedObject.getPostalValidationResult() != null) {
            uavCacheService.cleanUavPostalCachedObject(uavPostalCachedObject);
            postalValidationResult = uavPostalCachedObject.getPostalValidationResult();
        } else {
            throw new NotFoundException("Couldn't find address to clear with parameters " + customAddressRequest);
        }

        return postalValidationResult;
    }

    @Override
    public UavPostalValidationResult clearCustomAddressValidationByUID(String uid) {
        return uavCacheService.clearCustomAddressValidationByUID(uid);
    }

    @Override
    public UavPostalCachedObject editShippingValidOfAddressPoBoxByUID(String uid, Boolean shippingValid) {
        UavPostalCachedObject currentUavPostalCachedObject = getFromCacheCheckEmpty(uid);
        checkIsPoBox(currentUavPostalCachedObject);
        currentUavPostalCachedObject.getPostalValidationResult().setShippingValid(shippingValid);
        currentUavPostalCachedObject.setPostalValidationResultUpdated(true);
        uavCacheService.updateUavPostalCachedObject(currentUavPostalCachedObject);
        return currentUavPostalCachedObject;
    }

    @Override
    public List<DeleteAddressResult> deleteCustomAddresses(List<String> uids) {
        return uavCacheService.batchDeleteUavPostalCachedObjectByUids(uids);
    }

    @Override
    public List<EditShippingValidResult> editShippingValidOfAddresses(Map<String, Boolean> uidShippingValidMap) {
        return uavCacheService.batchEditShippingValid(uidShippingValidMap);
    }

    @Override
    public UavPostalCachedObject editCustomAddressByUID(String uid, CustomAddressRequest editedCustomAddressRequest) {
        enrichCustomAddressRequestWithAddressLinesAndStateCode(editedCustomAddressRequest);
        UavPostalCachedObject currentUavPostalCachedObject = getFromCacheCheckNotEmptyAndCustom(uid);
        if (!currentUavPostalCachedObject.getPostalValidationResult().getShippingRC().getShippingValid().equals(editedCustomAddressRequest.getShippingValid())) {
            checkIsPoBox(currentUavPostalCachedObject);
        }
        UavPostalCachedObject possibleExistingRecord = findPossibleExistingRecordInCache(editedCustomAddressRequest, currentUavPostalCachedObject);
        UavPostalValidationResult editedCustomAddress = createCustomAddress(editedCustomAddressRequest);
        updateCurrentRecordWithValidationResult(editedCustomAddress, currentUavPostalCachedObject, possibleExistingRecord);
        return currentUavPostalCachedObject;
    }

    private UavPostalCachedObject getFromCacheCheckNotEmptyAndCustom(String uid) {
        UavPostalCachedObject uavPostalCachedObjectByUid = getFromCacheCheckEmpty(uid);
        checkCustom(uid, uavPostalCachedObjectByUid);
        return uavPostalCachedObjectByUid;
    }

    private UavPostalCachedObject getFromCacheCheckEmpty(String uid) {
        UavPostalCachedObject uavPostalCachedObjectByUid = uavCacheService.getUavPostalCachedObjectByUid(uid);
        if (uavPostalCachedObjectByUid.isEmpty()) {
            throw new NotFoundException(String.format("Record with UID %s doesn't have any validation result", uid));
        }
        return uavPostalCachedObjectByUid;
    }

    private void checkCustom(String uid, UavPostalCachedObject uavPostalCachedObjectByUid) {
        if (BooleanUtils.isNotTrue(uavPostalCachedObjectByUid.getIsCustomAddress())) {
            throw new IllegalStateException(String.format("Attempt to edit non-custom address with UID %s", uid));
        }
    }

    private void checkIsPoBox(UavPostalCachedObject currentUavPostalCachedObject) {
        if (BooleanUtils.isNotTrue(currentUavPostalCachedObject.getPostalValidationResult().getIsPostBox())) {
            throw new IllegalStateException(String.format("Attempt to edit non-PO box with UID %s", currentUavPostalCachedObject.getUid()));
        }
    }

    private UavPostalCachedObject findPossibleExistingRecordInCache(CustomAddressRequest
                                                                            editedPostalRequest, UavPostalCachedObject currentUavPostalCachedObject) {
        UavPostalCachedObject possibleExistingUavPostalCachedObject = uavCacheService.getUavPostalCachedObject(editedPostalRequest);
        if ((!possibleExistingUavPostalCachedObject.getUid().equalsIgnoreCase(currentUavPostalCachedObject.getUid())) &&
                (possibleExistingUavPostalCachedObject.getPostalValidationResult() != null)) {
            throw new IllegalStateException(String.format("Attempt to edit record with existing values: Record with key %s is already present.", possibleExistingUavPostalCachedObject.getCacheKey()));
        }
        return possibleExistingUavPostalCachedObject;
    }

    private void updateCurrentRecordWithValidationResult(UavPostalValidationResult
                                                                 editedUavPostalValidationResult, UavPostalCachedObject currentUavPostalCachedObject, UavPostalCachedObject
                                                                 possibleExistingUavPostalCachedObject) {
        currentUavPostalCachedObject.setPostalValidationResult(editedUavPostalValidationResult);
        currentUavPostalCachedObject.setCacheKey(possibleExistingUavPostalCachedObject.getCacheKey());
        currentUavPostalCachedObject.setPostalValidationResultUpdated(true);
        uavCacheService.updateUavPostalCachedObject(currentUavPostalCachedObject);
    }


}
