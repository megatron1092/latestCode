package net.intermedia.uav.postal.custom.handlers.international;

import net.intermedia.uav.postal.UavPostalValidationResult;
import net.intermedia.uav.postal.custom.handlers.AbstractCustomAddressHandler;

public abstract class InternationalAbstractCustomAddressHandler extends AbstractCustomAddressHandler {

    @Override
    public String generateAddressLine1(UavPostalValidationResult uavPostalValidationResult) {
        return normalize(String.format("%s %s %s", uavPostalValidationResult.getStreetName(),
                uavPostalValidationResult.getHouseNumber(),
                uavPostalValidationResult.getUnitNumber())
                .replace("null", ""));
    }


}
