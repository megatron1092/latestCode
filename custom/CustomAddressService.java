package net.intermedia.uav.postal.custom;

import net.intermedia.uav.cache.UavPostalCachedObject;
import net.intermedia.uav.pojo.postal.CustomAddressRequest;
import net.intermedia.uav.pojo.postal.DeleteAddressResult;
import net.intermedia.uav.pojo.postal.EditShippingValidResult;
import net.intermedia.uav.postal.UavPostalValidationResult;

import java.util.List;
import java.util.Map;

public interface CustomAddressService {

    UavPostalValidationResult createCustomAddress(CustomAddressRequest customAddressRequest);
    CustomAddressRequest enrichCustomAddressRequestWithAddressLinesAndStateCode(CustomAddressRequest customAddressRequest);

    UavPostalValidationResult clearCustomAddressValidation(CustomAddressRequest customAddressRequest);
    UavPostalValidationResult clearCustomAddressValidationByUID(String uid);

    UavPostalCachedObject editCustomAddressByUID(String uid, CustomAddressRequest editedPostalRequest);
    UavPostalCachedObject editShippingValidOfAddressPoBoxByUID(String uid, Boolean shippingValid);

    List<DeleteAddressResult> deleteCustomAddresses(List<String> uids);
    List<EditShippingValidResult> editShippingValidOfAddresses(Map<String, Boolean> uidShippingValidMap);
}
