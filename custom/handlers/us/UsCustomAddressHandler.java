package net.intermedia.uav.postal.custom.handlers.us;

import net.intermedia.uav.country.Country;
import net.intermedia.uav.postal.UavPostalValidationResult;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public class UsCustomAddressHandler extends CaCustomAddressHandler {
    @Override
    public String generateAddressLine1(UavPostalValidationResult uavPostalValidationResult) {
        String poBoxPart = generatePoBoxPart(uavPostalValidationResult);
        String normalizedAddressLine1 = generateNormalizedAddressLine1(uavPostalValidationResult);
        String pmbBoxPart = generatePmbPart(uavPostalValidationResult);
        if (!StringUtils.isEmpty(pmbBoxPart)) {
            return (normalizedAddressLine1 + pmbBoxPart).trim();
        } else {
            return (poBoxPart + normalizedAddressLine1).trim();
        }
    }

    @Override
    public String generateAddressLine2(UavPostalValidationResult uavPostalValidationResult) {
        return "";
    }

    @Override
    public void fillPostBoxField(UavPostalValidationResult uavPostalValidationResult) {
        String pmbNumber = uavPostalValidationResult.getPmbNumber();
        String pmbPrefix = uavPostalValidationResult.getPmbPrefix();
        uavPostalValidationResult.setIsPostBox(StringUtils.isNotBlank(uavPostalValidationResult.getPostBox()) ||
                (BooleanUtils.isTrue(uavPostalValidationResult.getIsPostBox())) ||
                StringUtils.isNotBlank(pmbNumber));

        if (StringUtils.isNotBlank(pmbNumber)) {
            uavPostalValidationResult.setPostBox(pmbNumber);
            if (StringUtils.isBlank(pmbPrefix)) {
                uavPostalValidationResult.setPmbPrefix("PMB");
            }
        }
    }


    protected String generatePmbPart(UavPostalValidationResult uavPostalValidationResult) {
        String pmbBoxPart = "";
        if ("pmb".equalsIgnoreCase(uavPostalValidationResult.getPmbPrefix())) {
            pmbBoxPart = " " + normalize(uavPostalValidationResult.getPmbPrefix() + " " + uavPostalValidationResult.getPmbNumber());
        }
        return pmbBoxPart;
    }

    @Override
    public String[] generateFullAddress(UavPostalValidationResult uavPostalValidationResult) {
        return Stream.of(uavPostalValidationResult.getAddressLine1(), uavPostalValidationResult.getAddressLine2(), uavPostalValidationResult.getAddressLine3(),
                uavPostalValidationResult.getCity() + ", " +
                        getStateCodeOrDefault(Country.US.getAlpha2Code(), uavPostalValidationResult.getState()) + " " +
                        uavPostalValidationResult.getZip())
                .filter(add -> !StringUtils.isEmpty(add))
                .toArray(String[]::new);
    }
}
