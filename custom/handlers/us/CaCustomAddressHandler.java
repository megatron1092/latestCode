package net.intermedia.uav.postal.custom.handlers.us;

import net.intermedia.uav.country.Country;
import net.intermedia.uav.postal.UavPostalValidationResult;
import net.intermedia.uav.postal.custom.handlers.AbstractCustomAddressHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class CaCustomAddressHandler extends AbstractCustomAddressHandler {
    @Override
    public String generateAddressLine1(UavPostalValidationResult uavPostalValidationResult) {
        return (generatePoBoxPart(uavPostalValidationResult) + generateNormalizedAddressLine1(uavPostalValidationResult)).trim();
    }

    @Override
    public String generateAddressLine2(UavPostalValidationResult uavPostalValidationResult) {
        return "";
    }

    @Override
    public String[] generateFullAddress(UavPostalValidationResult uavPostalValidationResult) {
        return Stream.of(
                uavPostalValidationResult.getAddressLine1(), uavPostalValidationResult.getAddressLine2(),
                uavPostalValidationResult.getAddressLine3(),
                uavPostalValidationResult.getCity() + ", " + getStateCodeOrDefault(Country.CA.getAlpha2Code(), uavPostalValidationResult.getState())
                        + " " + uavPostalValidationResult.getZip())
                .filter(add -> !StringUtils.isEmpty(add))
                .map(String::toUpperCase)
                .toArray(String[]::new);
    }


    protected String generatePoBoxPart(UavPostalValidationResult uavPostalValidationResult) {
        String poBoxPart = "";
        if (!StringUtils.isEmpty(uavPostalValidationResult.getPostBox())) {
            poBoxPart = normalize("PO Box " + uavPostalValidationResult.getPostBox()) + " ";
        }
        return poBoxPart;
    }
    protected String generateNormalizedAddressLine1(UavPostalValidationResult uavPostalValidationResult) {
        return normalize(String.join(" ",
                uavPostalValidationResult.getHouseNumber(),
                uavPostalValidationResult.getPreDir(),
                uavPostalValidationResult.getStreetName(),
                uavPostalValidationResult.getSuffix(),
                uavPostalValidationResult.getPostDir(),
                uavPostalValidationResult.getUnitType(),
                uavPostalValidationResult.getUnitNumber())
                .replace("null", ""));
    }



}
