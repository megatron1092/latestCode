package net.intermedia.uav.postal.custom.handlers;

import lombok.extern.log4j.Log4j2;
import net.intermedia.uav.country.CountryState;
import net.intermedia.uav.postal.UavPostalValidationResult;
import org.apache.commons.lang3.StringUtils;

@Log4j2
public abstract class AbstractCustomAddressHandler implements CustomAddressHandler {

    @Override
    public void fillAddressLine1InUavPostalValidationResult(UavPostalValidationResult uavPostalValidationResult) {
        uavPostalValidationResult.setAddressLine1(generateAddressLine1(uavPostalValidationResult));
    }

    @Override
    public void fillAddressLine2InUavPostalValidationResult(UavPostalValidationResult uavPostalValidationResult) {
        uavPostalValidationResult.setAddressLine2(generateAddressLine2(uavPostalValidationResult));
    }

    @Override
    public void checkAddressLines1and2(UavPostalValidationResult uavPostalValidationResult) {
        if (StringUtils.isEmpty(uavPostalValidationResult.getAddressLine1())) {
            uavPostalValidationResult.setAddressLine1(uavPostalValidationResult.getAddressLine2());
            uavPostalValidationResult.setAddressLine2("");
        }
    }

    public abstract String generateAddressLine1(UavPostalValidationResult uavPostalValidationResult);

    public abstract String generateAddressLine2(UavPostalValidationResult uavPostalValidationResult);

    protected abstract String[] generateFullAddress(UavPostalValidationResult uavPostalValidationResult);

    @Override
    public void populateOtherFields(UavPostalValidationResult uavPostalValidationResult) {
        fillCustomAddressRelatedFields(uavPostalValidationResult);
        fillPostBoxField(uavPostalValidationResult);
        fillTimezone(uavPostalValidationResult);
    }

    @Override
    public void fillPostBoxField(UavPostalValidationResult uavPostalValidationResult) {
        uavPostalValidationResult.setIsPostBox(StringUtils.isNotBlank(uavPostalValidationResult.getPostBox()));
    }

    @Override
    public void fillTimezone(UavPostalValidationResult uavPostalValidationResult) {
        uavPostalValidationResult.setTimeZone("");
    }

    @Override
    public void fillCustomAddressRelatedFields(UavPostalValidationResult uavPostalValidationResult) {
        uavPostalValidationResult.setSyntaxValid(Boolean.TRUE);
        uavPostalValidationResult.setIsCustom(Boolean.TRUE);
    }

    @Override
    public void fillStateName(UavPostalValidationResult uavPostalValidationResult) {
        String state = uavPostalValidationResult.getState();
        CountryState countryState = CountryState.find(uavPostalValidationResult.getCountry(), state);
        if (countryState.isFound()) {
            uavPostalValidationResult.setState(countryState.getStateCode());
            uavPostalValidationResult.setStateName(countryState.getStateName());
        }
    }

    protected String getStateCodeOrDefault(String country, String state) {
        CountryState countryState = CountryState.find(country, state);
        return countryState.isFound() ? countryState.getStateCode() : state;
    }

    @Override
    public void fillFullAddress(UavPostalValidationResult uavPostalValidationResult) {
        uavPostalValidationResult.setFullAddress(generateFullAddress(uavPostalValidationResult));
    }

    protected String normalize(String sourceString) {
        if (sourceString == null) return null;
        return sourceString.replaceAll("\\t", " ").replaceAll("\\s{2,}", " ").trim();
    }
}
