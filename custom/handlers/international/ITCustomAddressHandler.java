package net.intermedia.uav.postal.custom.handlers.international;

import net.intermedia.uav.country.Country;
import net.intermedia.uav.postal.UavPostalValidationResult;
import org.springframework.util.StringUtils;

import java.util.stream.Stream;

public class ITCustomAddressHandler extends InternationalAbstractCustomAddressHandler {

    @Override
    public String generateAddressLine2(UavPostalValidationResult uavPostalValidationResult) {
        String addressLine2 = "";
        if (!StringUtils.isEmpty(uavPostalValidationResult.getPostBox())) {
            addressLine2 = normalize("Casella postale " + uavPostalValidationResult.getPostBox());
        }
        return addressLine2;
    }

    @Override
    public String[] generateFullAddress(UavPostalValidationResult uavPostalValidationResult) {
        return Stream.of(uavPostalValidationResult.getAddressLine1(), uavPostalValidationResult.getAddressLine2(), uavPostalValidationResult.getAddressLine3(),
                String.join(" ", uavPostalValidationResult.getZip(), uavPostalValidationResult.getCity(),
                        getStateCodeOrDefault(Country.IT.getAlpha2Code(), uavPostalValidationResult.getState())))
                .filter(add -> !StringUtils.isEmpty(add))
                .toArray(String[]::new);
    }
}
