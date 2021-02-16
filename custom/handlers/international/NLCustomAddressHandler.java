package net.intermedia.uav.postal.custom.handlers.international;

import net.intermedia.uav.postal.UavPostalValidationResult;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

public class NLCustomAddressHandler extends InternationalAbstractCustomAddressHandler {

    @Override
    public String generateAddressLine1(UavPostalValidationResult uavPostalValidationResult) {
        String addressLine1 = "";
        if (!StringUtils.isEmpty(uavPostalValidationResult.getPostBox())) {
            addressLine1 = normalize("Postbus " + uavPostalValidationResult.getPostBox());
        }
        return addressLine1;
    }

    @Override
    public String generateAddressLine2(UavPostalValidationResult uavPostalValidationResult) {
        return normalize(String.format("%s %s %s", uavPostalValidationResult.getStreetName(),
                uavPostalValidationResult.getHouseNumber(),
                uavPostalValidationResult.getUnitNumber())
                .replace("null", ""));
    }

    @Override
    public String[] generateFullAddress(UavPostalValidationResult uavPostalValidationResult) {
        return Stream.of(uavPostalValidationResult.getAddressLine1(), uavPostalValidationResult.getAddressLine2(), uavPostalValidationResult.getAddressLine3(),
                String.join(" ", uavPostalValidationResult.getZip(), uavPostalValidationResult.getCity()))
                .filter(add -> !StringUtils.isEmpty(add))
                .toArray(String[]::new);
    }
}
