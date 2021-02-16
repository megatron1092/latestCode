package net.intermedia.uav.postal;

import net.intermedia.uav.cache.UavPostalCachedObject;
import net.intermedia.uav.context.ValidationContext;
import net.intermedia.uav.pojo.postal.PostalRequest;
import net.intermedia.uav.pojo.postal.PostalValidationRequest;
import net.intermedia.uav.pojo.postal.SearchAddressType;
import net.intermedia.uav.pojo.postal.SearchCustomAddressesResponse;

/**
 * Business logic for postal validation
 */
public interface PostalValidationService {

    <T extends PostalRequest> UavPostalValidationResult validate(ValidationContext validationContext, T postalRequest);

    UavPostalCachedObject lookup(ValidationContext validationContext, PostalValidationRequest postalValidationRequest);

    UavPostalCachedObject lookupByUid(String uid);

    SearchCustomAddressesResponse searchCustomByParams(Integer pageSize, Integer pageNumber, SearchAddressType searchAddressType, String country, String state, String city, String zip, String fullStreetAddress);

    UavPostalCachedObject deleteAddress(PostalRequest postalValidationRequest);

    UavPostalCachedObject deleteAddressByUID(String uid);


}
