package net.intermedia.uav.postal.custom.handlers.international;

import net.intermedia.uav.country.Country;
import net.intermedia.uav.postal.UavPostalValidationResult;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

public class AUCustomAddressHandler extends InternationalAbstractCustomAddressHandler {

    @Override
    public String generateAddressLine2(UavPostalValidationResult uavPostalValidationResult) {
        String addressLine2 = "";
        if (!StringUtils.isEmpty(uavPostalValidationResult.getPostBox())) {
            addressLine2 = normalize("PO Box " + uavPostalValidationResult.getPostBox());
        }
        return addressLine2;
    }

    @Override
    public String[] generateFullAddress(UavPostalValidationResult uavPostalValidationResult) {
        return Stream.of(uavPostalValidationResult.getAddressLine1(), uavPostalValidationResult.getAddressLine2(), uavPostalValidationResult.getAddressLine3(),
                String.join(" ", uavPostalValidationResult.getCity(),
                        getStateCodeOrDefault(Country.AU.getAlpha2Code(), uavPostalValidationResult.getState()),
                        uavPostalValidationResult.getZip()))
                .filter(add -> !StringUtils.isEmpty(add))
                .toArray(String[]::new);
    }
}
